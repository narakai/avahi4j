/*
* Copyright (C) 2009 Gilles Gigan (gilles.gigan@gmail.com)
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public  License as published by the
* Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
* or FITNESS FOR A PARTICULAR PURPOSE.
* See the GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <http://www.gnu.org/licenses/>.
*
*/
#include <avahi-common/address.h>
#include <avahi-common/strlst.h>
#include <jni.h>
#include <stdint.h>

#include "common.h"


static void resolver_callback(AvahiServiceResolver *r, AvahiIfIndex interface,
		AvahiProtocol protocol, AvahiResolverEvent event, const char *name,
		const char *type, const char *domain, const char *host_name,
		const AvahiAddress *address, uint16_t port, AvahiStringList *txt,
		AvahiLookupResultFlags flags, void *userdata)  {

	dprint("[LOG] Entering %s\n", __PRETTY_FUNCTION__);

	struct avahi4j_service_resolver *resolver = (struct avahi4j_service_resolver *) userdata;
	char address_str[AVAHI_ADDRESS_STR_MAX];
	jstring jname=NULL, jtype=NULL, jdomain=NULL, jhost=NULL, jaddress=NULL, jcurrent_txt=NULL;
	jobjectArray txt_list=NULL;
	jint jif_idx=0, jproto=0, jevent=0, jflags=0, jaddress_proto=0;
	int index, num_txt_records=avahi_string_list_length(txt);
	JNIEnv *e;
	JavaVM *vm;

	// save a ref to the VM so it can be called after the callback dispatch method
	// returns, since  the avahi4j_record_browser structure may have been freed
	vm = resolver->jvm;

	// attach the jvm to this thread
	(*vm)->AttachCurrentThread(vm, (void **)&e, NULL);

	// check event
	GET_JAVA_RESOLVER_EVT(event, jevent);
	if(event==AVAHI_RESOLVER_FAILURE) {
		jname=NULL;
		jtype=NULL;
		jdomain=NULL;
		jhost=NULL;
		jaddress=NULL;
		jcurrent_txt=NULL;
		txt_list=NULL;
		jif_idx=0;
		jproto=0;
		jevent=0;
		jflags=0;
		jaddress_proto=0;
		num_txt_records=0;
	} else {
		// translate if, protocol, event and address type
		GET_JAVA_IF_IDX(interface, jif_idx);
		GET_JAVA_PROTO(protocol, jproto);
		GET_JAVA_RESOLVER_EVT(event, jevent);
		GET_JAVA_PROTO(address->proto, jaddress_proto);

		// translate lookup result flags
		GET_JAVA_LOOKUP_RES_FLAG(flags, jflags);

		// get java strings
		GET_JSTRING_JUMP(name,jname,e, bail);
		GET_JSTRING_JUMP(type,jtype,e, bail);
		GET_JSTRING_JUMP(domain,jdomain,e, bail);
		GET_JSTRING_JUMP(host_name,jhost,e, bail);
		avahi_address_snprint(address_str, AVAHI_ADDRESS_STR_MAX, address);
		GET_JSTRING_JUMP(address_str, jaddress,e, bail);

		// build txt record array
		txt_list = (*e)->NewObjectArray(e, num_txt_records, resolver->stringClass, NULL);
		if(txt_list==NULL){
			dprint("Error creating txt list\n");
			goto bail;
		}

		for(index=0; index<num_txt_records;index++){
			jcurrent_txt = (*e)->NewStringUTF(e, (char *)avahi_string_list_get_text(txt));

			if(jcurrent_txt!=NULL){
				dprint("Adding txt record '%s' to array\n",avahi_string_list_get_text(txt));
				// add txt record to array
				(*e)->SetObjectArrayElement(e, txt_list, index, jcurrent_txt);
			} else {
				dprint("Error creating jstring form txt record %s\n",
						avahi_string_list_get_text(txt));
			}

			// move on to next one
			txt = avahi_string_list_get_next(txt);
		}
	}

	// call the callback dispatch method
	(*e)->CallVoidMethod(e, resolver->resolverObject,
			resolver->resolverCallbackDispatch, jif_idx, jproto, jevent,
			jname, jtype, jdomain, jhost, jaddress, jaddress_proto, port,
			txt_list, jflags);

bail:
	// detach the jvm
	(*vm)->DetachCurrentThread(vm);
}

JNIEXPORT jlong JNICALL Java_avahi4j_ServiceResolver_init_1resolver(JNIEnv *e,
		jobject t, jlong ptr, jint jif_idx, jint jproto, jstring jname, jstring jtype,
		jstring jdomain, jint jaddressProtocol, jint jlookupFlags){
	dprint("[LOG] Entering %s\n", __PRETTY_FUNCTION__);

	struct avahi4j_client *client = (struct avahi4j_client *) (uintptr_t) ptr;
	struct avahi4j_service_resolver *resolver = NULL;
	jclass ourClass;
	const char *name=NULL, *type=NULL, *domain=NULL;
	int if_idx, proto, address_proto, flags;

	// translate if idx, proto, address_proto and flags
	GET_AVAHI_IF_IDX(if_idx, jif_idx);
	GET_AVAHI_PROTO(proto, jproto);
	GET_AVAHI_PROTO(address_proto, jaddressProtocol);
	GET_AVAHI_LOOKUP_FLAGS(flags, jlookupFlags);

	// create struct service_resolver
	XMALLOC(resolver, struct avahi4j_service_resolver *, sizeof(struct avahi4j_service_resolver));
	if (resolver==NULL) {
		THROW_EXCEPTION(e, JNI_EXCP, "Not enough memory");
		goto bail;
	}

	// save JavaVM ref
	if ((*e)->GetJavaVM(e, &resolver->jvm)!=0){
		THROW_EXCEPTION(e, JNI_EXCP, "Cant get hold of a JavaVM pointer");
		goto bail;
	}

	// cache methodID to java group callback method
	ourClass = (*e)->GetObjectClass(e, t);
	resolver->resolverCallbackDispatch = (*e)->GetMethodID(e, ourClass,
			"dispatchCallback",
			"(IIILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;II[Ljava/lang/String;I)V");
    if (resolver->resolverCallbackDispatch == NULL) {
         THROW_EXCEPTION(e, JNI_EXCP, "Unable to get callback dispatch method ID");
         goto bail;
    }

    // create global ref to our object
    resolver->resolverObject = (*e)->NewGlobalRef(e, t);
    if (resolver->resolverObject==NULL) {
        THROW_EXCEPTION(e, JNI_EXCP, "Unable to create global ref to resolver object");
        goto bail;
    }

    // get ref to string array class
    jclass string_class = (*e)->FindClass(e, "Ljava/lang/String;");
    if (string_class == NULL) {
		THROW_EXCEPTION(e, JNI_EXCP, "Unable to get a string array class reference");
		goto bail;
    }

    // create global ref to the string array class ref
	resolver->stringClass = (*e)->NewGlobalRef(e, string_class);
	if (resolver->stringClass==NULL) {
		THROW_EXCEPTION(e, JNI_EXCP, "Unable to create global ref to string array class");
		goto bail;
	}

	// create utf strings
	GET_UTF_STR_JUMP(name, jname,e, bail);
	GET_UTF_STR_JUMP(type, jtype, e, bail);
	GET_UTF_STR_JUMP(domain, jdomain, e, bail);

	// create avahi resolver
	AVAHI_LOCK(client);
	if (!(resolver->resolver=avahi_service_resolver_new(client->client, if_idx,
			proto, name, type, domain, address_proto, flags, resolver_callback,
			resolver))) {
		AVAHI_UNLOCK(client);
		THROW_EXCEPTION(e, GENERIC_EXCP, "Error creating avahi resolver");
		goto bail;
	}
	AVAHI_UNLOCK(client);

	return (uintptr_t) resolver;

bail:
	PUT_UTF_STR(name, jname, e);
	PUT_UTF_STR(type, jtype, e);
	PUT_UTF_STR(domain, jdomain, e);

	if(resolver->resolverObject)
		(*e)->DeleteGlobalRef(e, resolver->resolverObject);

	if(resolver->stringClass)
			(*e)->DeleteGlobalRef(e, resolver->stringClass);

	// XFREE check for null ptr
	XFREE(resolver);

	return 0;
}

JNIEXPORT jint JNICALL Java_avahi4j_ServiceResolver_release(JNIEnv *e, jobject t, jlong ptr){
	dprint("[LOG] Entering %s\n", __PRETTY_FUNCTION__);

	struct avahi4j_service_resolver *resolver = (struct avahi4j_service_resolver *) (uintptr_t) ptr;
	int result;

	// free avahi resolver
	result = avahi_service_resolver_free(resolver->resolver);

	// delete global ref
	(*e)->DeleteGlobalRef(e, resolver->resolverObject);
	(*e)->DeleteGlobalRef(e, resolver->stringClass);

	// free avahi4j resolver struct
	XFREE(resolver);

	CHECK_N_RET(avahi_service_resolver_free, result);
}

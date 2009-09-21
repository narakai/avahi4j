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
		const AvahiAddress *a, uint16_t port, AvahiStringList *txt,
		AvahiLookupResultFlags flags, void *userdata)  {

	dprint("[LOG] Entering %s\n", __PRETTY_FUNCTION__);
	jstring jname, jtype, jdomain, jhost;
	jobject address;
	jobjectArray txt_list;
	jint jevent, jflags;

	struct avahi4j_service_resolver *resolver = (struct avahi4j_service_resolver *) userdata;
	JNIEnv *e;

	// attach the jvm to this thread
	(*resolver->jvm)->AttachCurrentThread(resolver->jvm, (void **)&e, NULL);

	// call the callback dispatch method
	(*e)->CallVoidMethod(e, resolver->resolverObject,
			resolver->resolverCallbackDispatch, translate_state(state));

	// detach the jvm
	(*resolver->jvm)->DetachCurrentThread(resolver->jvm);
}

JNIEXPORT jlong JNICALL Java_avahi4j_ServiceResolver_init_1resolver(JNIEnv *e,
		jobject t, jlong ptr, jint jif_idx, jint jproto, jstring jname, jstring jtype,
		jstring jdomain, jint jaddressProtocol, jint jlookupFlags){
	dprint("[LOG] Entering %s\n", __PRETTY_FUNCTION__);

	struct avahi4j_client *client = (struct avahi4j_client *) (uintptr_t) ptr;
	struct avahi4j_service_resolver *resolver = NULL;
	jclass ourClass;
	const char *name, *type, *domain;
	int if_idx, proto, address_proto, flags;

	// create struct service_resolver
	XMALLOC(resolver, struct avahi4j_service_resolver *, sizeof(struct avahi4j_service_resolver));
	if (resolver==NULL) {
		THROW_EXCEPTION(e, JNI_EXCP, "Not enough memory");
		return 0;
	}

	// save JavaVM ref
	if ((*e)->GetJavaVM(e, &resolver->jvm)!=0){
		XFREE(resolver);
		THROW_EXCEPTION(e, JNI_EXCP, "Cant get hold of a JavaVM pointer");
		return 0;
	}

	// cache methodID to java group callback method
	ourClass = (*e)->GetObjectClass(e, t);
	resolver->resolverCallbackDispatch = (*e)->GetMethodID(e, ourClass,
			"dispatchCallback",
			"(IIILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lavahi4j/Address;I[Ljava/lang/String;I)V");
    if (resolver->resoolverCallbackDispatch == NULL) {
         XFREE(resolver);
         THROW_EXCEPTION(e, GENERIC_EXCP, "Unable to get callback dispatch method ID");
         return 0;
    }

    // create global ref to our object
    resolver->resolverObject = (*e)->NewGlobalRef(e, t);
    if (resolver->resolverObject==NULL) {
    	XFREE(resolver);
        THROW_EXCEPTION(e, GENERIC_EXCP, "Unable to create global ref to resolver object");
        return 0;
    }

	// create avahi resolver
	AVAHI_LOCK(client);
	if (!(resolver->resolver=avahi_service_resolver_new(client->client, if_idx,
			proto, name, type, domain, address_proto, flags, resolver_callback,
			resolver))) {
		AVAHI_UNLOCK(client);
		(*e)->DeleteGlobalRef(e, resolver->resolverObject);
		XFREE(resolver);
		THROW_EXCEPTION(e, JNI_EXCP, "Error creating avahi resolver");
		return 0;
	}
	AVAHI_UNLOCK(client);

	return (uintptr_t) resolver;
}

JNIEXPORT jint JNICALL Java_avahi4j_ServiceResolver_release(JNIEnv *e, jobject t, jlong ptr){
	dprint("[LOG] Entering %s\n", __PRETTY_FUNCTION__);

	struct avahi4j_service_resolver *resolver = (struct avahi4j_service_resolver *) (uintptr_t) ptr;
	int result;

	// free avahi resolver
	result = avahi_service_resolver_free(resolver->resolver);

	// delete global ref
	(*e)->DeleteGlobalRef(e, resolver->resolverObject);

	// free avahi4j resolver struct
	XFREE(resolver);

	CHECK_N_RET(avahi_service_resolver_free, result);
}

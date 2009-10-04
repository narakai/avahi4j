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
#include <avahi-client/lookup.h>
#include <jni.h>
#include <stdint.h>

#include "common.h"
#include "jni_helpers.h"

static void browser_callback(AvahiRecordBrowser *b, AvahiIfIndex interface,
		AvahiProtocol protocol, AvahiBrowserEvent event, const char *name,
		uint16_t clazz, uint16_t type, const void *rdata, size_t size,
		AvahiLookupResultFlags flags, void *userdata){

	dprint("[LOG] Entering %s\n", __PRETTY_FUNCTION__);

	struct avahi4j_record_browser *browser = (struct avahi4j_record_browser *) userdata;
	JNIEnv *e;
	JavaVM *vm;
	jint jif_idx, jproto, jevent, jclazz, jtype, jflags;
	jstring jname;
	jbyteArray array;

	// save a ref to the VM so it can be called after the callback dispatch method
	// returns, since  the avahi4j_record_browser structure may have been freed
	vm = browser->jvm;

	// attach the jvm to this thread
	(*vm)->AttachCurrentThread(vm, (void **)&e, NULL);

	// check event
	GET_JAVA_BROWSER_EVT(event,  jevent);
	if(event==AVAHI_BROWSER_FAILURE){
		jif_idx=0;
		jproto=0;
		jevent=0;
		jclazz=0;
		jtype=0;
		jflags=0;
		jname=NULL;
		array=NULL;
	} else {
		// translate ints
		GET_JAVA_IF_IDX(interface, jif_idx);
		GET_JAVA_PROTO(protocol, jproto);
		GET_JAVA_BROWSER_EVT(event,  jevent);
		A2J_DNS_CLASS(clazz, jclazz);
		A2J_DNS_RR_TYPE(type, jtype);
		GET_JAVA_LOOKUP_RES_FLAG(flags, jflags);

		// create jstring from name
		GET_JSTRING_JUMP(name, jname, e, bail);

		// create byte array
		array = (*e)->NewByteArray(e, size);
		if(array==NULL) {
			dprint("Unable to create a byte array\n");
			goto bail;
		}

		// copy bytes
		(*e)->SetByteArrayRegion(e, array, 0, size, rdata);
	}

	// call callback dispatch method
	(*e)->CallVoidMethod(e, browser->browserObject, browser->browserCallbackDispatch,
			jif_idx, jproto, jevent, jname, jclazz, jtype, array, jflags);

bail:
	// detach the jvm
	(*vm)->DetachCurrentThread(vm);
}


JNIEXPORT jlong JNICALL Java_avahi4j_RecordBrowser_initBrowser(JNIEnv *e,
		jobject t, jlong ptr, jint jif_idx, jint jproto, jstring jname,
		jint jclazz, jint jtype, jint jflags){

	dprint("[LOG] Entering %s\n", __PRETTY_FUNCTION__);

	struct avahi4j_client *client = (struct avahi4j_client *) (uintptr_t) ptr;
	struct avahi4j_record_browser *browser = NULL;
	jclass ourClass;
	const char *name=NULL;
	int if_idx, proto, clazz, type, flags;

	// get utf string
	GET_UTF_STR_JUMP(name, jname, e, bail);

	// tranlsate ints
	GET_AVAHI_IF_IDX(if_idx,jif_idx);
	GET_AVAHI_PROTO(proto, jproto);
	J2A_DNS_CLASS(clazz, jclazz);
	J2A_DNS_RR_TYPE(type, jtype);
	GET_AVAHI_LOOKUP_FLAGS(flags, jflags);


	// create struct record browser
	XMALLOC(browser, struct avahi4j_record_browser *, sizeof(struct avahi4j_record_browser));
	if (browser==NULL) {
		THROW_EXCEPTION(e, JNI_EXCP, "Not enough memory");
		goto bail;
	}

	// save JavaVM ref
	if ((*e)->GetJavaVM(e, &browser->jvm)!=0){
		THROW_EXCEPTION(e, JNI_EXCP, "Cant get hold of a JavaVM pointer");
		goto bail;
	}

	// cache methodID to java group callback method
	ourClass = (*e)->GetObjectClass(e, t);
	browser->browserCallbackDispatch = (*e)->GetMethodID(e, ourClass,
			"dispatchCallback",	"(IIILjava/lang/String;II[BI)V");
    if (browser->browserCallbackDispatch == NULL) {
		THROW_EXCEPTION(e, JNI_EXCP, "Unable to get callback dispatch method ID");
		goto bail;
    }

    // create global ref to our object
    browser->browserObject = (*e)->NewGlobalRef(e, t);
    if (browser->browserObject==NULL) {
        THROW_EXCEPTION(e, JNI_EXCP, "Unable to create global ref to browser object");
        goto bail;
    }

	// create browser
	AVAHI_LOCK(client);
	if (!(browser->browser=avahi_record_browser_new(client->client, if_idx,
			proto, name, clazz, type, flags, browser_callback, browser))) {
		AVAHI_UNLOCK(client);
		THROW_EXCEPTION(e, GENERIC_EXCP, "Error creating avahi record browser");
		goto bail;
	}
	AVAHI_UNLOCK(client);

	// free utf string
	PUT_UTF_STR(name, jname, e);

	return (uintptr_t) browser;


bail:
	if(browser->browserObject)
		(*e)->DeleteGlobalRef(e, browser->browserObject);

	XFREE(browser);

	PUT_UTF_STR(name, jname, e);

	return 0;
}


JNIEXPORT jlong JNICALL Java_avahi4j_RecordBrowser_releaseBrowser(JNIEnv *e,
		jobject t, jlong ptr){

	dprint("[LOG] Entering %s\n", __PRETTY_FUNCTION__);
	struct avahi4j_record_browser *browser = (struct avahi4j_record_browser *) (uintptr_t) ptr;
	int result;

	result = avahi_record_browser_free(browser->browser);

	if(browser->browserObject)
		(*e)->DeleteGlobalRef(e, browser->browserObject);

	XFREE(browser);

	CHECK_N_RET(avahi_record_browser_free, result);
}


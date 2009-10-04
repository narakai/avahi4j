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

#include "common.h"

static void browser_callback(AvahiServiceBrowser *b, AvahiIfIndex if_idx,
		AvahiProtocol protocol, AvahiBrowserEvent event, const char *name,
		const char *type, const char *domain, AvahiLookupResultFlags flags,
		void *userdata) {
	dprint("[LOG] Entering %s\n", __PRETTY_FUNCTION__);

	struct avahi4j_service_browser *browser = (struct avahi4j_service_browser *) userdata;
	JNIEnv *e;
	jstring jname, jtype, jdomain;
	jint jproto, jif_idx, jflags, jevent;
	JavaVM *vm;

	// save a ref to the VM so it can be called after the callback dispatch method
	// returns, since  the avahi4j_record_browser structure may have been freed
	vm = browser->jvm;

	// attach the jvm to this thread
	(*vm)->AttachCurrentThread(vm, (void **)&e, NULL);

	// check event
	GET_JAVA_BROWSER_EVT(event, jevent);
	if(event==AVAHI_BROWSER_FAILURE){
		jname=NULL;
		jtype=NULL;
		jdomain=NULL;
		jproto=0;
		jif_idx=0;
		jflags=0;
	} else {
		jname = (*e)->NewStringUTF(e, name);
		jtype = (*e)->NewStringUTF(e, type);
		jdomain = (*e)->NewStringUTF(e, domain);
		GET_JAVA_PROTO(protocol, jproto);
		GET_JAVA_IF_IDX(if_idx,jif_idx);
		GET_JAVA_LOOKUP_RES_FLAG(flags, jflags);
		GET_JAVA_BROWSER_EVT(event, jevent);
	}

	// call the callback dispatch method
	(*e)->CallVoidMethod(e, browser->browserObject,
			browser->browserCallbackDispatch, jif_idx, jproto, jevent, jname,
			jtype, jdomain, jflags);

	// detach the jvm
	(*vm)->DetachCurrentThread(vm);
}

JNIEXPORT jlong JNICALL Java_avahi4j_ServiceBrowser_initBrowser(JNIEnv *e, jobject t,
		jlong ptr, jint jif_idx, jint jproto, jstring jtype, jstring jdomain,
		jint jflags) {

	dprint("[LOG] Entering %s\n", __PRETTY_FUNCTION__);

	struct avahi4j_client *client = (struct avahi4j_client *) (uintptr_t) ptr;
	struct avahi4j_service_browser *browser = NULL;
	jclass ourClass;
	int if_idx;
	AvahiProtocol proto;
	AvahiLookupFlags flags;
	const char *type, *domain;

	GET_UTF_STR(type, jtype, e, 0);
	GET_UTF_STR(domain, jdomain, e, 0);
	GET_AVAHI_LOOKUP_FLAGS(flags, jflags);
	GET_AVAHI_PROTO(proto, jproto);
	GET_AVAHI_IF_IDX(if_idx, jif_idx);


	// create struct service browser
	XMALLOC(browser, struct avahi4j_service_browser *, sizeof(struct avahi4j_service_browser));
	if (browser==NULL) {
		PUT_UTF_STR(type, jtype,e);
		PUT_UTF_STR(domain, jdomain, e);
		THROW_EXCEPTION(e, JNI_EXCP, "Not enough memory");
		return 0;
	}

	// save JavaVM ref
	if ((*e)->GetJavaVM(e, &browser->jvm)!=0){
		XFREE(browser);
		PUT_UTF_STR(type, jtype,e);
		PUT_UTF_STR(domain, jdomain, e);
		THROW_EXCEPTION(e, JNI_EXCP, "Cant get hold of a JavaVM pointer");
		return 0;
	}

	// cache methodID to java group callback method
	ourClass = (*e)->GetObjectClass(e, t);
	browser->browserCallbackDispatch = (*e)->GetMethodID(e, ourClass, "browserCallback",
			"(IIILjava/lang/String;Ljava/lang/String;Ljava/lang/String;I)V");
    if (browser->browserCallbackDispatch == NULL) {
		XFREE(browser);
		PUT_UTF_STR(type, jtype,e);
		PUT_UTF_STR(domain, jdomain, e);
		THROW_EXCEPTION(e, GENERIC_EXCP, "Unable to get callback dispatch method ID");
		return 0;
    }

    // create global ref to our object
    browser->browserObject = (*e)->NewGlobalRef(e, t);
    if (browser->browserObject==NULL) {
    	XFREE(browser);
    	PUT_UTF_STR(type, jtype,e);
    	PUT_UTF_STR(domain, jdomain, e);
        THROW_EXCEPTION(e, GENERIC_EXCP, "Unable to create global ref to browser object");
        return 0;
    }

	// create browser
	AVAHI_LOCK(client);
	if (!(browser->browser=avahi_service_browser_new(client->client, if_idx,
			proto,type, domain, flags, browser_callback, browser))) {
		AVAHI_UNLOCK(client);
		(*e)->DeleteGlobalRef(e, browser->browserObject);
		XFREE(browser);
		PUT_UTF_STR(type, jtype,e);
		PUT_UTF_STR(domain, jdomain, e);
		THROW_EXCEPTION(e, JNI_EXCP, "Error creating avahi service browser");
		return 0;
	}
	AVAHI_UNLOCK(client);

	PUT_UTF_STR(type, jtype,e);
	PUT_UTF_STR(domain, jdomain, e);

	return (uintptr_t) browser;
}

JNIEXPORT jlong JNICALL Java_avahi4j_ServiceBrowser_release(JNIEnv *e, jobject t, jlong ptr) {
	dprint("[LOG] Entering %s\n", __PRETTY_FUNCTION__);

	struct avahi4j_service_browser *browser = (struct avahi4j_service_browser *) (uintptr_t) ptr;
	int result;

	result = avahi_service_browser_free(browser->browser);

	// delete global ref
	(*e)->DeleteGlobalRef(e, browser->browserObject);

	// free browser struct
	XFREE(browser);

	CHECK_N_RET(avahi_service_browser_free, result);
}



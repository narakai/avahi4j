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
#include <avahi-common/error.h>
#include <avahi-common/malloc.h>
// use fixed local copy of thread-watch since Avahi's provided one has a bug
// that was fixed only since 0.6.24...
#include "thread-watch.h"
#include <jni.h>
#include <stdint.h>
#include <stdio.h>

#include "common.h"
#include "version.h"

JNIEXPORT jstring JNICALL Java_avahi4j_Client_getVersion(JNIEnv *e, jobject t) {
	char version[16] = {0};
	snprintf(version, 16, "%d.%d-%s", VER_MAJ, VER_MIN, VER_REV);
	return (*e)->NewStringUTF(e,version);
}

static int translate_state(AvahiClientState state) {

	// translate avahi state
	switch(state)
	{
	case AVAHI_CLIENT_S_REGISTERING:
		return 0;
	case AVAHI_CLIENT_S_RUNNING:
		return 1;
	case AVAHI_CLIENT_S_COLLISION:
		return 2;
	case AVAHI_CLIENT_FAILURE:
		return 3;
	case AVAHI_CLIENT_CONNECTING:
		return 4;
	default:
		info("Error translating avahi client state %d\n", state);
		return AVAHI_CLIENT_FAILURE;
	};
}

// Avahi client callback
static void avahiClientCallback(AvahiClient *s, AvahiClientState state, void *userdata){
	dprint("[LOG] Entering %s\n", __PRETTY_FUNCTION__);

	struct avahi4j_client *client = (struct avahi4j_client *) userdata;
	JNIEnv *e;

	// attach the jvm to this thread
	(*client->jvm)->AttachCurrentThread(client->jvm, (void **)&e, NULL);

	// call the callback dispatch method
	(*e)->CallVoidMethod(e, client->clientObject,
			client->clientCallbackDispatch, translate_state(state));

	// detach the jvm
	(*client->jvm)->DetachCurrentThread(client->jvm);
}

JNIEXPORT jlong JNICALL Java_avahi4j_Client_initClient(JNIEnv *e, jobject t){
	dprint("[LOG] Entering %s\n", __PRETTY_FUNCTION__);

	struct avahi4j_client *client = NULL;
	int error;
	jclass ourClass;

	// allocate avahi4j_client struct
	XMALLOC(client, struct avahi4j_client*, sizeof(struct avahi4j_client) );
	if (client==NULL){
		THROW_EXCEPTION(e, JNI_EXCP, "Not enough memory");
		return 0;
	}

	// save JavaVM ref
	if ((*e)->GetJavaVM(e, &client->jvm)!=0){
		XFREE(client);
		THROW_EXCEPTION(e, JNI_EXCP, "Cant get hold of a JavaVM pointer");
		return 0;
	}


	// cache methodID to java client callback method
	ourClass = (*e)->GetObjectClass(e, t);
	client->clientCallbackDispatch = (*e)->GetMethodID(e, ourClass, "dispatchCallback", "(I)V");
    if (client->clientCallbackDispatch == NULL) {
         XFREE(client);
         THROW_EXCEPTION(e, GENERIC_EXCP, "Unable to get callback dispatch method ID");
         return 0;
    }

    // create global ref to our object
    client->clientObject = (*e)->NewGlobalRef(e, t);
    if (client->clientObject==NULL) {
    	XFREE(client);
        THROW_EXCEPTION(e, GENERIC_EXCP, "Unable to create global ref to client object");
        return 0;
    }

	// Allocate main loop object
    dprint("Creating poll loop\n");
	if (!(client->pollLoop = avahi_threaded_poll_new())) {
		(*e)->DeleteGlobalRef(e, client->clientObject);
		XFREE(client);
		THROW_EXCEPTION(e, GENERIC_EXCP, "Error creating poll loop");
		return 0;
	}

	// create new avahi client
	dprint("Creating client\n");
	client->client = avahi_client_new(
			avahi_threaded_poll_get(client->pollLoop), 0, avahiClientCallback,
			client, &error);

	// Check whether creating the client object succeeded
	if (!client->client) {
		dprint("Failed to create client: %s\n", avahi_strerror(error));
		avahi_threaded_poll_free(client->pollLoop);
		(*e)->DeleteGlobalRef(e, client->clientObject);
		XFREE(client);
		THROW_EXCEPTION(e, GENERIC_EXCP, "Error creating client: %s",
				avahi_strerror(error));
		return 0;
	}

	return (uintptr_t) client;
}

JNIEXPORT jstring JNICALL Java_avahi4j_Client_get_1host_1name(JNIEnv *e, jobject t,
		jlong ptr){
	dprint("[LOG] Entering %s\n", __PRETTY_FUNCTION__);

	struct avahi4j_client *client = (struct avahi4j_client *) (uintptr_t) ptr;

	// get hostname
	AVAHI_LOCK(client);
	const char *utf_name = avahi_client_get_host_name(client->client);
	AVAHI_UNLOCK(client);

	return (*e)->NewStringUTF(e, utf_name);
}

JNIEXPORT jint JNICALL Java_avahi4j_Client_set_1host_1name(JNIEnv *e, jobject t,
		jlong ptr, jstring name){
	dprint("[LOG] Entering %s\n", __PRETTY_FUNCTION__);

	int result;
	struct avahi4j_client *client = (struct avahi4j_client *) (uintptr_t) ptr;
	const char *utf_name;

	// get string
	GET_UTF_STR(utf_name, name, e, -1);

	// set hostname
	AVAHI_LOCK(client);
	result = avahi_client_set_host_name(client->client, utf_name);
	AVAHI_UNLOCK(client);

	// release string
	PUT_UTF_STR(utf_name, name, e);

	CHECK_N_RET(avahi_client_set_host_name, result);
}

JNIEXPORT jstring JNICALL Java_avahi4j_Client_get_1domain_1name(JNIEnv *e, jobject t,
		jlong ptr){
	dprint("[LOG] Entering %s\n", __PRETTY_FUNCTION__);

	struct avahi4j_client *client = (struct avahi4j_client *) (uintptr_t) ptr;

	// get domain name
	AVAHI_LOCK(client);
	const char *utf_name = avahi_client_get_domain_name(client->client);
	AVAHI_UNLOCK(client);

	return (*e)->NewStringUTF(e, utf_name);
}

JNIEXPORT jstring JNICALL Java_avahi4j_Client_get_1fqdn(JNIEnv *e, jobject t,
		jlong ptr){
	dprint("[LOG] Entering %s\n", __PRETTY_FUNCTION__);

	struct avahi4j_client *client = (struct avahi4j_client *) (uintptr_t) ptr;

	// get fqdn
	AVAHI_LOCK(client);
	const char *utf_name = avahi_client_get_host_name_fqdn(client->client);
	AVAHI_UNLOCK(client);

	return (*e)->NewStringUTF(e, utf_name);
}

JNIEXPORT jint JNICALL Java_avahi4j_Client_get_1state(JNIEnv *e, jobject t,
		jlong ptr){
	dprint("[LOG] Entering %s\n", __PRETTY_FUNCTION__);

	struct avahi4j_client *client = (struct avahi4j_client *) (uintptr_t) ptr;
	int state;

	AVAHI_LOCK(client);
	state = avahi_client_get_state(client->client);
	AVAHI_UNLOCK(client);

	return translate_state(state);
}

JNIEXPORT void JNICALL Java_avahi4j_Client_releaseClient(JNIEnv *e, jobject t, jlong ptr){
	dprint("[LOG] Entering %s\n", __PRETTY_FUNCTION__);
	struct avahi4j_client *client = (struct avahi4j_client *) (uintptr_t) ptr;

	// free avahi client
	avahi_client_free(client->client);

	// free main loop object
	avahi_threaded_poll_free(client->pollLoop);

	// delete global ref to client object
	(*e)->DeleteGlobalRef(e, client->clientObject);

	// free avahi4j struct
	XFREE(client);
}

JNIEXPORT jint JNICALL Java_avahi4j_Client_startLoop(JNIEnv *e, jobject t, jlong ptr){
	dprint("[LOG] Entering %s\n", __PRETTY_FUNCTION__);
	struct avahi4j_client *client = (struct avahi4j_client *) (uintptr_t) ptr;

	// start poll loop
	int result = avahi_threaded_poll_start(client->pollLoop);
	CHECK_N_RET(avahi_threaded_poll_start, result);
}

JNIEXPORT jint JNICALL Java_avahi4j_Client_stopLoop(JNIEnv *e, jobject t, jlong ptr){
	dprint("[LOG] Entering %s\n", __PRETTY_FUNCTION__);
	struct avahi4j_client *client = (struct avahi4j_client *) (uintptr_t) ptr;

	// stop poll loop
	int result = avahi_threaded_poll_stop(client->pollLoop);
	CHECK_N_RET(avahi_threaded_poll_stop, result);
}

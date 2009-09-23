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
#include <avahi-common/alternative.h>
#include <avahi-common/malloc.h>
#include <avahi-common/strlst.h>
#include <jni.h>
#include <stdint.h>

#include "common.h"

static int translate_state(AvahiEntryGroupState state){
	switch(state){
	case AVAHI_ENTRY_GROUP_UNCOMMITED:
		return 0;
	case AVAHI_ENTRY_GROUP_REGISTERING:
		return 1;
	case AVAHI_ENTRY_GROUP_ESTABLISHED:
		return 2;
	case AVAHI_ENTRY_GROUP_COLLISION:
		return 3;
	case AVAHI_ENTRY_GROUP_FAILURE:
		return 4;
	default:
		info("Error translating group state %d\n",state);
		return 4;
	};
}

static void group_callback(AvahiEntryGroup *g, AvahiEntryGroupState state, void *userdata) {
	dprint("[LOG] Entering %s\n", __PRETTY_FUNCTION__);

	struct avahi4j_entry_group *group = (struct avahi4j_entry_group *) userdata;
	JNIEnv *e;

	// attach the jvm to this thread
	(*group->jvm)->AttachCurrentThread(group->jvm, (void **)&e, NULL);

	// call the callback dispatch method
	(*e)->CallVoidMethod(e, group->groupObject,
			group->groupCallbackDispatch, translate_state(state));

	// detach the jvm
	(*group->jvm)->DetachCurrentThread(group->jvm);
}

JNIEXPORT jlong JNICALL Java_avahi4j_EntryGroup_initGroup(JNIEnv *e, jobject t, jlong ptr){
	dprint("[LOG] Entering %s\n", __PRETTY_FUNCTION__);

	struct avahi4j_client *client = (struct avahi4j_client *) (uintptr_t) ptr;
	struct avahi4j_entry_group *group = NULL;
	jclass ourClass;

	// create struct group entry
	XMALLOC(group, struct avahi4j_entry_group *, sizeof(struct avahi4j_entry_group));
	if (group==NULL) {
		THROW_EXCEPTION(e, JNI_EXCP, "Not enough memory");
		return 0;
	}

	// save JavaVM ref
	if ((*e)->GetJavaVM(e, &group->jvm)!=0){
		XFREE(group);
		THROW_EXCEPTION(e, JNI_EXCP, "Cant get hold of a JavaVM pointer");
		return 0;
	}

	// cache methodID to java group callback method
	ourClass = (*e)->GetObjectClass(e, t);
	group->groupCallbackDispatch = (*e)->GetMethodID(e, ourClass, "dispatchCallback", "(I)V");
    if (group->groupCallbackDispatch == NULL) {
         XFREE(group);
         THROW_EXCEPTION(e, GENERIC_EXCP, "Unable to get callback dispatch method ID");
         return 0;
    }

    // create global ref to our object
    group->groupObject = (*e)->NewGlobalRef(e, t);
    if (group->groupObject==NULL) {
    	XFREE(group);
        THROW_EXCEPTION(e, GENERIC_EXCP, "Unable to create global ref to group object");
        return 0;
    }

	// create avahi group
	AVAHI_LOCK(client);
	if (!(group->group=avahi_entry_group_new(client->client, group_callback, group))) {
		AVAHI_UNLOCK(client);
		(*e)->DeleteGlobalRef(e, group->groupObject);
		XFREE(group);
		THROW_EXCEPTION(e, JNI_EXCP, "Error creating avahi group");
		return 0;
	}
	AVAHI_UNLOCK(client);

	return (uintptr_t) group;
}

JNIEXPORT jint JNICALL Java_avahi4j_EntryGroup_release(JNIEnv *e, jobject t, jlong ptr){
	dprint("[LOG] Entering %s\n", __PRETTY_FUNCTION__);

	struct avahi4j_entry_group *group = (struct avahi4j_entry_group *) (uintptr_t) ptr;
	int result;

	// free avahi group
	result = avahi_entry_group_free(group->group);

	// delete global ref
	(*e)->DeleteGlobalRef(e, group->groupObject);

	// free avahi4j group struct
	XFREE(group);

	CHECK_N_RET(avahi_entry_group_release, result);
}

JNIEXPORT jint JNICALL Java_avahi4j_EntryGroup_commit_1group(JNIEnv *e, jobject t, jlong ptr){
	dprint("[LOG] Entering %s\n", __PRETTY_FUNCTION__);

	struct avahi4j_entry_group *group = (struct avahi4j_entry_group *) (uintptr_t) ptr;
	int result;

	result = avahi_entry_group_commit(group->group);

	CHECK_N_RET(avahi_entry_group_commit, result);
}

JNIEXPORT jint JNICALL Java_avahi4j_EntryGroup_get_1state(JNIEnv *e, jobject t, jlong ptr){
	dprint("[LOG] Entering %s\n", __PRETTY_FUNCTION__);

	struct avahi4j_entry_group *group = (struct avahi4j_entry_group *) (uintptr_t) ptr;

	return translate_state(avahi_entry_group_get_state(group->group));
}

JNIEXPORT jint JNICALL Java_avahi4j_EntryGroup_reset_1group(JNIEnv *e, jobject t, jlong ptr){
	dprint("[LOG] Entering %s\n", __PRETTY_FUNCTION__);

	struct avahi4j_entry_group *group = (struct avahi4j_entry_group *) (uintptr_t) ptr;

	int result = avahi_entry_group_reset(group->group);

	CHECK_N_RET(avahi_entry_group_reset, result);
}

JNIEXPORT jint JNICALL Java_avahi4j_EntryGroup_is_1group_1empty(JNIEnv *e, jobject t, jlong ptr){
	dprint("[LOG] Entering %s\n", __PRETTY_FUNCTION__);

	struct avahi4j_entry_group *group = (struct avahi4j_entry_group *) (uintptr_t) ptr;

	int result = avahi_entry_group_is_empty(group->group);

	CHECK_N_RET(avahi_entry_group_is_empty, result);
}

JNIEXPORT jint JNICALL Java_avahi4j_EntryGroup_add_1service_1txt_1records
			(JNIEnv *e, jobject t, jlong ptr, jint interfaceNum, jint proto,
					jstring name, jstring type, jstring domain, jstring host,
					jint port, jarray txtRecord, jint length){
	dprint("[LOG] Entering %s\n", __PRETTY_FUNCTION__);

	struct avahi4j_entry_group *group = (struct avahi4j_entry_group *) (uintptr_t) ptr;

	AvahiIfIndex avahi_if;
	AvahiProtocol avahi_proto;
	const char *avahi_name=NULL, *avahi_type=NULL, *avahi_domain=NULL, *avahi_host=NULL;
	AvahiStringList *list = NULL;
	jstring current;
	const char* record;
	int result, i;
	uint16_t avahi_port = (uint16_t) port;

	// translate  interface num
	GET_AVAHI_IF_IDX(avahi_if, interfaceNum);

	// translate the Protocol Enum to an AvahiProtocol
	GET_AVAHI_PROTO(avahi_proto, proto);

	// go through the list of TXT  record and add them to the AvahiStringList
	for(i=0; i<length; i++) {
		current = (jstring) (*e)->GetObjectArrayElement(e, txtRecord, i);
		if(current==NULL)
		{
			dprint("error getting txt record %d\n",i);
			THROW_EXCEPTION(e, JNI_EXCP, "error getting record %d",i);
			return 0;
		}

		GET_UTF_STR(record, current, e, 0);
		dprint("Adding '%s' to TXT record\n", record);
		list = avahi_string_list_add(list, record);
		PUT_UTF_STR(record, current, e);
	}

	// get UTF string from name, type, domain (can be NULL) and host (can be NULL)
	GET_UTF_STR_JUMP(avahi_name, name, e, bail);
	GET_UTF_STR_JUMP(avahi_type, type,e ,bail);
	GET_UTF_STR_JUMP(avahi_domain, domain, e, bail);
	GET_UTF_STR_JUMP(avahi_host, host,e ,bail);

	result = avahi_entry_group_add_service_strlst(group->group, avahi_if,
			avahi_proto, 0, avahi_name, avahi_type, avahi_domain, avahi_host,
			avahi_port, list);

bail:
	// free UTF string
	PUT_UTF_STR(avahi_name, name, e);
	PUT_UTF_STR(avahi_type, type,e);
	PUT_UTF_STR(avahi_domain, domain, e);
	PUT_UTF_STR(avahi_host, host,e);

	// free string list
	avahi_string_list_free(list);

	CHECK_N_RET(avahi_entry_group_add_service_strlst, result);
}

JNIEXPORT jint JNICALL Java_avahi4j_EntryGroup_add_1service_1subtype(
			JNIEnv *e, jobject t, jlong ptr, int interfaceNum, jint proto,
			jstring name, jstring type, jstring domain, jstring subtype) {
	dprint("[LOG] Entering %s\n", __PRETTY_FUNCTION__);

	struct avahi4j_entry_group *group = (struct avahi4j_entry_group *) (uintptr_t) ptr;
	AvahiIfIndex avahi_if;
	AvahiProtocol avahi_proto;
	const char *avahi_name=NULL, *avahi_type=NULL, *avahi_domain=NULL, *avahi_subtype=NULL;
	int result;

	// translate if index and protocol
	GET_AVAHI_IF_IDX(avahi_if, interfaceNum);
	GET_AVAHI_PROTO(avahi_proto, proto);

	// get UTF strings (domain may be null)
	GET_UTF_STR_JUMP(avahi_name, name, e, bail);
	GET_UTF_STR_JUMP(avahi_type, type, e, bail);
	GET_UTF_STR_JUMP(avahi_domain, domain, e, bail);
	GET_UTF_STR_JUMP(avahi_subtype, subtype, e, bail);

	result = avahi_entry_group_add_service_subtype(group->group, avahi_if,
			avahi_proto, 0, avahi_name, avahi_type, avahi_domain, avahi_subtype);

bail:

	// release UTF string
	PUT_UTF_STR(avahi_name, name, e);
	PUT_UTF_STR(avahi_type, type, e);
	PUT_UTF_STR(avahi_domain, domain, e);
	PUT_UTF_STR(avahi_subtype, subtype, e);


	CHECK_N_RET(avahi_entry_group_add_service_subtype, result);
}

JNIEXPORT jint JNICALL Java_avahi4j_EntryGroup_update_1service_1txt_1records
			(JNIEnv *e, jobject t, jlong ptr, jint interfaceNum, jint proto,
					jstring name, jstring type, jstring domain, jarray txtRecord,
					jint length){
	dprint("[LOG] Entering %s\n", __PRETTY_FUNCTION__);

	struct avahi4j_entry_group *group = (struct avahi4j_entry_group *) (uintptr_t) ptr;

	AvahiIfIndex avahi_if;
	AvahiProtocol avahi_proto;
	const char *avahi_name=NULL, *avahi_type=NULL, *avahi_domain=NULL;
	AvahiStringList *list = NULL;
	jstring current;
	const char* record;
	int result, i;

	// translate the Protocol Enum and interface index
	GET_AVAHI_PROTO(avahi_proto, proto);
	GET_AVAHI_IF_IDX(avahi_if, interfaceNum);

	// go through the list of TXT  record and add them to the AvahiStringList
	for(i=0; i<length; i++) {
		current = (jstring) (*e)->GetObjectArrayElement(e, txtRecord, i);
		if(current==NULL)
		{
			dprint("error getting txt record %d\n",i);
			THROW_EXCEPTION(e, JNI_EXCP, "error getting record %d",i);
			return 0;
		}

		GET_UTF_STR(record, current, e, 0);
		dprint("Adding '%s' to TXT record\n", record);
		list = avahi_string_list_add(list, record);
		PUT_UTF_STR(record, current, e);
	}

	// get UTF string from name, type, domain (can be NULL)
	GET_UTF_STR_JUMP(avahi_name, name, e, bail);
	GET_UTF_STR_JUMP(avahi_type, type,e ,bail);
	GET_UTF_STR_JUMP(avahi_domain, domain, e, bail);

	result = avahi_entry_group_update_service_txt_strlst(group->group, avahi_if,
			avahi_proto, 0, avahi_name, avahi_type, avahi_domain, list);

bail:
	// free UTF string
	PUT_UTF_STR(avahi_name, name, e);
	PUT_UTF_STR(avahi_type, type,e);
	PUT_UTF_STR(avahi_domain, domain, e);

	// free string list
	avahi_string_list_free(list);

	CHECK_N_RET(avahi_entry_group_update_service_txt_strlst, result);
}

JNIEXPORT jstring JNICALL Java_avahi4j_EntryGroup_find_1alternative_1serivce_1name
			(JNIEnv *e, jobject t, jstring collidingName){
	dprint("[LOG] Entering %s\n", __PRETTY_FUNCTION__);

	const char *utf_name;
	char *fixed_utf_name;
	jstring fixed_name;

	// get utf colliding name
	GET_UTF_STR(utf_name, collidingName, e, 0);

	// get fixed name
	fixed_utf_name = avahi_alternative_service_name(utf_name);

	// release utf colliding name
	PUT_UTF_STR(utf_name, collidingName, e);

	// create java String
	fixed_name = (*e)->NewStringUTF(e, fixed_utf_name);

	// release utf name
	avahi_free(fixed_utf_name);

	return fixed_name;
}



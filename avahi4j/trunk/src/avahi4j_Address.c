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
#include <jni.h>
#include <stdint.h>

#include "common.h"


static jfieldID	address_field = NULL;


JNIEXPORT jint JNICALL Java_avahi4j_Address(JNIEnv *e, jobject t, jlong ptr){
	AvahiAddress *address = (AvahiAddress *) (uintptr_t) ptr;

	jstring jaddress;
	jint protocol;

	// get the field IDs for both the address and protocol fields
	if (address_field==NULL){
		jclass cls = (*e)->GetObjectClass(e, t);
		address_field = (*e)->GetFieldID(e, cls, "address", "Ljava/lang/String;");
		if(address_field==NULL){
			THROW_EXCEPTION(e, JNI_EXCP, "Unable to find the address field");
			return 0;
		}
	}

	// set address member
	if(address->proto==AVAHI_PROTO_INET){
		char nice_address[AVAHI_ADDRESS_STR_MAX];
		avahi_address_snprint(nice_address, AVAHI_ADDRESS_STR_MAX, address);
		jaddress = (*e)->NewStringUTF(e,nice_address);
		(*e)->SetObjectField(e, t, address_field, jaddress);
	} else if(address->proto==AVAHI_PROTO_INET6){
		char nice_address[AVAHI_ADDRESS_STR_MAX];
		avahi_address_snprint(nice_address, AVAHI_ADDRESS_STR_MAX, address);
		jaddress = (*e)->NewStringUTF(e,nice_address);
		(*e)->SetObjectField(e, t, address_field, jaddress);
	}

	GET_JAVA_PROTO(address->proto, protocol);

	return protocol;
}

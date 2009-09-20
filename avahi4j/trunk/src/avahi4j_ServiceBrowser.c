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

#include <common.h>

static void browser_callback(AvahiEntryServiceBrowser *b, AvahiIfIndex if_idx,
		AvahiProtocol protocol, AvahiBrowserEvent event, const char *name,
		const char *type, const char *domain, AvahiLookupResultFlags flags,
		void *userdata) {
	dprint("[LOG] Entering %s\n", __PRETTY_FUNCTION__);

	struct avahi4j_service_browser *browser = (struct avahi4j_service_browser *) userdata;
	JNIEnv *e;
	jstring jname, jtype, jdomain;
	jint jproto, jif_idx, jflags;

	// attach the jvm to this thread
	(*group->jvm)->AttachCurrentThread(group->jvm, (void **)&e, NULL);

	jname = (*e)->NewStringUTF(e, name);
	jtype = (*e)->NewStringUTF(e, type);
	jdomain = (*e)->NewStringUTF(e, domain);
	GET_JAVA_PROTO(protocol, jproto);
	jif_idx = GET_JAVA_IF_IDX(if_idx);
	GET_JAVA_LOOKUP_RES_FLAG(flags, jflags);

	// call the callback dispatch method
	(*e)->CallVoidMethod(e, browser->browserObject,
			browser->browserCallbackDispatch, jif_idx, jproto, jname, jtype,
			jdomain, jflags);

	// detach the jvm
	(*group->jvm)->DetachCurrentThread(group->jvm);
}


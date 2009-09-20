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


#ifndef COMMON_H_
#define COMMON_H_

#include <string.h>
#include <stdlib.h>
#include <avahi-client/client.h>
#include <avahi-common/error.h>
#include <avahi-common/malloc.h>
#include <avahi-common/simple-watch.h>
#include <avahi-client/publish.h>
// use fixed local copy of thread-watch since Avahi's provided one has a bug
// that was fixed only since 0.6.24...
#include "thread-watch.h"

#define CLEAR(x) do {memset(&x, 0x0, sizeof(x));}while(0)
#define info(format, ...) do {\
	fprintf(stderr, "[%s:%d] " format, __FILE__, __LINE__, ##__VA_ARGS__);\
	fflush(stderr);\
	} while(0)


#ifdef DEBUG
#define dprint(format, ...) do {\
		fprintf (stderr, "[%s:%d] " format, __FILE__, __LINE__, ##__VA_ARGS__); fflush(stderr); } while(0)

#define XMALLOC(var, type, size)	\
		do { \
			var = (type) malloc((size)); \
			if (!var) {dprint("[MEMALLOC]: Cant allocate %lu bytes.\n", (long unsigned int) (size));} \
			else { CLEAR(*var); \
				dprint("[MEMALLOC]: allocated %lu bytes of type %s for var %s (%p).\n", (long unsigned int)size, #type, #var, (var));}\
		} while (0)

#define XFREE(var)					\
		do { dprint("[MEMALLOC]: freeing memory for var %s (%p).\n", #var, var);\
			if (var) { free(var); } \
			else { dprint("[MEMALLOC]: Trying to free a NULL pointer.\n");}\
		} while (0)

#else
#define dprint(source, ...)

#define XMALLOC(var, type, size)	\
		do { \
			var = (type) malloc(size); \
			if (!var) {fprintf(stderr,"[%s:%d %s] MEMALLOC: OUT OF MEMORY !!! Cant allocate %lu bytes.\n", __FILE__, __LINE__, __PRETTY_FUNCTION__, (long unsigned int) size); fflush(stderr);} \
			else { CLEAR(*var);}\
		} while (0)

#define XFREE(var)					\
		do { if (var) { free(var); } } while (0)

#endif

#define AVAHI4J_PACKAGE			"avahi4j"
#define EXCEPTION_PACKAGE		AVAHI4J_PACKAGE "/exceptions"
#define GENERIC_EXCP			EXCEPTION_PACKAGE "/Avahi4JException"
#define JNI_EXCP				EXCEPTION_PACKAGE "/JNIException"


/* Exception throwing helper */
#define EXCEPTION_MSG_LENGTH	100
#define THROW_EXCEPTION(e, c, format, ...)\
		do {\
			char msg[EXCEPTION_MSG_LENGTH+1];\
			jclass exception = (*e)->FindClass(e,c);\
			snprintf(msg, EXCEPTION_MSG_LENGTH, format, ## __VA_ARGS__);\
			if(exception!=0) (*e)->ThrowNew(e, exception, msg);\
		} while(0)


// acquire / release avahi client/poll mutex
#define AVAHI_LOCK(client) avahi_threaded_poll_lock(client->pollLoop)
#define AVAHI_UNLOCK(client) avahi_threaded_poll_unlock(client->pollLoop)

// translate an interface index to an avahi interface index and back
#define GET_AVAHI_IF_IDX(i) (i==-1)?AVAHI_IF_UNSPEC:i
#define GET_JAVA_IF_IDX(avahi_if_idx) (avahi_if_idx==AVAHI_IF_UNSPEC)?-1:avahi_if_idx

// translate a Avahi4JConstant.Protocol enum to an AvahiProtocol
#define GET_AVAHI_PROTO(avahip, userp, e, ret) do {\
		avahip=-1;\
		jclass enum_class = (*e)->FindClass(e, "java/lang/Enum");\
		if (enum_class==NULL) {\
			dprint("unable to find Enum class\n");\
			THROW_EXCEPTION(e, JNI_EXCP, "unable to locate Enum class");\
			return ret;\
		}\
		jmethodID ordinal = (*e)->GetMethodID(e, enum_class, "ordinal", "()I");\
		if (ordinal==NULL) {\
			dprint("unable to find Enum.ordinal method\n");\
			THROW_EXCEPTION(e, JNI_EXCP, "unable to locate Enum.ordinal method");\
			return ret;\
		}\
		int value = (*e)->CallIntMethod(e, userp, ordinal);\
		avahip = (value==0)?AVAHI_PROTO_INET:(value==1)?AVAHI_PROTO_INET6:AVAHI_PROTO_UNSPEC;\
	} while(0)
// tranlsate an AvahiProtocol to a java enum
#define GET_JAVA_PROTO(avahip, javap) do {\
		switch(avahip){\
		case AVAHI_PROTO_INET:\
			javap=0;\
			break;\
		case AVAHI_PROTO_INET6:\
			javap=1;\
			break;\
		default:\
			javap=2;\
		};\
	} while(0)

// translate AvahiLookupResultFlags to java Avahi4jConstants.LOOKUP_RESULT_*
#define GET_JAVA_LOOKUP_RES_FLAG(avahif, javaf) do {\
		javaf = 0;\
		if(avahif & AVAHI_LOOKUP_RESULT_CACHED) javaf &= 1;\
		if(avahif & AVAHI_LOOKUP_RESULT_WIDE_AREA) javaf &= (1<<1);\
		if(avahif & AVAHI_LOOKUP_RESULT_MULTICAST) javaf &= (1<<2);\
		if(avahif & AVAHI_LOOKUP_RESULT_LOCAL) javaf &= (1<<3);\
		if(avahif & AVAHI_LOOKUP_RESULT_OUR_OWN) javaf &= (1<<4);\
		if(avahif & AVAHI_LOOKUP_RESULT_STATIC) javaf &= (1<<5);\
	}while(0)

// jstring to const char* helpers
#define GET_UTF_STR(cstr, jstr, e, ret) \
	do {\
		if (jstr!=NULL){\
			cstr = (*e)->GetStringUTFChars(e, jstr, NULL);\
			if (cstr==NULL)\
			{\
				dprint("error getting UTF string\n");\
				THROW_EXCEPTION(e, JNI_EXCP, "error getting UTF string");\
				return ret;\
			}\
		} else {\
			cstr=NULL;\
		}\
	} while(0)
#define GET_UTF_STR_JUMP(cstr, jstr, e, bail) \
	do {\
		if (jstr!=NULL){\
			cstr = (*e)->GetStringUTFChars(e, jstr, NULL);\
			if (cstr==NULL)\
			{\
				dprint("error getting UTF string\n");\
				goto bail;\
			}\
		} else {\
			cstr=NULL;\
		}\
	} while(0)
#define PUT_UTF_STR(cstr, jstr, e) if (jstr!=NULL && cstr!=NULL) (*e)->ReleaseStringUTFChars(e, jstr, cstr);

#define CHECK_N_RET(func, result) \
	dprint(#func " returned %d %s\n", result, (result<0)?avahi_strerror(result):"");\
	return result;

/*
 * main structure
 */
struct avahi4j_client {
	AvahiThreadedPoll 	*pollLoop;
	AvahiClient 			*client;
	JavaVM 					*jvm;
	jmethodID				clientCallbackDispatch;
	jobject					clientObject;
};

struct avahi4j_entry_group {
	AvahiEntryGroup		*group;
	JavaVM 				*jvm;
	jmethodID			groupCallbackDispatch;
	jobject				groupObject;
};

struct avahi4j_service_browser {
	AvahiServiceBrowser	*browser;
	JavaVM 				*jvm;
	jmethodID			browserCallbackDispatch;
	jobject				browserObject;
};

#endif /* COMMON_H_ */

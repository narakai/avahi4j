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

#include <stdlib.h>
#include <avahi-client/client.h>
#include <avahi-client/lookup.h>
#include <avahi-client/publish.h>
#include <avahi-common/address.h>
#include <avahi-common/error.h>
#include <avahi-common/simple-watch.h>
#include <string.h>

// use fixed local copy of thread-watch since Avahi's provided one has a bug
// that was fixed only since 0.6.24...
#include "thread-watch.h"
#include "jni_helpers.h"

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


#define CHECK_N_RET(func, result) \
	dprint(#func " returned %d %s\n", result, (result<0)?avahi_strerror(result):"");\
	return result;

/*
 * main structure
 */
struct avahi4j_client {
	AvahiThreadedPoll 	*pollLoop;
	AvahiClient 		*client;
	JavaVM 				*jvm;
	jmethodID			clientCallbackDispatch;
	jobject				clientObject;
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

struct avahi4j_service_resolver {
	AvahiServiceResolver 	*resolver;
	JavaVM 					*jvm;
	jmethodID				resolverCallbackDispatch;
	jobject					resolverObject;
	jclass					stringClass;
};

struct avahi4j_record_browser {
	AvahiRecordBrowser	 	*browser;
	JavaVM 					*jvm;
	jmethodID				browserCallbackDispatch;
	jobject					browserObject;
};

struct AvahiThreadedPoll {
    AvahiSimplePoll *simple_poll;
    pthread_t thread_id;
    pthread_mutex_t mutex;
    int thread_running;
    int retval;
};

#endif /* COMMON_H_ */

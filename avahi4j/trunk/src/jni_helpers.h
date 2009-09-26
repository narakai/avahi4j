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


#ifndef JNI_HELPERS_H_
#define JNI_HELPERS_H_

#include <pthread.h>

// acquire / release avahi client/poll mutex
#define AVAHI_LOCK(client) do{\
		if(pthread_equal(pthread_self(), client->pollLoop->thread_id)==0)\
			avahi_threaded_poll_lock(client->pollLoop);\
	}while(0)
#define AVAHI_UNLOCK(client)  do{\
		if(pthread_equal(pthread_self(), client->pollLoop->thread_id)==0)\
			avahi_threaded_poll_unlock(client->pollLoop);\
	}while(0)

// translate an interface index to an avahi interface index and back
#define GET_AVAHI_IF_IDX(avahi_if_idx, jif_idx) \
	avahi_if_idx = (jif_idx==-1)?AVAHI_IF_UNSPEC:jif_idx

#define GET_JAVA_IF_IDX(avahi_if_idx, jif_idx) \
	jif_idx = (avahi_if_idx==AVAHI_IF_UNSPEC)?-1:avahi_if_idx

// translate a Avahi4JConstant.Protocol enum ordinal to an AvahiProtocol
#define GET_AVAHI_PROTO(avahip, javap) do {\
		avahip=-1;\
		switch(javap){\
		case 0:\
			avahip=AVAHI_PROTO_INET;\
			break;\
		case 1:\
			avahip=AVAHI_PROTO_INET6;\
			break;\
		default:\
			avahip=AVAHI_PROTO_UNSPEC;\
		};\
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
		if(avahif & AVAHI_LOOKUP_RESULT_CACHED) javaf |= 1;\
		if(avahif & AVAHI_LOOKUP_RESULT_WIDE_AREA) javaf |= (1<<1);\
		if(avahif & AVAHI_LOOKUP_RESULT_MULTICAST) javaf |= (1<<2);\
		if(avahif & AVAHI_LOOKUP_RESULT_LOCAL) javaf |= (1<<3);\
		if(avahif & AVAHI_LOOKUP_RESULT_OUR_OWN) javaf |= (1<<4);\
		if(avahif & AVAHI_LOOKUP_RESULT_STATIC) javaf |= (1<<5);\
	}while(0)

#define GET_AVAHI_LOOKUP_FLAGS(avahif, javaf) do{\
		avahif=0;\
		if(javaf & 1) avahif |= AVAHI_LOOKUP_NO_TXT;\
		if(javaf & 2) avahif |= AVAHI_LOOKUP_NO_ADDRESS;\
	}while(0)

#define GET_JAVA_BROWSER_EVT(avahie,javae) do{\
		switch(avahie){\
		case AVAHI_BROWSER_NEW:\
			javae=0;\
			break;\
		case AVAHI_BROWSER_REMOVE:\
			javae=1;\
			break;\
		case AVAHI_BROWSER_CACHE_EXHAUSTED:\
			javae=2;\
			break;\
		case AVAHI_BROWSER_ALL_FOR_NOW:\
			javae=3;\
			break;\
		case AVAHI_BROWSER_FAILURE:\
			javae=4;\
			break;\
		default:\
			javae=4;\
		};\
	}while(0)

#define GET_JAVA_RESOLVER_EVT(avahie,javae) do{\
		switch(avahie){\
		case AVAHI_RESOLVER_FOUND:\
			javae=0;\
			break;\
		case AVAHI_RESOLVER_FAILURE:\
		default:\
			javae=1;\
			break;\
		};\
	}while(0)

#define TRANSLATE_DNS_CLASS(avahic, javac, toJava) do{\
		if(toJava){\
			switch (avahic){\
			case AVAHI_DNS_CLASS_IN:\
				javac=0;\
				break;\
			default:\
				javac=0;\
			};\
		} else {\
			switch (javac){\
			case 0:\
				avahic=AVAHI_DNS_CLASS_IN;\
				break;\
			default:\
				avahic=AVAHI_DNS_CLASS_IN;\
			};\
		}\
	}while(0)

#define A2J_DNS_CLASS(avahic, javac) TRANSLATE_DNS_CLASS(avahic,javac,1)
#define J2A_DNS_CLASS(avahic, javac) TRANSLATE_DNS_CLASS(avahic,javac,0)

#define TRANSLATE_DNS_RR_TYPE(avahit, javat, toJava) do{\
		if(toJava){\
			switch(avahit){\
			case AVAHI_DNS_TYPE_A:\
				javat=0;\
				break;\
			case AVAHI_DNS_TYPE_NS:\
				javat=1;\
				break;\
			case AVAHI_DNS_TYPE_CNAME:\
				javat=2;\
				break;\
			case AVAHI_DNS_TYPE_SOA:\
				javat=3;\
				break;\
			case AVAHI_DNS_TYPE_PTR:\
				javat=4;\
				break;\
			case AVAHI_DNS_TYPE_HINFO:\
				javat=5;\
				break;\
			case AVAHI_DNS_TYPE_MX:\
				javat=6;\
				break;\
			case AVAHI_DNS_TYPE_TXT:\
				javat=7;\
				break;\
			case AVAHI_DNS_TYPE_AAAA:\
				javat=8;\
				break;\
			case AVAHI_DNS_TYPE_SRV:\
				javat=9;\
				break;\
			default:\
				javat=0;\
				break;\
			};\
		} else {\
			switch(javat){\
			case 0:\
				avahit= AVAHI_DNS_TYPE_A;\
				break;\
			case 1:\
				avahit= AVAHI_DNS_TYPE_NS;\
				break;\
			case 2:\
				avahit= AVAHI_DNS_TYPE_CNAME;\
				break;\
			case 3:\
				avahit= AVAHI_DNS_TYPE_SOA;\
				break;\
			case 4:\
				avahit= AVAHI_DNS_TYPE_PTR;\
				break;\
			case 5:\
				avahit= AVAHI_DNS_TYPE_HINFO;\
				break;\
			case 6:\
				avahit= AVAHI_DNS_TYPE_MX;\
				break;\
			case 7:\
				avahit= AVAHI_DNS_TYPE_TXT;\
				break;\
			case 8:\
				avahit= AVAHI_DNS_TYPE_AAAA;\
				break;\
			case 9:\
				avahit= AVAHI_DNS_TYPE_SRV;\
				break;\
			default:\
				avahit= AVAHI_DNS_TYPE_A;\
				break;\
			};\
		}\
	}while(0)
#define A2J_DNS_RR_TYPE(avahit, javat) TRANSLATE_DNS_RR_TYPE(avahit, javat, 1)
#define J2A_DNS_RR_TYPE(avahit, javat) TRANSLATE_DNS_RR_TYPE(avahit, javat, 0)

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

#define PUT_UTF_STR(cstr, jstr, e) \
	if (jstr!=NULL && cstr!=NULL) \
		(*e)->ReleaseStringUTFChars(e, jstr, cstr);

#define GET_JSTRING_JUMP(cstr, jstr, e, bail) do{\
		if((jstr = (*e)->NewStringUTF(e, cstr))==NULL)\
			goto bail;\
	}while(0)

#endif /* JNI_HELPERS_H_ */

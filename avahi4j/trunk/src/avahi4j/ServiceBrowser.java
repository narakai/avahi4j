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
package avahi4j;

import avahi4j.Avahi4JConstants.LookupResultFlag;
import avahi4j.Avahi4JConstants.Protocol;
import avahi4j.exceptions.Avahi4JException;


public class ServiceBrowser {
	public enum ServiceBrowserEvent {
		NEW,
		REMOVE,
		CACHE_EXHAUSTED,
		NO_MORE,
		FAILURE
	};
	
	/*
	 * M E M B E R S
	 */
	/**
	 * A JNI pointer to a struct avahi4j_service_browser
	 */
	private long avahi4j_service_browser_ptr;
	/**
	 * the group's callback object (cannot be null)
	 */
	IServiceBrowserCallback browserCallback;
	
	
	/*
	 * N A T I V E   M E T H O D S
	 */
	/**
	 * This method create the struct avahi4j_service_browser
	 * @param o a jni avahi4j_client struct pointer
	 * @return a jni avahi4j_service_browser_ptr pointer
	 * @throws Avahi4JException if there is an error creating the browser
	 */
	private native long initBrowser(long avahi4j_client_ptr,
			int interfaceNum, Protocol proto, String type, String domain, 
			int lookupFlags) throws Avahi4JException;
	/**
	 * This method frees the avahi4j_service_browser
	 * @param o a jni avahi4j_service_browser pointer
	 * @return the raw result from avahi_service_browser_free()
	 */
	private native int release(long o);
	
	
	/*
	 * M E T H O D S
	 */
	/**
	 * This method builds a new service browser. 
	 * @param callback the callback object to receive notification about the entry group
	 * @param avahi4j_client_ptr a struct avahi4j_service_browser_ptr pointer
	 * @param intefaceNum the interface number
	 * @param proto the {@link Protocol} number
	 * @param type the service type to browse
	 * @param domain the domain to search (can be null to use client's domain)
	 * @param lookupFlags the lookup flags (see Avahi4JConstants.LOOKUP_*)
	 * @throws Avahi4JException if there is a problem creating the browser
	 * @throws NullPointerException iftype is null
	 */
	ServiceBrowser(IServiceBrowserCallback callback, long avahi4j_client_ptr,
			int interfaceNum, Protocol proto, String type, String domain, 
			int lookupFlags) throws Avahi4JException {
		
		if (type==null)
			throw new NullPointerException("The service type can not be null");
		
		browserCallback = callback;
		avahi4j_service_browser_ptr =  initBrowser(avahi4j_client_ptr, interfaceNum,
				proto, type, domain, lookupFlags);
	}
	
	/**
	 * This method must be called when this browser is no longer needed.
	 */
	public void release() {
		release(avahi4j_service_browser_ptr);
	}	
	
	
	/*
	 * C A L L B A C K
	 */
	/**
	 * This method is called from JNI to dispatch a callback
	 */
	@SuppressWarnings("unused")
	private void serviceCallback(int interfaceNum, int proto, int browserEvent,
			String name, String type, String domain, int flags){
		
		browserCallback.serviceCallback(interfaceNum, Protocol.values()[proto],
				ServiceBrowserEvent.values()[browserEvent], name, type, domain,
				flags);
	}
}

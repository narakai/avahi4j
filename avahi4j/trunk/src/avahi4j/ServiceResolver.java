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

import avahi4j.Avahi4JConstants.Protocol;
import avahi4j.exceptions.Avahi4JException;

/**
 * Objects of this class are used to resolve a service.<br>
 * To create a service resolver object for a given service, call 
 * {@link Client#createServiceResolver(IServiceResolverCallback, int, avahi4j.Avahi4JConstants.Protocol, String, String, String, avahi4j.Avahi4JConstants.Protocol, int) createServiceResolver()}
 * on an existing {@link Client} object. Once the service is resolved, results
 * will be delivered to the provided callback object until the service resolver
 * is released. <b>Service resolver objects MUST be released when no longer 
 * needed, by calling {@link #release()}.</b> 
 * 
 * @author gilles
 *
 */
public final class ServiceResolver {
	/**
	 * This enumeration lists the possible resolver events.
	 * @author gilles
	 *
	 */
	public enum ServiceResolverEvent {
		/**
		 * The specified service has been resolved.
		 */
		RESOLVER_FOUND,
		/**
		 * The specified service could not be resolved.
		 */
		RESOLVER_FAILURE
	};
	
	
	/*
	 * M E M B E R S
	 */
	private long avahi4j_resolver_ptr;
	private IServiceResolverCallback resolverCallback;
	private boolean released;
	
	
	/*
	 * N A T I V E   M E T H O D S
	 */
	/**
	 * this method creates an AvahiServiceResover object
	 * @param avahi4j_client_ptr the avahi4j_client struct
	 * @param ifNum the interface number
	 * @param proto the protocol
	 * @param name the name of the service
	 * @param type the type of the service
	 * @param domain the domain
	 * @param addressProtocol the desired protocol of the resolved address 
	 * @param lookupFlags the lookup flags (Avahi4JConstants.LOOKUP_*)
	 * @return a pointer to a avahi4j_resolver struct
	 * @throws Avahi4JException if there is an error creating the service resolver
	 */
	private native long init_resolver(long avahi4j_client_ptr, int ifNum, int proto,
			String name, String type, String domain, int addressProtocol, 
			int lookupFlags) throws Avahi4JException;
	
	/**
	 * This method releases the service resolver
	 * @param avahi4j_resolver_ptr the resolver to be released
	 * @param lookupFlags lookup flags (see Avahi4JConstants.LOOKUP_* )
	 * @return the raw result from avahi_service_resolver_free
	 */
	private native int release(long avahi4j_resolver_ptr);
	
	
	/*
	 * M E T H O D S
	 */
	/**
	 * This method builds a new service resolver. 
	 * @param avahi4j_client_ptr a JNI pointer to a avahi4j_client struct
	 * @param callback the callback object which will receive service resolving
	 * notifications
	 * @param ifNum the interface number of the service to be resolved. Pass 
	 * the interface number as received by the service browser
	 * @param proto the protocol of the service to be resolved. Pass the 
	 * protocol as received by the service browser
	 * @param name the name of the service to be resolved. Pass the name as 
	 * received by the service browser
	 * @param type the type of the service to be resolved. Pass the type as 
	 * received by the service browser
	 * @param domain the domain of the service to be resolved. Pass the domain as 
	 * received by the service browser
	 * @param addressProtocol the protocol of the resolved address.
	 * @param lookupFlags the lookup flags (Avahi4JConstants.LOOKUP_*)
	 * @throws Avahi4JException if there is an error creating the resolver 
	 */
	ServiceResolver(long avahi4j_client_ptr, IServiceResolverCallback callback,
			int ifNum, Protocol proto, String name,	String type, String domain, 
			Protocol addressProtocol, int lookupFlags) throws Avahi4JException {
		
		resolverCallback = callback;
		released = false;
		avahi4j_resolver_ptr = init_resolver(avahi4j_client_ptr, ifNum, proto.ordinal(), 
				name, type, domain, addressProtocol.ordinal(), lookupFlags);
	}
	
	/**
	 * This method must be called when this resolver is no longer needed, to 
	 * release resources.
	 */
	public synchronized void release() {
		if(!released){
			release(avahi4j_resolver_ptr);
			released = true;
		}
	}
	
	/**
	 * This method is called from JNI to dispatch a resolver callback
	 */
	@SuppressWarnings("unused")
	private void dispatchCallback(int interfaceNum, int proto, int resolverEvent,
			String name, String type, String domain, String hostname, 
			String address, int addressType, int port, String txtRecords[], int lookupResultFlag){
		
		Address a;
		try {
			a = new Address(address, Protocol.values()[addressType]);
		} catch (Exception e) {a = null;}

		resolverCallback.resolverCallback(this, interfaceNum, Protocol.values()[proto],
			ServiceResolverEvent.values()[resolverEvent], name, type, domain,
			hostname, a, port, txtRecords, lookupResultFlag);
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ (int) (avahi4j_resolver_ptr ^ (avahi4j_resolver_ptr >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof ServiceResolver))
			return false;
		ServiceResolver other = (ServiceResolver) obj;
		if (avahi4j_resolver_ptr != other.avahi4j_resolver_ptr)
			return false;
		return true;
	}
}

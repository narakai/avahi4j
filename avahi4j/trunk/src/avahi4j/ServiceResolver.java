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

import java.util.Vector;

import avahi4j.Avahi4JConstants.Protocol;
import avahi4j.exceptions.Avahi4JException;

public class ServiceResolver {
	public enum ServiceResolverEvent {
		RESOLVER_FOUND,
		RESOLVER_FAILURE
	};
	
	private long avahi4j_resolver_ptr;
	private IServiceResolverCallback resolverCallback;
	
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
	
	ServiceResolver(long avahi4j_client_ptr, IServiceResolverCallback callback,
			int ifNum, Protocol proto, String name,	String type, String domain, 
			Protocol addressProtocol, int lookupFlags) throws Avahi4JException {
		
		resolverCallback = callback;
		
		avahi4j_resolver_ptr = init_resolver(avahi4j_client_ptr, ifNum, proto.ordinal(), 
				name, type, domain, addressProtocol.ordinal(), lookupFlags);
	}
	
	public void release() {
		release(avahi4j_resolver_ptr);
	}
	
	/**
	 * This method is called from JNI to dispatch a resolver callback
	 */
	@SuppressWarnings("unused")
	private void dispatchCallback(int interfaceNum, int proto, int resolverEvent,
			String name, String type, String domain, String hostname, 
			String address, int addressType, int port, String txtRecords[], int lookupResultFlag){
		
		Vector<String> list = new Vector<String>(txtRecords.length);
		list.copyInto(txtRecords);
		
		resolverCallback.resolverCallback(interfaceNum, Protocol.values()[proto],
				ServiceResolverEvent.values()[resolverEvent], name, type, domain,
				hostname, new Address(address, Protocol.values()[addressType]) ,
				port, list, lookupResultFlag);
	}
}

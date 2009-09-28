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
import avahi4j.ServiceResolver.ServiceResolverEvent;

/**
 * Classes implementing this interface receive notifications from {@link ServiceResolver}
 * objects when a service (as registered with the service resovler object) has 
 * been resolved. 
 * @author gilles
 *
 */
public interface IServiceResolverCallback {
	/**
	 * This method is called when a service has been resolved. The service details
	 * are given as arguments. <b>Note that when a service is removed, the reported
	 * event is set to {@link ServiceResolverEvent#RESOLVER_FOUND}, but the other
	 * arguments are set to 0 and null.</b> So take care when handling these arguments.
	 * @param resolver the {@link ServiceResolver} object who 's calling this method
	 * @param interfaceNum the interface number the service is registered on
	 * @param proto the service's protocol
	 * @param resolverEvent the event associated with the service. If 
	 * {@link ServiceResolverEvent#RESOLVER_FAILURE} is received, all the other 
	 * fields are meaningless. 
	 * @param name the name of the service
	 * @param type the type of the service
	 * @param domain the domain where the service is registered
	 * @param hostname the name of the host offering this service
	 * @param address the address of the host
	 * @param port the port the service is running on
	 * @param txtRecords any TXT records associated with the service
	 * @param lookupResultFlag the lookup result flag LOOKUP_RESULT_* 
	 * (See {@link Avahi4JConstants})
	 */
	public void resolverCallback(ServiceResolver resolver, int interfaceNum,
			Protocol proto,	ServiceResolverEvent resolverEvent,	String name,
			String type, String domain, String hostname, Address address, 
			int port, String txtRecords[],int lookupResultFlag);
}

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

public interface IServiceResolverCallback {
	public void resolverCallback(ServiceResolver resolver, int interfaceNum,
			Protocol proto,	ServiceResolverEvent resolverEvent,	String name,
			String type, String domain, String hostname, Address address, 
			int port, String txtRecords[],int lookupResultFlag);
}

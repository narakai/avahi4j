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
import avahi4j.Avahi4JConstants.BrowserEvent;

/**
 * Objects implementing this interface can receive notifications from a 
 * {@link ServiceBrowser} about services matching a set of criteria (given to the
 * service browser when instantiated).
 * @author gilles
 *
 */
public interface IServiceBrowserCallback {
	/**
	 * This method is called by a {@link ServiceBrowser} when an event about a service 
	 * matching the criteria given to the service browser has been reported. 
	 * The details about this event are given as arguments to this method.
	 * @param interfaceNum the interface number the matching service is registered on
	 * @param proto the protocol of the matching service
	 * @param browserEvent the event that triggered this call. If 
	 * {@link BrowserEvent#FAILURE} is received, all the other values are 
	 * meaningless
	 * @param name the name of the service
	 * @param type the type of the service
	 * @param domain the domain of the service
	 * @param lookupResultFlag lookup result flags 
	 * (See Avahi4JConstants.LOOKUP_RESULT_* )
	 */
	public void serviceCallback(int interfaceNum, Protocol proto, BrowserEvent browserEvent,
			String name, String type, String domain, int lookupResultFlag);
}

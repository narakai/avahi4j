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

import avahi4j.Avahi4JConstants.BrowserEvent;
import avahi4j.Avahi4JConstants.DNS_Class;
import avahi4j.Avahi4JConstants.DNS_RRType;
import avahi4j.Avahi4JConstants.Protocol;

/**
 * Classes implementing this interface receive notifications from {@link RecordBrowser}
 * objects when records for a given host have been retrieved. 
 * @author gilles
 *
 */
public interface IRecordBrowserCallback {
	/**
	 * This method is called when the records for a host are available. The 
	 * record's details are given to this method as arguments.
	 * @param browser the {@link RecordBrowser} object calling this method.
	 * @param interfaceNum the interface number the host is registered on
	 * @param proto the protocol
	 * @param event the event associated with this record. If {@link BrowserEvent#FAILURE}
	 * is received, all the other fields are meaningless.
	 * @param name the hostname
	 * @param clazz the RR class
	 * @param type the RR type
	 * @param rdata the record's data
	 * @param lookupResultFlags the lookup result flag LOOKUP_RESULT_* 
	 * (See {@link Avahi4JConstants})
	 */
	public void recordBrowserCallback(RecordBrowser browser, int interfaceNum,
			Protocol proto,	BrowserEvent event,	String name,
			DNS_Class clazz, DNS_RRType type, byte rdata[], int lookupResultFlags);
}

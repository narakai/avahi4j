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
import avahi4j.exceptions.Avahi4JException;

/**
 * Objects of this class are used to browse DNS records for a specific host.
 * To create a record browser object for a given host, call 
 * {@link Client#createRecordBrowser(IRecordBrowserCallback, int, avahi4j.Avahi4JConstants.Protocol, String, avahi4j.Avahi4JConstants.DNS_Class, avahi4j.Avahi4JConstants.DNS_RRType, int) createRecordBrowser()}
 * on an existing {@link Client} object. Once the specified records are retrieved, results
 * will be delivered to the provided callback object. <b>Record browser objects 
 * MUST be released when no longer needed, by calling {@link #release()}.</b> 
 * @author gilles
 *
 */
public final class RecordBrowser {
	
	/*
	 * M E M B E R S
	 */
	private boolean released;
	private IRecordBrowserCallback callback;
	private long avahi4j_record_browser_ptr;
	
	
	/*
	 * N A T I V E   M E T H O D S
	 */
	/**
	 * This method create an avahi record browser object
	 * @return a JNI pointer to a avahi4j_record_browser struct
	 * @throws Avahi4JException if there is an error creating the record browser
	 */
	private native long initBrowser(long avahi4j_client_ptr,  int interfaceIdx,
			int proto, String name, int clazz, int type, int lookupFlag) 
			throws Avahi4JException;
	
	private native int releaseBrowser(long avahi4j_record_browser_ptr);
	
	
	/*
	 * M E T H O D S
	 */
	/**
	 * This method creates a new record browser
	 */
	RecordBrowser(long avahi4j_client_ptr, IRecordBrowserCallback cb, 
			int interfaceIdx, Protocol proto, String name, DNS_Class clazz, 
			DNS_RRType type, int lookupFlag) throws Avahi4JException{
		
		released = false;
		callback = cb;
		avahi4j_record_browser_ptr = initBrowser(avahi4j_client_ptr, interfaceIdx,
				proto.ordinal(), name, clazz.ordinal(), type.ordinal(), lookupFlag);
	}
	
	/**
	 * This method releases this record browser object.
	 */
	public synchronized void release() {
		if(!released){
			releaseBrowser(avahi4j_record_browser_ptr);
			released = true;
		}
	}
	
	/**
	 * callback dispatch method called form JNI
	 */
	@SuppressWarnings("unused")
	private void dispatchCallback(int interfaceNum, int proto, int event,
			String name, int clazz, int type, byte rdata[], int flags){
		
		callback.recordBrowserCallback(this, interfaceNum, Protocol.values()[proto],
				BrowserEvent.values()[event], name, DNS_Class.values()[clazz],
				DNS_RRType.values()[type], rdata, flags);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ (int) (avahi4j_record_browser_ptr ^ (avahi4j_record_browser_ptr >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof RecordBrowser))
			return false;
		RecordBrowser other = (RecordBrowser) obj;
		if (avahi4j_record_browser_ptr != other.avahi4j_record_browser_ptr)
			return false;
		return true;
	}
}

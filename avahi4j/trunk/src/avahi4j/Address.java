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
import avahi4j.exceptions.JNIException;

/**
 * This class encapsulate an IP address, either IPv4 or IPv6.
 * @author gilles
 *
 */
public class Address {
	private String address;
	private Protocol protocol;
	
	/**
	 * This method sets the address member of this class according to the address
	 * contained in the AvahiAddress object pointed to by the argument.
	 * @param avahi_address_ptr
	 * @return the address' protocol as an index into the {@link Protocol} enumeration 
	 */
	private native int parse_avahi_address(long avahi_address_ptr);
	
	Address(long avahi_address_ptr){
		protocol = Protocol.values()[parse_avahi_address(avahi_address_ptr)];
	}

	Address(String a, Protocol p){
		if(a==null)
			throw new NullPointerException("The address can not be null");
		if(p.equals(Protocol.ANY))
			throw new JNIException("The protocol can not be set to ANY");
		
		address = a;
		protocol = p;
	}
	
	/**
	 * This method returns the IP address as a string.
	 * @return the IP address contained in this object, as a string.
	 */
	public String getAddress() {
		return address;
	}
	
	/**
	 * This method returns the type of this IP address (IPv4 or IPv6).
	 * @return the type of this IP address
	 */
	public Protocol getProtocol() {
		return protocol;
	}
	
	public String toString() {
		if(protocol==Protocol.INET){
			return "IPv4: "+address;
		} else {
			return "IPv6: "+address;
		}
	}
}

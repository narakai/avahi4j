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
package avahi4j.examples;

import java.util.Vector;

import avahi4j.Avahi4JConstants;
import avahi4j.Client;
import avahi4j.EntryGroup;
import avahi4j.IClientCallback;
import avahi4j.IEntryGroupCallback;
import avahi4j.Avahi4JConstants.Protocol;
import avahi4j.EntryGroup.State;
import avahi4j.exceptions.Avahi4JException;

/**
 * This example class shows how to publish a service and some TXT records, and
 * how to update these.
 * @author gilles
 *
 */
public class TestServicePublish implements IClientCallback, IEntryGroupCallback{
	
	/**
	 * The Avahi4J {@link Client} object
	 */
	private Client client;
	
	/**
	 * The service's {@link EntryGroup}, which contains the service's details.
	 */
	private EntryGroup group;
	
	/**
	 * A list of TXT records for this service.
	 */
	private Vector<String> records;
	
	/**
	 * This method builds the test object.
	 * @throws Avahi4JException if there is an error creating or starting the 
	 * Avahi4J {@link Client}.
	 */
	public TestServicePublish() throws Avahi4JException {
		records = new Vector<String>();
		client = new Client(this);
		client.start();
		System.out.println("FQDN: "+client.getFQDN());
		System.out.println("Hostname: "+client.getHostName());
		System.out.println("domain name: "+client.getDomainName());
		System.out.println("state: "+client.getState());
	}
	
	/**
	 * This method creates the {@link EntryGroup}, adds some TXT records, a name,
	 * a service type and port number. It then proceeds to publish the service.
	 * If there is a name conflict, it asks Avahi4J for an alternate name and 
	 * tries again.
	 * @throws Avahi4JException if there is an error creating the {@link EntryGroup}
	 */
	public void addService() throws Avahi4JException {
		int result;

		// create group
		group = client.createEntryGroup(this);
		
		// create some fake TXT records
		records.add("record1=1");
		records.add("record2=2");
		
		// add service
		System.out.println("\n\nAdding new service to group");
		result = group.addService(Avahi4JConstants.AnyInterface, Protocol.ANY,
				"TestService", "_test._tcp", null, null, 1515, records);
		if (result!=Avahi4JConstants.AVAHI_OK) {
			System.out.println("Error adding service to group: "+
					Avahi4JConstants.getErrorString(result));
			
			// try with an alternate name
			String newName = EntryGroup.findAlternativeServiceName("TestService");
			System.out.println("\n\nRe-trying with new service name: "+newName);
			result = group.addService(Avahi4JConstants.AnyInterface, Protocol.ANY,
					newName, "_test._tcp", null, null, 1515, records);
			if (result!=Avahi4JConstants.AVAHI_OK)
				System.out.println("Error adding service to group: "+
						Avahi4JConstants.getErrorString(result));
		}
		
		// commit service
		System.out.println("Committing group");
		result = group.commit(); 
		if(result!=Avahi4JConstants.AVAHI_OK)
			System.out.println("Error committing group: "
					+Avahi4JConstants.getErrorString(result));
		
		System.out.println("done");
	}
	
	/**
	 * This method updates the TXT records of the previously created service
	 */
	public void updateService() throws Exception{
		int result;
		// TXT records
		Vector<String> records = new Vector<String>();
		
		// update records
		records.add("record1=1");
		records.add("UpdatedRecord2=NewUpdatedValue2");
		System.out.println("\n\nUpdating service");
		result = group.updateService(Avahi4JConstants.AnyInterface, Protocol.ANY,
				"TestService", "_test._tcp", null, records);
		if(result!=Avahi4JConstants.AVAHI_OK){
			System.out.println("Error updating service: "
					+ Avahi4JConstants.getErrorString(result));
		} else {
			System.out.println("done");
		}
	}
	
	/**
	 * This method resets (un-publishes) the service
	 */
	public void resetService(){
		int result;
		
		// reset group
		System.out.println("Resetting group");
		result = group.reset();
		if(result!=Avahi4JConstants.AVAHI_OK) { 
			System.out.println("Error resetting group: "
					+Avahi4JConstants.getErrorString(result));
		} else {
			System.out.println("done");
		}
	}
	
	/**
	 * This method releases the {@link EntryGroup} and {@link Client}
	 */
	public void stop(){
		group.release();
		client.stop();
		client.release();
	}

	/**
	 * This callback method is invoked whenever the Avahi4J {@link Client}'s 
	 * state changes. See {@link State} for a list of possible client states.
	 */
	@Override
	public void clientStateChanged(Client.State state) {
		System.out.println("client's new state: " + state);
	}
	
	/**
	 * This callback method is invoked whenever the {@link EntryGroup}'s 
	 * state changes. See {@link State} for a list of possible states.
	 */
	@Override
	public void groupStateChanged(State newState) {
		System.out.println("Group's new state: " + newState);
	}

	/**
	 * Main method which publishes a service, updates it & release it
	 * @param args (does not expect any argument)
	 * @throws Exception If there is an error of some sort
	 */
	public static void main(String args[]) throws Exception{
		TestServicePublish t = new TestServicePublish();
		System.out.println("Press <Enter>");
		System.in.read();
		t.addService();
		System.out.println("Press <Enter>");
		System.in.read();
		t.updateService();
		System.out.println("Press <Enter>");
		System.in.read();
		t.resetService();
		System.out.println("Press <Enter>");
		System.in.read();
		t.stop();
	}	
}

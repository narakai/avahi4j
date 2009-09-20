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



public class TestClient implements IClientCallback, IEntryGroupCallback{
	
	private Client client;
	private EntryGroup group;
	private Vector<String> records;
	
	public TestClient() throws Avahi4JException {
		records = new Vector<String>();
		client = new Client(this);
		client.start();
		System.out.println("FQDN: "+client.getFQDN());
		System.out.println("Hostname: "+client.getHostName());
		System.out.println("domain name: "+client.getDomainName());
		System.out.println("state: "+client.getState());
	}
	
	public void addService() throws Exception{
		int result;

		// create group
		group = client.createEntryGroup(this);
		
		// ad fake TXT records
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
			System.out.println("\n\nAdding new service to group");
			result = group.addService(Avahi4JConstants.AnyInterface, Protocol.ANY,
					newName, "_test._tcp", null, null, 1515, records);
			if (result!=Avahi4JConstants.AVAHI_OK)
				System.out.println("Error adding service to group: "+
						Avahi4JConstants.getErrorString(result));
		}

		// sleep 3 seconds
		Thread.sleep(3000);
		System.out.println("\n\n");
		
		// commit service
		System.out.println("Committing group");
		result = group.commit(); 
		if(result!=Avahi4JConstants.AVAHI_OK)
			System.out.println("Error committing group: "
					+Avahi4JConstants.getErrorString(result));
		
	}
	
	public void updateService() throws Exception{
		int result;
		// TXT records
		Vector<String> records = new Vector<String>();
		
		// update records
		records.remove("record1=1");
		records.add("newrecord1=11");
		System.out.println("\n\nUpdating service");
		result = group.updateService(Avahi4JConstants.AnyInterface, Protocol.ANY,
				"TestService", "_test._tcp", null, records);
		if(result!=Avahi4JConstants.AVAHI_OK){
			System.out.println("Error updating service: "+ Avahi4JConstants.getErrorString(result));
		}

		// sleep 3 seconds
		Thread.sleep(3000);
		System.out.println("\n\n");
		
		// reset group
		System.out.println("Resetting group");
		result = group.reset();
		if(result!=Avahi4JConstants.AVAHI_OK) 
			System.out.println("Error resetting group: "
					+Avahi4JConstants.getErrorString(result));
	}
	
	public void stop(){
		group.release();
		client.stop();
		client.release();
	}

	@Override
	public void clientStateChanged(Client.State state) {
		System.out.println("client's new state: " + state);
	}
	
	@Override
	public void groupStateChanged(State newState) {
		System.out.println("Group's new state: " + newState);
	}

	
	public static void main(String args[]) throws Exception{
		TestClient t = new TestClient();
		System.out.println("Press <Enter>");
		System.in.read();
		t.addService();
		System.out.println("Press <Enter>");
		System.in.read();
		t.updateService();
		System.out.println("Press <Enter>");
		System.in.read();
		t.stop();
	}	
}

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

import java.io.IOException;

import avahi4j.Address;
import avahi4j.Avahi4JConstants;
import avahi4j.Client;
import avahi4j.IClientCallback;
import avahi4j.IServiceBrowserCallback;
import avahi4j.IServiceResolverCallback;
import avahi4j.ServiceBrowser;
import avahi4j.ServiceResolver;
import avahi4j.Avahi4JConstants.BrowserEvent;
import avahi4j.Avahi4JConstants.Protocol;
import avahi4j.Client.State;
import avahi4j.ServiceResolver.ServiceResolverEvent;
import avahi4j.exceptions.Avahi4JException;

public class TestServiceBrowser implements IClientCallback, IServiceBrowserCallback, IServiceResolverCallback {
	private Client client;
	private ServiceBrowser browser;
	
	public TestServiceBrowser() throws Avahi4JException {
		client = new Client(this);
		client.start();
	}
	
	public void browse() throws Avahi4JException {
		browser = client.createServiceBrowser(this, Avahi4JConstants.AnyInterface,
				Protocol.ANY , "_test._tcp", null, 0);
	}
	
	public void stop() {
		browser.release();
		client.stop();
		client.release();
	}

	@Override
	public void clientStateChanged(State state) {
		System.out.println("Client state changed to "+state);
	}

	@Override
	public void serviceCallback(int interfaceNum, Protocol proto,
			BrowserEvent browserEvent, String name, String type,
			String domain, int lookupResultFlag) {
		
		// print event type
		System.out.println(" ****** Service browser event: "+browserEvent);
		
		if(browserEvent==BrowserEvent.NEW || browserEvent==BrowserEvent.REMOVE){
			
			// print service details
			System.out.println("Interface: "+interfaceNum + "\nProtocol :"
					+ proto +"\nEvent: " + browserEvent + "\nName: "+name+ "\nType:"
					+ type+ "\nDomain: "+domain+ "\nFlags: " 
					+ Avahi4JConstants.lookupResultToString(lookupResultFlag)
					+ "\n");
			
			// only if it's a new service, resolve the hostname and print
			// TXT records
			if(browserEvent==BrowserEvent.NEW){
				try {
					// we discard the returned reference to the resolver
					// it will be freed when the callback is received 
					client.createServiceResolver(this, interfaceNum, proto, name, type, 
							domain, Protocol.ANY, 0);
				} catch (Avahi4JException e) {
					System.out.println("error creating resolver");
					e.printStackTrace();
				}
			}			
		}
	}

	@Override
	public void resolverCallback(ServiceResolver resolver, int interfaceNum, 
			Protocol proto,	ServiceResolverEvent resolverEvent, String name, 
			String type, String domain, String hostname, Address address, 
			int port, String txtRecords[], int lookupResultFlag) {

		// print resolved name details
		if(resolverEvent==ServiceResolverEvent.RESOLVER_FOUND) {
			System.out.println(" ******  Service RESOLVED:\nInterface: "
					+ interfaceNum + "\nProtocol :"	+ proto + "\nName: " + name 
					+ "\nType: " + type+ "\nHostname: "+ hostname +"\nDomain: "
					+ domain+ "\nAddress: " + address + "\nFlags: " 
					+ Avahi4JConstants.lookupResultToString(lookupResultFlag)
					+ "\nTXT records:");
			for(String s: txtRecords)
				System.out.println(s);
		} else {
			System.out.println("Unable to resolve name");
		}
		
		// release 
		resolver.release();
	}

	public static void main(String args[]) throws Avahi4JException, IOException{
		TestServiceBrowser b = new TestServiceBrowser();
		b.browse();
		System.out.println("Press <Enter>");
		System.in.read();
		b.stop();
	}
}

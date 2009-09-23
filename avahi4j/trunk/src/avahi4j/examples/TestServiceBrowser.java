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
import java.util.List;

import avahi4j.Address;
import avahi4j.Avahi4JConstants;
import avahi4j.Client;
import avahi4j.IClientCallback;
import avahi4j.IServiceBrowserCallback;
import avahi4j.IServiceResolverCallback;
import avahi4j.ServiceBrowser;
import avahi4j.ServiceResolver;
import avahi4j.Avahi4JConstants.Protocol;
import avahi4j.Client.State;
import avahi4j.ServiceBrowser.ServiceBrowserEvent;
import avahi4j.ServiceResolver.ServiceResolverEvent;
import avahi4j.exceptions.Avahi4JException;

public class TestServiceBrowser implements IClientCallback, IServiceBrowserCallback, IServiceResolverCallback {
	private Client client;
	private ServiceBrowser browser;
	private ServiceResolver resolver;
	
	public TestServiceBrowser() throws Avahi4JException {
		client = new Client(this);
		client.start();
		resolver = null;
	}
	
	public void browse() throws Avahi4JException {
		browser = client.createServiceBrowser(this, Avahi4JConstants.AnyInterface,
				Protocol.ANY , "_test._tcp", null, 0);
	}
	
	public void stop() {
		if(resolver!=null)
			resolver.release();
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
			ServiceBrowserEvent browserEvent, String name, String type,
			String domain, int lookupResultFlag) {
		
		if(browserEvent==ServiceBrowserEvent.FAILURE){
			System.out.println("Failure to browse");
		} else if(browserEvent==ServiceBrowserEvent.NEW){
			System.out.println(" ****** Service ADDED:\nInterface: "+interfaceNum + "\nProtocol :"
					+ proto +"\nEvent: " + browserEvent + "\nName: "+name+ "\nType:"
					+ type+ "\nDomain: "+domain+ "\nFlags: " 
					+ Avahi4JConstants.lookupResultToString(lookupResultFlag)
					+ "\n");
			
			try {
				resolver = client.createServiceResolver(this, interfaceNum, proto, name, type, 
						domain, Protocol.ANY, 0);
			} catch (Avahi4JException e) {
				System.out.println("error creating resolver");
				e.printStackTrace();
				resolver = null;
			}
		} else if(browserEvent==ServiceBrowserEvent.CACHE_EXHAUSTED ) {
			System.out.println(" ****** Cache exhausted");
		} else if(browserEvent==ServiceBrowserEvent.NO_MORE) {
			System.out.println(" ****** No more entries");
		} else // browserEvent=ServiceBrowserEvent.REMOVED
		{
			System.out.println(" ****** Service REMOVED:\nInterface: "
					+ interfaceNum + "\nProtocol :"
					+ proto +"\nEvent: " + browserEvent + "\nName: "+name
					+ "\nType:"	+ type+ "\nDomain: "+domain+ "\nFlags: "
					+ Avahi4JConstants.lookupResultToString(lookupResultFlag)
					+"\n");
			
		}
	}

	@Override
	public void resolverCallback(int interfaceNum, Protocol proto,
			ServiceResolverEvent resolverEvent, String name, String type,
			String domain, String hostname, Address address, int port,
			List<String> txtRecords, int lookupResultFlag) {

		if(resolverEvent==ServiceResolverEvent.RESOLVER_FOUND) {
			System.out.println(" ******  Service RESOLVED:\nInterface: "
					+ interfaceNum + "\nProtocol :"	+ proto +"\nEvent: "
					+ resolverEvent + "\nName: "+name+ "\nType: "
					+ type+ "\nDomain: "+domain+ "\nAddress: "+ address 
					+ "\nFlags: "
					+ Avahi4JConstants.lookupResultToString(lookupResultFlag)
					+ "\n");
		} else {
			System.out.println("Failure to resolve name");
		}
	}
	
	public static void main(String args[]) throws Avahi4JException, IOException{
		TestServiceBrowser b = new TestServiceBrowser();
		System.in.read();
		b.browse();
		System.in.read();
		b.stop();
	}
}

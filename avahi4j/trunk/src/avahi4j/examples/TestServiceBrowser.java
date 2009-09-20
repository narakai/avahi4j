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

import avahi4j.Avahi4JConstants;
import avahi4j.Client;
import avahi4j.IClientCallback;
import avahi4j.IServiceBrowserCallback;
import avahi4j.ServiceBrowser;
import avahi4j.Avahi4JConstants.Protocol;
import avahi4j.Client.State;
import avahi4j.ServiceBrowser.ServiceBrowserEvent;
import avahi4j.exceptions.Avahi4JException;

public class TestServiceBrowser implements IClientCallback, IServiceBrowserCallback {
	private Client client;
	private ServiceBrowser browser;
	
	public TestServiceBrowser() throws Avahi4JException {
		client = new Client(this);
		client.start();
	}
	
	public void browse() throws Avahi4JException {
		browser = client.createServiceBrowser(this, Avahi4JConstants.AnyInterface,
				Protocol.ANY , "_daap._tcp", null, 0);
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
			ServiceBrowserEvent browserEvent, String name, String type,
			String domain, int lookupResultFlag) {
		System.out.println("Service: interface: "+interfaceNum + " protocol :"
				+ proto +" event: " + browserEvent + " name: "+name+ " type:"
				+ type+ " domain: "+domain+ " flags: "+
				Avahi4JConstants.lookupResultToString(lookupResultFlag));
	}
	
	public static void main(String args[]) throws Avahi4JException, IOException{
		TestServiceBrowser b = new TestServiceBrowser();
		System.in.read();
		b.browse();
		System.in.read();
		b.stop();
	}
}

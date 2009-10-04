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

import avahi4j.Avahi4JConstants.DNS_Class;
import avahi4j.Avahi4JConstants.DNS_RRType;
import avahi4j.Avahi4JConstants.Protocol;
import avahi4j.ServiceResolver.ServiceResolverEvent;
import avahi4j.examples.TestServiceBrowser;
import avahi4j.examples.TestServicePublish;
import avahi4j.exceptions.Avahi4JException;

/**
 * This class is the entry point of Avahi4J. A client can be used to:
 * <ul>
 * <li>publish new services,</li>
 * <li>browse the network for specific services and</li>
 * <li>retrieve client details (hostname, domain name, FQDN and state).
 * </ul>
 * A client MUST be released when done by calling {@link #release()}.
 * <br>
 * <h2>Instantiation</h2>
 * A client can be created using either of the constructors. If a 
 * {@link IClientCallback} object is provided to the constructor, the callback
 * object will be notified when the client's {@link State} changes.
 * <h2>Starting the client</h2>
 * After instantiation, a client must be started by calling {@link #start()}, 
 * before either registering new services or browsing for existing ones. Calling
 * any of the set/get methods can be done with a client started or stopped however.
 * <h2>Registering a new service</h2>
 * With a client started, create an {@link EntryGroup} using 
 * {@link #createEntryGroup(IEntryGroupCallback)} or 
 * {@link #createEntryGroup(IEntryGroupCallback)}. Add as many services as required
 * using {@link EntryGroup#addService(int, avahi4j.Avahi4JConstants.Protocol, String, String, String, String, int, java.util.List) addService()}
 * and publish them using {@link EntryGroup#commit()}. Make sure you also release
 * the entry group when no longer needed. See {@link EntryGroup} and the 
 * {@link TestServicePublish} sample application for more information.
 * <h2>Browsing for existing services</h2>
 * With a client started, create a {@link ServiceBrowser} by calling
 * {@link #createServiceBrowser(IServiceBrowserCallback, int, avahi4j.Avahi4JConstants.Protocol, String, String, int) createServiceBrowser()}.
 * The provided callback object will be called every time an event occurs on a 
 * matching service (service added, removed, error, ...). Again, make sure you 
 * release service browser objects (by calling {@link ServiceBrowser#release()}
 * when no longer needed. See {@link ServiceBrowser} and the 
 * {@link TestServiceBrowser} sample application for more information.
 * <h2>Stopping the client</h2>
 * You can stop the client by calling {@link #stop()}. After that, no more
 * callbacks to existing service browsers/entry groups/the client itself will be
 * made.
 * <h2>Releasing the client</h2>
 * <b>Once the client is no longer needed, it must be released by calling 
 * {@link #release()}. Existing {@link ServiceBrowser}, {@link EntryGroup} and
 * {@link ServiceResolver} object must be released PRIOR to releasing the client.</b>
 * @author gilles
 *
 */
public class Client {
	
	// try and load the JNI library
	static {
		try {
			System.loadLibrary("avahi4j");
		} catch (Throwable t) {
			System.out.println("Error loading the Avahi4J JNI library.");
			System.out.println("Make sure you have specified the right directory"
					+ " where the library can be found by passing;\n"
					+ " -Djava.library.path=/path/to/jni_lib/dir"
					+ " to the JVM. Currently, this is set to:\n"
					+ System.getProperty("java.library.path")+"\n");
			t.printStackTrace();
			// throw RunTimeError
			throw new Error("Error loading JNI library.", t);
		}
		
		String version = getVersion();
		if(version!=null)
			System.out.println("Avahi4J v"+version);
	}
	
	/**
	 * This enumeration lists the possible states of a client.
	 * @author gilles
	 */
	public static enum State {
		/**
		 * Server state: REGISTERING. 
		 */
		REGISTERING,
		/**
		 * Server state: RUNNING. 
		 */
		RUNNING,
		/**
		 * Server state: COLLISION. 
		 */
		COLLISION,
		/**
		 * Some kind of error happened on the client side. 
		 */
		FAILURE,
		/**
		 * We're still connecting.
		 */
		CONNECTING
	}

	
	/*
	 * M E M B E R S
	 */
	private IClientCallback clientCallback;
	private boolean 		pollLoopStarted;
	private long			avahi4j_client_ptr;
	
	
	/*
	 * N A T I V E   M E T H O D S
	 */
	/**
	 * This method initialises the client
	 * @throws Avahi4JException if there is an error initialising the client
	 */
	private native long initClient() throws Avahi4JException;
	/**
	 * This method return this client's domain name 
	 * @return this client's domain name
	 */
	private native String get_domain_name(long o);
	/**
	 * This method return this client's host name 
	 * @return this client's host name
	 */
	private native String get_host_name(long o);
	/**
	 * This method return this client's fqdn
	 * @return this client's fqdn
	 */
	private native String get_fqdn(long o);
	/**
	 * This method stes this client's host name
	 * @param name the new client's host name
	 * @return avahi_client_set_host_name() raw result
	 */
	private native int set_host_name(long o, String name);
	/**
	 * This method return this client's state as an int
	 * @return this client's state
	 */
	private native int get_state(long o);
	/**
	 * This method starts the threaded poll loop
	 * @return the raw result from avahi 
	 */
	private native int startLoop(long o);
	/**
	 * This method stops the threaded poll lopp
	 * @return the raw result from avahi
	 */
	private native int stopLoop(long o);
	/**
	 * This method releases the client
	 */
	private native void releaseClient(long o);
	/**
	 * This method returns the Avahi4J version
	 * @return the Avahi4J version
	 */
	private static native String getVersion();
	
	
	/*
	 * M E T H O D S
	 */
	/**
	 * This methods builds a new client.
	 * @param callback the callback object that will receive state change
	 * notifications, can be null (notifications will be ignored) 
	 * @throws Avahi4JException if there is an error creating the client
	 */
	public Client(IClientCallback callback) throws Avahi4JException{
		clientCallback = callback;
		pollLoopStarted = false;
		
		avahi4j_client_ptr = initClient();
	}
	
	/**
	 * This method builds a new client, equivalent to a call to 
	 * {@link #Client(IClientCallback)} with a null argument.
	 * @throws Avahi4JException if there is an error creating the client
	 */
	public Client() throws Avahi4JException {
		this(null);
	}
	
	/**
	 * This method starts the client.
	 * @return AVAHI_OK or one of AVAHI_ERR_* constants (see {@link Avahi4JConstants})
	 */
	public synchronized int start()
	{
		int result = Avahi4JConstants.AVAHI_OK;
		
		// if the poll loop is not started, start it
		if (!pollLoopStarted) {
			result = startLoop(avahi4j_client_ptr); 
			if (result==0)
				pollLoopStarted = true;
		}
		
		return result;
	}
	
	/**
	 * This method set the new host name.
	 * @param name the new host name
	 * @return AVAHI_OK or one of AVAHI_ERR_* constants (see {@link Avahi4JConstants})
	 */
	public synchronized int setHostName(String name) {
		return set_host_name(avahi4j_client_ptr, name);
	}
	
	/**
	 * This method returns the current host name
	 * @return the current host name
	 */
	public synchronized String getHostName() {
		return get_host_name(avahi4j_client_ptr);
	}
	
	/**
	 * This method returns the current domain name
	 * @return the current domain name
	 */
	public synchronized String getDomainName() {
		return get_domain_name(avahi4j_client_ptr);
	}
	
	/**
	 * This method returns the current fully-qualified domain name
	 * @return the current fully-qualified domain name
	 */
	public synchronized String getFQDN() {
		return get_fqdn(avahi4j_client_ptr);
	}
	
	/**
	 * This method returns this client's current {@link State}.
	 * @return this client's current {@link State}.
	 */
	public synchronized State getState() {
		return State.values()[get_state(avahi4j_client_ptr)];
	}
	
	/**
	 * This method stops this client
	 * @return AVAHI_OK or one of AVAHI_ERR_* constants (see {@link Avahi4JConstants})
	 */
	public synchronized int stop()
	{
		int result = Avahi4JConstants.AVAHI_ERR_BAD_STATE;
		
		// if the poll loop is started, stop it
		if (pollLoopStarted) {
			result = stopLoop(avahi4j_client_ptr);
			// somehow avahi_threaded_poll_stop() can return value >=0...
			if (result>=0)
				pollLoopStarted = false;
		}
		
		return result;
	}
	
	/**
	 * This method must be called when the client is no longer needed.
	 */
	public synchronized void release()
	{
		//make sure we are stopped
		stop();
		
		releaseClient(avahi4j_client_ptr);
	}
	
	/*
	 * F A C T O R Y   M E T H O D S
	 */
	/**
	 * This method create a new {@link EntryGroup} which can be used to register
	 * new services.
	 * @param callback a callback object which will receive group state change
	 * notifications
	 * @return an empty {@link EntryGroup}
	 * @throws {@link Avahi4JException} if there is an error creating the new group
	 */
	public synchronized EntryGroup createEntryGroup(IEntryGroupCallback callback)
				throws Avahi4JException{
		return new EntryGroup(callback, avahi4j_client_ptr);
	}
	
	/**
	 * This method create a new {@link EntryGroup} which can be used to register
	 * new services.
	 * @return an empty {@link EntryGroup}
	 * @throws {@link Avahi4JException} if there is an error creating the new group
	 */
	public synchronized EntryGroup createEntryGroup() throws Avahi4JException{
		return createEntryGroup(null);
	}
	
	/**
	 * This method create a new {@link ServiceBrowser} object used to look for
	 * services of a given type.
	 * @param callback the object which will receive notification of matching 
	 * services
	 * @param interfaceNum the interface number to be used for browsing, or
	 * {@link Avahi4JConstants#AnyInterface} to browse on all available interfaces
	 * @param proto the {@link Protocol} to use for browsing (in most cases, you
	 * want to use {@link Protocol#ANY}). 
	 * @param type the service type to browse (for instance '_workstation._tcp')
	 * @param domain the domain to browse  (set it to null to browse on all domains)
	 * @param lookupFlags lookup flags (See LOOKUP_* in {@link Avahi4JConstants})
	 * @return a service browser object which MUST be released (by calling
	 * {@link ServiceBrowser#release()}) when done.
	 * @throws Avahi4JException if there is an error creating the service browser
	 */
	public synchronized ServiceBrowser createServiceBrowser(
			IServiceBrowserCallback callback, int interfaceNum, Protocol proto, 
			String type, String domain,	int lookupFlags) throws Avahi4JException{
		
		if (callback==null || type==null)
			throw new NullPointerException("neither the callback nor the type can be null");
		
		return new ServiceBrowser(callback, avahi4j_client_ptr, interfaceNum, 
				proto, type, domain, lookupFlags);
	}
	
	/**
	 * This method create a new {@link ServiceResolver} object used to resolve 
	 * services, ie find out the IP address of the server and associated TXT 
	 * records. <b>The returned service resolver can be used to monitor changes
	 * to a given service (for example, a change in the TXT records) as long as
	 * the service resolver is active (ie, not released).</b>
	 * @param ifNum the interface the service is running on. Pass the exact 
	 * interface number as received by a service browser.
	 * @param proto the protocol used by the service. Pass the exact protocol as
	 * received by a service browser.
	 * @param name the name of the service. Pass the exact name as received by a
	 * service browser
	 * @param type the service type. Pass the exact type as received by a 
	 * service browser
	 * @param domain the domain the service belongs to. Pass the exact domain as
	 * received by a service browser
	 * @param addressProtocol the protocol of the address to be resolved
	 * @param lookupFlags lookup flags (See LOOKUP_* in {@link Avahi4JConstants})
	 * @return a service resolver object which MUST be released (by calling
	 * {@link ServiceResolver#release()}) when done. As long as the service 
	 * resolver is active (ie, not released), updates to the service, such as 
	 * changes to the service's TXT records or service removal, will be 
	 * delivered to the resolver.
	 * @throws Avahi4JException if there is an error creating the service resolver
	 */
	public synchronized ServiceResolver createServiceResolver(
			IServiceResolverCallback callback, int ifNum, Protocol proto, 
			String name, String type, String domain, Protocol addressProtocol, 
			int lookupFlags) throws Avahi4JException{
		
		if (callback==null || name==null || type==null)
			throw new NullPointerException("neither the callback nor the type nor the name can be null");
		
		return new ServiceResolver(avahi4j_client_ptr, callback, ifNum, proto, 
				name, type, domain, addressProtocol, lookupFlags);
	}
	
	/**
	 * This method creates  a new {@link RecordBrowser} object which can be used
	 * to query specific records for a host.
	 * @param callback the callback object which will receive the answer to the query
	 * @param interfaceIdx the interface where the query should be issued
	 * @param proto the protocol used by the query
	 * @param name the hostname 
	 * @param clazz the {@link DNS_Class}
	 * @param type the {@link DNS_RRType} to be queried
	 * @param lookupFlags lookup flags (See @link Avahi4JConstants.LOOKUP_*)
	 * @return a record browser object which must be released (by calling 
	 * {@link RecordBrowser#release()} when done.
	 * @throws Avahi4JException if there is an error creating the record browser
	 */
	public synchronized RecordBrowser createRecordBrowser(
			IRecordBrowserCallback callback, int interfaceIdx, Protocol proto, 
			String name, DNS_Class clazz, DNS_RRType type, int lookupFlags) 
				throws Avahi4JException{
		
		if (callback==null || name==null)
			throw new NullPointerException("neither the callback nor the name can be null");
		
		return new RecordBrowser(avahi4j_client_ptr, callback, interfaceIdx, proto, 
				name, clazz, type, lookupFlags);
	}
	
	/**
	 * This method resolves a service, blocks until the result is received and
	 * returns it.
	 * @param ifNum the interface the service is running on. Pass the exact 
	 * interface number as received by a service browser.
	 * @param proto the protocol used by the service. Pass the exact protocol as
	 * received by a service browser.
	 * @param name the name of the service. Pass the exact name as received by a
	 * service browser
	 * @param type the service type. Pass the exact type as received by a 
	 * service browser
	 * @param domain the domain the service belongs to. Pass the exact domain as
	 * received by a service browser
	 * @param addressProtocol the protocol of the address to be looked up
	 * @param lookupFlags lookup flags (See Avahi4JConstants.LOOKUP_* in 
	 * {@link Avahi4JConstants})
	 * @return a ResolvedService object containing the details of the resolve service.
	 * @throws Avahi4JException if there is an error resolving the service
	 */
	public static ResolvedService resolveService(int ifNum, Protocol proto, 
			String name, String type, String domain, Protocol addressProtocol, 
			int lookupFlags) throws Avahi4JException{
		
		Client client = null;
		final ResolvedService result = new ResolvedService();
		
		try {
			// create a new client and start it
			client = new Client();
			client.start();
			
			// create a service resolver and wait on the result. The callback
			// object will wake us up
			synchronized(result) {
				client.createServiceResolver(new IServiceResolverCallback() {
					
					@Override
					public void resolverCallback(ServiceResolver resolver, int interfaceNum,
							Protocol proto, ServiceResolverEvent resolverEvent, String name,
							String type, String domain, String hostname, Address address,
							int port, String[] txtRecords, int lookupResultFlag) {
						
						result.address = address;
						result.domain = domain;
						result.hostname = hostname;
						result.interfaceNum = interfaceNum;
						result.lookupResultFlag = lookupResultFlag;
						result.name = name;
						result.port = port;
						result.proto = proto;
						result.resolverEvent = resolverEvent;
						result.txtRecords = txtRecords;
						result.type = type;
						
						synchronized(result){
							// wake up waiting thread
							result.notify();
						}
						
						// discard resolver
						resolver.release();
					}
				}, ifNum, proto, name, type, domain, addressProtocol, lookupFlags);	
				result.wait();
			}
			
		} catch (Throwable t){
			if (client!=null)
				client.release();

			throw new Avahi4JException("Error resolving the service", t);
		}

		// stop and release the client
		client.release();
		
		return result;
	}
	
	/**
	 * Called from JNI context when a callback is received
	 * and needs to be dispatched to the registered {@link IClientCallback}
	 * @param newState the new client state
	 */
	@SuppressWarnings("unused")
	private void dispatchCallback(int newState){
		if(clientCallback!=null)
			clientCallback.clientStateChanged(Client.State.values()[newState]);
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ (int) (avahi4j_client_ptr ^ (avahi4j_client_ptr >>> 32));
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Client))
			return false;
		Client other = (Client) obj;
		if (avahi4j_client_ptr != other.avahi4j_client_ptr)
			return false;
		return true;
	}
}

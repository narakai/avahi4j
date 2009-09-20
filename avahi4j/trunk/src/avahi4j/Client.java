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

import avahi4j.exceptions.Avahi4JException;

public class Client {
	/**
	 * The state of the client
	 * @author gilles
	 *
	 */
	public static enum State {
		REGISTERING,
		RUNNING,
		COLLISION,
		FAILURE,
		CONNECTING
	}

	private IClientCallback clientCallback;
	private boolean 		pollLoopStarted;
	private long			avahi4j_client_ptr;
	
	static {
		System.loadLibrary("avahi4j");
	}
	
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
	 * This methods builds a new client
	 * @param callback the callback object that will receive state change
	 * notifications
	 * @throws Avahi4JException if there is an error creating the client
	 */
	public Client(IClientCallback callback) throws Avahi4JException{
		clientCallback = callback;
		pollLoopStarted = false;
		
		avahi4j_client_ptr = initClient();
	}
	
	/**
	 * This method builds a new client
	 * @throws Avahi4JException if there is an error creating the client
	 */
	public Client() throws Avahi4JException {
		this(null);
	}
	
	/**
	 * This method starts the client
	 * @return AVAHI_OK or one of AVAHI_ERR_* constants (see {@link Avahi4JConstants})
	 */
	public synchronized int start()
	{
		int result = Avahi4JConstants.AVAHI_ERR_BAD_STATE;
		
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
	public int setHostName(String name) {
		return set_host_name(avahi4j_client_ptr, name);
	}
	
	/**
	 * This method returns the current host name
	 * @return the current host name
	 */
	public String getHostName() {
		return get_host_name(avahi4j_client_ptr);
	}
	
	/**
	 * This method returns the current domain name
	 * @return the current domain name
	 */
	public String getDomainName() {
		return get_domain_name(avahi4j_client_ptr);
	}
	
	/**
	 * This method returns the current fully-qualified domain name
	 * @return the current fully-qualified domain name
	 */
	public String getFQDN() {
		return get_fqdn(avahi4j_client_ptr);
	}
	
	/**
	 * This method returns this client's current {@link State}.
	 * @return this client's current {@link State}.
	 */
	public State getState() {
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
	 * Entry group
	 */
	/**
	 * This method create a new {@link EntryGroup} which can be used to register
	 * new services.
	 * @param callback a callback object which will receive group state change
	 * notifications
	 * @return an empty {@link EntryGroup}
	 * @throw {@link Avahi4JException} if there is an error creating the new group
	 */
	public EntryGroup createEntryGroup(IEntryGroupCallback callback) throws Avahi4JException{
		return new EntryGroup(callback, avahi4j_client_ptr);
	}
	
	/**
	 * This method create a new {@link EntryGroup} which can be used to register
	 * new services.
	 * @return an empty {@link EntryGroup}
	 * @throw {@link Avahi4JException} if there is an error creating the new group
	 */
	public EntryGroup createEntryGroup() throws Avahi4JException{
		return createEntryGroup(null);
	}
	
	/**
	 * Called from JNI context when a callback from avahi is received
	 * and needs to be dispatched to the registered {@link IClientCallback}
	 * @param newState the new state
	 */
	@SuppressWarnings("unused")
	private void dispatchCallback(int newState){
		if(clientCallback!=null)
			clientCallback.clientStateChanged(Client.State.values()[newState]);
	}
}

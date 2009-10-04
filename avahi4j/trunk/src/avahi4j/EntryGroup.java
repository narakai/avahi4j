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

import java.util.List;
import java.util.Vector;

import avahi4j.Avahi4JConstants.Protocol;
import avahi4j.exceptions.Avahi4JException;

/**
 * An entry group is used to publish new services, and to update existing ones.
 * To publish a new service:
 * <ul>
 * <li>Create an entry group by calling {@link Client#createEntryGroup()} on an 
 * existing {@link Client}.</li>
 * <li>Add one or more services by calling 
 * {@link #addService(int, Avahi4JConstants.Protocol, String, String, String, String, int, List) addService()}</li>
 * <li>Commit the groupm which will publish the services, with {@link #commit()}</li>
 * </ul>
 * Published services in a group can be updated by calling
 * {@link #updateService(int, Avahi4JConstants.Protocol, String, String, String, List) updateService()}.
 * Calling {@link #release()} will unpublish all services in the group.
 * 
 * @author gilles
 *
 */
public class EntryGroup {
	/**
	 * This enumeration represents the various states an entry group
	 * can be in.
	 * @author gilles
	 *
	 */
	public enum State {
		/**
		 * When a new group is created, or when it is reset (with {@link EntryGroup#reset()})
		 * , it is automatically in the 'uncommitted' state. In this state, the 
		 * group is empty, and no entries are published. 
		 */
		AVAHI_ENTRY_GROUP_UNCOMMITED,
		/**
		 * A group switches to this state after a call to {@link EntryGroup#commit()},
		 * indicating that entries are currently being registered. If successful,
		 * the group's state will switch to {@link #AVAHI_ENTRY_GROUP_ESTABLISHED}
		 * shortly after.
		 */
		AVAHI_ENTRY_GROUP_REGISTERING,
		/**
		 * This state indicates that entries in a group are currently registered.
		 */
		AVAHI_ENTRY_GROUP_ESTABLISHED,
		/**
		 * A group enters this state if any of its entries collide with existing
		 * entries. No entries have been registered. Collisions happen when an
		 * trying to register a service whose name is already used by an existing
		 * already-registered service. You may use  
		 */
		AVAHI_ENTRY_GROUP_COLLISION,
		/**
		 * This state indicates some kind of failure. It is best to call 
		 * {@link EntryGroup#release()} and create a new one.
		 */
		AVAHI_ENTRY_GROUP_FAILURE 
	};
	
	
	/*
	 * N A T I V E   M E T H O D S
	 */
	/**
	 * This method create the avahi_entry_group
	 * @param o a jni avahi4j_client struct pointer
	 * @return a jni avahi4j_entry_group pointer
	 * @throws Avahi4JException if there is an error creating the group
	 */
	private native long initGroup(long o) throws Avahi4JException;
	/**
	 * This method frees the avahi entry group
	 * @param o a jni avahi4j_entry_group pointer
	 * @return the raw result from avahi_entry_group_free()
	 */
	private native int release(long o);
	/**
	 * This method commit the avahi entry group
	 * @param o a jni avahi4j_entry_group pointer
	 * @return the raw result from avahi_entry_group_commit()
	 */
	private native int commit_group(long o);
	/**
	 * This method gets the current state of the avahi entry group
	 * @param o a jni avahi4j_entry_group pointer
	 * @return an index into the {@link State} enum. 
	 */
	private native int get_state(long o);
	/**
	 * This method reset the avahi entry group
	 * @param o a jni avahi4j_entry_group pointer
	 * @return the raw result from avahi_entry_group_reset()
	 */
	private native int reset_group(long o);
	/**
	 * This method test if the avahi entry group is empty
	 * @param o a jni avahi4j_entry_group pointer
	 * @return the raw result from avahi_entry_group_is_empty()
	 */
	private native int is_group_empty(long o);
	/**
	 * This method adds a new service and its TXT record to this group
	 * @param o a jni avahi4j_entry_group pointer
	 * @param interfaceNum the interface this service shall be announced on
	 * @param proto the protocol this service shall be announced with
	 * @param name the name for this new service (less that 63 characters, UTF8), not null
	 * @param type the type of the new service, can not be null
	 * @param domain the domain to register this service on. If null, the domain
	 * this host belongs to will be used
	 * @param host the host the service resides on. If null, the current hostname
	 * will be used
	 * @param port the port number this service runs on
	 * @param txtRecord an array of TXT records for this service
	 * @param recordNum the number of TXT records in the previous array
	 * @return the raw result from avahi_entry_group_add_service()
	 */
	private native int add_service_txt_records(long o, int interfaceNum, 
			int proto, String name, String type, String domain, 
			String host, int port, String txtRecord[], int recordNum);
	/**
	 * This method test if the avahi entry group is empty
	 * @param o a jni avahi4j_entry_group pointer
	 * @param interfaceNum the interface this service shall be announced on
	 * @param proto the protocol this service shall be announced with
	 * @param name the name for this new service (less that 63 characters, UTF8), not null
	 * @param type the type of the new service, can not be null
	 * @param domain the domain to register this service on. If null, the domain
	 * this host belongs to will be used
	 * @param subtype the new subtype to register for the specified service. not null
	 * @return the raw result from avahi_entry_group_add_service_subtype()
	 */
	private native int add_service_subtype(long o, int interfaceNum, 
			int proto, String name, String type, String domain, 
			String subtype);
	/**
	 * This method update a existing service and its TXT record
	 * @param o a jni avahi4j_entry_group pointer
	 * @param interfaceNum the interface this service shall be announced on
	 * @param proto the protocol this service shall be announced with
	 * @param name the name for this new service (less that 63 characters, UTF8), not null
	 * @param type the type of the new service, can not be null
	 * @param domain the domain to register this service on. If null, the domain
	 * this host belongs to will be used
	 * @param txtRecord an array of TXT records for this service
	 * @param recordNum the number of TXT records in the previous array
	 * @return the raw result from avahi_entry_group_update_service_txt()
	 */
	private native int update_service_txt_records(long o, int interfaceNum, 
			int proto, String name, String type, String domain, 
			String txtRecord[], int recordNum);
	
	/**
	 * This method finds an alternative name for a service
	 * @param collidingName the service name for which there is a collision
	 * @return the fixed-up name
	 */
	private static native String find_alternative_serivce_name(String collidingName);
	
	
	
	/*
	 * M E M B E R S
	 */
	/**
	 * A JNI pointer to a struct avahi4j_group_entry
	 */
	private long avahi4j_group_entry_ptr;
	/**
	 * the group's callback object (may be null)
	 */
	private IEntryGroupCallback groupCallback;

	
	
	
	/*
	 * M E T H O D S
	 */
	/**
	 * This method builds a new entry group. 
	 * @param callback the callback object to receive notification about the entry group
	 * @param o a struct avahi4j_client pointer (can be null)
	 * @throws Avahi4JException if there is a problem creating the entry group
	 */
	EntryGroup(IEntryGroupCallback callback, long avahi4j_client_ptr) throws Avahi4JException{
		groupCallback = callback;
		avahi4j_group_entry_ptr = initGroup(avahi4j_client_ptr);
	}
	
	/**
	 * This method must be called when this group is no longer needed.
	 */
	public synchronized void release() {
		release(avahi4j_group_entry_ptr);
	}
	
	/**
	 * This method commits any services added to this group. If successful, the 
	 * state of this group will switch to {@link State#AVAHI_ENTRY_GROUP_REGISTERING},
	 * and then to {@link State#AVAHI_ENTRY_GROUP_ESTABLISHED} if the service is
	 * successfully registered.
	 * @return AVAHI_OK or one of AVAHI_ERR_* constants (see {@link Avahi4JConstants}).
	 * Note: AVAHI_ERR_COLLISION is returned if there is an existing service with
	 * the same name	 * 
	 */
	public synchronized int commit() {
		return commit_group(avahi4j_group_entry_ptr);	
	}
	
	/**
	 * This method returns the current {@link State} of this group
	 * @return the current {@link State} of this group
	 */
	public synchronized State getState() {
		return State.values()[get_state(avahi4j_group_entry_ptr)];
	}
	
	/**
	 * This method removes any previously registered services. If the group was
	 * committed, services are unregistered, and this group's state switches back
	 * to {@link State#AVAHI_ENTRY_GROUP_UNCOMMITED}.   
	 * @return AVAHI_OK or one of AVAHI_ERR_* constants (see {@link Avahi4JConstants}).
	 */
	public synchronized int reset() {
		return reset_group(avahi4j_group_entry_ptr);
	}
	
	/**
	 * This method tests whether this group has no contents, ie no services have
	 * been added yet.
	 * @return whether this group is empty
	 */
	public synchronized boolean isEmpty() {
		return is_group_empty(avahi4j_group_entry_ptr)>=0;
	}
	
	/**
	 * This method create a new service and adds a list of TXT records string 
	 * to this group.
	 * @param interfaceNum the interface this service shall be announced on
	 * @param proto the protocol this service shall be announced with
	 * @param name the name for this new service (less that 63 characters, UTF8), not null
	 * @param type the type of the new service, can not be null
	 * @param domain the domain to register this service on. If null, the domain
	 * this host belongs to will be used
	 * @param host the host the service resides on. If null, the current hostname
	 * will be used
	 * @param port the port number this service runs on
	 * @param txtRecord the list of TXT records for this service (may be null)
	 * @return AVAHI_OK or one of AVAHI_ERR_* constants (see {@link Avahi4JConstants}).
	 */
	public synchronized int addService(int interfaceNum, Protocol proto, String name,
			String type, String domain, String host, int port, List<String> txtRecord) {
		
		// check name & its length
		if (name==null || name.length()>63)
			return Avahi4JConstants.AVAHI_ERR_INVALID_SERVICE_NAME;
		
		// check service type
		if (type==null)
			return Avahi4JConstants.AVAHI_ERR_INVALID_SERVICE_TYPE;

		// check port numbeer
		if (port<0 || port>65535)
			return Avahi4JConstants.AVAHI_ERR_INVALID_PORT;
		
		// create empty list if null
		if (txtRecord==null)
			txtRecord = new Vector<String>();
		
		int numRecords = txtRecord.size();
		
		return add_service_txt_records(avahi4j_group_entry_ptr, interfaceNum, 
				proto.ordinal(), name, type, domain, host, port,
				txtRecord.toArray(new String[0]), numRecords);
	}
	
	/**
	 * This method adds a subtype to an existing service in this group. The service
	 * identified by interfaceNum, proto, name, type and domain must have been
	 * added (using {@link #addService(int, avahi4j.Avahi4JConstants.Protocol, String, String, String, String, int, List) addService()} )
	 * to this group prior to calling this method.
	 * @param interfaceNum the interface this service shall be announced on
	 * @param proto the protocol this service shall be announced with
	 * @param name the name for this new service (less that 63 characters, UTF8), not null
	 * @param type the type of the new service, can not be null
	 * @param domain the domain to register this service on. If null, the domain
	 * this host belongs to will be used
	 * @param subtype the new subtype to register for the specified service. not null
	 * @return AVAHI_OK or one of AVAHI_ERR_* constants (see {@link Avahi4JConstants}).
	 */
	public synchronized int addServicesubType(int interfaceNum, Protocol proto,
			String name, String type, String domain, String subtype){
		
		// check name & its length
		if (name==null || name.length()>63)
			return Avahi4JConstants.AVAHI_ERR_INVALID_SERVICE_NAME;
		
		// check service type and subtype
		if (type==null || subtype==null)
			return Avahi4JConstants.AVAHI_ERR_INVALID_SERVICE_SUBTYPE;
		
		return add_service_subtype(avahi4j_group_entry_ptr, interfaceNum, 
				proto.ordinal(), name, type, domain, subtype);
	}
	
	/**
	 * This method updates an existing service's list of TXT records string 
	 * to this group. The service identified by interfaceNum, proto, name, type 
	 * and domain must have been added 
	 * (using {@link #addService(int, avahi4j.Avahi4JConstants.Protocol, String, String, String, String, int, List) addService()})
	 * to this group prior to calling this method. Records are updated immediately,
	 * do not call {@link #commit()}.
	 * @param interfaceNum the interface this service shall be announced on
	 * @param proto the protocol this service shall be announced with
	 * @param name the name for this new service (less that 63 characters, UTF8), not null
	 * @param type the type of the new service, can not be null
	 * @param domain the domain to register this service on. If null, the domain
	 * this host belongs to will be used
	 * @param txtRecord the list of TXT records for this service (may be null)
	 * @return AVAHI_OK or one of AVAHI_ERR_* constants (see {@link Avahi4JConstants}).
	 */
	public synchronized int updateService(int interfaceNum, Protocol proto, 
			String name, String type, String domain, List<String> txtRecord) {
		
		// check name & its length
		if (name==null || name.length()>63)
			return Avahi4JConstants.AVAHI_ERR_INVALID_SERVICE_NAME;
		
		// check service type
		if (type==null)
			return Avahi4JConstants.AVAHI_ERR_INVALID_SERVICE_TYPE;
		
		// create empty list if null
		if (txtRecord==null)
			txtRecord = new Vector<String>();
		
		int numRecords = txtRecord.size();
		
		return update_service_txt_records(avahi4j_group_entry_ptr, interfaceNum, 
				proto.ordinal(), name, type, domain, txtRecord.toArray(new String[0]),
				numRecords);
	}
	
	/**
	 * This method can be used to resolve a service name collision. It suggests
	 * a new service name which won't collide with any existing service names.
	 * @param collidingServiceName the service name causing the collision
	 * @return a new service name which won't collide with any existing service 
	 * names.
	 */
	public static String findAlternativeServiceName(String collidingServiceName){
		return find_alternative_serivce_name(collidingServiceName);
	}
	
	
	
	
	/*
	 * C A L L B A C K    M E T H O D
	 */
	/**
	 * This method is invoked from JNI code to dispatch the callback
	 * @param newState the new state of this group entry
	 */
	@SuppressWarnings("unused")
	private void dispatchCallback(int newState){
		if(groupCallback!=null)
			groupCallback.groupStateChanged(State.values()[newState]);
	}
}

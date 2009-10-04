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

import avahi4j.Client.State;

/**
 * Classes implementing this interface can be notified when the state of a 
 * {@link Client} changes.  
 * @author gilles
 *
 */
public interface IClientCallback {
	/**
	 * This method is called when the state of a client changes.
	 * <b>Do not call {@link Client#release()} in this method.</b>
	 * @param state the new state
	 */
	public void clientStateChanged(State state);
}

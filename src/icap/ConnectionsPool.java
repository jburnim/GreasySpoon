/**----------------------------------------------------------------------------
 * GreasySpoon
 * Copyright (C) 2008 Karel Mittig
 *-----------------------------------------------------------------------------
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 *  Please refer to the LICENSE.txt file that comes along with this source file
 *  or to http://www.gnu.org/licenses/gpl.txt for a full version of the license.
 *
 *-----------------------------------------------------------------------------
 * For any comment, question, suggestion, bugfix or code contribution please
 * contact Karel Mittig : karel [dot] mittig [at] gmail [dot] com
 * Created	:	28/08/02
 *----------------------------------------------------------------------------------
 */
package icap;
///////////////////////////////////
//Import
import java.util.*;
import java.util.logging.Level;
import java.lang.reflect.Constructor;
import java.net.*;
import tools.logger.Log;
import icap.core.*;
///////////////////////////////////

/**
 * PoolThreads class allow to manage a thread pool for client connections.
 * Pool is set to nbThreads size, and managed threads must implement a 
 * method assignTask(Socket sock).
 * These threads are used to manage clients connections.
 * @version 1.0
 * @author k.mittig 
 */
//<------------------------------------------------------------------------------------------>
public class ConnectionsPool extends Thread{

    /**Number of threads created in pool*/
    int nbThreads = 20;

    /**Pool, with only sleeping threads (active are removed)*/
    Vector<AbstractService> poolThreads = new Vector<AbstractService>();
    /**Buffer containing waiting commands (here, clients connections requests)*/
    Vector<Socket> waitingCommand = new Vector<Socket>();

//  <------------------------------------------------------------------------------------------>
    /** Initialize an pool of "size" threads
     * @param _server ICAP Server managing the connection pool
     * @param size Pool size (number of possible threads)
     * @param constructor The ICAP service constructor
     */
    public ConnectionsPool(IcapServer _server, int size, Constructor<? extends AbstractService> constructor){
    	super("AdminServer - Thread pool");
        nbThreads = size;
        AbstractService.setPool(this);
        for (int i=0; i<size;i++){
        	try{
        		poolThreads.add(constructor.newInstance(_server,null));
        	}catch (Exception e){
        		e.printStackTrace();
        	}
        }//End for
    }
   
//  <------------------------------------------------------------------------------------------>

//  <------------------------------------------------------------------------------------------>
    /**
     * Disable all threads in the pool
     */
    
    public void disable(){
        for (AbstractService th:poolThreads){
            th.closeConnection();
        }//End for
        for (Socket s:waitingCommand){
            try{
                s.close();
            }catch (Exception e){
            	//nothing to do
            }
        }//End for
    }
    
//  <------------------------------------------------------------------------------------------>

//  <------------------------------------------------------------------------------------------>
    /**
     * Server receive clients connection requests and send them to assignTask method
     * each time. This task is given to one sleeping thread if available, otherwise 
     * stored in a buffer.
     * @param sock The pending socket to proceed
     */
    
    public void assignTask(Socket sock){
        //Log.service("Connection received from "+sock.getInetAddress().getHostAddress());
        try{
            if (poolThreads.size()>0) {
                AbstractService thread = poolThreads.firstElement();
                // remove thread from pool
                poolThreads.remove(thread);
                thread.assignTask(sock);
                // test if thread is waiting
                if (!thread.isAlive()) { // if not, launch it
                    thread.start();
                } else {
                    thread.interrupt(); // if yes, interrupt wait() to start it
                }
            } else { // no more thread available =>put request in the queue
                waitingCommand.add(sock);
            }//Endif
        } catch (Exception e){
            if (Log.finest()) Log.trace(Level.FINEST, e);
        }
    }
//  <------------------------------------------------------------------------------------------>

//  <------------------------------------------------------------------------------------------>
    /** 
     * Each thread call this method when it finishes its task
     * If there is awaiting command in buffer, thread takes it and restart.
     * Otherwise, it put itself in available threads and in sleep mode.
     * @param thread Thread that has finished its work and returns into pool
     * @return a pending socket if any is waiting, null otherwise.
     */
    
    public synchronized Socket restoreInPool(AbstractService thread){
        if (waitingCommand.size()>0){ // there are waiting commands in the queue
            Socket sock = waitingCommand.firstElement();
            waitingCommand.remove(sock);
            thread.assignTask(sock); // proceed first command (FIFO)
            return sock;
        }
        poolThreads.add(thread); // no waiting command=> restore thread in the pool
        return null;
    }//End class restoreInPool
    
//  <------------------------------------------------------------------------------------------>

}
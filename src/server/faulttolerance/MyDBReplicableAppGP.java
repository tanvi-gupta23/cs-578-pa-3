package server.faulttolerance;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

import edu.umass.cs.gigapaxos.PaxosConfig;
import edu.umass.cs.gigapaxos.interfaces.Replicable;
import edu.umass.cs.gigapaxos.interfaces.Request;
import edu.umass.cs.gigapaxos.paxospackets.RequestPacket;
import edu.umass.cs.gigapaxos.paxosutil.LargeCheckpointer;
import edu.umass.cs.nio.interfaces.IntegerPacketType;
import edu.umass.cs.nio.interfaces.NodeConfig;
import edu.umass.cs.nio.nioutils.NIOHeader;
import edu.umass.cs.nio.nioutils.NodeConfigUtils;
import edu.umass.cs.reconfiguration.reconfigurationutils.RequestParseException;
import edu.umass.cs.utils.Config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.RuntimeErrorException;

import org.apache.cassandra.cql3.Json;
import org.json.JSONObject;

/**
 * This class should implement your {@link Replicable} database app if you wish
 * to use Gigapaxos.
 * <p>
 * Make sure that both a single instance of Cassandra is running at the default
 * port on localhost before testing.
 * <p>
 * Tips:
 * <p>
 * 1) No server-server communication is permitted or necessary as you are using
 * gigapaxos for all that.
 * <p>
 * 2) A {@link Replicable} must be agnostic to "myID" as it is a standalone
 * replication-agnostic application that via its {@link Replicable} interface is
 * being replicated by gigapaxos. However, in this assignment, we need myID as
 * each replica uses a different keyspace (because we are pretending different
 * replicas are like different keyspaces), so we use myID only for initiating
 * the connection to the backend data store.
 * <p>
 * 3) This class is never instantiated via a main method. You can have a main
 * method for your own testing purposes but it won't be invoked by any of
 * Grader's tests.
 */
public class MyDBReplicableAppGP implements Replicable {

	/**
	 * Set this value to as small a value with which you can get tests to still
	 * pass. The lower it is, the faster your implementation is. Grader* will
	 * use this value provided it is no greater than its MAX_SLEEP limit.
	 * Faster
	 * is not necessarily better, so don't sweat speed. Focus on safety.
	 */
	public static final int SLEEP = 1000;

	private static Logger logger = Logger.getLogger(MyDBReplicableAppGP.class.getName());

	public static final int MAX_LOG_SIZE = 400;

	protected final String myID;

	final private Session session;
    final private Cluster cluster;

	/**
	 * All Gigapaxos apps must either support a no-args constructor or a
	 * constructor taking a String[] as the only argument. Gigapaxos relies on
	 * adherence to this policy in order to be able to reflectively construct
	 * customer application instances.
	 *
	 * @param args Singleton array whose args[0] specifies the keyspace in the
	 *             backend data store to which this server must connect.
	 *             Optional args[1] and args[2]
	 * @throws IOException
	 */
	public MyDBReplicableAppGP(String[] args) throws IOException {
		// TODO: setup connection to the data store and keyspace
		logger.info("Entered the constructor MyDBReplicableAppGP");
		myID = args[0];
		//logger.info("My name is: " + myID);
		session = (cluster=Cluster.builder().addContactPoint("127.0.0.1")
                .build()).connect(myID);
		logger.log(Level.INFO, "Server {0} added cluster contact point",new Object[]{myID,});
	}

	/**
	 * Refer documentation of {@link Replicable#execute(Request, boolean)} to
	 * understand what the boolean flag means.
	 * <p>
	 * You can assume that all requests will be of type {@link
	 * edu.umass.cs.gigapaxos.paxospackets.RequestPacket}.
	 *
	 * @param request
	 * @param b
	 * @return
	 */
	@Override
	public boolean execute(Request request, boolean b) {
		// TODO: submit request to data store
		logger.info("Entered execute with doNotReplyToClient flag for server: " + myID);
		boolean requestCompleted = false;
		try{
			if(request instanceof RequestPacket) {
				RequestPacket requestPacket = (RequestPacket)request;
				String requestValue = requestPacket.getRequestValue();
				logger.info("the request value is: " + requestValue);
				JSONObject requestJson = new JSONObject(requestValue);
				String query = requestJson.getString("QV");
				logger.info("The query is: " + query);
				
				session.execute(query);

				requestCompleted = true;
			}
			else {
				logger.severe("Invalid Packet type received");
				throw new RuntimeException("Invalid Packet Type received");
			}
		} catch (Exception e)
		{
			logger.severe("Error while executing slot request: " + e.getMessage());
			e.printStackTrace();
		}
		finally {
			logger.info("Exited execute with doNotReplyToClient flag");
		}
		return true;
	}

	/**
	 * Refer documentation of
	 * {@link edu.umass.cs.gigapaxos.interfaces.Application#execute(Request)}
	 *
	 * @param request
	 * @return
	 */
	@Override
	public boolean execute(Request request) {
		// TODO: execute the request by sending it to the data store
		logger.info("Entered execute without doNotReplyToClient flag for server: " + myID);
		boolean requestCompleted = false;
		try{
			if(request instanceof RequestPacket) {
				RequestPacket requestPacket = (RequestPacket)request;
				String requestValue = requestPacket.getRequestValue();
				logger.info("the request value is: " + requestValue);
				JSONObject requestJson = new JSONObject(requestValue);
				String query = requestJson.getString("QV");
				logger.info("The query is: " + query);
				session.execute(query);
				requestCompleted = true;
			}
			else {
				logger.severe("Invalid Packet type received");
				throw new RuntimeException("Invalid Packet Type received");
			}
		} catch (Exception e)
		{
			logger.severe("Error while executing slot request: " + e.getMessage());
			e.printStackTrace();
		}
		finally {
			logger.info("Exited execute without doNotReplyToClient flag");
		}
		return true;
	}

	/**
	 * Refer documentation of {@link Replicable#checkpoint(String)}.
	 *
	 * @param s
	 * @return
	 */
	@Override
	public String checkpoint(String s) {
		logger.info("Entered checkpoint for server: " + myID);
		FileWriter fw = null;
		String filename = "checkpoint_" + myID;
		File checkpointFile = new File(filename);
		try{
			fw = new FileWriter(checkpointFile, false);
			
			//If checkpoint file already exists, truncate it
			if(checkpointFile.exists() && !checkpointFile.isDirectory()) {
				logger.info("File " + filename + " already exists. Truncating it.");
				fw.flush();
			}

			logger.info("Creating checkpoint");
			ResultSet resultSet = session.execute(CassandraCommands.readResultFromTableCmd("grade", myID));
			for (Row row : resultSet) {
				logger.info("Retrieved row in checkpoint is: " + row);
				ArrayList<Integer> events = new ArrayList<Integer>(row.getList("events", Integer.class));
				Integer key = row.getInt("id");
				String eventString = "";
				if(events != null && !events.isEmpty()){
					for(Integer event : events){
						eventString = eventString + Integer.toString(event) + " ";
					}
				} else {
					eventString = "[]";
				}
				logger.info("Checkpointing to File: " + filename);
				String rowString = Integer.toString(key) + ":" + eventString;
				fw.write(rowString + System.getProperty("line.separator"));
			}
			fw.flush();
		} catch(Exception e) {
			logger.severe("Error when creating checkpoint: " + e.getMessage() + "--Retrying...");
			e.printStackTrace();
			checkpoint(s);
		}
		finally {
			try{
				if(fw != null) {
					fw.close();
				}
			} catch(Exception e)
			{
				logger.severe("Error while closing file writer in checkpoint: " + e.getMessage());
			}
			logger.info("Exited checkpoint");
		}
		String handle = LargeCheckpointer.createCheckpointHandle(checkpointFile.getAbsolutePath());
		logger.info("Handle returned by checkpoint in server: " + myID + " is: "+ handle);
		return handle;
	}

	/**
	 * Refer documentation of {@link Replicable#restore(String, String)}
	 *
	 * @param s
	 * @param s1
	 * @return
	 */
	@Override
	public boolean restore(String s, String s1) {
		logger.info("Entered restore for server: " + myID);
		FileReader fr = null;
		BufferedReader br = null;
		//Read and parse the file, ensure table is created and empty, write records from file to the table
		try{
			//Reinitialize if state is empty or null
			if(s1==null || s1.equals(Config.getGlobalString(PaxosConfig.PC.DEFAULT_NAME_INITIAL_STATE)))
			{
				logger.info("No checkpoints to restore to.. Reinitializing..");
				
				//create tables if it does not exist, if it exists then truncate it
				if(!CassandraCommands.checkTableExists(session, "grade", myID))
				{
					CassandraCommands.createEmptyTables(session, myID);
				} else {
					session.execute(CassandraCommands.getClearTableCmd("grade", myID));
				}
				return true;
			}

			logger.info("Restoring state with handle: " + s1);
			String localFileName =  "checkpoint_" + myID;
			String fileName = LargeCheckpointer.restoreCheckpointHandle(s1,localFileName);
			
			//create tables if it does not exist, if it exists then truncate it
			if(!CassandraCommands.checkTableExists(session, "grade", myID))
			{
				CassandraCommands.createEmptyTables(session, myID);
			} else {
				session.execute(CassandraCommands.getClearTableCmd("grade", myID));
			}
			
			logger.info("Reading state from file in restore: " + fileName);
			//restore table from state
			fr = new FileReader(fileName);
			br = new BufferedReader(fr);

			String line = "";

			while((line = br.readLine()) != null) {
				line = line.trim();
				String[] row = line.split(":");
				if(row.length<2) {
					logger.severe("Invalid row read from checkpoint file.");
					throw new RuntimeException("Invalid Row read from checkpoint file in restore: Row[" + row + "]");
				}
				logger.info("ROW[id: " + row[0] + " events: " + row[1]);
				Integer id = Integer.parseInt(row[0]);
				
				//insert new record with id into table
				session.execute(CassandraCommands.insertRecordIntoTableCmd(myID, id, "grade"));

				if(!("[]".equals(row[1]))){
					logger.info("Restoring events for id: " + id + " events: " + row[1]);
					String[] eventsString = row[1].split(" ");
					for(String eventString : eventsString){
						Integer event = Integer.parseInt(eventString);
						session.execute(CassandraCommands.updateRecordOfTableCmd(myID, id, event, "grade"));
					}
				}
			}
		} catch(Exception e) {
			logger.severe("Error when restoring checkpoint: " + e.getMessage());
			e.printStackTrace();
			throw new RuntimeException("Error when restoring checkpoint: " + e.getMessage());
		}
		finally{
			try{
				if(br != null) {
					br.close();
				}
				if(fr != null) {
					fr.close();
				}
			} catch(Exception e)
			{
				logger.severe("Error while closing file readers in restore: " + e.getMessage());
				throw new RuntimeException("Error while closing file readers in restore: " + e.getMessage());
			}
			logger.info("Exited restore");
		}
		return true;

	}


	/**
	 * No request types other than {@link edu.umass.cs.gigapaxos.paxospackets
	 * .RequestPacket will be used by Grader, so you don't need to implement
	 * this method.}
	 *
	 * @param s
	 * @return
	 * @throws RequestParseException
	 */
	@Override
	public Request getRequest(String s) throws RequestParseException {
		return null;
	}

	/**
	 * @return Return all integer packet types used by this application. For an
	 * example of how to define your own IntegerPacketType enum, refer {@link
	 * edu.umass.cs.reconfiguration.examples.AppRequest}. This method does not
	 * need to be implemented because the assignment Grader will only use
	 * {@link
	 * edu.umass.cs.gigapaxos.paxospackets.RequestPacket} packets.
	 */
	@Override
	public Set<IntegerPacketType> getRequestTypes() {
		return new HashSet<IntegerPacketType>();
	}
}

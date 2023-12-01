package server.faulttolerance;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

public class CassandraCommands {

    public static final String DEFAULT_TABLE_NAME = "grade";

    protected static boolean checkTableExists(Session session,String table, String keyspace) {
	ResultSet resultSet = session.execute("select table_name from " +
			"system_schema.tables where keyspace_name='" + keyspace + "'");
	if(!resultSet.isExhausted()){}
	    boolean match = false;
	    for (Row row : resultSet){
		    match = match || row.getString("table_name").equals(table);
        }
        return match;
    }

    protected static void createEmptyTables(Session session, String node) throws InterruptedException {
            // create default table (if not exists)  with node as the keypsace name
            session.execute(getCreateTableWithList(DEFAULT_TABLE_NAME, node));
            session.execute(getClearTableCmd(DEFAULT_TABLE_NAME, node));
    }
    
    protected static void clearTable(Session session, String node) throws InterruptedException {
        // create default table (if not exists)  with node as the keypsace name
        session.execute(getClearTableCmd(DEFAULT_TABLE_NAME, node));
    }

    protected static String getDropTableCmd(String table, String keyspace) {
        return "drop table if exists " + keyspace + "." + table;
    }
    
    protected static String getClearTableCmd(String table, String keyspace) {
        return "truncate " + keyspace + "." + table;
    }
    
    protected static String getCreateTableWithList(String table, String keyspace) {
        return "create table if not exists " + keyspace + "." + table + " (id " +
                "" + "" + "" + "" + "" + "" + "" + "" + "" + "int," + " " +
                "events" + "" + "" + " " + "list<int>, " + "primary " + "" + "key" + " " + "" + "" + "" + "(id)" + ");";
    }
    
    protected static String insertRecordIntoTableCmd(String keyspace,int key, String table) {
        return "insert into " + keyspace + "." + table + " (id, events) values (" + key + ", " +
                "[]);";
    }
    
    protected static String updateRecordOfTableCmd(String keyspace, int key, int event, String table) {
        return "update " + keyspace + "." + table + " SET events=events+[" + event + "] " +
                "where id=" + key + ";";
    }
    
    // reads the entire table, all keys
    protected static String readResultFromTableCmd(String table, String keyspace) {
        return "select * from " + keyspace + "." + table + ";";
    }
}

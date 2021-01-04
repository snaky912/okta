package com.okta.scim.server.mysql;

import com.mysql.cj.jdbc.MysqlDataSource;
import com.okta.scim.util.SCIMUtil;
import com.okta.scim.util.exception.SCIMSerializationException;
import com.okta.scim.util.model.Email;
import com.okta.scim.util.model.Name;
import com.okta.scim.util.model.PhoneNumber;
import com.okta.scim.util.model.PhoneNumber.PhoneNumberType;
import com.okta.scim.util.model.SCIMGroup;
import com.okta.scim.util.model.SCIMGroupQueryResponse;
import com.okta.scim.util.model.SCIMUser;
import com.okta.scim.util.model.SCIMUserQueryResponse;
import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import java.util.Properties;

public class SCIMConnectorUtil {

    private static ObjectMapper mapper = new ObjectMapper();
    private static final Logger LOGGER = LoggerFactory.getLogger(SCIMConnectorUtil.class);
    private static Connection con;
    
    private static MysqlDataSource mySqlDs = null;
    
    private static String recordType;

    
        public SCIMConnectorUtil(String mySQLHost, String mySQLPort, String mySQLLoginName,
        						 String mySQLPassword, String mySQLDatabaseName, String recordType) {
		
        	LOGGER.error("MySQLDataSource Initialization");
            try {
                mySqlDs = new MysqlDataSource();
                mySqlDs.setServerName(mySQLHost);
                mySqlDs.setPort(Integer.parseInt(mySQLPort));
                //mySqlDs.setURL("jdbc:mysql://demo.ct4efbiwhutj.us-east-2.rds.amazonaws.com:3306/demo");
                mySqlDs.setUser(mySQLLoginName);
                mySqlDs.setPassword(mySQLPassword);
                mySqlDs.setDatabaseName(mySQLDatabaseName);
                // caching parms
                mySqlDs.setCacheResultSetMetadata(true);
                this.recordType = recordType;
            }
            catch (SQLException e) {
                e.printStackTrace();
            }
	}
        
     /**
     * Save users to file
     *
     * @param userMap
     * @param usersFilePath
     * @throws java.io.IOException
     */
    public static void saveUsersToFile(Map<String, SCIMUser> userMap, String usersFilePath) throws IOException, SCIMSerializationException {
        SCIMUserQueryResponse allUsers = new SCIMUserQueryResponse();
        allUsers.setTotalResults(userMap.size());
        List<SCIMUser> users = new ArrayList<SCIMUser>();
        for (String key : userMap.keySet()) {
            users.add(userMap.get(key));
        }
        //Set the actual results
        allUsers.setScimUsers(users);
        String usersString = null;
        try {
            usersString = SCIMUtil.marshalSCIMUserQueryResponse(allUsers, false);
        } catch (SCIMSerializationException e) {
            LOGGER.error("Cannot serialize the users [" + allUsers + "]", e);
            throw e;
        }

        writeStringToFile(usersFilePath, usersString);
    }

    /**
     * Read the users from a file into a users map
     *
     * @param userMap
     * @param usersFilePath
     * @throws java.io.IOException
     */
    public static void readUsersFromFile(Map<String, SCIMUser> userMap, String usersFilePath) throws IOException, SCIMSerializationException {
        JsonNode usersNode = parseFile(usersFilePath);
        if (usersNode == null) {
            return;
        }
        Iterator<JsonNode> it = usersNode.iterator();

        while (it.hasNext()) {
            JsonNode userNode = it.next();
            try {
                SCIMUser user = SCIMUtil.unMarshalSCIMUser(userNode.toString());
                userMap.put(user.getId(), user);
            } catch (SCIMSerializationException e) {
                LOGGER.error("Exception in converting the user [" + userNode.toString() + "] into a string", e);
                throw e;
            }
        }
    }

    /**
    * Save users to MySQL
    *
    * @param userMap
    * @throws java.io.IOException
    */
   public static void saveUsersToMySQL(Map<String, SCIMUser> userMap) throws IOException, SCIMSerializationException {
       SCIMUserQueryResponse allUsers = new SCIMUserQueryResponse();
       allUsers.setTotalResults(userMap.size());
       List<SCIMUser> users = new ArrayList<SCIMUser>();
       for (String key : userMap.keySet()) {
           users.add(userMap.get(key));
       }
       //Set the actual results
       allUsers.setScimUsers(users);
       String usersString = null;
       try {
           usersString = SCIMUtil.marshalSCIMUserQueryResponse(allUsers, false);
       } catch (SCIMSerializationException e) {
           LOGGER.error("Cannot serialize the users [" + allUsers + "]", e);
           throw e;
       }

       writeStringToDb(usersString);
   }

   /**
   * Save users to MySQL
   *
   * @param userMap
   * @throws java.io.IOException
   */
  public static void saveUsersToMySQL(SCIMUser user) throws IOException, SCIMSerializationException {
      SCIMUserQueryResponse allUsers = new SCIMUserQueryResponse();
      allUsers.setTotalResults(1);
      List<SCIMUser> users = new ArrayList<SCIMUser>();
      users.add(user);
      //Set the actual results
      allUsers.setScimUsers(users);
      String usersString = null;
      try {
          usersString = SCIMUtil.marshalSCIMUserQueryResponse(allUsers, false);
      } catch (SCIMSerializationException e) {
          LOGGER.error("Cannot serialize the users [" + allUsers + "]", e);
          throw e;
      }

      writeStringToDb(usersString);
  }

   private static void writeStringToDb(String stringToWrite) throws IOException {

	   	try {
	   		LOGGER.error("{SCIMConnectorUtil} JSON to write to MySQL: " + stringToWrite);
		    con = mySqlDs.getConnection();
		    int result = 0;
			String sp = "{call spSave" + recordType + "(?,?)}";
			CallableStatement stmt = con.prepareCall(sp);
			CallableStatement acctStmt = con.prepareCall(sp);
			acctStmt.setString(1, stringToWrite);
			acctStmt.registerOutParameter(2, Types.INTEGER);
			acctStmt.executeQuery();
			if (acctStmt.getInt(2) == 0) {
				LOGGER.error("{SCIMConnectorUtil} Exception with stored procedure");
			}
			con.close();
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			LOGGER.error("Exception with database: " + e1.getLocalizedMessage());
		}  
   }

   /**
     * Read the users from a file into a users map
     *
     * @param userMap
     * @param usersFilePath
     * @throws java.io.IOException
     */
    public static void readUsersFromDb(Map<String, SCIMUser> userMap) throws IOException, SCIMSerializationException {
    	
    	try {
			con = mySqlDs.getConnection();
			String sp = "{call spGetAll" + recordType + "s()}";
			CallableStatement stmt = con.prepareCall(sp);
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				// get user details
				String acctSp = "{call spGet" + recordType + "JSON(?)}";
				CallableStatement acctStmt = con.prepareCall(acctSp);
				acctStmt.setInt(1, rs.getInt(1));
				ResultSet acctRs = acctStmt.executeQuery();
				while (acctRs.next()) {
/*
				SCIMUser user = new SCIMUser();
				
				user.setId(String.valueOf(acctRs.getInt("account_id")));
				Email email = new Email(acctRs.getString("email"), "work", true);
				ArrayList<Email> emails = new ArrayList<Email>();
				emails.add(email);
				user.setEmails(emails);
				user.setUserName(acctRs.getString("username"));
				Name name = new Name(acctRs.getString("first_name") + " " + acctRs.getString("last_name"),
									 acctRs.getString("last_name"), acctRs.getString("first_name"));
				user.setName(name);
				user.setPassword(acctRs.getString("password"));
				
				PhoneNumber phoneNumber = new PhoneNumber(acctRs.getString("mobile_phone"), 
														  PhoneNumberType.MOBILE, true);
				
				ArrayList<PhoneNumber> phoneNumbers = new ArrayList<PhoneNumber>();
				phoneNumbers.add(phoneNumber);
				user.setPhoneNumbers(phoneNumbers);
				*/
				JsonNode usersNode = parseJSONString(acctRs.getString(1));
		        Iterator<JsonNode> it = usersNode.iterator();

		        while (it.hasNext()) {
		            JsonNode userNode = it.next();
		            try {
		                SCIMUser user = SCIMUtil.unMarshalSCIMUser(userNode.toString());
		                userMap.put(user.getId(), user);
		                LOGGER.error("{SCIMConnectorUtil} json parsed: " + userNode.toString());
		            } catch (SCIMSerializationException e) {
		                LOGGER.error("Exception in converting the user [" + userNode.toString() + "] into a string", e);
		                throw e;
		            }
		        }
			}
			}
			con.close();
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			LOGGER.error("Exception with database: " + e1.getLocalizedMessage());
		}    	
    }

    /**
     * Read the groups from a file into a groups map
     *
     * @param groupMap
     * @param groupsFilePath
     * @throws Exception
     */
    public static void readGroupsFromFile(Map<String, SCIMGroup> groupMap, String groupsFilePath) throws Exception {
        JsonNode groupsNode = parseFile(groupsFilePath);
        if (groupsNode == null) {
            return;
        }
        Iterator<JsonNode> it = groupsNode.iterator();

        while (it.hasNext()) {
            JsonNode groupNode = it.next();
            try {
                SCIMGroup group = SCIMUtil.unMarshalSCIMGroup(groupNode.toString());
                groupMap.put(group.getId(), group);
            } catch (SCIMSerializationException e) {
                LOGGER.error("Exception in converting the group [" + groupNode.toString() + "] into a string", e);
                throw e;
            }
        }
    }

    /**
     * Parse a file into a json node
     *
     * @param filePath
     * @return
     * @throws Exception
     */
    private static JsonNode parseFile(String filePath) throws IOException {
        String usersString = readFromFile(filePath);

        if (StringUtils.isEmpty(usersString)) {
            LOGGER.error("Empty string found after parsing the file [" + filePath + "]");
            return null;
        }

        JsonNode node = mapper.readTree(usersString);

        return node.get("Resources");
    }

    private static JsonNode parseJSONString(String jsonString) throws IOException {

        if (StringUtils.isEmpty(jsonString)) {
            LOGGER.error("Empty string found");
            return null;
        }

        JsonNode node = mapper.readTree(jsonString);

        return node.get("Resources");
    }

    private static JsonNode parseTable() {
    	
    	try {
			Connection con = mySqlDs.getConnection();
			Statement stmt = con.createStatement();
			
			ResultSet rs = stmt.executeQuery("SELECT USERNAME, FIRST_NAME, LAST_NAME, EMAIL, MOBILE_PHONE, PASSWORD FROM dbo.ACCOUNTS");
			
			while (rs.next()) {
				JSONArray json = new JSONArray();
			    ResultSetMetaData rsmd = rs.getMetaData();

			    while(rs.next()) {
			      int numColumns = rsmd.getColumnCount();
			      JSONObject obj = new JSONObject();

			      for (int i=1; i<numColumns+1; i++) {
			        String column_name = rsmd.getColumnName(i);

			        if(rsmd.getColumnType(i)==java.sql.Types.ARRAY){
			         obj.put(column_name, rs.getArray(column_name));
			        }
			        else if(rsmd.getColumnType(i)==java.sql.Types.BIGINT){
			         obj.put(column_name, rs.getInt(column_name));
			        }
			        else if(rsmd.getColumnType(i)==java.sql.Types.BOOLEAN){
			         obj.put(column_name, rs.getBoolean(column_name));
			        }
			        else if(rsmd.getColumnType(i)==java.sql.Types.BLOB){
			         obj.put(column_name, rs.getBlob(column_name));
			        }
			        else if(rsmd.getColumnType(i)==java.sql.Types.DOUBLE){
			         obj.put(column_name, rs.getDouble(column_name)); 
			        }
			        else if(rsmd.getColumnType(i)==java.sql.Types.FLOAT){
			         obj.put(column_name, rs.getFloat(column_name));
			        }
			        else if(rsmd.getColumnType(i)==java.sql.Types.INTEGER){
			         obj.put(column_name, rs.getInt(column_name));
			        }
			        else if(rsmd.getColumnType(i)==java.sql.Types.NVARCHAR){
			         obj.put(column_name, rs.getNString(column_name));
			        }
			        else if(rsmd.getColumnType(i)==java.sql.Types.VARCHAR){
			         obj.put(column_name, rs.getString(column_name));
			        }
			        else if(rsmd.getColumnType(i)==java.sql.Types.TINYINT){
			         obj.put(column_name, rs.getInt(column_name));
			        }
			        else if(rsmd.getColumnType(i)==java.sql.Types.SMALLINT){
			         obj.put(column_name, rs.getInt(column_name));
			        }
			        else if(rsmd.getColumnType(i)==java.sql.Types.DATE){
			         obj.put(column_name, rs.getDate(column_name));
			        }
			        else if(rsmd.getColumnType(i)==java.sql.Types.TIMESTAMP){
			        obj.put(column_name, rs.getTimestamp(column_name));   
			        }
			        else{
			         obj.put(column_name, rs.getObject(column_name));
			        }
			      }

			      json.put(obj);
			    }				
			}
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	return null;
    	
    }
    /**
     * Save groups to a file
     *
     * @param groupMap
     * @param groupsFilePath
     * @throws java.io.IOException
     */
    public static void saveGroupsToFile(Map<String, SCIMGroup> groupMap, String groupsFilePath) throws IOException, SCIMSerializationException {
        SCIMGroupQueryResponse allGroups = new SCIMGroupQueryResponse();
        allGroups.setTotalResults(groupMap.size());
        List<SCIMGroup> groups = new ArrayList<SCIMGroup>();
        for (String key : groupMap.keySet()) {
            groups.add(groupMap.get(key));
        }
        //Set the actual results
        allGroups.setScimGroups(groups);
        String groupsString = null;
        try {
            groupsString = SCIMUtil.marshalSCIMGroupQueryResponse(allGroups, false);
        } catch (SCIMSerializationException e) {
            LOGGER.error("Cannot serialize the groups [" + allGroups + "]", e);
        }

        writeStringToFile(groupsFilePath, groupsString);
    }

    private static void writeStringToFile(String filePath, String stringToWrite) throws IOException {
        try {
            FileUtils.writeStringToFile(new File(filePath), stringToWrite);
        } catch (IOException e) {
            LOGGER.error("Cannot write to the file [" + filePath + "]", e);
            throw e;
        }
    }

    private static String readFromFile(String filePath) throws IOException {
        try {
            return FileUtils.readFileToString(new File(filePath));
        } catch (IOException e) {
            LOGGER.error("Cannot read from the file [" + filePath + "]", e);
        }

        return null;
    }
    
}

package org.toad.keycloak.storage.ldap.mappers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.mappers.AbstractLDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.AbstractLDAPStorageMapperFactory;

public class DbGroup2RoleLDAPStorageMapperFactory extends AbstractLDAPStorageMapperFactory {

	private static final Logger logger = Logger.getLogger(DbGroup2RoleLDAPStorageMapperFactory.class);
	
    public static final String PROVIDER_ID = "database-group-to-role-mapper";
	
    protected static final List<ProviderConfigProperty> configProperties;
   
    static {    	
        configProperties = getConfigProps(null);
    }

    private static List<ProviderConfigProperty> getConfigProps(ComponentModel parent) {

        return ProviderConfigurationBuilder.create()
                .property().name(DbGroup2RoleLDAPStorageMapperConfig.DB_ENGINE)
                            .label("Database engine")
                            .helpText("Choose your datrabase engine that store user's group/roles.")
                            .type(ProviderConfigProperty.LIST_TYPE)
                            .options(DbGroup2RoleLDAPStorageMapperConfig.dBEngines)
                            .defaultValue(DbGroup2RoleLDAPStorageMapperConfig.DB_ENGINE_ORACLE)
                            .add()
                .property().name(DbGroup2RoleLDAPStorageMapperConfig.DB_IP_ATTRIBUTE)
                			.label("Database IP or DNS address")
                			.helpText("IP address of the database")
                			.type(ProviderConfigProperty.STRING_TYPE)
                			.defaultValue("MyDatabse_IP_OR_FQDN")
                			.add()
                .property().name(DbGroup2RoleLDAPStorageMapperConfig.DB_PORT_ATTRIBUTE)
                			.label("Database TCP port")
                			.helpText("IP port of the database")
                			.type(ProviderConfigProperty.STRING_TYPE)
                			.defaultValue("1521")
                			.add()
                .property().name(DbGroup2RoleLDAPStorageMapperConfig.DB_DBNAME_ATTRIBUTE)
                			.label("Database name")
                			.helpText("Database name. With H2 local DB, set the database path (for example C:/data/sample)")
                			.type(ProviderConfigProperty.STRING_TYPE)
                			.defaultValue("DBNAME")
                			.add()
                .property().name(DbGroup2RoleLDAPStorageMapperConfig.DB_LOGIN_ATTRIBUTE)
                			.label("Database account login")
                			.helpText("Database username that will be used to query the database. this user must have READ access on the requested tables")
                			.type(ProviderConfigProperty.STRING_TYPE)
                			.defaultValue("DBLOGIN")
                			.add()
                .property().name(DbGroup2RoleLDAPStorageMapperConfig.DB_PASSWD_ATTRIBUTE)
                			.label("Database account password")
                			.helpText("Password account that will be used to query the Database")
                			.type(ProviderConfigProperty.PASSWORD)
                			.secret(true)
                			.defaultValue("DBPASSWORD")
                			.add()
                .property().name(DbGroup2RoleLDAPStorageMapperConfig.DB_SQL_QUERY_4_ROLES)
                			.label("SQL to get roles/groups")
                			.helpText("This query will load roles/groups in the database, \n Note that only one ? is mandatory, and will be replaced by the ad user's login")
                			.type(ProviderConfigProperty.STRING_TYPE)
                			.defaultValue(DbGroup2RoleLDAPStorageMapperConfig.SQL_QUERY_DEFAULT)
                			.add()
                .property().name(DbGroup2RoleLDAPStorageMapperConfig.SQL_QUERY_CACHE_TTL)
    			            .label("SQL query cache TTL (s)")
    			            .helpText("Configure the database cache in seconds")
    			            .type(ProviderConfigProperty.STRING_TYPE)
    			            .defaultValue("360")
    			            .add()
                .property().name(DbGroup2RoleLDAPStorageMapperConfig.USE_REALM_ROLES_MAPPING)
                            .label("Use Realm Roles Mapping")
                            .helpText("If true, then LDAP role mappings will be mapped to realm role mappings in Keycloak. Otherwise it will be mapped to client role mappings")
                            .type(ProviderConfigProperty.BOOLEAN_TYPE)
                            .defaultValue("true")
                            .add()
                .property().name(DbGroup2RoleLDAPStorageMapperConfig.DELETE_REALMS_IF_NOT_IN_RDB)
                            .label("Sync Keycloak roles with RDB")
                            .helpText("If true, then Keycloak roles are removed if group in SGBD is removed")
                            .type(ProviderConfigProperty.BOOLEAN_TYPE)
                            .defaultValue("true")
                            .add()
                .property().name(DbGroup2RoleLDAPStorageMapperConfig.CLIENT_ID)
                            .label("Client ID")
                            .helpText("Client ID of client to which LDAP role mappings will be mapped. Applicable just if 'Use Realm Roles Mapping' is false")
                            .type(ProviderConfigProperty.CLIENT_LIST_TYPE)
                            .add()
                .property().name(DbGroup2RoleLDAPStorageMapperConfig.DB_POOL_MIN_IDLE)
                            .label("DB Pool : Min Idle")
                            .helpText("Sets the minimum number of idle connections in the pool")
                            .type(ProviderConfigProperty.STRING_TYPE)
                			.defaultValue(GenericObjectPool.DEFAULT_MIN_IDLE)
                            .add()
                .property().name(DbGroup2RoleLDAPStorageMapperConfig.DB_POOL_MAX_IDLE)
                            .label("DB Pool : Max Idle")
                            .helpText("Sets the maximum number of connections that can remain idle in the pool.")
                            .type(ProviderConfigProperty.STRING_TYPE)
                			.defaultValue(GenericObjectPool.DEFAULT_MAX_IDLE)
                            .add()
                .property().name(DbGroup2RoleLDAPStorageMapperConfig.DB_POOL_MAX_WAIT)
                            .label("DB Pool : Max Wait")
                            .helpText("The maximum number of milliseconds that the pool will wait(when there are no available connections) for a connection to be returned before throwing an exception, or <= 0 to wait indefinitely.")
                            .type(ProviderConfigProperty.STRING_TYPE)
                			.defaultValue(GenericObjectPool.DEFAULT_MAX_WAIT)
                            .add()
                .property().name(DbGroup2RoleLDAPStorageMapperConfig.DB_POOL_MAX_ACTIVE)
                            .label("DB Pool : Max Active")
                            .helpText("Sets the maximum number of active connections that can be allocated at the same time. Use a negative value for no limit.")
                            .type(ProviderConfigProperty.STRING_TYPE)
                			.defaultValue(GenericObjectPool.DEFAULT_MAX_ACTIVE)
                            .add()
                .property().name(DbGroup2RoleLDAPStorageMapperConfig.DB_POOL_MAX_OPS)
                            .label("DB Pool : Max open prepared statement")
                            .helpText("The maximum number of open statements that can be allocated from the statement pool at the same time, or non-positive for no limit.  Since  a connection usually only uses one or two statements at a time, this is mostly used to help detect resource leaks.")
                            .type(ProviderConfigProperty.STRING_TYPE)
                			.defaultValue(GenericKeyedObjectPool.DEFAULT_MAX_TOTAL)
                            .add()
                         .build();
    }
    
    @Override
    public String getHelpText() {
        return "When user is imported from LDAP or AD, Keycloak will map roles/groups stored in an external Database.";
    }
    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(RealmModel realm, ComponentModel parent) {
        return getConfigProps(parent);
    }
	@Override
	public String getId() {
		return PROVIDER_ID;
	}
	
	@Override
	protected AbstractLDAPStorageMapper createMapper(ComponentModel mapperModel, LDAPStorageProvider federationProvider) {
		return new DbGroup2RoleLDAPStorageMapper(mapperModel, federationProvider);
	}
	
    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel config) throws ComponentValidationException {
    	
    	//Check 
        checkMandatoryConfigAttribute(DbGroup2RoleLDAPStorageMapperConfig.DB_IP_ATTRIBUTE,     "Database server IP address Attribute", config);
        checkMandatoryConfigAttribute(DbGroup2RoleLDAPStorageMapperConfig.DB_PORT_ATTRIBUTE,   "Database server TCP Port Attribute", config);
        checkMandatoryConfigAttribute(DbGroup2RoleLDAPStorageMapperConfig.DB_DBNAME_ATTRIBUTE, "Database name Attribute", config);
        checkMandatoryConfigAttribute(DbGroup2RoleLDAPStorageMapperConfig.DB_LOGIN_ATTRIBUTE,  "Database user Attribute", config);
        checkMandatoryConfigAttribute(DbGroup2RoleLDAPStorageMapperConfig.DB_PASSWD_ATTRIBUTE, "Database password Attribute", config);
        checkMandatoryConfigAttribute(DbGroup2RoleLDAPStorageMapperConfig.DB_SQL_QUERY_4_ROLES,"Database SQL Query", config);

        //Test DB Connection*
        DbGroup2RoleLDAPStorageMapperConfig db_g2r_Config= new DbGroup2RoleLDAPStorageMapperConfig(config);
    	String jdbcURL = db_g2r_Config.getJDBCURL();
    	
        try {
        	String jdbcDriver = db_g2r_Config.getJDBCDriverClass();
        	Class.forName(jdbcDriver);
        } catch (ClassNotFoundException e) {
        	throw new ComponentValidationException("Failed to load "+db_g2r_Config.getRDBEngine()+" JDBC Driver! Check that the jdbc jar is in <KC_dir>/providers",e);
        }
        
        //Getting a JDBC connection
        Connection connection = null;
        try {
        	String dbuserlogin    = db_g2r_Config.getDBUserLogin();
        	String dbuserpassword = db_g2r_Config.getDBUserPassword();
        	
        	connection = DriverManager.getConnection(jdbcURL, dbuserlogin, dbuserpassword);
        	
            if (connection != null) {
            	
            	Statement st = null;
            	ResultSet rs = null;
            	try {
              	  String dbProductName = connection.getMetaData().getDatabaseProductName();
            	  String dbProductVer = connection.getMetaData().getDatabaseProductVersion();

            	  st = connection.createStatement();
            	  rs = st.executeQuery("select 1 from dual");
            	  /*
            	  if ( "PostgreSQL".equalsIgnoreCase(dbProductName) ) {
            	    rs = st.executeQuery("select version();");
            	  } else if ( "Oracle".equalsIgnoreCase(dbProductName) ) {
            	    rs = st.executeQuery("select 1 from dual");
            	  } else {
            	   ...
            	  }
            	  */
            	  //System.out.println("You are logged into the database now!");
            	  logger.infof("Keycloak is logged into the database [%s] [%s] with user [%s]", dbProductName, dbProductVer, dbuserlogin);
            	} catch ( Exception ex ) {
                	throw new ComponentValidationException("Connection established, but test SQL command failed!",ex);
            	} finally {
                	if (connection!=null) {
                		try {
        					connection.close();
        				} catch (SQLException e) {
        					e.printStackTrace();
        				}
                	}
            		rs.close();
            		st.close();
            	}

            } else {
                	throw new ComponentValidationException("Connection to database failed, please verify settings and firewall!");
            }
        } catch (SQLException e) {
        	System.out.println("Connection Failed! Check output console");
        	throw new ComponentValidationException("Connection to database failed, please verify settings and firewall!",e);
        } finally {
        	if (connection!=null) {
        		try {
					connection.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
        	}
		}
    }
}

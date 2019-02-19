package org.toad.keycloak.storage.ldap.mappers;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.dbcp.BasicDataSource;
import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;

public class DbGroup2RoleLDAPStorageMapperConfig {

	private static final Logger logger = Logger.getLogger(DbGroup2RoleLDAPStorageMapperConfig.class);
	
	public static final String DB_ENGINE            = "db.roles.engine";
	
    public static final String DB_ENGINE_ORACLE     = "Oracle";
    public static final String DB_ENGINE_POSTGRESQL = "PostGreSQL";
    public static final String DB_ENGINE_MARIADB    = "MariaDB";
    public static final String DB_ENGINE_MYSQL      = "MySQL";
    public static final String DB_ENGINE_SQLSERVER  = "SQL Server";
    public static final String DB_ENGINE_SYBASE     = "Sybase";
    public static final String DB_ENGINE_H2         = "H2 (Local only)";
	
    public static final String DB_DBNAME_ATTRIBUTE  = "db.roles.dbname.attribute";
    public static final String DB_LOGIN_ATTRIBUTE   = "db.roles.login.attribute";
    public static final String DB_PASSWD_ATTRIBUTE  = "db.roles.password.attribute";
    public static final String DB_IP_ATTRIBUTE      = "db.roles.ip.attribute";
    public static final String DB_PORT_ATTRIBUTE    = "db.roles.port.attribute";
    
    public static final String DB_SQL_QUERY_4_ROLES = "db.roles.sql.query";
    public static final String SQL_QUERY_CACHE_TTL = "db.roles.sql.query.cache.ttl";

    public static final String DB_POOL_MIN_IDLE = "db.pool.min.idle";
    public static final String DB_POOL_MAX_IDLE = "db.pool.max.idle";
    public static final String DB_POOL_MAX_WAIT = "db.pool.max.wait";
    public static final String DB_POOL_MAX_ACTIVE = "db.pool.max.active";
    public static final String DB_POOL_MAX_OPS  = "db.pool.max.open.prepared.statement";
    
    
    // Boolean option. If true, we will map LDAP roles to realm roles. If false, we will map to client roles (client specified by option CLIENT_ID)
    public static final String USE_REALM_ROLES_MAPPING = "db.use.realm.roles.mapping";

    //Delete roles in KC if removed in DB
    public static final String DELETE_REALMS_IF_NOT_IN_RDB = "db.roles.sync.kc.from.db";
    
    // ClientId, which we want to map roles. Applicable just if "USE_REALM_ROLES_MAPPING" is false
    public static final String CLIENT_ID = "db.role.client.id";

    public static final String SQL_QUERY_DEFAULT = "SELECT ROLES FROM MEMBERSHIP Where USER_ID=?";
    
    protected static final List<String> dBEngines;
    protected static final Map<String, String> dBEnginesJDBCDriverList = new LinkedHashMap<>();
    
    protected final ComponentModel mapperModel;
    
    protected BasicDataSource ds = null;

    static {
    	dBEnginesJDBCDriverList.put(DbGroup2RoleLDAPStorageMapperConfig.DB_ENGINE_ORACLE,     "oracle.jdbc.driver.OracleDriver");
    	dBEnginesJDBCDriverList.put(DbGroup2RoleLDAPStorageMapperConfig.DB_ENGINE_POSTGRESQL, "org.postgresql.Driver");
    	dBEnginesJDBCDriverList.put(DbGroup2RoleLDAPStorageMapperConfig.DB_ENGINE_MARIADB,    "org.mariadb.jdbc.Driver");
    	dBEnginesJDBCDriverList.put(DbGroup2RoleLDAPStorageMapperConfig.DB_ENGINE_MYSQL,      "com.mysql.jdbc.Driver");
    	dBEnginesJDBCDriverList.put(DbGroup2RoleLDAPStorageMapperConfig.DB_ENGINE_SQLSERVER,  "net.sourceforge.jtds.jdbc.Driver");
    	dBEnginesJDBCDriverList.put(DbGroup2RoleLDAPStorageMapperConfig.DB_ENGINE_SYBASE,     "net.sourceforge.jtds.jdbc.Driver");
    	dBEnginesJDBCDriverList.put(DbGroup2RoleLDAPStorageMapperConfig.DB_ENGINE_H2,         "org.h2.Driver");
    	
    	dBEngines = new LinkedList<>(dBEnginesJDBCDriverList.keySet());
    	
    }
    
    public DbGroup2RoleLDAPStorageMapperConfig(ComponentModel mapperModel) {
        this.mapperModel = mapperModel;
    }

    public String getRDBEngine() {
        String jdbcDriver = mapperModel.getConfig().getFirst(DB_ENGINE);
        return jdbcDriver!=null ? jdbcDriver : DB_ENGINE_ORACLE;
    }
    
    //TODO : jdbc url depends on db engine
	protected String getJDBCURL() {
		String jdbcURL="";
		
		switch (getRDBEngine()) {
		  case DB_ENGINE_ORACLE:
			   jdbcURL = "jdbc:oracle:thin:@" + mapperModel.getConfig().getFirst(DB_IP_ATTRIBUTE)
	    			+ ":" + mapperModel.getConfig().getFirst(DB_PORT_ATTRIBUTE) 
	    			+ ":" + mapperModel.getConfig().getFirst(DB_DBNAME_ATTRIBUTE);
			break;

		  case DB_ENGINE_MARIADB:
			   //doc : https://github.com/MariaDB/mariadb-connector-j
			   //      https://mariadb.com/kb/en/library/about-mariadb-connector-j/#connection-strings
			   jdbcURL = "jdbc:mariadb://" + mapperModel.getConfig().getFirst(DB_IP_ATTRIBUTE)
	    			+ ":" + mapperModel.getConfig().getFirst(DB_PORT_ATTRIBUTE) 
	    			+ "/" + mapperModel.getConfig().getFirst(DB_DBNAME_ATTRIBUTE);
			break;

		  case DB_ENGINE_MYSQL:
			   jdbcURL = "jdbc:mysql://" + mapperModel.getConfig().getFirst(DB_IP_ATTRIBUTE)
		    			+ ":" + mapperModel.getConfig().getFirst(DB_PORT_ATTRIBUTE) 
		    			+ "/" + mapperModel.getConfig().getFirst(DB_DBNAME_ATTRIBUTE);
			break;
			
		  case DB_ENGINE_POSTGRESQL:
			    //doc : https://jdbc.postgresql.org/documentation/head/connect.html
			   jdbcURL = "jdbc:postgresql://" + mapperModel.getConfig().getFirst(DB_IP_ATTRIBUTE)
	    			+ ":" + mapperModel.getConfig().getFirst(DB_PORT_ATTRIBUTE) 
	    			+ "/" + mapperModel.getConfig().getFirst(DB_DBNAME_ATTRIBUTE);
			break;

		  case DB_ENGINE_SQLSERVER:
				//doc : http://jtds.sourceforge.net/faq.html#urlFormat
			   jdbcURL = "jdbc:jtds:sqlserver://" + mapperModel.getConfig().getFirst(DB_IP_ATTRIBUTE)
	    			+ ":" + mapperModel.getConfig().getFirst(DB_PORT_ATTRIBUTE) 
	    			+ "/" + mapperModel.getConfig().getFirst(DB_DBNAME_ATTRIBUTE);
			break;

		  case DB_ENGINE_SYBASE:
				//doc : http://jtds.sourceforge.net/faq.html#urlFormat
			   jdbcURL = "jdbc:jtds:sybase://" + mapperModel.getConfig().getFirst(DB_IP_ATTRIBUTE)
	    			+ ":" + mapperModel.getConfig().getFirst(DB_PORT_ATTRIBUTE) 
	    			+ "/" + mapperModel.getConfig().getFirst(DB_DBNAME_ATTRIBUTE);
			break;

		  case DB_ENGINE_H2:
				//doc : http://www.h2database.com/html/features.html#database_url
			   jdbcURL = "jdbc:h2:file:" + mapperModel.getConfig().getFirst(DB_DBNAME_ATTRIBUTE);
			break;

		default:
			logger.debugf("LDAP Mapper %s : JDBC Driver URL was not built as DB Engine is not recognized ", mapperModel.getName() );
			break;
		}
		logger.debugf("LDAP Mapper %s : JDBC Driver URL is %s ", mapperModel.getName(), jdbcURL );
		return jdbcURL;
	}

	//SQL Cache TTL in seconds
	protected int getSQLCacheTTL() {
		String sttl = mapperModel.getConfig().getFirst(SQL_QUERY_CACHE_TTL);
		try {
			return new Integer(sttl).intValue();
		} catch (NumberFormatException nfe) {
			nfe.printStackTrace();
			return 60;
		}
	}
	protected String getDBUserLogin() {
		return mapperModel.getConfig().getFirst(DB_LOGIN_ATTRIBUTE);
	}
	
	protected String getDBUserPassword() {
		return mapperModel.getConfig().getFirst(DB_PASSWD_ATTRIBUTE);
	}
	
	protected String getSQLQuery() {
		return mapperModel.getConfig().getFirst(DB_SQL_QUERY_4_ROLES);
	}
	
	//From RoleMapperConfig
    public boolean isRealmRolesMapping() {
        String realmRolesMapping = mapperModel.getConfig().getFirst(USE_REALM_ROLES_MAPPING);
        boolean isRealmRole = realmRolesMapping==null || Boolean.parseBoolean(realmRolesMapping) ;
    	logger.tracef("LDAP Mapper %s : Realm roles mapping : ", mapperModel.getName(), isRealmRole );
        return isRealmRole;
    }
    
	//From RoleMapperConfig
    public boolean isDeletingKCRolesIfRemovedFromRDB() {
        String delRolesMapping = mapperModel.getConfig().getFirst(DELETE_REALMS_IF_NOT_IN_RDB);
    	logger.tracef("LDAP Mapper %s : Deleting roles in Keycloak : ", delRolesMapping!=null && Boolean.parseBoolean(delRolesMapping) );
        return delRolesMapping!=null && Boolean.parseBoolean(delRolesMapping);
    }
    public String getClientId() {
        return mapperModel.getConfig().getFirst(CLIENT_ID);
    }

    //TODO : load correct driver if oracle, mysql, postgresql, ...
	protected Connection getDBConnection() throws SQLException {
		if (ds == null) {
			initDatasource();
		}
		return ds.getConnection();
	}
    
	private void initDatasource() {
        ds = new BasicDataSource();
        ds.setDriverClassName(getJDBCDriverClass());
        ds.setUsername(getDBUserLogin());
        ds.setPassword(getDBUserPassword());
        ds.setUrl(getJDBCURL());
        
        // the settings below are optional -- dbcp can work with defaults
        int minIdle = new Integer(mapperModel.getConfig().getFirst(DB_POOL_MIN_IDLE)).intValue();
        int maxIdle = new Integer(mapperModel.getConfig().getFirst(DB_POOL_MAX_IDLE)).intValue();
        int maxOPS = new Integer(mapperModel.getConfig().getFirst(DB_POOL_MAX_OPS)).intValue();
        int maxWait = new Integer(mapperModel.getConfig().getFirst(DB_POOL_MAX_WAIT)).intValue();
        int maxActive = new Integer(mapperModel.getConfig().getFirst(DB_POOL_MAX_ACTIVE)).intValue();
        ds.setMinIdle(minIdle);
        ds.setMaxIdle(maxIdle);
        ds.setMaxOpenPreparedStatements(maxOPS);
        ds.setMaxWait(maxWait);
        ds.setMaxActive(maxActive);
    	logger.debugf("LDAP Mapper %s : Datasource created", mapperModel.getName() );
	}

    protected String getJDBCDriverClass() {
    	logger.tracef("LDAP Mapper %s : JDBC Driver class : %s", mapperModel.getName(), dBEnginesJDBCDriverList.get(getRDBEngine()) );
        return dBEnginesJDBCDriverList.get(getRDBEngine());
    }
	
}

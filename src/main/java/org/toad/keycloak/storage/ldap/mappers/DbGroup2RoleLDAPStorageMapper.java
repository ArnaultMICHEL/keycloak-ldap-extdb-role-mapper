package org.toad.keycloak.storage.ldap.mappers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.UserModelDelegate;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.idm.query.internal.LDAPQuery;
import org.keycloak.storage.ldap.mappers.AbstractLDAPStorageMapper;

public class DbGroup2RoleLDAPStorageMapper extends AbstractLDAPStorageMapper {

	private static final Logger logger = Logger.getLogger(DbGroup2RoleLDAPStorageMapper.class);
    
	private final DbGroup2RoleLDAPStorageMapperConfig config;
	
	public DbGroup2RoleLDAPStorageMapper(ComponentModel mapperModel, LDAPStorageProvider ldapProvider) {
		super(mapperModel, ldapProvider);
		this.config = new DbGroup2RoleLDAPStorageMapperConfig(mapperModel);
	}

	@Override
	public void onImportUserFromLDAP(LDAPObject ldapUser, UserModel user, RealmModel realm, boolean isCreate) {
        // For now, import LDAP role mappings just during create
        if (isCreate) {
        	
        	List<String> dbGroups = getGroupsFromDB(user.getUsername());
        	
        	 // Import role mappings from LDAP into Keycloak DB
            for (String dbGroup : dbGroups) {

                RoleContainerModel roleContainer = getTargetRoleContainer(realm);
                RoleModel role = roleContainer.getRole(dbGroup);

                if (role == null) {
                    role = roleContainer.addRole(dbGroup);
                    logger.debugf("Adding role [%s] ", dbGroup);
                }

                logger.debugf("Granting role [%s] to user [%s] during user import from LDAP", dbGroup, user.getUsername());
                user.grantRole(role);
            }
        }
	}

	private List<String> getGroupsFromDB(String username) {
		
		Connection connection = null;
		PreparedStatement  prep_statement = null;
        ResultSet resultSet = null;
        ArrayList<String> groupsFromExternalDB = new ArrayList<String>();
        try {
            connection = config.getDBConnection();
            logger.debugf("LDAP Mapper %s : Executing SQL to get groups for user '%s'",mapperModel.getName(),username );
            prep_statement = connection.prepareStatement(config.getSQLQuery());
            prep_statement.setString(1, username);
            resultSet = prep_statement.executeQuery();
             while (resultSet.next()) {
            	 String single_group = resultSet.getString(1);
            	 groupsFromExternalDB.add(single_group);
             }
        } catch (SQLException sqle) {
        	sqle.printStackTrace();
        }  catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (resultSet != null) try { resultSet.close(); } catch (SQLException e) {e.printStackTrace();}
            if (prep_statement != null) try { prep_statement.close(); } catch (SQLException e) {e.printStackTrace();}
            if (connection != null) try { connection.close(); } catch (SQLException e) {e.printStackTrace();}
        }
   	    logger.debugf("LDAP Mapper %s : Groups for user %s found in Database are : %s ",mapperModel.getName(),username,groupsFromExternalDB);
		return groupsFromExternalDB;
	}

	@Override
	public UserModel proxy(LDAPObject ldapUser, UserModel delegate, RealmModel realm) {
		return new DB2LDAPRoleMappingsUserDelegate(realm, delegate, delegate.getUsername());
	}
    
    protected RoleContainerModel getTargetRoleContainer(RealmModel realm) {
        boolean realmRolesMapping = config.isRealmRolesMapping();
        if (realmRolesMapping) {
            return realm;
        } else {
            String clientId = config.getClientId();
            if (clientId == null) {
                throw new ModelException("Using client roles mapping is requested, but parameter client.id not found!");
            }
            ClientModel client = realm.getClientByClientId(clientId);
            if (client == null) {
                throw new ModelException("Can't found requested client with clientId: " + clientId);
            }
            return client;
        }
    }

	@Override
	public void onRegisterUserToLDAP(LDAPObject ldapUser, UserModel localUser, RealmModel realm) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeLDAPQuery(LDAPQuery query) {
		// TODO Auto-generated method stub
		
	}
	
    public class DB2LDAPRoleMappingsUserDelegate extends UserModelDelegate {

        private final RealmModel realm;
        private final String ldapUser;
        private final RoleContainerModel roleContainer;

        // Avoid loading role mappings from LDAP more times per-request
        private List<String> cachedRDBRoleMappings;
        private long cacheRDBRoleTime = 0;

        public DB2LDAPRoleMappingsUserDelegate(RealmModel realm, UserModel user, String ldapUsername) {
            super(user);
            this.realm = realm;
            this.ldapUser = ldapUsername;
            this.roleContainer = getTargetRoleContainer(realm);
            //logger.debugf("DB2LDAPRoleMappingsUserDelegate object is created!");
        }

        @Override
        public Set<RoleModel> getRealmRoleMappings() {
            if (roleContainer.equals(realm)) {
            	syncRBDGroupsWithKeycloakRolesForUser(super.getRealmRoleMappings());
            }
            return super.getRealmRoleMappings();
        }

        @Override
        public Set<RoleModel> getClientRoleMappings(ClientModel client) {
            if (roleContainer.equals(client)) {
            	syncRBDGroupsWithKeycloakRolesForUser(super.getClientRoleMappings(client));
            } 
            return super.getClientRoleMappings(client);
        }

        /** Sync Group from database with Keycloak Roles
         * 
         * @param roleMappings
         */
        private void syncRBDGroupsWithKeycloakRolesForUser(Set<RoleModel> roleMappings) {
        	
        	//Verification du TTL de la requete
            if ( cachedRDBRoleMappings == null || isSQLCacheExpired() ) {
            	logger.debugf("LDAP Mapper %s : Getting groups from DB",mapperModel.getName());
            	List<String> rdbRoles = getGroupsFromDB(ldapUser);
            	
            	cachedRDBRoleMappings = rdbRoles;
                cacheRDBRoleTime = System.currentTimeMillis();
                
                //Always add DB Group as Keycloak role
                for (String rDBrole : cachedRDBRoleMappings) {
                    RoleModel modelRole = roleContainer.getRole(rDBrole);
                    if (modelRole == null) {
                        // Add role to local DB
                    	logger.debugf("LDAP Mapper %s : Adding role '%s' to keycloak",mapperModel.getName(),rDBrole);
                        modelRole = roleContainer.addRole(rDBrole);
                    }
                    if (!delegate.hasRole(modelRole)) {
                    	logger.debugf("LDAP Mapper %s : Grant role '%s' to user '%s'", mapperModel.getName(), rDBrole,delegate.getUsername());
                    	delegate.grantRole(modelRole);
                    }
                }
                
                //Delete Roles in Keycloak DB if it's configured
                if (config.isDeletingKCRolesIfRemovedFromRDB())
                	for(RoleModel kcrole : roleMappings) {
                		if (!rdbRoles.contains(kcrole.getName())) {
                			logger.debugf("LDAP Mapper %s : Remove role '%s' to user '%s'", mapperModel.getName(),kcrole.getName(),delegate.getUsername());
                			delegate.deleteRoleMapping(kcrole);
                		}
                	}
            } else {
            	logger.debugf("LDAP Mapper %s : Using Database Cache",mapperModel.getName());
            }
		}

		private boolean isSQLCacheExpired() {
			
        	long currentTime = System.currentTimeMillis();
        	long deltaTime = currentTime - cacheRDBRoleTime;
        	
        	long ttlInms = config.getSQLCacheTTL() * 1000;
        	
			return deltaTime >= ttlInms;
		}

        @Override
        public Set<RoleModel> getRoleMappings() {
         	syncRBDGroupsWithKeycloakRolesForUser(super.getRoleMappings());
            return super.getRoleMappings();
        }

    }
}

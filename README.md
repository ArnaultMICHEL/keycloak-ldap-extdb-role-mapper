# keycloak-ldap-extdb-role-mapper
---------------------------------

After LDAP authentication, retrieving roles from an external database.

## Abstract

I designed this mapper for internal applications in this use case :
  * plan to change their authentication mechanism to Keycloak,
  * the old authentication/authorization system was based on :
    * LDAP for authentication,
    * a proprietary database to store user's authorizations.

So Keycloak recover dynamically existing authorizations and convert them into realm or client roles.
No need for authorizations and user's account migration script : just keep the old database up and running for a few months.:)

## Uses cases

When a user is logged on Keycloak through an account stored in an LDAP directory, the groups or roles linked to it's LDAP user ID/logon name are :

1. recovered from the (old) external database, 
2. if it's a new role, Keycloak create a new realm role or client role,
3. link the role to the Keycloak user's account (in keycloak DB)

This group synchronization is done :
  * On each user's logon, after credential validation in LDAP directory,
  * When an admin is searching or viewing the end user's account in Keycloack web admin UI

## Compilation

First, check your keycloak server version, and edit the pom.xml file to set same library versions on the following properties :
  * keycloak.version
  * resteasy.version
  * jboss.logging.version
  * junit.version
  * jboss-transaction-api_1.2_spec

For exemple, if you use Keycloak 4.6.0.Final, set the same version properties info from : https://github.com/keycloak/keycloak/blob/4.6.0.Final/pom.xml
Then you can compile the mapper.

```{r, engine='bash', count_lines}
vim pom.xml
mvn clean install
```

## Installation

Copy target/*.jar in keycloak-X.X.X.Final/providers directory (create the directory if needed)

JAR list :
  * the mapper : keycloak-ldap-extdb-role-mapper-1.0.0.SNAPSHOT.jar
  * the jdbc driver, according to your database :
    * Oracle : ojdbc7-12.1.0.1.0.jar
    * PostgreSQL : postgresql-42.1.4.jar
    * MySQL/MariaDB : mariadb-java-client-2.2.1.jar
    * SQL Server or Sybase : jtds-1.3.1.jar
  * jakarta commons connection pool : commons-dbcp-1.4.jar and commons-pool-1.5.4.jar


## Configuration

First, you need to create a new LDAP user federation if it's not already configured.

In the corresponding user federation entry, select the mapper tab and create a new mapper.
 select 'database-group-to-role-mapper'.

Then fill in the correct parameters in the following configuration :
![LDAP mapper configuration](src/main/resources/configuration_mapper.png?raw=true "Configuration")

I strongly recommend to test the SQL command in your favorite database SQL tool (phpmyadmin, ...)

## Increasing log level

If you want to increase log level on mapper classes, it's 3 simple steps :

1. add this configuration in keycloak-X.X.X.Final/standalone/configuration/standalone[-ha].xml :

```XML
...
    <profile>
        <subsystem xmlns="urn:jboss:domain:logging:3.0">
...
            <logger category="org.toad.keycloak.storage.ldap.mappers">
                <level name="TRACE"/>
            </logger>
...
```

2. Restart Keycloak server/daemon

3. Test and view generated logs in keycloak-X.X.X.Final/standalone/log/server.log :

```bash
$ tail -f keycloak-X.X.X.Final/standalone/log/server.log
2017-12-27 17:02:15,943 INFO  [org.keycloak.storage.ldap.LDAPIdentityStoreRegistry] (default task-1) Creating new LDAP Store for the LDAP storage provider: 'ldap', LDAP Configuration: {fullSyncPeriod=[-1], pagination=[true], debfalse], searchScope=[2], useTruststoreSpi=[ldapsOnly], usersDn=[DC=example,DC=org], connectionPooling=[true], cachePolicy=[DEFAULT], useKerberosForPasswordAuthentication=[false], priority=[0], userObjectClasseerson, organizationalPerson, user], changedSyncPeriod=[-1], usernameLDAPAttribute=[sAMAccountName], bindDn=[CN=SAMPLE_USER,OU=Users,DC=example,DC=org], rdnLDAPAttribute=[cn], vendor=[ad], editMode=[READ_ONLY], uuidLDAPAttribute=[objectGUID], connectionUrl=[LDAPS://example.org], allowKerberosAuthentication=[false], syncRegistrations=[false], authType=[simp batchSizeForSync=[1000]}, binaryAttributes: []
2017-12-27 17:02:16,436 DEBUG [org.toad.keycloak.storage.ldap.mappers.DbGroup2RoleLDAPStorageMapper] (default task-1) getRoleMappings is called
2017-12-27 17:02:16,480 DEBUG [org.toad.keycloak.storage.ldap.mappers.DbGroup2RoleLDAPStorageMapper] (default task-1) getRDBRoleMappingsConverted is called
2017-12-27 17:02:16,481 DEBUG [org.toad.keycloak.storage.ldap.mappers.DbGroup2RoleLDAPStorageMapper] (default task-1) getRDBRoleMappingsConverted is really executed
2017-12-27 17:02:17,408 DEBUG [org.toad.keycloak.storage.ldap.mappers.DbGroup2RoleLDAPStorageMapper] (default task-1)   Load group EXPRESS for user i2118j8from Database with Mapper
2017-12-27 17:02:17,409 DEBUG [org.toad.keycloak.storage.ldap.mappers.DbGroup2RoleLDAPStorageMapper] (default task-1)   Load group ATLASSIAN-administrators for user i2118j8from Database with Mapper
2017-12-27 17:02:17,409 DEBUG [org.toad.keycloak.storage.ldap.mappers.DbGroup2RoleLDAPStorageMapper] (default task-1)   Load group SOFACT-users for user i2118j8from Database with Mapper
2017-12-27 17:02:17,432 DEBUG [org.toad.keycloak.storage.ldap.mappers.DbGroup2RoleLDAPStorageMapper] (default task-1) Adding role SOFACT-users to keycloak (mapper Crowd)
2017-12-27 17:02:17,490 DEBUG [org.toad.keycloak.storage.ldap.mappers.DbGroup2RoleLDAPStorageMapper] (default task-1) Grant role SOFACT-users to user i2118j8 (mapper Crowd)
```

## References

### JDBC Driver URL Construction

The JDBC Driver URL is built by the, according to the official documentation : 
  * [JTDS FAQ](http://jtds.sourceforge.net/faq.html#urlFormat) : ```jdbc:jtds:<server_type>://<server>[:<port>][/<database>][;<property>=<value>[;...]]```
  * [MariaDB] (https://mariadb.com/kb/en/library/about-mariadb-connector-j/#connection-strings) : ```jdbc:(mysql|mariadb):[replication:|failover:|sequential:|aurora:]//<hostDescription>[,<hostDescription>...]/[database][?<key1>=<value1>[&<key2>=<value2>]]```
  * [Oracle] (https://docs.oracle.com/cd/B28359_01/java.111/b31224/jdbcthin.htm) : ```  jdbc:oracle:[oci8|thin]:<user>/<password>@<database>```
  * [PostgreSQL](https://jdbc.postgresql.org/documentation/head/connect.html) : ``` jdbc:postgresql://<host>:<port>/<database> ```
   
et voilà! :)

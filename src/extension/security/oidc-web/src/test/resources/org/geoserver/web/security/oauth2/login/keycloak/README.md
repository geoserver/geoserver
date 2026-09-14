Keycloak
========

These are the keycloak configuration files.


Hand-edited entries
-------------------

`gs-realm-realm.json` carries a `root` user that was added by hand rather than exported, with a plain-text
credential (`root`/`root`). It exists so `KeyCloakAdminPrincipalIntegrationTest` can prove that a provider-asserted
`root` is refused GeoServer roles. It has the same `geoserverAdmin` client role as `admin`, so that test fails loudly
if the guard ever stops firing. If you regenerate the realm by exporting from a container, add the user back.

Create New Realm Export Files
-----------------------------

First, create a new configuration;

1. Bring the container up
   
   There are a few ways to do this.  The easiest way is to `#start()` the keycloak container.
   
   Run a test case and put a breakpoint in after `keycloakContainer.start();`.  Do a `docker ps`
   to get the random port number, and point your browser to `http://localhost:port`.
 
   Login as `geoserver`/`geoserver`

2. Use the keycloak web interface to make changes

Export the Realm Files
----------------------

Its difficult to get realm configuration with passwords actually inside them.  This process works:

1. get shell on the keycloak container
    
   ```
   docker ps
   docker exec -it <keycloak container id> bash
   ```

2. In the shell, setup a location to store the export file

    ```
    rm -r /tmp/h2 ; cp -rp /opt/keycloak/data/h2 /tmp ; cd /opt/keycloak ; mkdir tmp; cd tmp
   ```
   
NOTE: `rm: cannot remove '/tmp/h2': No such file or directory` can be ignored

3. Perform export (replace <realm name> with the realm you want to export)

   ```
   /opt/keycloak/bin/kc.sh export --dir . --realm <realm name>  --users realm_file  --db dev-file --db-url 'jdbc:h2:file:/tmp/h2/keycloakdb;NON_KEYWORDS=VALUE' 
   ```

4. exit the docker shell
5. on the host (machine running docker), copy the exported file:

   ```
   docker cp <keycloak docker id>:/opt/keycloak/tmp/<realmname>-realm.json <realmname>-realm-new.json
   ```

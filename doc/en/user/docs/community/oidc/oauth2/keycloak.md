# Configuring with Keycloak

[Keycloak](https://www.keycloak.org/) is a popular open source Identity and Access Management system that supports OIDC.

In GeoServer, some of the core tests are against keycloak.

This will setup a Keycloak [Docker](https://www.docker.com/) container and manage user's inside Keycloak.

See the [Keycloak Documentation](https://www.keycloak.org/guides) for more information on setting up Keycloak. Keycloak can also be use "in between" GeoServer and another OIDC IDP.

!!! note

    If running both GeoServer and Keycloak in Docker, see the [Docker networking](../installing.md) section in the installation guide for important notes about `host.docker.internal` and ensuring consistent hostnames.

## Configure Keycloak

This will setup a Docker container running Keycloak and setup a [Keycloak Realm](https://www.keycloak.org/docs/latest/server_admin/index.md#_configuring-realms) and Client for GeoServer. For more information on the Keycloak Docker container see [here](https://www.keycloak.org/getting-started/getting-started-docker) and [here](https://www.keycloak.org/server/containers) and [here](https://hub.docker.com/r/keycloak/keycloak).

### Docker Install and Basic Setup

1.  Run Keycloak v26.1 as a Docker Container running on port 7777 with the admin login user "geoserver" and admin password "geoserver". This assumes your GeoServer is running on <http://localhost:8080>.

> ``` bash
> docker run --name geoserver_keycloak -p 7777:8080 \
>    -e KC_BOOTSTRAP_ADMIN_USERNAME=geoserver \
>    -e KC_BOOTSTRAP_ADMIN_PASSWORD=geoserver \
>    quay.io/keycloak/keycloak:26.1 \
>    start-dev
> ```

1.  Access the Keycloak administration interface at <http://localhost:7777/> and login as "geoserver/geoserver".

2.  In the top left corner, click on "Keycloak master" and then "Create realm"

    > ![](../img/keycloak-create-realm.png)

3.  Give the realm a name (i.e. "gs-realm") and then press "Create"

    > ![](../img/keycloak-create-realm2.png)

4.  On the left bar, press "Clients" for your new realm, then "Create client"

    > ![](../img/keycloak-create-client.png)

5.  Give the new Client a name ("gs-client") and press "Next"

    > ![](../img/keycloak-create-client2.png)

6.  Turn on "Client authentication" and then press "Next"

    > ![](../img/keycloak-create-client3.png)

7.  Set the "Root URL" and "Home URL" as "http://localhost:8080". Set the "Valid post logout redirect URIs" and "Valid redirect URIs" as "http://localhost:8080/*". Then press "Save".

    !!! tip

        The exact redirect URI that GeoServer will use is shown as the read-only **Redirect URI** field in the filter configuration form (e.g. `http://localhost:8080/geoserver/web/login/oauth2/code/oidc`). In production, you may want to register only this specific URI rather than a wildcard. See [Redirect Base URI](../configuring.md#community_oidc_redirect_base_uri).
    
        ![](../img/keycloak-create-client4.png)

#\. Press "Save" (again).

> ![](../img/keycloak-create-client5.png)

You have now configured the "gs-realm" with a "gs-client".

### Setting up Roles

This will create a "geoserverAdmin" role that can be used to give users admin access to GeoServer.

1.  Go to your client ("gs-client"):

    > - Make sure you are in the correct realm ("gs-realm") in the top left corner
    > - Click on "Clients" (left bar)
    > - Choose your client ("gs-client")

2.  In the client's top bar, press "Roles", then "Create Role".

    > ![](../img/keycloak-create-role1.png)

#\. Set the role's name as "geoserverAdmin" and press "Save"

> ![](../img/keycloak-create-role2.png)

### Setting Up Users

We will create two user:

> - "admin/admin" who has administration rights (role "geoserverAdmin")
> - "user/user" who does not have administration rights

We will also put the Keycloak roles in the ID Token. By default, keycloak only puts the roles in the Access Token JWT (not in the ID Token).

1.  Go to your Realm ("gs-realm") - check the top left corner.

2.  Press "Users" (left column) and then "Create new User"

    > ![](../img/keycloak-create-user1.png)

3.  Create the "admin" user - you can use your own name and email if you want. Ensure that the user's email is verified. When finished, press "Create".

    > ![](../img/keycloak-create-user2.png)

4.  Press the "Users" (left column) again, and then "Add User".

    > ![](../img/keycloak-create-user3.png)

5.  Add the "user" user. Ensure that the user's email is verified. When finished, press "Create". On he next screen, press "Save".

    > ![](../img/keycloak-create-user4.png)

#\. Press the "Users" (left column) again, then click on the "admin" user.

> ![](../img/keycloak-create-user5.png)
>
> 1.  Press "Role mapping"
>
> ![](../img/keycloak-create-user6.png)
>
> 1.  Press "Assign role" - you will get a pop-up
>
> ![](../img/keycloak-create-user7.png)
>
> 1.  Check the "geoserverAdmin" role, and then press "Assign"
>
> ![](../img/keycloak-create-user8.png)

1.  Go to the "Credentials" tab and Press "Set Password"

    > ![](../img/keycloak-set-password-admin1.png)

2.  Fill in the Password as "admin" and set "Temporary" to "off". Press "Save" and Confirm setting the password.

    > ![](../img/keycloak-set-password-admin2.png)

3.  Do the same for the User "user"

    > - Press "Users" (left column)
    > - Select the "user" User
    > - Press "Credentials"
    > - Press "Set Password"
    > - Fill in "user" as the password
    > - Set "Temporary" to "off"
    > - Press "Save" and Confirm setting the password.
    >
    > ![](../img/keycloak-set-password-user1.png)

4.  One the left column, choose "Client scope". In the search box, enter "roles" and press the "->" search button. In the results, click on "roles".

    > ![](../img/keycloak-id-token.png)

5.  Click on "Mappers" (top), and then "client roles" (middle).

    > ![](../img/keycloak-id-token2.png)

#\. Turn on "Add to ID token" and "Add to userinfo". Then press "Save".

> ![](../img/keycloak-id-token3.png)

You have now created two users - "admin" and "user". We then attached the "geoserverAdmin" role to the "admin" user. We also added the keycloak roles to the ID Token.

### Generate Client Secret

This allow you to get a Client Secret.

1.  Navigate to your Client ("gs-client") in your Realm ("gs-realm")

    > - Make sure you are in the correct realm ("gs-realm") in the top left corner
    > - Click on "Clients" (left bar)
    > - Choose your client ("gs-client")

#\. Click on the "Credentials" tab (top) and then the copy button.

> ![](../img/keycloak-client-secret.png)

**Save your Client Secret for use in the GeoServer Configuration**

### Debugging

This is for technical people wanting to see the Access Token, ID Token, and User Info for a user.

1.  Navigate to your Client ("gs-client") in your Realm ("gs-realm")

    > - Make sure you are in the correct realm ("gs-realm") in the top left corner
    > - Click on "Clients" (left bar)
    > - Choose your client ("gs-client")

2.  At the top, go to "Client Scopes", then press "Evaluate" (near the top), then select a User. You must not just type in the user's name, you **must** select it from the auto-complete pop-up!

    > ![](../img/keycloak-debug1.png)

3.  In the bottom right, you can look at the Access Token, ID Token, and Userinfo JSON claims payloads.

Sample Access Token (yours will be slightly different):

> ``` json
> {
>     "exp": 1759435301,
>     "iat": 1759435001,
>     "jti": "73171fc2-3827-414f-a03e-f862a550caf4",
>     "iss": "http://localhost:7777/realms/gs-realm",
>     "aud": "gs-client",
>     "sub": "d28f1cb8-704b-4f5e-b24e-0385af136739",
>     "typ": "ID",
>     "azp": "gs-client",
>     "sid": "1b897b54-e4d7-4a3f-9204-7b3e045d8900",
>     "acr": "1",
>     "resource_access": {
>         "gs-client": {
>         "roles": [
>             "geoserverAdmin"
>         ]
>         },
>         "account": {
>         "roles": [
>             "manage-account",
>             "manage-account-links",
>             "view-profile"
>         ]
>         }
>     },
>     "email_verified": true,
>     "name": "david blasby",
>     "preferred_username": "admin",
>     "given_name": "david",
>     "family_name": "blasby",
>     "email": "admin@example.com"
> }
> ```

Sample ID Token (yours will be slightly different):

> ``` json
> {
>     "exp": 1759435301,
>     "iat": 1759435001,
>     "jti": "e5366c1b-f669-44d4-a6c2-465a06f15997",
>     "iss": "http://localhost:7777/realms/gs-realm",
>     "aud": "account",
>     "sub": "d28f1cb8-704b-4f5e-b24e-0385af136739",
>     "typ": "Bearer",
>     "azp": "gs-client",
>     "sid": "27490dc0-05ef-4979-bafc-6d8f854cf6ad",
>     "acr": "1",
>     "allowed-origins": [
>         "http://localhost:8080"
>     ],
>     "realm_access": {
>         "roles": [
>         "default-roles-gs-realm",
>         "offline_access",
>         "uma_authorization"
>         ]
>     },
>     "resource_access": {
>         "gs-client": {
>         "roles": [
>             "geoserverAdmin"
>         ]
>         },
>         "account": {
>         "roles": [
>             "manage-account",
>             "manage-account-links",
>             "view-profile"
>         ]
>         }
>     },
>     "scope": "openid profile email",
>     "email_verified": true,
>     "name": "david blasby",
>     "preferred_username": "admin",
>     "given_name": "david",
>     "family_name": "blasby",
>     "email": "admin@example.com"
> }
> ```

Sample userinfo (yours will be slightly different):

> ``` json
> {
> "sub": "d28f1cb8-704b-4f5e-b24e-0385af136739",
> "resource_access": {
>     "gs-client": {
>     "roles": [
>         "geoserverAdmin"
>     ]
>     },
>     "account": {
>     "roles": [
>         "manage-account",
>         "manage-account-links",
>         "view-profile"
>     ]
>     }
> },
> "email_verified": true,
> "name": "david blasby",
> "preferred_username": "admin",
> "given_name": "david",
> "family_name": "blasby",
> "email": "admin@example.com"
> }
> ```

## Configure GeoServer

The next step is to configure your Keycloak as the OIDC IDP for GeoServer. You will need the Client Id ("gs-client") and the Client Secret (see above).

### Create the OIDC Filter

1.  Login to GeoServer as an Admin

2.  On the left bar under "Security", click "Authentication", and then "OpenID Connect Login"

    > ![](../img/google-gs1.png)

3.  Give the it a name like "test-keycloak", then from the **Provider** dropdown select **OpenID Connect Provider** (this is the default selection).

    > ![](../img/keycloak-gs-filter1.png)

4.  Fill in the required information:

    - "Client Id" is "gs-client" (name of the Keycloak client you created)
    - "Client Secret" which was copied from the keycloak client's "Credentials" tab (see above)
    - Turn off "Force Access Token URI HTTPS Secured Protocol" (at the bottom under "Advanced Settings")
    - Turn off " Force User Authorization URI HTTPS Secured Protocol" (at the bottom under "Advanced Settings")
    - In the "OpenID Discovery Document" type in "http://localhost:7777/realms/gs-realm/.well-known/openid-configuration"
    - Press "Discover" (this will download OIDC metadata from your keycloak client)

    ![](../img/keycloak-gs-filter2.png)

#\. After you press the "Discovery" button, most of the information will be filled out for you

> ![](../img/keycloak-gs-filter3.png)

### Configure Role Source

One the same page, we will configure the Role source:

> - Get the roles from the ID Token's "resource_access.gs-client.roles" claim
> - Convert the keycloak "geoserverAdmin" role to GeoServer's "ROLE_ADMINISTRATOR".

#\. Go down to the bottom and configure the role source (for more info see [role source](../role-config.md)).

> - Choose "ID Token" as the "Role Source"
> - Set "resource_access.gs-client.roles" as the "JSON Path"
> - Set "geoserverAdmin=ROLE_ADMINISTRATOR" as the "Role Converter Map"
> - (optionally) Check "Only allow External Roles that are explicitly named above"
> - Press Save
>
> ![](../img/keycloak-rolesource-id.png)

**NOTE:** You can also change the above to get the role from the ID Token, Access Token, or userinfo.

### Allow Web Access (Filter Chain)

> * On the left bar under "Security", click "Authentication", and then click "Web" under "Filter Chains"
>
> > ![](../img/google-filterchain1.png)
> >
> > - Scroll down, and move the new Keycloak OIDC Filter to the Selected side by pressing the "->" button.
> >
> > ![](../img/keycloak-filterchain2.png)
> >
> > - Move the new Keycloak OIDC Filter above "anonymous" by pressing the up arrow button.
> >
> > ![](../img/keycloak-filterchain3.png)
> >
> > - Press "Close"
> > - Press "Save"

## Testing

See [troubleshooting](../advanced.md#community_oidc_troubleshooting).

1.  log out of GeoServer (or open an incognito tab)

2.  Press the OIDC login button in the top left of the GeoServer Main Page

    > ![](../img/keycloak-login1.png)

3.  The keycloak Login screen will appear. Login as:

    > - user "admin", password "admin"
    > - user "user", password "user"

4.  If you login as "admin", you should see the GeoServer administration screens. If you login as "user", you will not.

    > - Admin
    >
    >   > ![](../img/keycloak-login2.png)
    >
    > - User
    >
    >   > ![](../img/keycloak-login3.png)

---
render_macros: true
---

# Security

The REST API allows you to adjust GeoServer security settings.

!!! note

    Read the [API reference for /security]({{ api_url }}/security.yaml).

## Listing the keystore password {: #rest_security_keystore }

**Retrieve the keystore password** used to [encode secrets](../security/webadmin/passwords.md#security_webadmin_masterpasswordprovider) in **`geoserver.jceks`**, and [optional login](../security/webadmin/passwords.md#security_webadmin_passwd_keystore) as `root` user (this is the source xml name `masterPassword` shown below).

*Request*

!!! abstract "curl"

        curl -v -u admin:geoserver -XGET http://localhost:8080/geoserver/rest/security/masterpw.xml

*Response*

``` xml
<?xml version="1.0" encoding="UTF-8"?><masterPassword>
    <oldMasterPassword>geoserver</oldMasterPassword>
</masterPassword>
```

## Changing the keystore password

**Change to a new keystore password**

!!! note

    Requires knowledge of the current keystore password.

Given a `changes.xml` file:

``` xml
<masterPassword>
   <oldMasterPassword>-"}3a^Kh</oldMasterPassword>
   <newMasterPassword>geoserver1</newMasterPassword>
</masterPassword>
```

*Request*

!!! abstract "curl"

        curl -v -u admin:geoserver -XPUT -H "Content-type: text/xml" -d @change.xml http://localhost:8080/geoserver/rest/security/masterpw.xml

*Response*

    200 OK

## Listing the catalog mode

**Fetch the current catalog mode**

*Request*

!!! abstract "curl"

        curl -v -u admin:geoserver -XGET   http://localhost:8080/geoserver/rest/security/acl/catalog.xml

*Response*

``` xml
<?xml version="1.0" encoding="UTF-8"?>
<catalog>
    <mode>HIDE</mode>
</catalog>
```

## Changing the catalog mode

**Set a new catalog mode**

Given a `newMode.xml` file:

``` xml
<?xml version="1.0" encoding="UTF-8"?>
<catalog>
    <mode>MIXED</mode>
</catalog>
```

*Request*

!!! abstract "curl"

        curl -v -u admin:geoserver -XPUT -H "Content-type: text/xml" -d @newMode.xml http://localhost:8080/geoserver/rest/security/acl/catalog.xml

## Listing access control rules

**Retrieve current list of access control rules**

*Request*

!!! abstract "curl"

        curl -v -u admin:geoserver -XGET http://localhost:8080/geoserver/rest/security/acl/layers.xml

*Response*

``` xml
<?xml version="1.0" encoding="UTF-8"?>
<rules />
```

!!! note

    The above response shows no rules specified.

## Changing access control rules

**Set a new list of access control rules**

Given a `rules.xml` file:

``` xml
<?xml version="1.0" encoding="UTF-8"?>
<rules>
   <rule resource="topp.*.r">ROLE_AUTHORIZED</rule>
   <rule resource="topp.mylayer.w">ROLE_1,ROLE_2</rule>      
</rules>
```

*Request*

!!! abstract "curl"

        curl -v -u admin:geoserver -XPOST -H "Content-type: text/xml" -d @rules.xml http://localhost:8080/geoserver/rest/security/acl/layers.xml 

*Response*

    201 Created

## Deleting access control rules

**Delete individual access control rule**

*Request*

!!! abstract "curl"

        curl -v -u admin:geoserver -XDELETE  http://localhost:8080/geoserver/rest/security/acl/layers/topp.*.r

*Response*

    200 OK

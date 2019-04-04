The tests in these modules are of the "online" kind, they need to connect to an empty
PosgreSQL database with PostGIS support enabled.

The tests will be skipped unless the $HOME/.geoserver/oseo-postgis.properties file is found
and contains information needed to connect to the database, here is an example (replace
all values with your own):

```
user=theUser
port=5432
passwd=thePassword
host=localhost
database=oseo-tests
```
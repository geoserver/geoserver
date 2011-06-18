Here's the summary:
1. get postgres and postgis
2. create a "spearfish" superuser with spearfish password: createuser -s -d -P spearfish
3. create a "spearfish" database: createdb spearfish -U spearfish
4. uncompress spearfish.dmp.gz
5. run "pg_restore -d spearfish -U spearfish -F c spearfish.dmp"
6. run geoserver
7. enjoy
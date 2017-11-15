# load the postgres db
dropdb  -U cite cite
createdb  -U cite cite
psql -U cite -d cite -c "CREATE extension postgis;"
psql  -U cite cite < dataset-sf0-postgis2.sql

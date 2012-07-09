# load the postgres db
dropdb  -U cite cite
createdb  -U cite -T template_postgis cite
psql  -U cite cite < dataset-sf0.sql

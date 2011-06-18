#nurc
#curl -XPUT -H 'Content-type: application/zip' --data-binary @usa.zip http://localhost:8080/geoserver/rest/workspaces/nurc/coveragestores/worldImageSample/file.worldimage?coverageName=Img_Sample

curl -u admin:geoserver -XPUT -H 'Content-type: application/zip' --data-binary @mosaic.zip http://localhost:8080/geoserver/rest/workspaces/nurc/coveragestores/mosaic/file.imagemosaic

curl -u admin:geoserver -XPUT -H 'Content-type: application/zip' --data-binary @Pk50095.zip http://localhost:8080/geoserver/rest/workspaces/nurc/coveragestores/img_sample2/file.worldimage

curl -u admin:geoserver -XPUT -H 'Content-type: application/zip' --data-binary @precip30min.zip http://localhost:8080/geoserver/rest/workspaces/nurc/coveragestores/arcGridSample/file.arcgrid?coverageName=Arc_Sample

#sf
curl -u admin:geoserver -XPUT -H 'Content-type: application/zip' --data-binary @sfdem.zip http://localhost:8080/geoserver/rest/workspaces/nurc/coveragestores/sfdem/file.geotiff

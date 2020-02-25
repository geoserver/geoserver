/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.changeset;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.channels.Channels;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.geoserver.api.APIException;
import org.geoserver.gwc.GWC;
import org.geowebcache.conveyor.ConveyorTile;
import org.geowebcache.grid.Grid;
import org.geowebcache.grid.GridSet;
import org.geowebcache.io.Resource;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.mime.MimeType;
import org.geowebcache.storage.StorageBroker;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

/** Writes a changeset as zip file with the changeset document and the set of modified tiles */
@Component
public class ZippedChangesetMessageConverter implements HttpMessageConverter<ChangeSet> {

    private static final MediaType ZIP_MEDIA_TYPE =
            MediaType.parseMediaType(ChangesetTilesService.ZIP_MIME);

    private ObjectMapper mapper;
    private GWC gwc;
    private StorageBroker storageBroker;

    public ZippedChangesetMessageConverter(GWC gwc, StorageBroker storageBroker) {
        this.gwc = gwc;
        this.storageBroker = storageBroker;
        // custom configured JSON mapper to avoid stream being closed
        JsonFactory jsonFactory = new JsonFactory();
        jsonFactory.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
        mapper = new ObjectMapper(jsonFactory);
    }

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return false;
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return clazz.equals(ChangeSet.class) && ZIP_MEDIA_TYPE.equals(mediaType);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Collections.singletonList(ZIP_MEDIA_TYPE);
    }

    @Override
    public ChangeSet read(Class<? extends ChangeSet> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void write(ChangeSet changeSet, MediaType contentType, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        ZipOutputStream zos = new ZipOutputStream(outputMessage.getBody());
        try {
            // write out the changeset
            zos.putNextEntry(new ZipEntry("changeset.json"));
            mapper.writeValue(zos, changeSet);

            // write out the tiles
            ModifiedTiles modifiedTiles = changeSet.getModifiedTiles();
            MimeType tilesMime = changeSet.getTilesMime();
            TileLayer tileLayer = modifiedTiles.getTileLayer();
            Iterator<long[]> tiles = modifiedTiles.getTiles();
            GridSet gridSet = modifiedTiles.getGridSubset().getGridSet();
            while (tiles.hasNext()) {
                // compute the tile
                long[] tileIndex = tiles.next();
                ConveyorTile tile =
                        new ConveyorTile(
                                storageBroker,
                                tileLayer.getName(), // using the tile id won't work with storage
                                // broker
                                gridSet.getName(),
                                tileIndex,
                                tilesMime,
                                changeSet.getFilterParameters(),
                                null,
                                null);
                tile = tileLayer.getTile(tile);
                if (tile != null) {
                    // add the tile if missing
                    String tilePath = getTilePath(gridSet, tileIndex, tilesMime);
                    zos.putNextEntry(new ZipEntry(tilePath));
                    final Resource tileContents = tile.getBlob();
                    tileContents.transferTo(Channels.newChannel(zos));
                    zos.closeEntry();
                }
            }
        } catch (Exception e) {
            throw new APIException(
                    "InternalError",
                    "Failed during changeset encoding",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e);
        } finally {
            zos.flush();
        }
    }

    private String getTilePath(GridSet gridSet, long[] tileIndex, MimeType mime) {
        long x = tileIndex[0];
        int z = (int) tileIndex[2];
        Grid grid = gridSet.getGrid(z);
        long tilesHigh = grid.getNumTilesHigh();
        long y = tilesHigh - tileIndex[1] - 1;

        return gridSet.getName()
                + "/"
                + grid.getName()
                + "/"
                + y
                + "/"
                + x
                + "."
                + mime.getFileExtension();
    }
}

/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mbtiles;

import com.google.common.collect.Sets;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.gwc.GWC;
import org.geoserver.platform.ServiceException;
import org.geoserver.tiles.AbstractTilesGetMapOutputFormat;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WebMapService;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.mbtiles.MBTilesFile;
import org.geotools.mbtiles.MBTilesMetadata;
import org.geotools.mbtiles.MBTilesTile;
import org.geotools.referencing.CRS;
import org.geowebcache.grid.GridSubset;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * WMS GetMap Output Format for mbtiles
 *
 * @author Justin Deoliveira, Boundless
 * @author Niels Charlier
 */
public class MBTilesGetMapOutputFormat extends AbstractTilesGetMapOutputFormat {

    private static final CoordinateReferenceSystem SPHERICAL_MERCATOR;

    private static final CoordinateReferenceSystem WGS_84;

    static {
        try {
            SPHERICAL_MERCATOR = CRS.decode("EPSG:3857", true);
            WGS_84 = CRS.decode("EPSG:4326", true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static class MbTilesFileWrapper implements TilesFile {

        MBTilesFile mbTiles;

        public MbTilesFileWrapper() throws IOException {
            mbTiles = new MBTilesFile();
            mbTiles.init();
        }

        public MbTilesFileWrapper(MBTilesFile file) throws IOException {
            mbTiles = file;
        }

        @Override
        public void setMetadata(
                String name,
                ReferencedEnvelope box,
                String imageFormat,
                int srid,
                List<MapLayerInfo> mapLayers,
                int[] minmax,
                GridSubset gridSubset)
                throws IOException, ServiceException {
            MBTilesMetadata metadata = new MBTilesMetadata();

            metadata.setName(name);
            metadata.setVersion("0");

            if (mapLayers.size() == 1) {
                ResourceInfo r = mapLayers.get(0).getResource();
                metadata.setDescription(r.getDescription());
                metadata.setType(MBTilesMetadata.t_type.BASE_LAYER);
            } else {
                String descr = "";
                for (MapLayerInfo l : mapLayers) {
                    descr += l.getResource().getDescription() + ", ";
                }
                descr = descr.substring(0, descr.length() - 2);
                metadata.setDescription(descr);
                metadata.setType(MBTilesMetadata.t_type.OVERLAY);
            }

            try {
                metadata.setBounds(box.transform(WGS_84, true));
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to transform bounding box", e);
            }

            // save metadata
            metadata.setFormat(
                    JPEG_MIME_TYPE.equals(imageFormat)
                            ? MBTilesMetadata.t_format.JPEG
                            : MBTilesMetadata.t_format.PNG);
            LOGGER.fine("Creating tile entry" + metadata.getName());
            mbTiles.saveMetaData(metadata);
        }

        @Override
        public void addTile(int zoom, int x, int y, byte[] data) throws IOException {
            MBTilesTile tile = new MBTilesTile(zoom, x, y);
            tile.setData(data);
            mbTiles.saveTile(tile);
        }

        @Override
        public File getFile() {
            return mbTiles.getFile();
        }

        @Override
        public void close() {
            mbTiles.close();
        }
    }

    static final String MIME_TYPE = "application/x-sqlite3";

    static final String EXTENSION = ".mbtiles";

    static final Set<String> NAMES = Sets.newHashSet("mbtiles");

    // lazy loading converted bounds
    protected ReferencedEnvelope convertedBounds = null;

    public MBTilesGetMapOutputFormat(WebMapService webMapService, WMS wms, GWC gwc) {
        super(MIME_TYPE, EXTENSION, NAMES, webMapService, wms, gwc);
    }

    @Override
    protected TilesFile createTilesFile() throws IOException {
        return new MbTilesFileWrapper();
    }

    @Override
    protected ReferencedEnvelope bounds(GetMapRequest req) {
        ReferencedEnvelope convertedBounds = null;
        try {
            convertedBounds =
                    new ReferencedEnvelope(req.getBbox(), req.getCrs())
                            .transform(SPHERICAL_MERCATOR, true);
        } catch (Exception e) {
            throw new ServiceException(e);
        }
        return convertedBounds;
    }

    @Override
    protected CoordinateReferenceSystem getCoordinateReferenceSystem(GetMapRequest req) {
        return SPHERICAL_MERCATOR;
    }

    @Override
    protected String getSRS(GetMapRequest req) {
        return "EPSG:900913";
    }

    /** Add tiles to an existing MBtile file */
    public void addTiles(MBTilesFile mbtiles, GetMapRequest req, String name) throws IOException {
        addTiles(new MbTilesFileWrapper(mbtiles), req, name);
    }
}

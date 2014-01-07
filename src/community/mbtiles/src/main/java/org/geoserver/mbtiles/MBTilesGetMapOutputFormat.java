/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mbtiles;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.geoserver.catalog.ResourceInfo;
import org.geoserver.gwc.GWC;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WebMapService;
import org.geoserver.wms.map.AbstractTilesGetMapOutputFormat;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.mbtiles.MBTilesTile;
import org.geotools.mbtiles.MBTilesFile;
import org.geotools.mbtiles.MBTilesMetadata;
import org.geotools.referencing.CRS;
import org.geowebcache.grid.GridSubset;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.google.common.collect.Sets;

/**
 * 
 * WMS GetMap Output Format for mbtiles
 * 
 * @author Justin Deoliveira, Boundless
 * @author Niels Charlier
 * 
 */
public class MBTilesGetMapOutputFormat extends AbstractTilesGetMapOutputFormat {

    private static class MbTilesFileWrapper implements TilesFile {

        MBTilesFile mbTiles;

        public MbTilesFileWrapper() throws IOException {
            mbTiles = new MBTilesFile();
            mbTiles.init();
        }

        @Override
        public void setMetadata(String name, ReferencedEnvelope box, String imageFormat, int srid,
                List<MapLayerInfo> mapLayers, int[] minmax, GridSubset gridSubset)
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
                descr = descr.substring(0, descr.length()-2);
                metadata.setDescription(descr);
                metadata.setType(MBTilesMetadata.t_type.OVERLAY);
            }

            metadata.setBounds(box);

            // save metadata
            metadata.setFormat(JPEG_MIME_TYPE.equals(imageFormat) ? MBTilesMetadata.t_format.JPEG
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

    private final static CoordinateReferenceSystem SPHERICAL_MERCATOR;

    static {
        try {
            SPHERICAL_MERCATOR = CRS.decode("EPSG:3857");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static final String MIME_TYPE = "application/x-sqlite3";

    static final String EXTENSION = ".mbtiles";

    static final Set<String> NAMES = Sets.newHashSet("mbtiles");

    public MBTilesGetMapOutputFormat(WebMapService webMapService, WMS wms, GWC gwc) {
        super(MIME_TYPE, EXTENSION, NAMES, webMapService, wms, gwc);
    }

    @Override
    protected TilesFile createTilesFile() throws IOException {
        return new MbTilesFileWrapper();
    }

    @Override
    protected ReferencedEnvelope bounds(WMSMapContent map) {
        try {
            return super.bounds(map).transform(SPHERICAL_MERCATOR, true);
        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    @Override
    protected CoordinateReferenceSystem getCoordinateReferenceSystem(WMSMapContent map) {
        return SPHERICAL_MERCATOR;
    }

}
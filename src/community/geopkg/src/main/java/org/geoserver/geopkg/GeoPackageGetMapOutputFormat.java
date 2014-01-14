/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geopkg;

import static org.geoserver.geopkg.GeoPkg.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import org.geoserver.catalog.ResourceInfo;
import org.geoserver.gwc.GWC;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WebMapService;
import org.geoserver.tiles.AbstractTilesGetMapOutputFormat;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geopkg.GeoPackage;
import org.geotools.geopkg.Tile;
import org.geotools.geopkg.TileEntry;
import org.geotools.geopkg.TileMatrix;
import org.geotools.util.logging.Logging;
import org.geowebcache.grid.Grid;
import org.geowebcache.grid.GridSet;
import org.geowebcache.grid.GridSubset;

import com.google.common.collect.Sets;

/**
 * 
 * WMS GetMap Output Format for GeoPackage
 * 
 * @author Justin Deoliveira, Boundless
 *
 */
public class GeoPackageGetMapOutputFormat extends AbstractTilesGetMapOutputFormat {

    static Logger LOGGER = Logging.getLogger("org.geoserver.geopkg");

    
    static final String EXTENSION = ".gpkg";

    public GeoPackageGetMapOutputFormat(WebMapService webMapService, WMS wms, GWC gwc) {
        super(MIME_TYPE, EXTENSION, Sets.newHashSet(NAMES), webMapService, wms, gwc);
    }

    private static class GeopackageWrapper implements TilesFile {

        GeoPackage geopkg;

        TileEntry e;

        public GeopackageWrapper() throws IOException {
            geopkg = new GeoPackage();
            geopkg.init();

            e = new TileEntry();
        }

        @Override
        public void setMetadata(String name, ReferencedEnvelope box, String imageFormat, int srid,
                List<MapLayerInfo> mapLayers, int[] minmax, GridSubset gridSubset)
                throws IOException, ServiceException {

            e.setTableName(name);
            if (mapLayers.size() == 1) {
                ResourceInfo r = mapLayers.get(0).getResource();
                e.setIdentifier(r.getTitle());
                e.setDescription(r.getAbstract());
            }
            e.setBounds(box);
            e.setSrid(srid);

            GridSet gridSet = gridSubset.getGridSet();
            for (int z = minmax[0]; z < minmax[1]; z++) {
                Grid g = gridSet.getGrid(z);

                TileMatrix m = new TileMatrix();
                m.setZoomLevel(z);
                m.setMatrixWidth((int) g.getNumTilesWide());
                m.setMatrixHeight((int) g.getNumTilesHigh());
                m.setTileWidth(gridSubset.getTileWidth());
                m.setTileHeight(gridSubset.getTileHeight());

                // TODO: not sure about this
                m.setXPixelSize(g.getResolution());
                m.setYPixelSize(g.getResolution());
                // m.setXPixelSize(gridSet.getPixelSize());
                // m.setYPixelSize(gridSet.getPixelSize());

                e.getTileMatricies().add(m);
            }

            // figure out the actual bounds of the tiles to be renderered
            LOGGER.fine("Creating tile entry" + e.getTableName());
            geopkg.create(e);

        }

        @Override
        public void addTile(int zoom, int x, int y, byte[] data) throws IOException {
            Tile t = new Tile();
            t.setZoom(zoom);
            t.setColumn(x);
            t.setRow(y);
            t.setData(data);
            geopkg.add(e, t);
        }

        @Override
        public File getFile() {
            return geopkg.getFile();
        }

        @Override
        public void close() {
            geopkg.close();
        }
    }
   
    @Override
    protected TilesFile createTilesFile() throws IOException{
    	return new GeopackageWrapper();
    }

}

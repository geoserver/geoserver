/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geopkg.wms;

import static org.geoserver.geopkg.GeoPkg.EXTENSION;
import static org.geoserver.geopkg.GeoPkg.MIME_TYPE;
import static org.geoserver.geopkg.GeoPkg.NAMES;

import com.google.common.collect.Sets;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.gwc.GWC;
import org.geoserver.platform.ServiceException;
import org.geoserver.tiles.AbstractTilesGetMapOutputFormat;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WebMap;
import org.geoserver.wms.WebMapService;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geopkg.GeoPackage;
import org.geotools.geopkg.Tile;
import org.geotools.geopkg.TileEntry;
import org.geotools.geopkg.TileMatrix;
import org.geotools.referencing.CRS;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.Grid;
import org.geowebcache.grid.GridSet;
import org.geowebcache.grid.GridSubset;
import org.locationtech.jts.geom.Envelope;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.ProgressListener;

/**
 * WMS GetMap Output Format for GeoPackage
 *
 * @author Justin Deoliveira, Boundless
 */
public class GeoPackageGetMapOutputFormat extends AbstractTilesGetMapOutputFormat {

    public GeoPackageGetMapOutputFormat(WebMapService webMapService, WMS wms, GWC gwc) {
        super(MIME_TYPE, "." + EXTENSION, Sets.newHashSet(NAMES), webMapService, wms, gwc);
    }

    /** TilesFile interface implementation for a GeoPackage */
    private static class GeopackageWrapper implements TilesFile {

        GeoPackage geopkg;

        TileEntry e;

        public GeopackageWrapper(GeoPackage geopkg, TileEntry e) throws IOException {
            this.geopkg = geopkg;
            this.e = e;
        }

        public GeopackageWrapper() throws IOException {
            this(new GeoPackage(), new TileEntry());
            geopkg.init();
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

            e.setTableName(name);
            if (mapLayers.size() == 1) {
                ResourceInfo r = mapLayers.get(0).getResource();
                if (e.getIdentifier() == null) {
                    e.setIdentifier(r.getTitle());
                }
                if (e.getDescription() == null) {
                    e.setDescription(r.getAbstract());
                }
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
            // tile index is calculated relative to the gridset, so we need to use
            // the gridset bounds (otherwise the layer bounds will be used as the gridset ones)
            BoundingBox subsetBounds = gridSubset.getGridSetBounds();
            if (subsetBounds != null) {
                ReferencedEnvelope re =
                        new ReferencedEnvelope(
                                subsetBounds.getMinX(),
                                subsetBounds.getMaxX(),
                                subsetBounds.getMinY(),
                                subsetBounds.getMaxY(),
                                box.getCoordinateReferenceSystem());
                e.setTileMatrixSetBounds(re);
            }

            // figure out the actual bounds of the tiles to be renderered
            LOGGER.fine("Creating tile entry" + e.getTableName());
            geopkg.create(e);
        }

        // add a tile (image) into the geoopackage with the given grid coordinates
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
    public WebMap produceMap(WMSMapContent map) throws ServiceException, IOException {
        /*
         * From the OGC GeoPackage Specification [1]:
         *
         * "The tile coordinate (0,0) always refers to the tile in the upper left corner of the tile matrix at any zoom
         * level, regardless of the actual availability of that tile"
         *
         * This is opposite the default GeoServer grid behavior, so we must always flip the y here.
         *
         * [1]: http://www.geopackage.org/spec/#tile_matrix
         */
        // setup format option (i.e. flipy) for incoming request, then make the tiles.
        map.getRequest().getFormatOptions().put("flipy", "true");
        rewriteRequest(map.getRequest());
        return super.produceMap(map);
    }

    @Override
    protected TilesFile createTilesFile() throws IOException {
        return new GeopackageWrapper();
    }

    /**
     * re-formats the original request to one that's inline with the GeoPKG specification. If the
     * CRS is XY (EAST_NORTH), the we do nothing. If the CRS is YX (NORTH_EAST), we attempt to
     * re-write the request in XY format. i.e. change the request CRS to the equivalent EAST_NORTH
     * CRS and flip the request's ordinates around in the request NOTE: geopkg requires XY
     * coordinate systems and WMS 1.3 assumes 4326 is YX (WMS 1.1's 4326 is XY). For a WMS 1.3
     * EPSG:4326 request, this will flip. For a WMS 1.1 EPSG:4326 request, this will NOT flip. NOTE:
     * typically, a WMS 1.1 and WMS 1.3 EPSG:4326 request will have the bbox ordinates flipped.
     * NOTE: updates request in-situ
     */
    private GetMapRequest rewriteRequest(GetMapRequest req) {
        CoordinateReferenceSystem sourceCRS = req.getCrs();

        if ((CRS.getAxisOrder(sourceCRS) == CRS.AxisOrder.EAST_NORTH)
                || (CRS.getAxisOrder(sourceCRS) == CRS.AxisOrder.INAPPLICABLE)) {
            return req;
        }

        for (ReferenceIdentifier identifier : sourceCRS.getIdentifiers()) {
            try {
                String _identifier = identifier.toString();
                CoordinateReferenceSystem flippedCRS = CRS.decode(_identifier, true);
                if (CRS.getAxisOrder(flippedCRS) == CRS.AxisOrder.EAST_NORTH) {
                    Envelope reqJTSEnvelope = req.getBbox();
                    ReferencedEnvelope reqEnvelope =
                            ReferencedEnvelope.create(reqJTSEnvelope, req.getCrs());
                    ReferencedEnvelope flippedEnvelope = reqEnvelope.transform(flippedCRS, false);

                    req.setBbox(flippedEnvelope);
                    req.setCrs(flippedCRS);
                }
            } catch (Exception e) {
                // couldn't flip - try again
            }
        }
        return req; // couldn't do the xform
    }

    /** Add tiles to an existing GeoPackage */
    public void addTiles(
            GeoPackage geopkg,
            TileEntry e,
            GetMapRequest req,
            String name,
            ProgressListener listener)
            throws IOException {
        addTiles(new GeopackageWrapper(geopkg, e), req, name, listener);
    }
}

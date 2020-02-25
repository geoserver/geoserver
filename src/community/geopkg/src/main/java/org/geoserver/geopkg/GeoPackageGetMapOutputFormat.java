/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geopkg;

import static org.geoserver.geopkg.GeoPkg.*;

import com.google.common.collect.Sets;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.gwc.GWC;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ServiceException;
import org.geoserver.tiles.AbstractTilesGetMapOutputFormat;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.RasterCleaner;
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
import org.geotools.util.logging.Logging;
import org.geowebcache.grid.Grid;
import org.geowebcache.grid.GridSet;
import org.geowebcache.grid.GridSubset;
import org.locationtech.jts.geom.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * WMS GetMap Output Format for GeoPackage
 *
 * @author Justin Deoliveira, Boundless
 */
public class GeoPackageGetMapOutputFormat extends AbstractTilesGetMapOutputFormat {

    static Logger LOGGER = Logging.getLogger("org.geoserver.geopkg");

    public GeoPackageGetMapOutputFormat(WebMapService webMapService, WMS wms, GWC gwc) {
        super(MIME_TYPE, "." + EXTENSION, Sets.newHashSet(NAMES), webMapService, wms, gwc);
    }

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
        map.getRequest().getFormatOptions().put("flipy", "true");
        return super.produceMap(map);
    }

    @Override
    protected TilesFile createTilesFile() throws IOException {
        return new GeopackageWrapper();
    }

    /** Add tiles to an existing GeoPackage */
    public void addTiles(GeoPackage geopkg, TileEntry e, GetMapRequest req, String name)
            throws IOException {
        addTiles(new GeopackageWrapper(geopkg, e), req, name);
    }

    /**
     * Special method to add tiles using Geopackage's own grid matrix system rather than GWC
     * gridsubsets
     */
    public void addTiles(
            GeoPackage geopkg,
            TileEntry e,
            GetMapRequest request,
            List<TileMatrix> matrices,
            String name)
            throws IOException, ServiceException {

        List<MapLayerInfo> mapLayers = request.getLayers();

        SortedMap<Integer, TileMatrix> matrixSet = new TreeMap<Integer, TileMatrix>();
        for (TileMatrix matrix : matrices) {
            matrixSet.put(matrix.getZoomLevel(), matrix);
        }

        if (mapLayers.isEmpty()) {
            return;
        }

        // Get the RasterCleaner object
        RasterCleaner cleaner = GeoServerExtensions.bean(RasterCleaner.class);

        // figure out the actual bounds of the tiles to be renderered
        ReferencedEnvelope bbox = bounds(request);

        // set metadata
        e.setTableName(name);
        e.setBounds(bbox);
        e.setSrid(srid(request));
        e.getTileMatricies().addAll(matrices);
        LOGGER.fine("Creating tile entry" + e.getTableName());
        geopkg.create(e);

        GetMapRequest req = new GetMapRequest();
        OwsUtils.copy(request, req, GetMapRequest.class);
        req.setLayers(mapLayers);

        Map formatOpts = req.getFormatOptions();

        Integer minZoom = null;
        if (formatOpts.containsKey("min_zoom")) {
            minZoom = Integer.parseInt(formatOpts.get("min_zoom").toString());
        }

        Integer maxZoom = null;
        if (formatOpts.containsKey("max_zoom")) {
            maxZoom = Integer.parseInt(formatOpts.get("max_zoom").toString());
        } else if (formatOpts.containsKey("num_zooms")) {
            maxZoom = minZoom + Integer.parseInt(formatOpts.get("num_zooms").toString());
        }

        if (minZoom != null || maxZoom != null) {
            matrixSet = matrixSet.subMap(minZoom, maxZoom);
        }

        String imageFormat =
                formatOpts.containsKey("format")
                        ? parseFormatFromOpts(formatOpts)
                        : findBestFormat(request);
        req.setFormat(imageFormat);

        CoordinateReferenceSystem crs = getCoordinateReferenceSystem(request);
        if (crs == null) {
            String srs = getSRS(request);
            try {
                crs = CRS.decode(srs);
            } catch (Exception ex) {
                throw new ServiceException(ex);
            }
        }
        double xSpan =
                crs.getCoordinateSystem().getAxis(0).getMaximumValue()
                        - crs.getCoordinateSystem().getAxis(0).getMinimumValue();
        double ySpan =
                crs.getCoordinateSystem().getAxis(1).getMaximumValue()
                        - crs.getCoordinateSystem().getAxis(1).getMinimumValue();
        double xOffset = crs.getCoordinateSystem().getAxis(0).getMinimumValue();
        double yOffset = crs.getCoordinateSystem().getAxis(1).getMinimumValue();

        req.setCrs(crs);

        // column and row bounds
        Integer minColumn = null, maxColumn = null, minRow = null, maxRow = null;
        if (formatOpts.containsKey("min_column")) {
            minColumn = Integer.parseInt(formatOpts.get("min_column").toString());
        }
        if (formatOpts.containsKey("max_column")) {
            maxColumn = Integer.parseInt(formatOpts.get("max_column").toString());
        }
        if (formatOpts.containsKey("min_row")) {
            minRow = Integer.parseInt(formatOpts.get("min_row").toString());
        }
        if (formatOpts.containsKey("max_row")) {
            maxRow = Integer.parseInt(formatOpts.get("max_row").toString());
        }

        for (TileMatrix matrix : matrixSet.values()) {

            req.setWidth(matrix.getTileWidth());
            req.setHeight(matrix.getTileHeight());

            // long[] intersect = gridSubset.getCoverageIntersection(z, bbox);
            double resX = xSpan / matrix.getMatrixWidth();
            double resY = ySpan / matrix.getMatrixHeight();

            long minX = Math.round(Math.floor((bbox.getMinX() - xOffset) / resX));
            long minY = Math.round(Math.floor((bbox.getMinY() - yOffset) / resY));
            long maxX = Math.round(Math.ceil((bbox.getMaxX() - xOffset) / resX));
            long maxY = Math.round(Math.ceil((bbox.getMaxY() - yOffset) / resY));

            minX = minColumn == null ? minX : Math.max(minColumn, minX);
            maxX = maxColumn == null ? maxX : Math.min(maxColumn, maxX);
            minY = minRow == null ? minY : Math.max(minRow, minY);
            maxY = maxRow == null ? maxY : Math.min(maxRow, maxY);

            for (long x = minX; x < maxX; x++) {
                for (long y = minY; y < maxY; y++) {
                    req.setBbox(
                            new Envelope(
                                    xOffset + x * resX,
                                    xOffset + (x + 1) * resX,
                                    yOffset + y * resY,
                                    yOffset + (y + 1) * resY));
                    WebMap result = webMapService.getMap(req);
                    Tile t = new Tile();
                    t.setZoom(matrix.getZoomLevel());
                    t.setColumn((int) x);
                    t.setRow((int) y);
                    t.setData(toBytes(result));
                    geopkg.add(e, t);
                    // Cleanup
                    cleaner.finished(null);
                }
            }
        }
    }
}

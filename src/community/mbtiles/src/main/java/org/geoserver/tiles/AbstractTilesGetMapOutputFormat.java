/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.tiles;

import static java.lang.String.format;

import com.google.common.base.Preconditions;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import org.geoserver.gwc.GWC;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.MapProducerCapabilities;
import org.geoserver.wms.RasterCleaner;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WebMap;
import org.geoserver.wms.WebMapService;
import org.geoserver.wms.map.AbstractMapOutputFormat;
import org.geoserver.wms.map.JPEGMapResponse;
import org.geoserver.wms.map.PNGMapResponse;
import org.geoserver.wms.map.RawMap;
import org.geoserver.wms.map.RenderedImageMap;
import org.geoserver.wms.map.RenderedImageMapResponse;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.Layer;
import org.geotools.referencing.CRS;
import org.geotools.renderer.lite.RendererUtilities;
import org.geotools.util.logging.Logging;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.Grid;
import org.geowebcache.grid.GridSet;
import org.geowebcache.grid.GridSetBroker;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.grid.GridSubsetFactory;
import org.geowebcache.grid.SRS;
import org.geowebcache.layer.TileLayer;
import org.locationtech.jts.geom.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Abstract class for tiles style GetMapOutputFormat (mbtiles && geopackage)
 *
 * @author Justin Deoliveira, Boundless
 * @author Niels Charlier
 */
public abstract class AbstractTilesGetMapOutputFormat extends AbstractMapOutputFormat {

    /** Wrapper class for tiles file, allows generic access */
    protected static interface TilesFile {

        public void setMetadata(
                String name,
                ReferencedEnvelope box,
                String imageFormat,
                int srid,
                List<MapLayerInfo> mapLayers,
                int[] minmax,
                GridSubset gridSubset)
                throws IOException, ServiceException;

        public void addTile(int zoom, int x, int y, byte[] data)
                throws IOException, ServiceException;

        public File getFile();

        public void close();
    }

    protected static final int TILE_CLEANUP_INTERVAL;

    static {
        // calculate the number of tiles we can generate before having to cleanup, value is
        // 25% of total memory / approximte size of single tile
        TILE_CLEANUP_INTERVAL = (int) (Runtime.getRuntime().maxMemory() * 0.05 / (256.0 * 256 * 4));
    }

    protected static Logger LOGGER = Logging.getLogger(AbstractTilesGetMapOutputFormat.class);

    protected static final String PNG_MIME_TYPE = "image/png";

    protected static final String JPEG_MIME_TYPE = "image/jpeg";

    protected WebMapService webMapService;

    protected WMS wms;

    protected GWC gwc;

    protected String extension;

    public AbstractTilesGetMapOutputFormat(
            String mimeType,
            String extension,
            Set<String> names,
            WebMapService webMapService,
            WMS wms,
            GWC gwc) {
        super(mimeType, names);
        this.webMapService = webMapService;
        this.wms = wms;
        this.gwc = gwc;
        this.extension = extension;
    }

    @Override
    public MapProducerCapabilities getCapabilities(String format) {
        return new MapProducerCapabilities(false, false, false, true, null);
    }

    @Override
    public WebMap produceMap(WMSMapContent map) throws ServiceException, IOException {
        TilesFile tiles = createTilesFile();
        addTiles(tiles, map);
        tiles.close();

        final File dbFile = tiles.getFile();
        final BufferedInputStream bin = new BufferedInputStream(new FileInputStream(dbFile));

        RawMap result =
                new RawMap(map, bin, getMimeType()) {
                    @Override
                    public void writeTo(OutputStream out) throws IOException {
                        String dbFilename = getAttachmentFileName();
                        if (dbFilename != null) {
                            dbFilename =
                                    dbFilename.substring(0, dbFilename.length() - 4) + extension;
                        } else {
                            // this shouldn't really ever happen, but fallback anyways
                            dbFilename = "tiles" + extension;
                        }

                        IOUtils.copy(bin, out);
                        out.flush();
                        bin.close();
                        try {
                            dbFile.delete();
                        } catch (Exception e) {
                            LOGGER.log(
                                    Level.WARNING,
                                    "Error deleting file: " + dbFile.getAbsolutePath(),
                                    e);
                        }
                    }
                };

        result.setContentDispositionHeader(map, extension, true);
        return result;
    }

    /** Factory method for Tiles File */
    protected abstract TilesFile createTilesFile() throws IOException;

    protected void addTiles(TilesFile tiles, WMSMapContent map)
            throws ServiceException, IOException {
        GetMapRequest req = map.getRequest();

        List<Layer> layers = map.layers();
        List<MapLayerInfo> mapLayers = req.getLayers();

        Preconditions.checkState(
                layers.size() == mapLayers.size(),
                "Number of map layers not same as number of rendered layers");

        addTiles(tiles, req, map.getTitle());
    }

    protected void addTiles(TilesFile tiles, GetMapRequest req, String name)
            throws ServiceException, IOException {
        List<MapLayerInfo> mapLayers = req.getLayers();

        // list of layers to render directly and include as tiles
        List<MapLayerInfo> tileLayers = new ArrayList<MapLayerInfo>();

        // tiled mode means render all as map tile layer
        tileLayers.addAll(mapLayers);

        addTiles(tiles, tileLayers, req, name);
    }

    /** Add the tiles */
    protected void addTiles(
            TilesFile tiles, List<MapLayerInfo> mapLayers, GetMapRequest request, String name)
            throws IOException, ServiceException {

        if (mapLayers.isEmpty()) {
            return;
        }

        // Get the RasterCleaner object
        RasterCleaner cleaner = GeoServerExtensions.bean(RasterCleaner.class);

        // figure out a name for the file entry
        String tileEntryName = null;
        Map formatOpts = request.getFormatOptions();
        if (formatOpts.containsKey("tileset_name")) {
            tileEntryName = (String) formatOpts.get("tileset_name");
        }
        if (name != null) {
            tileEntryName = name;
        }
        if (tileEntryName == null) {
            Iterator<MapLayerInfo> it = mapLayers.iterator();
            tileEntryName = "";
            while (it.hasNext()) {
                tileEntryName += it.next().getLayerInfo().getName() + "_";
            }
            tileEntryName = tileEntryName.substring(0, tileEntryName.length() - 1);
        }

        // figure out the actual bounds of the tiles to be renderered
        BoundingBox bbox = bbox(request);
        GridSubset gridSubset = findBestGridSubset(request);
        int[] minmax = findMinMaxZoom(gridSubset, request);
        // ReferencedEnvelope bounds = new ReferencedEnvelope(findTileBounds(gridSubset, bbox,
        //        minmax[0]), getCoordinateReferenceSystem(map));

        // create a prototype getmap request
        GetMapRequest req = new GetMapRequest();
        OwsUtils.copy(request, req, GetMapRequest.class);
        req.setLayers(mapLayers);

        String imageFormat =
                formatOpts.containsKey("format")
                        ? parseFormatFromOpts(formatOpts)
                        : findBestFormat(request);

        req.setFormat(imageFormat);
        req.setWidth(gridSubset.getTileWidth());
        req.setHeight(gridSubset.getTileHeight());
        req.setCrs(getCoordinateReferenceSystem(request));

        // store metadata
        tiles.setMetadata(
                tileEntryName,
                bounds(request),
                imageFormat,
                srid(request),
                mapLayers,
                minmax,
                gridSubset);

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

        // flag determining if tile row indexes we store in database should be inverted
        boolean flipy = Boolean.valueOf((String) formatOpts.get("flipy"));
        for (int z = minmax[0]; z < minmax[1]; z++) {
            long[] intersect = gridSubset.getCoverageIntersection(z, bbox);
            long minX = minColumn == null ? intersect[0] : Math.max(minColumn, intersect[0]);
            long maxX = maxColumn == null ? intersect[2] : Math.min(maxColumn, intersect[2]);
            long minY = minRow == null ? intersect[1] : Math.max(minRow, intersect[1]);
            long maxY = maxRow == null ? intersect[3] : Math.min(maxRow, intersect[3]);
            for (long x = minX; x <= maxX; x++) {
                for (long y = minY; y <= maxY; y++) {
                    BoundingBox box = gridSubset.boundsFromIndex(new long[] {x, y, z});
                    req.setBbox(
                            new Envelope(
                                    box.getMinX(), box.getMaxX(), box.getMinY(), box.getMaxY()));
                    WebMap result = webMapService.getMap(req);
                    tiles.addTile(
                            z,
                            (int) x,
                            (int) (flipy ? gridSubset.getNumTilesHigh(z) - (y + 1) : y),
                            toBytes(result));
                    // Cleanup
                    cleaner.finished(null);
                }
            }
        }
    }

    protected ReferencedEnvelope bounds(GetMapRequest req) {
        return new ReferencedEnvelope(req.getBbox(), req.getCrs());
    }

    protected CoordinateReferenceSystem getCoordinateReferenceSystem(GetMapRequest req) {
        return req.getCrs();
    }

    protected String getSRS(GetMapRequest req) {
        return req.getSRS() != null ? req.getSRS().toUpperCase() : null;
    }

    // utility methods:

    protected BoundingBox bbox(GetMapRequest req) {
        Envelope bnds = bounds(req);
        return new BoundingBox(bnds.getMinX(), bnds.getMinY(), bnds.getMaxX(), bnds.getMaxY());
    }

    protected Integer srid(GetMapRequest req) {
        Integer srid = null;
        try {
            if (getCoordinateReferenceSystem(req) != null) {
                srid = CRS.lookupEpsgCode(getCoordinateReferenceSystem(req), false);
            }
            if (srid == null) {
                srid = Integer.parseInt(getSRS(req).split(":")[1]);
            }
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Error determining srid", ex);
        }
        return srid;
    }

    protected Envelope findTileBounds(GridSubset gridSubset, BoundingBox bbox, int z) {

        long[] i = gridSubset.getCoverageIntersection(z, bbox);

        BoundingBox b1 = gridSubset.boundsFromIndex(new long[] {i[0], i[1], i[4]});
        BoundingBox b2 = gridSubset.boundsFromIndex(new long[] {i[2], i[3], i[4]});
        return new Envelope(
                Math.min(b1.getMinX(), b2.getMinX()),
                Math.max(b1.getMaxX(), b2.getMaxX()),
                Math.min(b1.getMinY(), b2.getMinY()),
                Math.max(b1.getMaxY(), b2.getMaxY()));
    }

    protected GridSubset findBestGridSubset(GetMapRequest req) {
        Map formatOpts = req.getFormatOptions();

        GridSetBroker gridSetBroker = gwc.getGridSetBroker();
        GridSet gridSet = null;

        // first check format options to see if explicitly specified
        if (formatOpts.containsKey("gridset")) {
            gridSet = gridSetBroker.get(formatOpts.get("gridset").toString());
        }

        // next check srs
        if (gridSet == null && getSRS(req) != null) {
            gridSet = gridSetBroker.get(getSRS(req));
        }

        if (gridSet != null) {
            return GridSubsetFactory.createGridSubSet(gridSet);
        }

        CoordinateReferenceSystem crs = getCoordinateReferenceSystem(req);

        // look up epsg code
        Integer epsgCode = null;
        try {
            epsgCode = CRS.lookupEpsgCode(crs, false);
        } catch (Exception e) {
            throw new ServiceException("Unable to determine epsg code for " + crs, e);
        }
        if (epsgCode == null) {
            throw new ServiceException("Unable to determine epsg code for " + crs);
        }

        SRS srs = SRS.getSRS(epsgCode);

        // figure out the appropriate grid sub set
        Set<GridSubset> gridSubsets = new LinkedHashSet<GridSubset>();
        for (MapLayerInfo l : req.getLayers()) {
            TileLayer tl = gwc.getTileLayerByName(l.getName());
            if (tl == null) {
                throw new ServiceException("No tile layer for " + l.getName());
            }

            List<GridSubset> theseGridSubsets = tl.getGridSubsetsForSRS(srs);
            if (gridSubsets.isEmpty()) {
                gridSubsets.addAll(theseGridSubsets);
            } else {
                gridSubsets.retainAll(theseGridSubsets);
            }

            if (gridSubsets.isEmpty()) {
                throw new ServiceException(
                        "No suitable " + epsgCode + " grid subset for " + req.getLayers());
            }
        }

        if (gridSubsets.size() > 1) {
            if (LOGGER.isLoggable(Level.WARNING)) {
                StringBuilder msg = new StringBuilder("Found multiple grid subsets: ");
                for (GridSubset gs : gridSubsets) {
                    msg.append(gs.getName()).append(", ");
                }
                msg.setLength(msg.length() - 2);
                msg.append(". Choosing first.");
                LOGGER.warning(msg.toString());
            }
        }

        return gridSubsets.iterator().next();
    }

    protected int[] findMinMaxZoom(GridSubset gridSubset, GetMapRequest req) {
        GridSet gridSet = gridSubset.getGridSet();
        Map formatOpts = req.getFormatOptions();

        Integer minZoom = null;
        if (formatOpts.containsKey("min_zoom")) {
            minZoom = Integer.parseInt(formatOpts.get("min_zoom").toString());
        }
        if (minZoom == null) {
            minZoom = findClosestZoom(gridSet, req);
        }

        Integer maxZoom = null;
        if (formatOpts.containsKey("max_zoom")) {
            maxZoom = Integer.parseInt(formatOpts.get("max_zoom").toString());
        } else if (formatOpts.containsKey("num_zooms")) {
            maxZoom = minZoom + Integer.parseInt(formatOpts.get("num_zooms").toString());
        }

        if (maxZoom == null) {
            // walk down until we hit too many tiles
            maxZoom = findMaxZoomAuto(gridSubset, minZoom, req);
        }

        if (maxZoom < minZoom) {
            throw new ServiceException(
                    format("maxZoom (%d) can not be less than minZoom (%d)", maxZoom, minZoom));
        }

        // end index
        if (maxZoom > gridSet.getNumLevels()) {
            LOGGER.warning(
                    format(
                            "Max zoom (%d) can't be greater than number of zoom levels (%d)",
                            maxZoom, gridSet.getNumLevels()));
            maxZoom = gridSet.getNumLevels();
        }

        return new int[] {minZoom, maxZoom};
    }

    protected Integer findClosestZoom(GridSet gridSet, GetMapRequest req) {
        double reqScale =
                RendererUtilities.calculateOGCScale(bounds(req), gridSet.getTileWidth(), null);

        int i = 0;
        double error = Math.abs(gridSet.getGrid(i).getScaleDenominator() - reqScale);
        while (i < gridSet.getNumLevels() - 1) {
            Grid g = gridSet.getGrid(i + 1);
            double e = Math.abs(g.getScaleDenominator() - reqScale);

            if (e > error) {
                break;
            } else {
                error = e;
            }
            i++;
        }

        return Math.max(i, 0);
    }

    protected Integer findMaxZoomAuto(GridSubset gridSubset, Integer minZoom, GetMapRequest req) {
        BoundingBox bbox = bbox(req);

        int zoom = minZoom;
        int ntiles = 0;

        while (ntiles < 256 && zoom < gridSubset.getGridSet().getNumLevels()) {
            long[] intersect = gridSubset.getCoverageIntersection(zoom, bbox);
            ntiles += (intersect[2] - intersect[0] + 1) * (intersect[3] - intersect[1] + 1);
            zoom++;
        }
        return zoom;
    }

    protected String parseFormatFromOpts(Map formatOpts) {
        String format = (String) formatOpts.get("format");
        return format.contains("/") ? format : "image/" + format;
    }

    protected String findBestFormat(GetMapRequest req) {
        // if request is a single coverage layer return jpeg, otherwise use just png
        List<MapLayerInfo> layers = req.getLayers();
        if (layers.size() == 1 && layers.get(0).getType() == MapLayerInfo.TYPE_RASTER) {
            return JPEG_MIME_TYPE;
        }
        return PNG_MIME_TYPE;
    }

    protected byte[] toBytes(WebMap map) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();

        if (map instanceof RenderedImageMap) {
            RenderedImageMapResponse response =
                    JPEG_MIME_TYPE.equals(map.getMimeType())
                            ? new JPEGMapResponse(wms)
                            : new PNGMapResponse(wms);
            response.write(map, bout, null);
        } else if (map instanceof RawMap) {
            ((RawMap) map).writeTo(bout);
        }
        bout.flush();
        return bout.toByteArray();
    }
}

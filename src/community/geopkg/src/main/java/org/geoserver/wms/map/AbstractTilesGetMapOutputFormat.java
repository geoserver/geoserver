package org.geoserver.wms.map;

import static java.lang.String.format;

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
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.google.common.base.Preconditions;
import com.vividsolutions.jts.geom.Envelope;

/**
 * 
 * Abstract class for tiles style GetMapOutputFormat (mbtiles && geopackage)
 * 
 * @author Justin Deoliveira, Boundless
 * @author Niels Charlier
 * 
 */
public abstract class AbstractTilesGetMapOutputFormat extends AbstractMapOutputFormat {

    /**
     * Wrapper class for tiles file, allows generic access
     * 
     */
    protected static interface TilesFile {

        public void setMetadata(String name, ReferencedEnvelope box, String imageFormat, int srid,
                List<MapLayerInfo> mapLayers, int[] minmax, GridSubset gridSubset)
                throws IOException, ServiceException;

        public void addTile(int zoom, int x, int y, byte[] data) throws IOException,
                ServiceException;

        public File getFile();
        
        public void close();

    }

    static final protected int TILE_CLEANUP_INTERVAL;
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

    public AbstractTilesGetMapOutputFormat(String mimeType, String extension, Set<String> names,
            WebMapService webMapService, WMS wms, GWC gwc) {
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
        GetMapRequest req = map.getRequest();

        List<Layer> layers = map.layers();
        List<MapLayerInfo> mapLayers = req.getLayers();

        Preconditions.checkState(layers.size() == mapLayers.size(),
                "Number of map layers not same as number of rendered layers");

        // list of layers to render directly and include as tiles
        List<MapLayerInfo> tileLayers = new ArrayList<MapLayerInfo>();

        // tiled mode means render all as map tile layer
        tileLayers.addAll(mapLayers);

        TilesFile tiles = createTilesFile();
        addTiles(tiles, tileLayers, map);
        tiles.close();

        final File dbFile = tiles.getFile();
        final BufferedInputStream bin = new BufferedInputStream(new FileInputStream(dbFile));

        RawMap result = new RawMap(map, bin, getMimeType()) {
            @Override
            public void writeTo(OutputStream out) throws IOException {
                String dbFilename = getAttachmentFileName();
                if (dbFilename != null) {
                    dbFilename = dbFilename.substring(0, dbFilename.length() - 4) + extension;
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
                    LOGGER.log(Level.WARNING, "Error deleting file: " + dbFile.getAbsolutePath(), e);
                }
            }
        };

        result.setContentDispositionHeader(map, extension, true);
        return result;
    }

    /**
     * Factory method for Tiles File
     * 
     * @return
     * @throws IOException
     */
    protected abstract TilesFile createTilesFile() throws IOException;

    /**
     * Add the tiles
     * 
     * @param tiles
     * @param mapLayers
     * @param map
     * @throws IOException
     * @throws ServiceException
     */
    protected void addTiles(TilesFile tiles, List<MapLayerInfo> mapLayers, WMSMapContent map)
            throws IOException, ServiceException {

        if (mapLayers.isEmpty()) {
            return;
        }

        // figure out a name for the file entry
        String tileEntryName = null;
        Map formatOpts = map.getRequest().getFormatOptions();
        if (formatOpts.containsKey("tileset_name")) {
            tileEntryName = (String) formatOpts.get("tileset_name");
        }
        if (tileEntryName == null) {
            tileEntryName = map.getTitle();
        }
        if (tileEntryName == null) {
            Iterator<MapLayerInfo> it = mapLayers.iterator();
            tileEntryName = "";
            while (it.hasNext()){
                tileEntryName += it.next().getLayerInfo().getName() + "_";
            }
            tileEntryName = tileEntryName.substring(0, tileEntryName.length()-1);
        } 

        // figure out the actual bounds of the tiles to be renderered
        BoundingBox bbox = bbox(map);
        GridSubset gridSubset = findBestGridSubset(map);
        int[] minmax = findMinMaxZoom(gridSubset, map);
        ReferencedEnvelope bounds = new ReferencedEnvelope(findTileBounds(gridSubset, bbox,
                minmax[0]), getCoordinateReferenceSystem(map));

        // create a prototype getmap request
        GetMapRequest req = new GetMapRequest();
        OwsUtils.copy(map.getRequest(), req, GetMapRequest.class);
        req.setLayers(mapLayers);

        String imageFormat = formatOpts.containsKey("format") ? parseFormatFromOpts(formatOpts)
                : findBestFormat(map);

        req.setFormat(imageFormat);
        req.setWidth(gridSubset.getTileWidth());
        req.setHeight(gridSubset.getTileHeight());

        // store metadata
        tiles.setMetadata(tileEntryName, bounds, imageFormat, srid(map), mapLayers, minmax,
                gridSubset);

        // count tiles as we generate them
        int ntiles = 0;

        // flag determining if tile row indexes we store in database should be inverted
        boolean flipy = Boolean.valueOf((String) formatOpts.get("flipy"));
        for (int z = minmax[0]; z < minmax[1]; z++) {
            long[] intersect = gridSubset.getCoverageIntersection(z, bbox);
            for (long x = intersect[0]; x <= intersect[2]; x++) {
                for (long y = intersect[1]; y <= intersect[3]; y++) {
                    BoundingBox box = gridSubset.boundsFromIndex(new long[] { x, y, z });
                    req.setBbox(new Envelope(box.getMinX(), box.getMaxX(), box.getMinY(), box
                            .getMaxY()));

                    WebMap result = webMapService.getMap(req);
                    tiles.addTile(z, (int) x, (int) (flipy ? gridSubset.getNumTilesHigh(z)
                            - (y + 1) : y), toBytes(result));

                    // images we encode are actually kept around, we need to clean them up
                    if (ntiles++ == TILE_CLEANUP_INTERVAL) {
                        cleanUpImages();
                        ntiles = 0;
                    }
                }
            }
        }
    }

    protected ReferencedEnvelope bounds(WMSMapContent map) {
        return new ReferencedEnvelope(map.getRequest().getBbox(),
                map.getCoordinateReferenceSystem());
    }

    protected CoordinateReferenceSystem getCoordinateReferenceSystem(WMSMapContent map) {
        return map.getCoordinateReferenceSystem();
    }

    // utility methods:

    protected BoundingBox bbox(WMSMapContent map) {
        Envelope bnds = bounds(map);
        return new BoundingBox(bnds.getMinX(), bnds.getMinY(), bnds.getMaxX(), bnds.getMaxY());
    }

    Integer srid(WMSMapContent map) {
        Integer srid = null;
        try {
            srid = CRS.lookupEpsgCode(getCoordinateReferenceSystem(map), false);
            if (srid == null) {
                srid = Integer.parseInt(map.getRequest().getSRS().split(":")[1]);
            }
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Error determining srid", ex);
        }
        return srid;
    }

    protected Envelope findTileBounds(GridSubset gridSubset, BoundingBox bbox, int z) {

        long[] i = gridSubset.getCoverageIntersection(z, bbox);

        BoundingBox b1 = gridSubset.boundsFromIndex(new long[] { i[0], i[1], i[4] });
        BoundingBox b2 = gridSubset.boundsFromIndex(new long[] { i[2], i[3], i[4] });
        return new Envelope(Math.min(b1.getMinX(), b2.getMinX()), Math.max(b1.getMaxX(),
                b2.getMaxX()), Math.min(b1.getMinY(), b2.getMinY()), Math.max(b1.getMaxY(),
                b2.getMaxY()));
    }

    protected GridSubset findBestGridSubset(WMSMapContent map) {
        GetMapRequest req = map.getRequest();
        Map formatOpts = req.getFormatOptions();

        GridSetBroker gridSetBroker = gwc.getGridSetBroker();
        GridSet gridSet = null;

        // first check format options to see if explicitly specified
        if (formatOpts.containsKey("gridset")) {
            gridSet = gridSetBroker.get(formatOpts.get("gridset").toString());
        }

        // next check srs
        if (gridSet == null) {
            gridSet = gridSetBroker.get(req.getSRS().toUpperCase());
        }

        if (gridSet != null) {
            return GridSubsetFactory.createGridSubSet(gridSet);
        }

        CoordinateReferenceSystem crs = getCoordinateReferenceSystem(map);

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
                throw new ServiceException("No suitable " + epsgCode + " grid subset for "
                        + req.getLayers());
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

    protected int[] findMinMaxZoom(GridSubset gridSubset, WMSMapContent map) {
        GridSet gridSet = gridSubset.getGridSet();
        Map formatOpts = map.getRequest().getFormatOptions();

        Integer minZoom = null;
        if (formatOpts.containsKey("min_zoom")) {
            minZoom = Integer.parseInt(formatOpts.get("min_zoom").toString());
        }
        if (minZoom == null) {
            minZoom = findClosestZoom(gridSet, map);
        }

        Integer maxZoom = null;
        if (formatOpts.containsKey("max_zoom")) {
            maxZoom = Integer.parseInt(formatOpts.get("max_zoom").toString());
        } else if (formatOpts.containsKey("num_zooms")) {
            maxZoom = minZoom + Integer.parseInt(formatOpts.get("num_zooms").toString());
        }

        if (maxZoom == null) {
            // walk down until we hit too many tiles
            maxZoom = findMaxZoomAuto(gridSubset, minZoom, map);
        }

        if (maxZoom < minZoom) {
            throw new ServiceException(format("maxZoom (%d) can not be less than minZoom (%d)",
                    maxZoom, minZoom));
        }

        // end index
        if (maxZoom > gridSet.getNumLevels()) {
            LOGGER.warning(format("Max zoom (%d) can't be greater than number of zoom levels (%d)",
                    maxZoom, gridSet.getNumLevels()));
            maxZoom = gridSet.getNumLevels();
        }

        return new int[] { minZoom, maxZoom };
    }

    protected Integer findClosestZoom(GridSet gridSet, WMSMapContent map) {
        double reqScale = RendererUtilities.calculateOGCScale(bounds(map), gridSet.getTileWidth(),
                null);

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

    protected Integer findMaxZoomAuto(GridSubset gridSubset, Integer minZoom, WMSMapContent map) {
        BoundingBox bbox = bbox(map);

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

    protected String findBestFormat(WMSMapContent map) {
        // if request is a single coverage layer return jpeg, otherwise use just png
        List<MapLayerInfo> layers = map.getRequest().getLayers();
        if (layers.size() == 1 && layers.get(0).getType() == MapLayerInfo.TYPE_RASTER) {
            return JPEG_MIME_TYPE;
        }
        return PNG_MIME_TYPE;
    }

    protected byte[] toBytes(WebMap map) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();

        if (map instanceof RenderedImageMap) {
            RenderedImageMapResponse response = JPEG_MIME_TYPE.equals(map.getMimeType()) ? new JPEGMapResponse(
                    wms) : new PNGMapResponse(wms);
            response.write(map, bout, null);
        } else if (map instanceof RawMap) {
            ((RawMap) map).writeTo(bout);
        }
        bout.flush();
        return bout.toByteArray();
    }

    protected void cleanUpImages() {
        RasterCleaner cleaner = GeoServerExtensions.bean(RasterCleaner.class);
        cleaner.finished(null);
    }
}

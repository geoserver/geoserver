package org.geoserver.geopkg;

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
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.ResourceInfo;
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
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geopkg.Entry;
import org.geotools.geopkg.FeatureEntry;
import org.geotools.geopkg.GeoPackage;
import org.geotools.geopkg.RasterEntry;
import org.geotools.geopkg.Tile;
import org.geotools.geopkg.TileEntry;
import org.geotools.geopkg.TileMatrix;
import org.geotools.map.FeatureLayer;
import org.geotools.map.GridCoverageLayer;
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
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.spatial.BBOX;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.vividsolutions.jts.geom.Envelope;

public class GeoPackageOutputFormat extends AbstractMapOutputFormat {

    static enum Mode {
        VECTOR, HYBRID,TILED;
    }
    static Logger LOGGER = Logging.getLogger("org.geoserver.geopkg");

    static final String MIME_TYPE = "application/x-sqlite3";
    //static final String MIME_TYPE = "application/zip";

    static final String PNG_MIME_TYPE = "image/png";

    static final String JPEG_MIME_TYPE = "image/jpeg";

    static final Set<String> NAMES = Sets.newHashSet("geopackage", "geopkg", "gpkg");

    static final int TILE_CLEANUP_INTERVAL;
    static {
        //calculate the number of tiles we can generate before having to cleanup, value is
        //  25% of total memory / approximte size of single tile
        TILE_CLEANUP_INTERVAL = (int) (Runtime.getRuntime().maxMemory() * 0.05 / (256.0*256*4)); 
    }

    static FilterFactory filterFactory = CommonFactoryFinder.getFilterFactory();

    WebMapService webMapService;
    WMS wms;
    GWC gwc;

    public GeoPackageOutputFormat(WebMapService webMapService, WMS wms, GWC gwc) {
        super(MIME_TYPE, NAMES);
        this.webMapService = webMapService;
        this.wms = wms;
        this.gwc = gwc;
    }

    @Override
    public MapProducerCapabilities getCapabilities(String format) {
        return new MapProducerCapabilities(false, false, false, true, null);
    }

    @Override
    public WebMap produceMap(WMSMapContent map) throws ServiceException, IOException {
        GeoPackage geopkg = new GeoPackage();
        geopkg.init();

        GetMapRequest req = map.getRequest();

        List<Layer> layers = map.layers();
        List<MapLayerInfo> mapLayers = req.getLayers();

        Preconditions.checkState(layers.size() == mapLayers.size(), 
            "Number of map layers not same as number of rendered layers");

        //list of layers to render directly and include as tiles
        List<MapLayerInfo> tileLayers = new ArrayList(); 

        //check mode, one of:
        // vector - render vector layers as feature entries and all else as tiles (default)
        // hybrid - render vector layers as feature entries, raster layers as raster entries, all
        //          others as tile entries
        // tiled - all layers as a single tile set
        Map formatOpts = req.getFormatOptions();
        Mode mode = formatOpts.containsKey("mode") 
            ? Mode.valueOf(((String) formatOpts.get("mode")).toUpperCase()) : Mode.VECTOR;

        if (mode == Mode.TILED) {
            //tiled mode means render all as map tile layer
            tileLayers.addAll(mapLayers);
        }
        else {
            
            //hybrid mode, dump as raw vector or raster unless the request specifically asks for a
            // layer to be rendered as tiles
            for (int i = 0; i < layers.size(); i++) {
                Layer layer = layers.get(i);
                MapLayerInfo mapLayer = mapLayers.get(i);
    
                if (layer instanceof FeatureLayer) {
                    addFeatureLayer(geopkg, (FeatureLayer)layer, mapLayer, map);
                }
                else if (layer instanceof GridCoverageLayer) {
                    if (mode == Mode.HYBRID) {
                        addCoverageLayer(geopkg, (GridCoverageLayer)layer, mapLayer, map);    
                    }
                    else {
                        tileLayers.add(mapLayer);
                    }
                }
                else {
                    tileLayers.add(mapLayer);
                }
            }
        }

        addTileLayers(geopkg, tileLayers, map);

        geopkg.close();

        final File dbFile = geopkg.getFile();
        final BufferedInputStream bin = new BufferedInputStream(new FileInputStream(dbFile));

        RawMap result = new RawMap(map, bin, MIME_TYPE) {
            @Override
            public void writeTo(OutputStream out) throws IOException {
                String dbFilename = getAttachmentFileName();
                if (dbFilename != null) {
                    dbFilename = dbFilename.substring(0, dbFilename.length()-4) + ".gpkg";
                }
                else {
                    //this shouldn't really ever happen, but fallback anyways
                    dbFilename = "geoserver.gpkg";
                }

                IOUtils.copy(bin, out);
                out.flush();
                

//               JD: disabling zip compression for now
//                ZipOutputStream zout = new ZipOutputStream(out);
//                zout.putNextEntry(new ZipEntry(dbFilename));
//
//                super.writeTo(zout);
//                zout.closeEntry();
//                zout.close();

                bin.close();
                try {
                    dbFile.delete();
                }
                catch(Exception e) {
                    LOGGER.log(Level.WARNING, "Error deleting file: " + dbFile.getAbsolutePath(), e);
                }
            }
        };

        result.setContentDispositionHeader(map, ".gpkg", true);
        return result;
    }

    /*
     * Adds a feature layer to the geopackage.
     */
    void addFeatureLayer(GeoPackage geopkg, FeatureLayer layer, MapLayerInfo mapLayer, 
        WMSMapContent map) throws IOException {

        FeatureEntry e = new FeatureEntry();
        initEntry(e, layer, mapLayer, map);

        Filter filter = layer.getQuery().getFilter();
        GeometryDescriptor gd = mapLayer.getFeature().getFeatureType().getGeometryDescriptor();
        if (gd != null) {
            Envelope bnds = bounds(map);
            BBOX bboxFilter = filterFactory.bbox(gd.getLocalName(), bnds.getMinX(), bnds.getMinY(), 
                bnds.getMaxX(), bnds.getMaxY(), map.getRequest().getSRS());
            filter = filterFactory.and(filter, bboxFilter);
        }

        LOGGER.fine("Creating feature entry" + e.getTableName());
        geopkg.add(e, layer.getSimpleFeatureSource(), filter);
    }

    void addCoverageLayer(GeoPackage geopkg, GridCoverageLayer layer, MapLayerInfo mapLayer, WMSMapContent map) 
        throws IOException {

        RasterEntry e = new RasterEntry();
        initEntry(e, layer, mapLayer, map);

        //TODO: ensure this is one of the supported formats
        AbstractGridFormat format = mapLayer.getCoverage().getStore().getFormat();

        LOGGER.fine("Creating raster entry" + e.getTableName());
        geopkg.add(e, layer.getCoverage(), format);
    }

    void addTileLayers(GeoPackage geopkg, List<MapLayerInfo> mapLayers, WMSMapContent map) 
        throws IOException {

        if (mapLayers.isEmpty()) {
            return;
        }

        //figure out a name for the file entry
        String tileEntryName = null;
        Map formatOpts = map.getRequest().getFormatOptions();
        if (formatOpts.containsKey("tileset_name")) {
            tileEntryName = (String) formatOpts.get("tileset_name");
        }
        if (tileEntryName == null) {
            tileEntryName = map.getTitle();
        }
        if (tileEntryName == null && mapLayers.size() == 1) {
            Iterator<MapLayerInfo> it = mapLayers.iterator();
            tileEntryName = it.next().getLayerInfo().getName();
        }

        GridSubset gridSubset = findBestGridSubset(map);
        int[] minmax = findMinMaxZoom(gridSubset, map);

        BoundingBox bbox = bbox(map);

        TileEntry e = new TileEntry();
        e.setTableName(tileEntryName);
        
        if (mapLayers.size() == 1) {
            ResourceInfo r = mapLayers.get(0).getResource();
            e.setIdentifier(r.getTitle());
            e.setDescription(r.getAbstract());
        }
        e.setBounds(new ReferencedEnvelope(findTileBounds(gridSubset, bbox, minmax[0]), 
            map.getCoordinateReferenceSystem()));
        e.setSrid(srid(map));

        GridSet gridSet = gridSubset.getGridSet();
        for (int z = minmax[0]; z < minmax[1]; z++) {
            Grid g = gridSet.getGrid(z);

            TileMatrix m = new TileMatrix();
            m.setZoomLevel(z);
            m.setMatrixWidth((int) g.getNumTilesWide());
            m.setMatrixHeight((int) g.getNumTilesHigh());
            m.setTileWidth(gridSubset.getTileWidth());
            m.setTileHeight(gridSubset.getTileHeight());

            //TODO: not sure about this
            m.setXPixelSize(g.getResolution());
            m.setYPixelSize(g.getResolution());
            //m.setXPixelSize(gridSet.getPixelSize());
            //m.setYPixelSize(gridSet.getPixelSize());

            e.getTileMatricies().add(m);
        }

        //figure out the actual bounds of the tiles to be renderered
        LOGGER.fine("Creating tile entry" + e.getTableName());
        geopkg.create(e);

        //create a prototype getmap request
        GetMapRequest req = new GetMapRequest();
        OwsUtils.copy(map.getRequest(), req, GetMapRequest.class);
        req.setLayers(mapLayers);

        String imageFormat = formatOpts.containsKey("format") ? 
                parseFormatFromOpts(formatOpts) : findBestFormat(map);

        req.setFormat(imageFormat);
        req.setWidth(gridSubset.getTileWidth());
        req.setHeight(gridSubset.getTileHeight());

        //count tiles as we generate them
        int ntiles = 0;

        //flag determining if tile row indexes we store in database should be inverted 
        boolean flipy = Boolean.valueOf((String)formatOpts.get("flipy"));
        for (int z = minmax[0]; z < minmax[1]; z++) {
            long[] intersect = gridSubset.getCoverageIntersection(z, bbox);
            for (long x = intersect[0]; x <= intersect[2]; x++) {
                for (long y = intersect[1]; y <= intersect[3]; y++) {
                    BoundingBox box = gridSubset.boundsFromIndex(new long[]{x,y,z});
                    req.setBbox(
                        new Envelope(box.getMinX(),box.getMaxX(),box.getMinY(),box.getMaxY()));

                    Tile t = new Tile();
                    t.setZoom(z);
                    t.setColumn((int) x);
                    t.setRow((int)(flipy?gridSubset.getNumTilesHigh(z)-(y+1):y));

                    WebMap result = webMapService.getMap(req);
                    t.setData(toBytes(result));

                    geopkg.add(e, t);

                    //images we encode are actually kept around, we need to clean them up
                    if (ntiles++ == TILE_CLEANUP_INTERVAL) {
                        cleanUpImages();
                        ntiles = 0;
                    }
                }
            }
        }
    }

    Envelope findTileBounds(GridSubset gridSubset, BoundingBox bbox, int z) {

        long[] i = gridSubset.getCoverageIntersection(z, bbox);

        BoundingBox b1 = gridSubset.boundsFromIndex(new long[]{i[0], i[1],i[4]});
        BoundingBox b2 = gridSubset.boundsFromIndex(new long[]{i[2], i[3],i[4]});
        return new Envelope(
            Math.min(b1.getMinX(), b2.getMinX()),
            Math.max(b1.getMaxX(), b2.getMaxX()),
            Math.min(b1.getMinY(), b2.getMinY()),
            Math.max(b1.getMaxY(), b2.getMaxY()));
    }

    void initEntry(Entry e, Layer layer, MapLayerInfo mapLayer, WMSMapContent map) 
        throws IOException {

        ResourceInfo r = mapLayer.getResource();

        e.setTableName(r.getName());
        e.setIdentifier(r.getTitle());
        e.setDescription(r.getAbstract());
        e.setBounds(bounds(map));
        e.setSrid(srid(map));
    }

    Integer srid(WMSMapContent map) {
        Integer srid = null;
        try {
            srid = CRS.lookupEpsgCode(map.getCoordinateReferenceSystem(), false);
            if (srid == null) {
                srid = Integer.parseInt(map.getRequest().getSRS().split(":")[1]);
            }
        }
        catch(Exception ex) {
            LOGGER.log(Level.WARNING, "Error determining srid", ex);
        }
        return srid;
    }

    ReferencedEnvelope bounds(Layer layer, WMSMapContent map) {
        ReferencedEnvelope e = layer.getBounds();
        if (e == null) {
            e = bounds(map);
        }
        return e;
    }

    ReferencedEnvelope bounds(WMSMapContent map) {
        return new ReferencedEnvelope(map.getRequest().getBbox(), map.getCoordinateReferenceSystem());
    }

    BoundingBox bbox(WMSMapContent map) {
        Envelope bnds = bounds(map);
        return new BoundingBox(bnds.getMinX(), bnds.getMinY(), bnds.getMaxX(), bnds.getMaxY());
    }

    GridSubset findBestGridSubset(WMSMapContent map) {
        GetMapRequest req = map.getRequest();
        Map formatOpts = req.getFormatOptions();

        GridSetBroker gridSetBroker = gwc.getGridSetBroker();
        GridSet gridSet = null;

        //first check format options to see if explicitly specified
        if (formatOpts.containsKey("gridset")) {
            gridSet = gridSetBroker.get(formatOpts.get("gridset").toString());
        }

        //next check srs
        if (gridSet == null) {
            gridSet = gridSetBroker.get(req.getSRS().toUpperCase());
        }

        if (gridSet != null) {
            return GridSubsetFactory.createGridSubSet(gridSet);
        }

        CoordinateReferenceSystem crs = map.getCoordinateReferenceSystem();

        //look up epsg code
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

        //figure out the appropriate grid sub set
        Set<GridSubset> gridSubsets = new LinkedHashSet<GridSubset>();
        for (MapLayerInfo l : req.getLayers()) {
            TileLayer tl = gwc.getTileLayerByName(l.getName());
            if (tl == null) {
                throw new ServiceException("No tile layer for " + l.getName());
            }

            List<GridSubset> theseGridSubsets = tl.getGridSubsetsForSRS(srs);
            if (gridSubsets.isEmpty()) {
                gridSubsets.addAll(theseGridSubsets);
            }
            else {
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
                msg.setLength(msg.length()-2);
                msg.append(". Choosing first.");
                LOGGER.warning(msg.toString());
            }
        }

        return gridSubsets.iterator().next();
    }

    int[] findMinMaxZoom(GridSubset gridSubset, WMSMapContent map) {
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
        }
        else if (formatOpts.containsKey("num_zooms")) {
            maxZoom = minZoom + Integer.parseInt(formatOpts.get("num_zooms").toString());
        }

        if (maxZoom == null) {
            //walk down until we hit too many tiles
            maxZoom = findMaxZoomAuto(gridSubset, minZoom, map); 
        }

        if (maxZoom < minZoom) {
            throw new ServiceException(
                format("maxZoom (%d) can not be less than minZoom (%d)", maxZoom, minZoom));
        }

        //end index
        if (maxZoom > gridSet.getNumLevels()) {
            LOGGER.warning(format("Max zoom (%d) can't be greater than number of zoom levels (%d)", 
                maxZoom, gridSet.getNumLevels()));
            maxZoom = gridSet.getNumLevels();
        }

        return new int[]{minZoom, maxZoom};
    }

    Integer findClosestZoom(GridSet gridSet, WMSMapContent map) {
        double reqScale = 
            RendererUtilities.calculateOGCScale(bounds(map), gridSet.getTileWidth(), null);

        int i = 0; 
        double error = Math.abs(gridSet.getGrid(i).getScaleDenominator() - reqScale);
        while (i < gridSet.getNumLevels()-1) {
            Grid g = gridSet.getGrid(i+1);
            double e = Math.abs(g.getScaleDenominator() - reqScale);

            if (e > error) {
                break;
            }
            else {
                error = e;
            }
            i++;
        }

        return Math.max(i, 0);
    }
    

    Integer findMaxZoomAuto(GridSubset gridSubset, Integer minZoom, WMSMapContent map) {
        BoundingBox bbox = bbox(map);

        int zoom = minZoom;
        int ntiles = 0;

        while(ntiles < 256 && zoom < gridSubset.getGridSet().getNumLevels()) {
            long[] intersect = gridSubset.getCoverageIntersection(zoom, bbox);
            ntiles += (intersect[2]-intersect[0]+1)*(intersect[3]-intersect[1]+1);
            zoom++;
        }
        return zoom;
    }

    String parseFormatFromOpts(Map formatOpts) {
        String format = (String) formatOpts.get("format");
        return format.contains("/") ? format : "image/" + format;
    }

    String findBestFormat(WMSMapContent map) {
        //if request is a single coverage layer return jpeg, otherwise use just png
        List<MapLayerInfo> layers = map.getRequest().getLayers();
        if (layers.size() == 1 && layers.get(0).getType() == MapLayerInfo.TYPE_RASTER) {
            return JPEG_MIME_TYPE;
        }
        return PNG_MIME_TYPE;
    }

    byte[] toBytes(WebMap map) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();

        if (map instanceof RenderedImageMap) {
            RenderedImageMapResponse response = JPEG_MIME_TYPE.equals(map.getMimeType()) ?
                new JPEGMapResponse(wms) : new PNGMapResponse(wms);
            response.write(map, bout, null);
        }
        else if (map instanceof RawMap) {
            ((RawMap) map).writeTo(bout);
        }
        bout.flush();
        return bout.toByteArray();
    }

    void cleanUpImages() {
        RasterCleaner cleaner = GeoServerExtensions.bean(RasterCleaner.class);
        cleaner.finished(null);
    }
}

/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geopkg.wps;

import static org.geoserver.geopkg.GeoPkg.MIME_TYPE;

import com.google.common.base.Strings;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;
import net.opengis.wps10.ExecuteType;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.geopkg.GeoPkg;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.KvpMap;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.ServiceException;
import org.geoserver.util.EntityResolverProvider;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.map.GetMapKvpRequestReader;
import org.geoserver.wps.gs.GeoServerProcess;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geotools.api.data.Query;
import org.geotools.api.data.SimpleFeatureStore;
import org.geotools.api.data.Transaction;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.sort.SortBy;
import org.geotools.api.filter.sort.SortOrder;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.style.StyledLayerDescriptor;
import org.geotools.api.util.ProgressListener;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.store.ReTypingFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.feature.collection.SortedSimpleFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.function.EnvFunction;
import org.geotools.filter.spatial.DefaultCRSFilterVisitor;
import org.geotools.filter.spatial.ReprojectingFilterVisitor;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geopkg.Entry;
import org.geotools.geopkg.FeatureEntry;
import org.geotools.geopkg.GeoPackage;
import org.geotools.geopkg.GeoPkgDataStoreFactory;
import org.geotools.geopkg.GeoPkgDialect;
import org.geotools.geopkg.TileEntry;
import org.geotools.geopkg.wps.GeoPackageProcessRequest;
import org.geotools.geopkg.wps.GeoPackageProcessRequest.FeaturesLayer;
import org.geotools.geopkg.wps.GeoPackageProcessRequest.Layer;
import org.geotools.geopkg.wps.GeoPackageProcessRequest.LayerType;
import org.geotools.geopkg.wps.GeoPackageProcessRequest.TilesLayer;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.referencing.CRS;
import org.geotools.renderer.lite.RendererUtilities;
import org.geotools.util.Converters;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Envelope;

@DescribeProcess(title = "GeoPackage", description = "Geopackage Process")
public class GeoPackageProcess implements GeoServerProcess {

    static final Logger LOGGER = Logging.getLogger(GeoPackageProcess.class);
    static final double OGC_DEGREE_TO_METERS = 6378137.0 * 2.0 * Math.PI / 360;
    private static final double DISTANCE_SCALE_FACTOR = 0.0254 / (25.4 / 0.28);

    private final GeoServerDataDirectory dataDirectory;
    private final EntityResolverProvider resolverProvider;
    private final GeoServer geoServer;
    private final GetMapKvpRequestReader getMapReader;

    private Catalog catalog;

    private WPSResourceManager resources;

    private GeoPackageGetMapOutputFormatWPS mapOutput;

    private FilterFactory filterFactory;

    public GeoPackageProcess(
            GeoServer geoServer,
            GeoPackageGetMapOutputFormatWPS mapOutput,
            WPSResourceManager resources,
            FilterFactory filterFactory,
            GeoServerDataDirectory dataDirectory,
            EntityResolverProvider resolverProvider,
            GetMapKvpRequestReader getMapReader) {
        this.resources = resources;
        this.mapOutput = mapOutput;
        this.filterFactory = filterFactory;
        this.dataDirectory = dataDirectory;
        this.resolverProvider = resolverProvider;
        this.geoServer = geoServer;
        this.catalog = geoServer.getCatalog();
        this.getMapReader = getMapReader;
    }

    private static final int TEMP_DIR_ATTEMPTS = 10000;

    public static File createTempDir(File baseDir) {
        String baseName = System.currentTimeMillis() + "-";

        for (int counter = 0; counter < TEMP_DIR_ATTEMPTS; counter++) {
            File tempDir = new File(baseDir, baseName + counter);
            if (tempDir.mkdir()) {
                return tempDir;
            }
        }
        throw new IllegalStateException(
                "Failed to create directory within "
                        + TEMP_DIR_ATTEMPTS
                        + " attempts (tried "
                        + baseName
                        + "0 to "
                        + baseName
                        + (TEMP_DIR_ATTEMPTS - 1)
                        + ')');
    }

    @DescribeResult(name = "geopackage", description = "Link to Compiled Geopackage File")
    public URL execute(
            @DescribeParameter(
                            name = "contents",
                            description = "xml scheme describing geopackage contents")
                    GeoPackageProcessRequest contents,
            ProgressListener listener)
            throws IOException {

        String outputName = contents.getName() + ".gpkg";
        File file = resources.getOutputResource(null, outputName).file();

        GeoPackage gpkg = GeoPkg.getGeoPackage(file);
        // Initialize the GeoPackage file in order to avoid exceptions when accessing the geoPackage
        // file
        gpkg.init();

        MetadataManager metadataManager = new MetadataManager(gpkg);
        OWSContextWriter contextWriter =
                new OWSContextWriter(
                        geoServer, gpkg, new StyleWorker(dataDirectory, resolverProvider));
        if (contents.isContext()) {
            contextWriter.addRequestContext();
        }
        int stylesCount = 0;
        for (int i = 0; i < contents.getLayerCount(); i++) {
            Layer layer = contents.getLayer(i);

            if (layer.getType() == LayerType.FEATURES) {
                FeaturesLayer fl = (FeaturesLayer) layer;
                if (fl.isStyles()) stylesCount++;
                // TODO: not getting a listener cause the low level GeoPackage API does not
                // support stopping the process of adding features
                addFeatureEntry(contents, gpkg, layer, metadataManager, contextWriter);
            } else if (layer.getType() == LayerType.TILES) {
                addTilesEntry(gpkg, layer, listener);
            }
        }

        // can we try write out eventual layer group info about styles and the like?
        if (contents.isContext() && stylesCount > 1) {
            contextWriter.addStyleGroupInformations(contents);
        }

        gpkg.close();
        return new URL(resources.getOutputResourceUrl(outputName, MIME_TYPE));
    }

    private void addTilesEntry(GeoPackage gpkg, Layer layer, ProgressListener listener)
            throws IOException {
        TilesLayer tiles = (TilesLayer) layer;
        GetMapRequest request = buildGetMapRequest(tiles);

        request.setFormat("none");
        Map formatOptions = new HashMap();
        formatOptions.put("flipy", "true");
        if (tiles.getFormat() != null) {
            formatOptions.put("format", tiles.getFormat());
        }
        if (tiles.getCoverage() != null) {
            if (tiles.getCoverage().getMinZoom() != null) {
                formatOptions.put("min_zoom", tiles.getCoverage().getMinZoom());
            }
            if (tiles.getCoverage().getMaxZoom() != null) {
                formatOptions.put("max_zoom", tiles.getCoverage().getMaxZoom());
            }
            if (tiles.getCoverage().getMinColumn() != null) {
                formatOptions.put("min_column", tiles.getCoverage().getMinColumn());
            }
            if (tiles.getCoverage().getMaxColumn() != null) {
                formatOptions.put("max_column", tiles.getCoverage().getMaxColumn());
            }
            if (tiles.getCoverage().getMinRow() != null) {
                formatOptions.put("min_row", tiles.getCoverage().getMinRow());
            }
            if (tiles.getCoverage().getMaxRow() != null) {
                formatOptions.put("max_row", tiles.getCoverage().getMaxRow());
            }
        }
        if (tiles.getGridSetName() != null) {
            formatOptions.put("gridset", tiles.getGridSetName());
        }
        request.setFormatOptions(formatOptions);

        TileEntry e = new TileEntry();
        addLayerMetadata(e, tiles);

        if (tiles.getGrids() != null) {
            mapOutput.addTiles(gpkg, e, request, tiles.getGrids(), layer.getName(), listener);
        } else {
            mapOutput.addTiles(gpkg, e, request, layer.getName(), listener);
        }
    }

    /**
     * Simulates the parsing of a GetMap in WMS to get full benefit of existing and future vendor
     * options support. Yes we had some information alreay in parsed form, but we cannot do a
     * partial parse, it has to start back straight from the strings.
     *
     * @param tiles
     * @return
     */
    private GetMapRequest buildGetMapRequest(TilesLayer tiles) {
        try {
            Map<String, Object> kvp = new KvpMap<>();
            Map<String, Object> rawKvp = new KvpMap<>();

            // generic params first, so that they cannot override the built-in ones
            if (tiles.getParameters() != null) {
                tiles.getParameters().forEach(p -> rawKvp.put(p.getName(), p.getValue()));
            }

            List<PublishedInfo> layers = getLayers(tiles);

            rawKvp.put(
                    "layers",
                    layers.stream().map(l -> l.prefixedName()).collect(Collectors.joining(",")));

            Envelope bbox = tiles.getBbox();
            if (bbox == null) bbox = getBoundsFromLayers(tiles, layers);
            rawKvp.put("bbox", getCommaSeparated(bbox));

            String srs =
                    Optional.ofNullable(tiles.getSrs())
                            .map(s -> s.toString())
                            .orElseGet(() -> getFirstCRS(layers));
            rawKvp.put("srs", srs);

            if (tiles.getBgColor() != null) {
                rawKvp.put("bgcolor", Converters.convert(tiles.getBgColor(), String.class));
            }
            rawKvp.put("transparent", String.valueOf(tiles.isTransparent()));

            if (tiles.getSldBody() != null) {
                rawKvp.put("sld_body", tiles.getSldBody());
            } else if (tiles.getSld() != null) {
                rawKvp.put("sld", tiles.getSld().toString());
            } else if (tiles.getStyles() != null) {
                rawKvp.put("styles", tiles.getStyles().stream().collect(Collectors.joining(",")));
            }
            rawKvp.put("format", "none");
            rawKvp.put("service", "WMS");
            rawKvp.put("request", "GetMap");
            rawKvp.put("version", "1.1.0");

            GetMapRequest request = getMapReader.createRequest();
            kvp.putAll(rawKvp);
            List<Throwable> errors = KvpUtils.parse(kvp);
            if (!errors.isEmpty())
                throw new ServiceException("Failed to parse KVPs", errors.get(0));
            GetMapRequest getMap = getMapReader.read(request, kvp, rawKvp);

            // env is normally setup by a dispatcher callback, doing it manually here
            Map<String, Object> env = getMap.getEnv();
            if (env != null && !env.isEmpty()) {
                EnvFunction.setLocalValues(env);
            }

            return getMap;
        } catch (Exception e) {
            throw new RuntimeException("Failed to build GetMap request for tile construction", e);
        }
    }

    private Object getCommaSeparated(Envelope bbox) {
        return bbox.getMinX() + "," + bbox.getMinY() + "," + bbox.getMaxX() + "," + bbox.getMaxY();
    }

    private String getFirstCRS(List<PublishedInfo> layers) {
        try {
            return ResourcePool.lookupIdentifier(getCRS(layers.get(0)), false);
        } catch (FactoryException e) {
            throw new RuntimeException(e);
        }
    }

    private List<PublishedInfo> getLayers(TilesLayer tiles) {
        List<PublishedInfo> layers = new ArrayList<>();
        for (QName layerQName : tiles.getLayers()) {
            PublishedInfo publishable;
            String namespaceURI = layerQName.getNamespaceURI();
            String localPart = layerQName.getLocalPart();
            if ("".equals(namespaceURI)) {
                publishable = catalog.getLayerByName(localPart);
                if (publishable == null) publishable = catalog.getLayerGroupByName(localPart);
            } else {
                // the association with a NS URI is weird for layer groups, trying to do
                // a bunch of different lookups
                publishable = catalog.getLayerByName(new NameImpl(namespaceURI, localPart));
                if (publishable == null) {
                    NamespaceInfo ns = catalog.getNamespaceByURI(namespaceURI);
                    if (ns != null) {
                        publishable = catalog.getLayerGroupByName(ns.getPrefix(), localPart);
                    } else if (layerQName.getPrefix() != null) {
                        publishable =
                                catalog.getLayerGroupByName(layerQName.getPrefix(), localPart);
                    }
                }
                if (publishable == null) {
                    publishable = catalog.getLayerGroupByName(localPart);
                }
            }
            if (publishable == null) {
                throw new ServiceException("Layer not found: " + layerQName);
            }
            layers.add(publishable);
        }
        return layers;
    }

    private ReferencedEnvelope getBoundsFromLayers(
            TilesLayer tiles, List<PublishedInfo> publisheds) {
        try {
            // generate one from requests layers
            CoordinateReferenceSystem crs =
                    tiles.getSrs() != null
                            ? CRS.decode(tiles.getSrs().toString())
                            : getCRS(publisheds.get(0));

            ReferencedEnvelope bbox = null;
            for (PublishedInfo p : publisheds) {
                ReferencedEnvelope b = null;
                if (p instanceof LayerInfo) {
                    LayerInfo l = (LayerInfo) p;
                    ResourceInfo r = l.getResource();
                    b = r.getLatLonBoundingBox().transform(crs, true);
                } else {
                    LayerGroupInfo lg = (LayerGroupInfo) p;
                    ReferencedEnvelope bounds = lg.getBounds();
                    b = bounds.transform(crs, true);
                }

                if (bbox != null) {
                    bbox.include(b);
                } else {
                    bbox = b;
                }
            }

            return bbox;
        } catch (Exception e) {
            String msg = "Must specify bbox, unable to derive from requested layers";
            throw new RuntimeException(msg, e);
        }
    }

    private CoordinateReferenceSystem getCRS(PublishedInfo publishedInfo) {
        if (publishedInfo instanceof LayerInfo) {
            return ((LayerInfo) publishedInfo).getResource().getCRS();
        } else {
            return ((LayerGroupInfo) publishedInfo).getBounds().getCoordinateReferenceSystem();
        }
    }

    private void addFeatureEntry(
            GeoPackageProcessRequest contents,
            GeoPackage gpkg,
            Layer layer,
            MetadataManager metadataManager,
            OWSContextWriter contextWriter)
            throws IOException {
        FeaturesLayer features = (FeaturesLayer) layer;
        QName ftName = features.getFeatureType();
        FeatureTypeInfo ft;
        if (Strings.isNullOrEmpty(ftName.getNamespaceURI())) {
            if (Strings.isNullOrEmpty(ftName.getPrefix())) {
                ft = catalog.getFeatureTypeByName(ftName.getLocalPart());
            } else {
                ft = catalog.getFeatureTypeByName(ftName.getPrefix() + ":" + ftName.getLocalPart());
            }
        } else {
            ft = catalog.getFeatureTypeByName(ftName.getNamespaceURI(), ftName.getLocalPart());
        }

        Query q = new Query();
        if (features.getSrs() == null) {
            if (ft != null) {
                try {
                    q.setCoordinateSystemReproject(CRS.decode(ft.getSRS(), true));
                } catch (FactoryException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        if (features.getPropertyNames() != null) {
            q.setPropertyNames(
                    features.getPropertyNames().stream()
                            .map(qn -> qn.getLocalPart())
                            .toArray(n -> new String[n]));
        }

        Filter filter = features.getFilter();
        // add bbox to filter if there is one
        FeatureType sourceSchema = ft.getFeatureType();
        if (features.getBbox() != null) {
            String defaultGeometry = sourceSchema.getGeometryDescriptor().getLocalName();

            Envelope e = features.getBbox();
            Filter bboxFilter =
                    filterFactory.bbox(
                            filterFactory.property(defaultGeometry),
                            ReferencedEnvelope.reference(e));
            if (filter == null) {
                filter = bboxFilter;
            } else {
                filter = filterFactory.and(filter, bboxFilter);
            }
        }
        if (filter != null) {
            // handle geometric filter reprojection
            if (sourceSchema.getCoordinateReferenceSystem() != null) {
                filter = applyDefaultCRS(filter, sourceSchema.getCoordinateReferenceSystem());
            }
            filter = reprojectFilter(filter, sourceSchema);
            q.setFilter(filter);
        }

        // delegate to the data source if possible
        boolean sortOnGeometry = isGeometrySorted(features.getSort(), sourceSchema);
        if (features.getSort() != null && !sortOnGeometry) {
            q.setSortBy(features.getSort());
        }

        FeatureCollection<? extends FeatureType, ? extends Feature> fc =
                ft.getFeatureSource(null, null).getFeatures(q);

        if (!(fc instanceof SimpleFeatureCollection)) {
            throw new ServiceException(
                    "GeoPackage OutputFormat does not support Complex Features.");
        }
        SimpleFeatureCollection collection = (SimpleFeatureCollection) fc;

        FeatureEntry e = new FeatureEntry();
        e.setTableName(layer.getName());
        addLayerMetadata(e, features);
        ReferencedEnvelope bounds = collection.getBounds();
        if (features.getBbox() != null) {
            bounds = ReferencedEnvelope.reference(bounds.intersection(features.getBbox()));
        }

        e.setBounds(bounds);

        if (sortOnGeometry) {
            SimpleFeatureCollection sorted = sort(collection, features.getSort());
            gpkg.add(e, sorted);
        } else {
            gpkg.add(e, collection);
        }

        if (features.isIndexed()) {
            gpkg.createSpatialIndex(e);
        }

        List<GeoPackageProcessRequest.Overview> overviews = features.getOverviews();
        if (overviews != null) {
            addOverviews(gpkg, features, overviews);
        }

        List<LayerInfo> layers = catalog.getLayers(ft);
        if (features.isStyles() && layers != null && !layers.isEmpty()) {
            LayerInfo layerInfo = layers.get(0);
            if (layerInfo != null) {
                addLayerStyles(gpkg, layerInfo);
            }
            if (contents.isContext() && features.isStyles()) {
                contextWriter.trackLayerStyles(layer.getName(), layerInfo);
            }
        }

        if (features.isMetadata()) {
            metadataManager.addMetadata(ft);
        }

        if (contents.isContext()) {
            contextWriter.addFeatureTypeContext(ft, layer.getName());
        }
    }

    /** Applies a default CRS to all geometric filter elements that do not already have one */
    public Filter applyDefaultCRS(Filter filter, CoordinateReferenceSystem defaultCRS) {
        DefaultCRSFilterVisitor defaultVisitor =
                new DefaultCRSFilterVisitor(filterFactory, defaultCRS);
        return (Filter) filter.accept(defaultVisitor, null);
    }

    /** Reprojects all geometric filter elements to the native CRS of the provided schema */
    public Filter reprojectFilter(Filter filter, FeatureType schema) {
        ReprojectingFilterVisitor visitor = new ReprojectingFilterVisitor(filterFactory, schema);
        return (Filter) filter.accept(visitor, null);
    }

    /** Sorts the feature collection locally, replacing geometry sorting by geohash sorting */
    private SimpleFeatureCollection sort(SimpleFeatureCollection fc, SortBy[] sort) {
        GeoHashCollection ghc = new GeoHashCollection(fc);

        // adapt sort to use geohash instead of the geometry
        SortBy[] adaptedSort =
                Arrays.stream(sort)
                        .map(
                                sb -> {
                                    String name = sb.getPropertyName().getPropertyName();
                                    if (fc.getSchema().getDescriptor(name)
                                            instanceof GeometryDescriptor) {
                                        return filterFactory.sort(
                                                ghc.getGeoHashFieldName(), SortOrder.ASCENDING);
                                    } else {
                                        return sb;
                                    }
                                })
                        .toArray(n -> new SortBy[n]);

        // sort by geohash
        SortedSimpleFeatureCollection sorted =
                new SortedSimpleFeatureCollection(ghc, adaptedSort, 100000);

        // remove the geohash, casting to the original feature type
        return new ReTypingFeatureCollection(sorted, fc.getSchema());
    }

    private boolean isGeometrySorted(SortBy[] sort, FeatureType featureType) {
        if (sort == null || sort.length == 0) return false;

        return Arrays.stream(sort)
                .map(s -> featureType.getDescriptor(s.getPropertyName().getPropertyName()))
                .anyMatch(p -> p instanceof GeometryDescriptor);
    }

    /**
     * Converts a scale denominator to a generalization distance using the OGC SLD scale denominator
     * computation rules
     *
     * @param crs The CRS of the data
     * @param scaleDenominator The target scale denominator
     * @return
     */
    static double scaleToDistance(CoordinateReferenceSystem crs, double scaleDenominator) {
        return scaleDenominator * DISTANCE_SCALE_FACTOR / RendererUtilities.toMeters(1, crs);
    }

    /**
     * Converts a generalization distance to a scale denominator using the OGC SLD scale denominator
     * computation rules
     *
     * @param crs The CRS of the data
     * @param distance The target generalization distance
     * @return
     */
    static double distanceToScale(CoordinateReferenceSystem crs, double distance) {
        return distance * RendererUtilities.toMeters(1, crs) / 0.00028;
    }

    private void addOverviews(
            GeoPackage gpkg, FeaturesLayer layer, List<GeoPackageProcessRequest.Overview> overviews)
            throws IOException {
        Collections.sort(overviews);
        GeoPackageProcessRequest.Overview previousOverview = null;

        // create the datastore, allow access to tables not registered among the contents
        JDBCDataStore dataStore = getStoreFromPackage(gpkg, false);
        try {
            for (GeoPackageProcessRequest.Overview overview : overviews) {
                // create the overview schema
                String orginalLayerName = layer.getFeatureType().getLocalPart();
                SimpleFeatureType schema = dataStore.getSchema(orginalLayerName);
                SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
                tb.init(schema);
                String overviewName = overview.getName();
                tb.setName(overviewName);
                SimpleFeatureType ft = tb.buildFeatureType();
                ft.getUserData().put(GeoPackage.SKIP_REGISTRATION, true);
                dataStore.createSchema(ft);

                // prepare the query
                Query q = new Query();
                Filter filter = overview.getFilter();
                if (overview.getFilter() != null) {
                    q.setFilter(filter);
                }

                // build from previous overview if possible, base table otherwise
                String sourceTable =
                        previousOverview != null ? previousOverview.getName() : orginalLayerName;
                SimpleFeatureStore source =
                        (SimpleFeatureStore) dataStore.getFeatureSource(sourceTable);

                // make sure the distance is updated
                double distance = overview.getDistance();
                double scaleDenominator = overview.getScaleDenominator();
                CoordinateReferenceSystem crs = schema.getCoordinateReferenceSystem();
                if (distance == 0) {
                    if (scaleDenominator > 0) {
                        distance = scaleToDistance(crs, scaleDenominator);
                    }
                } else if (scaleDenominator == 0) {
                    scaleDenominator = distanceToScale(crs, distance);
                }

                // grab the data source, using a single transaction for both reading and writing, to
                // avoid exclusive locks

                try (Transaction t = new DefaultTransaction()) {
                    source.setTransaction(t);
                    SimpleFeatureCollection fc = source.getFeatures(q);

                    SimpleFeatureStore featureStore =
                            (SimpleFeatureStore) dataStore.getFeatureSource(ft.getTypeName());
                    featureStore.setTransaction(t);
                    featureStore.addFeatures(SimplifyingFeatureCollection.simplify(fc, distance));
                    t.commit();
                }

                // create the spatial index, tricking the store with a fake entry
                FeatureEntry fe = new FeatureEntry();
                fe.setTableName(ft.getTypeName());
                fe.setGeometryColumn(ft.getGeometryDescriptor().getLocalName());
                new GeoPackage(dataStore).createSpatialIndex(fe);

                // register the overview table
                try {
                    GeneralizedTablesExtension generalized =
                            gpkg.getExtension(GeneralizedTablesExtension.class);
                    GeneralizedTable gt =
                            new GeneralizedTable(orginalLayerName, overviewName, distance);
                    String provenance = "Source table: " + sourceTable;
                    if (filter != null) {
                        provenance += "\nFilter as CQL: " + ECQL.toCQL(filter);
                    }
                    gt.setProvenance(provenance);
                    generalized.addTable(gt);
                } catch (SQLException e) {
                    throw new IOException(e);
                }
            }
        } finally {
            // just to avoid a nagging message about not closing stores, it does not really
            // do anything since the data source is not a ManageableDataSource
            dataStore.dispose();
        }
    }

    static JDBCDataStore getStoreFromPackage(GeoPackage gpkg, boolean contentsOnly)
            throws IOException {
        Map<String, Object> params = new HashMap<>();
        params.put(GeoPkgDataStoreFactory.DATASOURCE.key, gpkg.getDataSource());
        JDBCDataStore dataStore =
                new GeoPkgDataStoreFactory(gpkg.getWriterConfiguration()).createDataStore(params);
        ((GeoPkgDialect) dataStore.getSQLDialect()).setContentsOnly(contentsOnly);
        return dataStore;
    }

    private void addLayerStyles(GeoPackage gpkg, LayerInfo layerInfo) throws IOException {
        try {
            PortrayalExtension portrayal = gpkg.getExtension(PortrayalExtension.class);
            SemanticAnnotationsExtension annotations =
                    gpkg.getExtension(SemanticAnnotationsExtension.class);

            StyleInfo defaultStyle = layerInfo.getDefaultStyle();
            GeoPkgStyle defaultGeoPkgStyle = addStyle(portrayal, annotations, defaultStyle);
            linkStyle(annotations, layerInfo, defaultGeoPkgStyle);

            for (StyleInfo style : layerInfo.getStyles()) {
                GeoPkgStyle geoPkgStyle = addStyle(portrayal, annotations, style);
                linkStyle(annotations, layerInfo, geoPkgStyle);
            }
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    private void linkStyle(
            SemanticAnnotationsExtension annotations, LayerInfo layerInfo, GeoPkgStyle style)
            throws SQLException {
        // need to link style and layer (it's not done while adding the style, cause the
        // style could have been shared among different layers, and thus, created only once)
        List<GeoPkgSemanticAnnotation> styleAnnotations =
                annotations.getAnnotationsByURI(style.getUri());
        if (styleAnnotations.size() > 0) {
            GeoPkgSemanticAnnotation annotation = styleAnnotations.get(0);
            annotations.addReference(
                    new GeoPkgAnnotationReference(
                            layerInfo.getResource().getNativeName(), annotation));
            annotations.addReference(
                    new GeoPkgAnnotationReference(
                            PortrayalExtension.STYLES_TABLE, "id", style.getId(), annotation));
        }
    }

    private GeoPkgStyle addStyle(
            PortrayalExtension portrayal, SemanticAnnotationsExtension annotations, StyleInfo style)
            throws SQLException, IOException {
        String styleName = style.prefixedName();
        String styleURI = builStyledURI(style);
        // if it's missing yet, add it (multiple layers could be referring to the same style)
        GeoPkgStyle gs = portrayal.getStyle(styleName);
        if (gs == null) {
            StyleWorker worker = new StyleWorker(dataDirectory, resolverProvider);
            StyledLayerDescriptor sld = worker.getSLD(style);

            // save the style
            String description = worker.getDescription(sld);
            gs = new GeoPkgStyle(styleName, styleURI);
            gs.setDescription(description);
            portrayal.addStyle(gs);

            // the stylesheet (for now, in a single format, multi-format support can be added later
            // easily
            String body = worker.getStyleBody(style);
            String mimeType = worker.getMimeType(style);
            GeoPkgStyleSheet styleSheet = new GeoPkgStyleSheet(gs, mimeType, body);
            portrayal.addStylesheet(styleSheet);

            // now go hunt for symbology
            WorkspaceInfo ws = style.getWorkspace();
            String symbolPrefix =
                    "symbols://" + (ws != null ? ws.getName() + "_" : "") + style.getName() + "/";
            StyleResourceCollector collector =
                    new StyleResourceCollector(dataDirectory.getResourceLocator(ws), symbolPrefix);
            sld.accept(collector);
            int symbolId = 0;
            for (Map.Entry<String, GeoPkgSymbolImage> entry : collector.getResources().entrySet()) {
                String name = entry.getKey();
                GeoPkgSymbolImage image = entry.getValue();
                GeoPkgSymbol symbol = image.getSymbol();

                if (portrayal.getSymbol(symbol.getSymbol()) == null) {
                    portrayal.addSymbol(symbol);
                    portrayal.addImage(image);
                }
            }

            // create a semantic annotation for the style
            GeoPkgSemanticAnnotation annotation =
                    new GeoPkgSemanticAnnotation(
                            PortrayalExtension.SA_TYPE_STYLE, styleName, styleURI);
            annotation.setDescription(description);
            annotations.addAnnotation(annotation);
        }

        return gs;
    }

    private String builStyledURI(StyleInfo style) {
        try {
            ExecuteType request =
                    (ExecuteType) Dispatcher.REQUEST.get().getOperation().getParameters()[0];
            WorkspaceInfo ws = style.getWorkspace();
            String path = "styles/" + (ws != null ? ws.getName() + "/" : "") + style.getFilename();
            return ResponseUtils.buildURL(
                    request.getBaseUrl(),
                    path,
                    Collections.emptyMap(),
                    URLMangler.URLType.RESOURCE);
        } catch (Exception e) {
            LOGGER.log(
                    Level.INFO,
                    "Failed to build back-reference to the style, using a unique URI",
                    e);
            return style.prefixedName();
        }
    }

    private void addLayerMetadata(Entry e, Layer layer) {
        e.setDescription(layer.getDescription());
        e.setIdentifier(layer.getIdentifier());
    }
}

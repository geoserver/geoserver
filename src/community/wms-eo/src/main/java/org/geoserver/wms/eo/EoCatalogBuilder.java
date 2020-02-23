/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.eo;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.DimensionInfoImpl;
import org.geoserver.catalog.util.ReaderDimensionsAccessor;
import org.geotools.coverage.grid.io.DimensionDescriptor;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.data.DataAccessFactory.Param;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.util.NullProgressListener;
import org.geotools.gce.imagemosaic.Utils;
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.geotools.util.URLs;
import org.geotools.util.Utilities;
import org.geotools.util.logging.Logging;

/**
 * Builder class which provides convenience methods for managing EO stores, resources, layers and
 * layer groups.
 *
 * @author Davide Savazzi - geo-solutions.it
 */
public class EoCatalogBuilder implements EoStyles {

    private Catalog catalog;

    private static final Logger LOGGER = Logging.getLogger(EoCatalogBuilder.class);

    /** EoCatalogBuilder constructor */
    public EoCatalogBuilder(Catalog catalog) {
        this.catalog = catalog;
    }

    /**
     * Create an EO Geophysical Parameters layer
     *
     * @param ws workspace
     * @param groupName group name
     * @param parametersName Geophysical Parameters name
     * @param parametersUrl Geophysical Parameters url
     * @return created layer
     */
    public LayerInfo createEoParametersLayer(
            WorkspaceInfo ws, String groupName, String parametersName, String parametersUrl) {
        String parametersLayerName = groupName + "_" + parametersName;
        return createEoMosaicLayer(
                ws, parametersLayerName, EoLayerType.GEOPHYSICAL_PARAMETER, parametersUrl, false);
    }

    /**
     * Create an EO Bitmasks layer
     *
     * @param ws workspace
     * @param groupName group name
     * @param masksName bitmasks name
     * @param masksUrl bitmasks url
     * @return created layer
     */
    public LayerInfo createEoMasksLayer(
            WorkspaceInfo ws, String groupName, String masksName, String masksUrl) {
        Utilities.ensureNonNull("groupName", groupName);
        String masksLayerName = groupName + "_" + masksName;
        LayerInfo masksLayer =
                createEoMosaicLayer(ws, masksLayerName, EoLayerType.BITMASK, masksUrl, false);
        if (masksLayer != null) {
            addEoStyles(masksLayer, DEFAULT_BITMASK_STYLE);
        }
        return masksLayer;
    }

    public LayerInfo createEoBandsLayer(WorkspaceInfo ws, String groupName, String bandsUrl) {
        Utilities.ensureNonNull("groupName", groupName);
        String bandsLayerName = groupName + "_BANDS";
        return createEoMosaicLayer(ws, bandsLayerName, EoLayerType.BAND_COVERAGE, bandsUrl, true);
    }

    public LayerInfo createEoBrowseImageLayer(
            WorkspaceInfo ws, String groupName, String browseImageUrl) {
        /*
         * Browse Image layer name must be different from EO group name (otherwise GWC will
         * complain) In GetCapabilities this name will not appear
         */
        Utilities.ensureNonNull("groupName", groupName);
        String browseLayerName = groupName + "_BROWSE";
        return createEoMosaicLayer(
                ws, browseLayerName, EoLayerType.BROWSE_IMAGE, browseImageUrl, false);
    }

    /**
     * Create an EO layer group
     *
     * @param ws workspace
     * @param groupName group name
     * @param groupTitle group title
     * @param browseImageUrl Browse Image url
     * @param bandsUrl Band Coverage url
     * @param masksName Bitmasks name
     * @param masksUrl Bitmasks url
     * @param parametersName Geophysical Parameters name
     * @param parametersUrl Geophysical Parameters url
     * @return created group
     */
    public LayerGroupInfo createEoLayerGroup(
            WorkspaceInfo ws,
            String groupName,
            String groupTitle,
            String browseImageUrl,
            String bandsUrl,
            String masksName,
            String masksUrl,
            String parametersName,
            String parametersUrl) {

        LayerInfo bandsLayer = createEoBandsLayer(ws, groupName, bandsUrl);
        LayerInfo browseLayer = createEoBrowseImageLayer(ws, groupName, browseImageUrl);
        LayerInfo paramsLayer =
                createEoParametersLayer(ws, groupName, parametersName, parametersUrl);
        LayerInfo masksLayer = createEoMasksLayer(ws, groupName, masksName, masksUrl);

        LayerInfo outlineLayer;
        try {
            StructuredGridCoverage2DReader reader =
                    (StructuredGridCoverage2DReader)
                            ((CoverageInfo) bandsLayer.getResource())
                                    .getGridCoverageReader(null, null);
            outlineLayer = createEoOutlineLayer(bandsUrl, ws, groupName, null, reader);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "The Outline layer could not be created. Failure message: " + e.getMessage(),
                    e);
        }

        // create layer group
        LayerGroupInfo layerGroup = catalog.getFactory().createLayerGroup();
        layerGroup.setWorkspace(ws);
        layerGroup.setName(groupName);
        layerGroup.setTitle(groupTitle);
        layerGroup.setMode(LayerGroupInfo.Mode.EO);
        layerGroup.setRootLayer(browseLayer);
        layerGroup.setRootLayerStyle(browseLayer.getDefaultStyle());
        layerGroup.getLayers().add(outlineLayer);
        layerGroup.getStyles().add(outlineLayer.getDefaultStyle());
        layerGroup.getLayers().add(bandsLayer);
        layerGroup.getStyles().add(bandsLayer.getDefaultStyle());
        if (masksLayer != null) {
            layerGroup.getLayers().add(masksLayer);
            layerGroup.getStyles().add(masksLayer.getDefaultStyle());
        }
        if (paramsLayer != null) {
            layerGroup.getLayers().add(paramsLayer);
            layerGroup.getStyles().add(paramsLayer.getDefaultStyle());
        }

        try {
            CatalogBuilder builder = new CatalogBuilder(catalog);
            builder.calculateLayerGroupBounds(layerGroup);

            catalog.add(layerGroup);
            return layerGroup;
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "The layer group '"
                            + groupName
                            + "' could not be created. Failure message: "
                            + e.getMessage(),
                    e);
        }
    }

    private Properties loadProperties(File propertiesFile) throws IOException {
        Properties properties = new Properties();
        InputStream inputStream = new BufferedInputStream(new FileInputStream(propertiesFile));
        try {
            properties.load(inputStream);
        } finally {
            inputStream.close();
        }
        return properties;
    }

    protected DataStoreFactorySpi getOutlineDataStoreFactory(File dir) throws Exception {
        File datastorePropertiesFile = new File(dir, "datastore.properties");
        if (datastorePropertiesFile.exists()) {
            Properties datastoreProperties = loadProperties(datastorePropertiesFile);
            String SPIClass = datastoreProperties.getProperty("SPI");
            return (DataStoreFactorySpi) Class.forName(SPIClass).newInstance();
        } else {
            return new ShapefileDataStoreFactory();
        }
    }

    /**
     * Get database type from DataStoreFactorySpi
     *
     * @return database type
     */
    protected String getDbType(DataStoreFactorySpi dataStoreFactory) {
        String dbType = null;
        Param[] params = dataStoreFactory.getParametersInfo();
        for (Param param : params) {
            if (JDBCDataStoreFactory.DBTYPE.key.equals(param.key)) {
                dbType = (String) param.getDefaultValue();
            }
        }

        if (dbType == null) {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.log(
                        Level.WARNING,
                        "dbtype parameter not found in dataStoreFactory "
                                + dataStoreFactory
                                + ", using default.");
            }
        }

        return dbType;
    }

    /**
     * Create Outline store parameters
     *
     * @param dir mosaic directory
     * @return parameters
     */
    protected Map<String, Serializable> getOutlineDataStoreParameters(
            File dir, DataStoreFactorySpi dataStoreFactory) throws IOException {
        File datastorePropertiesFile = new File(dir, "datastore.properties");
        if (datastorePropertiesFile.exists()) {
            Properties datastoreProperties = loadProperties(datastorePropertiesFile);
            Map<String, Serializable> params =
                    Utils.createDataStoreParamsFromPropertiesFile(
                            datastoreProperties, dataStoreFactory);
            String dbType = getDbType(dataStoreFactory);
            params.put("dbtype", dbType);
            if ("h2".equals(dbType)) {
                String dbParameter = (String) params.get("database");
                // if the reference is relative, we need to build the absolute path
                if (!new File(dbParameter).isAbsolute()) {
                    File actualPath = new File(dir, dbParameter).getCanonicalFile();
                    params.put("database", actualPath.getAbsolutePath());
                }
            }
            return params;
        } else {
            // shp store
            File shpFile = new File(dir, dir.getName() + ".shp");

            Map<String, Serializable> params = new HashMap<String, Serializable>();

            // TODO is there a better way to convert a path to a URL?
            // URLs.fileToUrl(file) doesn't work (GeoServer saves an empty url)
            params.put(ShapefileDataStoreFactory.URLP.key, "file://" + shpFile.getAbsolutePath());

            params.put(ShapefileDataStoreFactory.MEMORY_MAPPED.key, true);
            // TODO other params?
            // params.put(ShapefileDataStoreFactory.DBFTIMEZONE.key, Utils.UTC_TIME_ZONE);

            return params;
        }
    }

    /** Create EO Outline layer */
    public LayerInfo createEoOutlineLayer(
            String url,
            WorkspaceInfo ws,
            String groupName,
            String coverageName,
            StructuredGridCoverage2DReader reader)
            throws Exception {
        File dir = URLs.urlToFile(new URL(url));

        if (ws == null) {
            ws = catalog.getDefaultWorkspace();
        }

        // store creation from bands directory
        String storeName = dir.getName();
        String layerName = groupName + "_outlines";

        CatalogBuilder builder = new CatalogBuilder(catalog);
        DataStoreInfo store = null;
        FeatureTypeInfo featureType = null;
        LayerInfo layer = null;
        boolean success = false;
        try {
            store = builder.buildDataStore(layerName);

            DataStoreFactorySpi dataStoreFactory = getOutlineDataStoreFactory(dir);

            Map<String, Serializable> parameters =
                    getOutlineDataStoreParameters(dir, dataStoreFactory);
            NamespaceInfo ns = catalog.getNamespaceByPrefix(ws.getName());
            parameters.put("namespace", ns.getURI());

            store.setType(dataStoreFactory.getDisplayName());
            store.setWorkspace(ws);
            store.getConnectionParameters().putAll(parameters);
            catalog.add(store);

            builder.setStore(store);

            // featuretyepinfo and layerinfo
            DataStore dataStore = (DataStore) store.getDataStore(new NullProgressListener());
            String featureTypeName = coverageName != null ? coverageName : storeName;
            SimpleFeatureSource featureSource = dataStore.getFeatureSource(featureTypeName);
            featureType = builder.buildFeatureType(featureSource);
            featureType.setName(layerName);
            featureType.setTitle(layerName);
            builder.setupBounds(featureType, featureSource);
            // dimensions
            boolean foundTime = enableDimensions(featureType, coverageName, reader);
            if (!foundTime) {
                throw new IllegalArgumentException(
                        "Unable to enable TIME dimension on outline layer:" + layerName);
            }
            catalog.add(featureType);

            // layer
            layer = builder.buildLayer(featureType);
            layer.setName(layerName);
            layer.setTitle(layerName);
            layer.setEnabled(true);
            layer.setQueryable(true);
            layer.setType(PublishedType.VECTOR);
            layer.getMetadata().put(EoLayerType.KEY, EoLayerType.COVERAGE_OUTLINE.name());
            addEoStyles(layer, DEFAULT_OUTLINE_STYLE);
            catalog.add(layer);

            success = true;

            return layer;
        } finally {
            // poor excuse for a rollback, but better than nothing
            if (!success) {
                if (layer != null) {
                    catalog.remove(layer);
                }
                if (featureType != null) {
                    catalog.remove(featureType);
                }
                if (store != null) {
                    catalog.remove(store);
                }
            }
        }
    }

    /** Add EO styles to layer */
    private void addEoStyles(LayerInfo layer, String defaultStyleName) {
        StyleInfo defaultStyle = catalog.getStyleByName(defaultStyleName);
        if (defaultStyle != null) {
            layer.setDefaultStyle(defaultStyle);
        } else {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.log(Level.WARNING, "EO Style not found: " + defaultStyleName);
            }
        }

        for (String styleName : EO_STYLE_NAMES) {
            StyleInfo style = catalog.getStyleByName(styleName);
            if (style != null) {
                layer.getStyles().add(style);
            } else {
                if (LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.log(Level.WARNING, "EO Style not found: " + styleName);
                }
            }
        }
    }

    /**
     * Create a new mosaic store
     *
     * @param ws workspace
     * @param name store name
     * @return created store
     */
    protected CoverageStoreInfo createEoMosaicStore(WorkspaceInfo ws, String name, String url) {
        CoverageStoreInfo storeInfo = catalog.getFactory().createCoverageStore();
        storeInfo.setWorkspace(ws);
        storeInfo.setType("ImageMosaic");
        storeInfo.setEnabled(true);
        storeInfo.setName(name);
        storeInfo.setURL(url);

        try {
            catalog.add(storeInfo);
            return storeInfo;
        } catch (RuntimeException e) {
            String msg =
                    "The coverage store '"
                            + name
                            + "' could not be created. Failure message: "
                            + e.getMessage();
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.log(Level.INFO, msg, e);
            }

            throw new IllegalArgumentException(msg, e);
        }
    }

    /**
     * Create a new mosaic layer
     *
     * @param ws workspace
     * @param name store name and layer name
     * @param type EO layer type
     * @param url mosaic url
     * @param checkDimensions check time and at least another dimension is present
     * @return created layer
     */
    public LayerInfo createEoMosaicLayer(
            WorkspaceInfo ws, String name, EoLayerType type, String url, boolean checkDimensions) {
        if (StringUtils.isEmpty(url)) {
            return null;
        }

        CoverageStoreInfo store = createEoMosaicStore(ws, name, url);

        CatalogBuilder builder = new CatalogBuilder(catalog);
        builder.setStore(store);
        try {
            CoverageInfo resource = builder.buildCoverage();

            boolean dimensionsPresent = enableDimensions(resource);
            if (checkDimensions) {
                if (!dimensionsPresent) {
                    // rollback: delete store
                    catalog.remove(store);
                    throw new IllegalArgumentException(
                            "The layer '" + name + "' could not be created: no dimensions found");
                }
            }

            resource.setName(name);
            resource.setTitle(name);
            catalog.add(resource);

            LayerInfo layer = builder.buildLayer(resource);
            layer.setName(name);
            layer.setTitle(name);
            layer.setEnabled(true);
            layer.setQueryable(true);
            layer.setType(PublishedType.RASTER);
            layer.getMetadata().put(EoLayerType.KEY, type.name());
            catalog.add(layer);

            return layer;
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "The layer '"
                            + name
                            + "' could not be created. Failure message: "
                            + e.getMessage(),
                    e);
        }
    }

    /**
     * Check presence of TIME dimension and at least one custom dimension. Enable all dimensions
     * found.
     */
    private boolean enableDimensions(CoverageInfo ci) {
        boolean timeDimension = false;
        boolean customDimension = false;
        GridCoverage2DReader reader = null;
        try {
            // acquire a reader
            reader = (GridCoverage2DReader) ci.getGridCoverageReader(null, null);
            if (reader == null) {
                throw new RuntimeException(
                        "Unable to acquire reader for this coverageinfo: " + ci.getName());
            }

            // inspect dimensions
            final ReaderDimensionsAccessor ra = new ReaderDimensionsAccessor(reader);
            for (String domain : ra.getCustomDomains()) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    boolean hasRange = ra.hasRange(domain);
                    boolean hasResolution = ra.hasResolution(domain);
                    LOGGER.fine(
                            ci.getName()
                                    + ": found "
                                    + domain
                                    + " dimension (hasRange: "
                                    + hasRange
                                    + ", hasResolution: "
                                    + hasResolution
                                    + ")");
                }

                DimensionInfo dimension = new DimensionInfoImpl();
                dimension.setEnabled(true);
                dimension.setPresentation(DimensionPresentation.LIST);
                ci.getMetadata().put(ResourceInfo.CUSTOM_DIMENSION_PREFIX + domain, dimension);

                customDimension = true;
            }

            String elev = reader.getMetadataValue(GridCoverage2DReader.HAS_ELEVATION_DOMAIN);
            if (Boolean.parseBoolean(elev)) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine(ci.getName() + ": found ELEVATION dimension");
                }

                DimensionInfo dimension = new DimensionInfoImpl();
                dimension.setEnabled(true);
                dimension.setPresentation(DimensionPresentation.LIST);
                ci.getMetadata().put(ResourceInfo.ELEVATION, dimension);

                customDimension = true;
            }

            String time = reader.getMetadataValue(GridCoverage2DReader.HAS_TIME_DOMAIN);
            if (Boolean.parseBoolean(time)) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine(ci.getName() + ": found TIME dimension");
                }

                DimensionInfo dimension = new DimensionInfoImpl();
                dimension.setEnabled(true);
                dimension.setPresentation(DimensionPresentation.LIST);
                ci.getMetadata().put(ResourceInfo.TIME, dimension);

                timeDimension = true;
            }
        } catch (IOException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, "Failed to access coverage reader custom dimensions", e);
            }
        }

        return timeDimension && customDimension;
    }

    /** Delete a layer, its resource and its store */
    private void delete(LayerInfo layer) {
        ResourceInfo resource = layer.getResource();
        StoreInfo store = resource.getStore();
        catalog.remove(layer);
        catalog.remove(resource);
        catalog.remove(store);
    }

    /** Delete a layer group, all its layers and their respective stores */
    public void delete(LayerGroupInfo group) {
        // load layers in group
        group = catalog.getLayerGroupByName(group.getWorkspace(), group.getName());
        try {
            catalog.remove(group);
            delete(group.getRootLayer());
            for (PublishedInfo p : group.getLayers()) {
                if (p instanceof LayerGroupInfo) {
                    delete(group);
                } else {
                    delete((LayerInfo) p);
                }
            }
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(
                    "The group '"
                            + group.getName()
                            + "' could not be removed. Failure message: "
                            + e.getMessage(),
                    e);
        }
    }

    /** Check presence of TIME dimension . Enable all dimensions found. */
    private boolean enableDimensions(
            FeatureTypeInfo fi, String coverageName, StructuredGridCoverage2DReader reader)
            throws IOException {
        Utilities.ensureNonNull("FeatureTypeInfo", fi);
        Utilities.ensureNonNull("reader", reader);

        List<DimensionDescriptor> dimensionDescriptors =
                reader.getDimensionDescriptors(
                        coverageName == null ? reader.getGridCoverageNames()[0] : coverageName);
        boolean timeDimension = false;
        for (DimensionDescriptor dd : dimensionDescriptors) {
            DimensionInfo di = new DimensionInfoImpl();
            String key;
            String units = dd.getUnits();
            String symbol = dd.getUnitSymbol();
            if (ResourceInfo.TIME.equalsIgnoreCase(dd.getName())) {
                timeDimension = true;
                key = ResourceInfo.TIME;
                units = DimensionInfo.TIME_UNITS;
            } else if (ResourceInfo.ELEVATION.equalsIgnoreCase(dd.getName())) {
                key = ResourceInfo.ELEVATION;
                units = DimensionInfo.ELEVATION_UNITS;
                symbol = DimensionInfo.ELEVATION_UNIT_SYMBOL;
            } else {
                key = ResourceInfo.CUSTOM_DIMENSION_PREFIX + dd.getName();
            }

            di.setEnabled(true);
            di.setAttribute(dd.getStartAttribute());
            di.setEndAttribute(dd.getEndAttribute());
            di.setPresentation(DimensionPresentation.LIST);
            di.setUnits(units);
            di.setUnitSymbol(symbol);
            fi.getMetadata().put(key, di);
        }

        return timeDimension;
    }
}

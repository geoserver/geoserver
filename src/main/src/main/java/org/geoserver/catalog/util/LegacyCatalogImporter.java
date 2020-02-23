/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.CoverageDimensionInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.Keyword;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.LegendInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.util.CoverageStoreUtils;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Files;
import org.geotools.coverage.grid.GeneralGridEnvelope;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.DataAccess;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.operation.DefaultMathTransformFactory;
import org.geotools.referencing.operation.matrix.GeneralMatrix;
import org.geotools.util.NumberRange;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Envelope;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

/**
 * Imports data from a legacy "catalog.xml" file into the catalog.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public class LegacyCatalogImporter {

    /** logger */
    static Logger LOGGER = Logging.getLogger("org.geoserver.catalog");

    /** catalog */
    Catalog catalog;
    /** resource loader */
    GeoServerResourceLoader resourceLoader;

    /**
     * Creates the importer.
     *
     * @param catalog The catalog to import into.
     */
    public LegacyCatalogImporter(Catalog catalog) {
        this.catalog = catalog;
    }

    /**
     * No argument constructor.
     *
     * <p>Calling code should use {@link #setCatalog(Catalog)} when using this constructor.
     */
    public LegacyCatalogImporter() {}

    /** Sets the resource loader. */
    public void setResourceLoader(GeoServerResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /** Sets the catalog to import into. */
    public void setCatalog(Catalog catalog) {
        this.catalog = catalog;
    }

    /** The catalog being imported into. */
    public Catalog getCatalog() {
        return catalog;
    }

    /**
     * Imports configuration from a geoserver data directory into the catalog.
     *
     * @param dir The root of the data directory.
     */
    public void imprt(File dir) throws Exception {
        CatalogFactory factory = catalog.getFactory();

        // first off, import the main catalog so that namespaces, workspaces, styles,
        // datastores and coveragestores are read
        File catalogFile = new File(dir, "catalog.xml");
        if (!catalogFile.exists()) {
            throw new FileNotFoundException(
                    "Could not find catalog.xml under:" + dir.getAbsolutePath());
        }
        importCatalog(catalogFile);

        // for each feature type file, load the info.xml into a FeatureTypeInfo
        File featureTypes = new File(dir, "featureTypes");
        if (!featureTypes.exists()) featureTypes.mkdir();
        File[] featureTypeDirectories = featureTypes.listFiles();
        if (featureTypeDirectories != null) {
            for (int i = 0; i < featureTypeDirectories.length; i++) {
                File featureTypeDirectory = featureTypeDirectories[i];
                if (!featureTypeDirectory.isDirectory() || featureTypeDirectory.isHidden())
                    continue;

                // load info.xml
                File ftInfoFile = new File(featureTypeDirectory, "info.xml");
                if (!ftInfoFile.exists()) {
                    LOGGER.fine(
                            "No info.xml found in directory: '"
                                    + featureTypeDirectory.getName()
                                    + "', ignoring");
                    continue;
                }

                LegacyFeatureTypeInfoReader ftInfoReader = new LegacyFeatureTypeInfoReader();
                try {
                    ftInfoReader.read(Files.asResource(ftInfoFile));
                    FeatureTypeInfo featureType =
                            readFeatureType(ftInfoReader, featureTypeDirectory);
                    if (featureType == null) {
                        continue;
                    }
                    catalog.add(featureType);

                    LOGGER.info("Loaded feature type '" + featureType.prefixedName() + "'");

                    // create a wms layer for the feature type
                    LayerInfo layer = factory.createLayer();
                    layer.setResource(featureType);
                    layer.setName(featureType.getName());
                    layer.setPath(ftInfoReader.wmsPath());
                    if (layer.getPath() == null) {
                        layer.setPath("/");
                    }
                    layer.setType(PublishedType.VECTOR);

                    String defaultStyleName = ftInfoReader.defaultStyle();
                    if (defaultStyleName != null) {
                        StyleInfo style = catalog.getStyleByName(defaultStyleName);
                        if (style != null) {
                            layer.setDefaultStyle(style);
                        }
                    }
                    List<String> styles = ftInfoReader.styles();
                    if (styles != null) {
                        for (String styleName : styles) {
                            StyleInfo style = catalog.getStyleByName(styleName);
                            if (style != null) {
                                layer.getStyles().add(style);
                            }
                        }
                    }

                    Map legendURL = ftInfoReader.legendURL();
                    if (legendURL != null) {
                        LegendInfo legend = factory.createLegend();
                        legend.setHeight((Integer) legendURL.get("height"));
                        legend.setWidth((Integer) legendURL.get("width"));
                        legend.setFormat((String) legendURL.get("format"));
                        legend.setOnlineResource((String) legendURL.get("onlineResource"));
                        layer.setLegend(legend);
                    }

                    layer.setEnabled(featureType.isEnabled());
                    catalog.add(layer);
                } catch (Exception e) {
                    LOGGER.warning(
                            "Error loadin '"
                                    + featureTypeDirectory.getName()
                                    + "/info.xml', ignoring");
                    LOGGER.log(Level.INFO, "", e);
                    continue;
                }
            }
        }

        // for each coverage definition in coverage, read it
        File coverages = new File(dir, "coverages");
        if (!coverages.exists()) coverages.mkdir();
        File[] coverageDirectories = coverages.listFiles();
        if (coverageDirectories != null) {
            for (int i = 0; i < coverageDirectories.length; i++) {
                File coverageDirectory = coverageDirectories[i];
                if (!coverageDirectory.isDirectory() || coverageDirectory.isHidden()) continue;

                // load info.xml
                File cInfoFile = new File(coverageDirectory, "info.xml");
                if (!cInfoFile.exists()) {
                    LOGGER.fine(
                            "No info.xml found in directory: '"
                                    + coverageDirectory.getName()
                                    + "', ignoring");
                    continue;
                }

                LegacyCoverageInfoReader cInfoReader = new LegacyCoverageInfoReader();
                try {
                    cInfoReader.read(cInfoFile);

                    CoverageInfo coverage = readCoverage(cInfoReader);
                    if (coverage == null) {
                        continue;
                    }
                    catalog.add(coverage);

                    // create a wms layer for the feature type
                    LayerInfo layer = factory.createLayer();
                    layer.setResource(coverage);
                    layer.setName(coverage.getName());
                    layer.setPath(cInfoReader.wmsPath());
                    if (layer.getPath() == null) {
                        layer.setPath("/");
                    }
                    layer.setType(PublishedType.RASTER);

                    String defaultStyleName = cInfoReader.defaultStyle();
                    if (defaultStyleName != null) {
                        StyleInfo style = catalog.getStyleByName(defaultStyleName);
                        if (style != null) {
                            layer.setDefaultStyle(style);
                        }
                    }
                    List<String> styles = cInfoReader.styles();
                    if (styles != null) {
                        for (String styleName : styles) {
                            StyleInfo style = catalog.getStyleByName(styleName);
                            if (style != null) {
                                layer.getStyles().add(style);
                            }
                        }
                    }
                    layer.setEnabled(coverage.isEnabled());

                    catalog.add(layer);
                } catch (Exception e) {
                    LOGGER.warning(
                            "Error loading '"
                                    + coverageDirectory.getName()
                                    + "/info.xml', ignoring");
                    LOGGER.log(Level.INFO, "", e);
                    continue;
                }
            }
        }
    }

    void importCatalog(File catalogFile) throws FileNotFoundException, IOException, Exception {
        CatalogFactory factory = catalog.getFactory();

        LegacyCatalogReader reader = new LegacyCatalogReader();
        reader.read(Files.asResource(catalogFile));

        // build all the catalog objects that can be read from the catalog.xml file
        importNamespaces(factory, reader.namespaces(), false);
        importNamespaces(factory, reader.isolatedNamespaces(), true);
        importStyles(factory, reader.styles());
        importDataStores(factory, reader.dataStores());
        importFormats(factory, reader.formats());
    }

    void importFormats(CatalogFactory factory, List formats) {
        for (Iterator f = formats.iterator(); f.hasNext(); ) {
            Map map = (Map) f.next();
            CoverageStoreInfo coverageStore = factory.createCoverageStore();

            coverageStore.setName((String) map.get("id"));
            coverageStore.setType((String) map.get("type"));
            coverageStore.setURL((String) map.get("url"));
            coverageStore.setDescription((String) map.get("description"));

            String namespacePrefix = (String) map.get("namespace");
            // coverageStore.setNamespace( catalog.getNamespaceByPrefix( namespacePrefix ));
            coverageStore.setWorkspace(catalog.getWorkspaceByName(namespacePrefix));

            coverageStore.setEnabled((Boolean) map.get("enabled"));
            catalog.add(coverageStore);

            LOGGER.info(
                    "Processed coverage store '"
                            + coverageStore.getName()
                            + "', "
                            + (coverageStore.isEnabled() ? "enabled" : "disabled"));
        }
    }

    void importDataStores(CatalogFactory factory, Map dataStores) {
        for (Iterator d = dataStores.values().iterator(); d.hasNext(); ) {
            Map map = (Map) d.next();
            DataStoreInfo dataStore = factory.createDataStore();
            dataStore.setName((String) map.get("id"));

            String namespacePrefix = (String) map.get("namespace");
            // dataStore.setNamespace( catalog.getNamespaceByPrefix( namespacePrefix ));
            dataStore.setWorkspace(catalog.getWorkspaceByName(namespacePrefix));

            Map connectionParams = (Map) map.get("connectionParams");
            for (Iterator e = connectionParams.entrySet().iterator(); e.hasNext(); ) {
                Map.Entry entry = (Map.Entry) e.next();
                String key = (String) entry.getKey();
                Serializable value = (Serializable) entry.getValue();

                dataStore.getConnectionParameters().put(key, value);
            }
            // set the namespace parameter
            NamespaceInfo ns = catalog.getNamespaceByPrefix(dataStore.getWorkspace().getName());
            dataStore.getConnectionParameters().put("namespace", ns.getURI());

            dataStore.setEnabled((Boolean) map.get("enabled"));
            catalog.add(dataStore);

            if (dataStore.isEnabled()) {
                try {
                    // test connection to data store
                    dataStore.getDataStore(null);

                    // connection ok
                    LOGGER.info(
                            "Processed data store '"
                                    + dataStore.getName()
                                    + "', "
                                    + (dataStore.isEnabled() ? "enabled" : "disabled"));
                } catch (Exception e) {
                    LOGGER.warning("Error connecting to '" + dataStore.getName() + "'");
                    LOGGER.log(Level.INFO, "", e);

                    dataStore.setError(e);
                    dataStore.setEnabled(false);
                }
            }
        }
    }

    /** Imports all styles and loads them into the catalog */
    void importStyles(CatalogFactory factory, Map styles) {
        for (Iterator s = styles.entrySet().iterator(); s.hasNext(); ) {
            Map.Entry entry = (Map.Entry) s.next();
            StyleInfo style = factory.createStyle();
            style.setName((String) entry.getKey());
            style.setFilename((String) entry.getValue());

            catalog.add(style);
            LOGGER.info("Loaded style '" + style.getName() + "'");
        }
    }

    /** Imports namespaces and create symmetric workspaces for them */
    void importNamespaces(CatalogFactory factory, Map namespaces) {
        importNamespaces(factory, namespaces, false);
    }

    /**
     * Imports namespaces and create symmetric workspaces for them setting isolation using the
     * provided value.
     */
    void importNamespaces(CatalogFactory factory, Map namespaces, boolean isolated) {
        for (Iterator n = namespaces.entrySet().iterator(); n.hasNext(); ) {
            Map.Entry entry = (Map.Entry) n.next();
            if (entry.getKey() == null || "".equals(entry.getKey())) {
                continue;
            }

            NamespaceInfo namespace = factory.createNamespace();
            namespace.setPrefix((String) entry.getKey());
            namespace.setURI((String) entry.getValue());
            namespace.setIsolated(isolated);
            catalog.add(namespace);

            WorkspaceInfo workspace = factory.createWorkspace();
            workspace.setName((String) entry.getKey());
            workspace.setIsolated(isolated);
            catalog.add(workspace);

            if (namespace.getURI().equals(namespaces.get(""))) {
                catalog.setDefaultNamespace(namespace);
                catalog.setDefaultWorkspace(workspace);
            }

            LOGGER.info(
                    "Loaded namespace '"
                            + namespace.getPrefix()
                            + "' ("
                            + namespace.getURI()
                            + ")");
        }

        if (catalog.getDefaultNamespace() != null) {
            LOGGER.info("Default namespace: '" + catalog.getDefaultNamespace().getPrefix() + "'");
        } else {
            LOGGER.warning("No default namespace set.");
        }
    }

    /** TODO: code smell: no method should be this long */
    FeatureTypeInfo readFeatureType(LegacyFeatureTypeInfoReader ftInfoReader, File ftDirectory)
            throws Exception {
        CatalogFactory factory = catalog.getFactory();
        FeatureTypeInfo featureType = factory.createFeatureType();

        featureType.setNativeName(ftInfoReader.name());
        if (ftInfoReader.alias() != null) {
            featureType.setName(ftInfoReader.alias());
        } else {
            featureType.setName(ftInfoReader.name());
        }

        featureType.setSRS("EPSG:" + ftInfoReader.srs());

        ProjectionPolicy pp = ProjectionPolicy.get(ftInfoReader.srsHandling());
        featureType.setProjectionPolicy(pp);

        featureType.setTitle(ftInfoReader.title());
        featureType.setAbstract(ftInfoReader.abstrct());
        for (String kw : ftInfoReader.keywords()) {
            featureType.getKeywords().add(new Keyword(kw));
        }

        for (Map m : ftInfoReader.metadataLinks()) {
            MetadataLinkInfo link = factory.createMetadataLink();
            link.setContent((String) m.get(null));
            link.setMetadataType((String) m.get("metadataType"));
            link.setType((String) m.get("type"));
            featureType.getMetadataLinks().add(link);
        }

        featureType.setLatLonBoundingBox(
                new ReferencedEnvelope(
                        ftInfoReader.latLonBoundingBox(), DefaultGeographicCRS.WGS84));
        featureType.setEnabled(true);
        featureType.setMaxFeatures(ftInfoReader.maxFeatures());
        featureType.getMetadata().put("dirName", ftInfoReader.parentDirectoryName());
        featureType.getMetadata().put("indexingEnabled", ftInfoReader.searchable());
        featureType.getMetadata().put(ResourceInfo.CACHING_ENABLED, ftInfoReader.cachingEnabled());
        featureType.getMetadata().put(ResourceInfo.CACHE_AGE_MAX, ftInfoReader.cacheAgeMax());
        featureType.getMetadata().put("kml.regionateAttribute", ftInfoReader.regionateAttribute());
        featureType.getMetadata().put("kml.regionateStrategy", ftInfoReader.regionateStrategy());
        featureType
                .getMetadata()
                .put("kml.regionateFeatureLimit", ftInfoReader.regionateFeatureLimit());

        // link to datastore
        String dataStoreName = ftInfoReader.dataStore();
        DataStoreInfo dataStore = catalog.getDataStoreByName(dataStoreName);
        if (dataStore == null) {
            LOGGER.warning(
                    "Ignoring feature type: '"
                            + ftInfoReader.parentDirectoryName()
                            + "', data store '"
                            + dataStoreName
                            + "'  not found");
            return null;
        }
        featureType.setStore(dataStore);

        // link to namespace
        String prefix = dataStore.getWorkspace().getName();
        featureType.setNamespace(catalog.getNamespaceByPrefix(prefix));

        if (featureType.isEnabled() && !dataStore.isEnabled()) {
            LOGGER.info(
                    "Ignoring feature type: '"
                            + ftInfoReader.parentDirectoryName()
                            + "', data store is disabled");
            featureType.setEnabled(false);
        }

        if (featureType.isEnabled()) {
            Exception error = null;

            // native crs
            DataAccess<? extends FeatureType, ? extends Feature> ds = null;
            try {
                ds = dataStore.getDataStore(null);
            } catch (Exception e) {
                LOGGER.warning(
                        "Ignoring feature type: '"
                                + featureType.getName()
                                + "', error occured connecting to data store: "
                                + e.getMessage());
                LOGGER.log(Level.INFO, "", e);
                error = e;
            }

            if (error == null) {
                try {
                    // load the native feature type, and generate attributes from that
                    FeatureType ft = ds.getSchema(featureType.getQualifiedNativeName());
                    featureType.setNativeCRS(ft.getCoordinateReferenceSystem());
                } catch (Exception e) {
                    LOGGER.warning(
                            "Ignoring feature type: '"
                                    + featureType.getNativeName()
                                    + "', error occured loading schema: "
                                    + e.getMessage());
                    LOGGER.log(Level.INFO, "", e);
                    error = e;
                }
            }

            if (error == null) {
                // native bounds
                Envelope nativeBBOX = ftInfoReader.nativeBoundingBox();
                if (nativeBBOX != null) {
                    featureType.setNativeBoundingBox(
                            new ReferencedEnvelope(nativeBBOX, featureType.getNativeCRS()));
                }
            }

            if (error != null) {
                featureType.setEnabled(false);
            }
        }

        return featureType;
    }

    CoverageInfo readCoverage(LegacyCoverageInfoReader cInfoReader) throws Exception {
        CatalogFactory factory = catalog.getFactory();

        // link to coverage store
        String coverageStoreName = cInfoReader.format();
        CoverageStoreInfo coverageStore = catalog.getCoverageStoreByName(coverageStoreName);

        if (coverageStore == null) {
            LOGGER.warning(
                    "Ignoring coverage: '"
                            + cInfoReader.parentDirectoryName()
                            + "', coverage store '"
                            + coverageStoreName
                            + "'  not found");
            return null;
        }

        if (!coverageStore.isEnabled()) {
            LOGGER.info(
                    "Ignoring coverage: '"
                            + cInfoReader.parentDirectoryName()
                            + "', coverage store is disabled");
            return null;
        }

        CoverageInfo coverage = factory.createCoverage();
        coverage.setStore(coverageStore);

        coverage.setName(cInfoReader.name());
        coverage.setNativeName(cInfoReader.name());
        coverage.setTitle(cInfoReader.label());
        coverage.setDescription(cInfoReader.description());
        for (String kw : cInfoReader.keywords()) {
            coverage.getKeywords().add(new Keyword(kw));
        }

        Map<String, Object> envelope = cInfoReader.envelope();
        String userDefinedCrsIdentifier = (String) envelope.get("srsName");
        String nativeCrsWkt = (String) envelope.get("crs");

        coverage.setSRS(userDefinedCrsIdentifier);
        CoordinateReferenceSystem crs = CRS.parseWKT(nativeCrsWkt);
        coverage.setNativeCRS(crs);

        ReferencedEnvelope bounds =
                new ReferencedEnvelope(
                        (Double) envelope.get("x1"),
                        (Double) envelope.get("x2"),
                        (Double) envelope.get("y1"),
                        (Double) envelope.get("y2"),
                        crs);
        coverage.setNativeBoundingBox(bounds);

        GeneralEnvelope boundsLatLon =
                CoverageStoreUtils.getWGS84LonLatEnvelope(new GeneralEnvelope(bounds));
        coverage.setLatLonBoundingBox(new ReferencedEnvelope(boundsLatLon));

        GeneralEnvelope gridEnvelope = new GeneralEnvelope(bounds);
        Map grid = cInfoReader.grid();
        if (grid != null) {
            int[] low = (int[]) grid.get("low");
            int[] high = (int[]) grid.get("high");

            GeneralGridEnvelope range = new GeneralGridEnvelope(low, high);

            Map<String, Double> tx = (Map<String, Double>) grid.get("geoTransform");
            if (tx != null) {
                double[] matrix = new double[3 * 3];
                matrix[0] = tx.get("scaleX") != null ? tx.get("scaleX") : matrix[0];
                matrix[1] = tx.get("shearX") != null ? tx.get("shearX") : matrix[1];
                matrix[2] = tx.get("translateX") != null ? tx.get("translateX") : matrix[2];
                matrix[3] = tx.get("shearY") != null ? tx.get("shearY") : matrix[3];
                matrix[4] = tx.get("scaleY") != null ? tx.get("scaleY") : matrix[4];
                matrix[5] = tx.get("translateY") != null ? tx.get("translateY") : matrix[5];
                matrix[8] = 1.0;

                MathTransform gridToCRS =
                        new DefaultMathTransformFactory()
                                .createAffineTransform(new GeneralMatrix(3, 3, matrix));
                coverage.setGrid(new GridGeometry2D(range, gridToCRS, crs));
            } else {
                coverage.setGrid(new GridGeometry2D(range, gridEnvelope));
            }
        } else {
            // new grid range
            GeneralGridEnvelope range = new GeneralGridEnvelope(new int[] {0, 0}, new int[] {1, 1});
            coverage.setGrid(new GridGeometry2D(range, gridEnvelope));
        }

        for (Iterator x = cInfoReader.coverageDimensions().iterator(); x.hasNext(); ) {
            Map map = (Map) x.next();
            CoverageDimensionInfo cd = factory.createCoverageDimension();
            cd.setName((String) map.get("name"));
            cd.setDescription((String) map.get("description"));
            cd.setRange(NumberRange.create((Double) map.get("min"), (Double) map.get("max")));
            coverage.getDimensions().add(cd);
        }

        coverage.setNativeFormat(cInfoReader.nativeFormat());
        coverage.getSupportedFormats().addAll(cInfoReader.supportedFormats());

        coverage.setDefaultInterpolationMethod(cInfoReader.defaultInterpolation());
        coverage.getInterpolationMethods().addAll(cInfoReader.supportedInterpolations());

        coverage.getRequestSRS().addAll(cInfoReader.requestCRSs());
        coverage.getResponseSRS().addAll(cInfoReader.responseCRSs());

        coverage.getMetadata().put("dirName", cInfoReader.parentDirectoryName());
        coverage.setEnabled(coverageStore.isEnabled());

        // parameters
        coverage.getParameters().putAll(cInfoReader.parameters());

        // link to namespace
        String prefix = catalog.getCoverageStoreByName(coverageStoreName).getWorkspace().getName();
        coverage.setNamespace(catalog.getNamespaceByPrefix(prefix));

        return coverage;
    }
}

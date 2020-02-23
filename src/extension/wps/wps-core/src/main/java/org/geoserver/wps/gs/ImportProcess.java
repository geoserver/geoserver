/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import javax.media.jai.Interpolation;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.wps.WPSException;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.imageio.GeoToolsWriteParams;
import org.geotools.data.DataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.data.util.NullProgressListener;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.gce.geotiff.GeoTiffWriteParams;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.process.ProcessException;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.URLs;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.util.ProgressListener;
import org.vfny.geoserver.util.WCSUtils;

/**
 * Imports a feature collection into the GeoServer catalog
 *
 * @author Andrea Aime - OpenGeo
 */
@DescribeProcess(
    title = "Import to Catalog",
    description = "Imports a feature collection into the catalog"
)
public class ImportProcess implements GeoServerProcess {

    static final Logger LOGGER = Logging.getLogger(ImportProcess.class);

    private static final GeoTiffWriteParams DEFAULT_WRITE_PARAMS;

    static {
        // setting the write parameters (we my want to make these configurable in the future
        DEFAULT_WRITE_PARAMS = new GeoTiffWriteParams();
        DEFAULT_WRITE_PARAMS.setCompressionMode(GeoTiffWriteParams.MODE_EXPLICIT);
        DEFAULT_WRITE_PARAMS.setCompressionType("LZW");
        DEFAULT_WRITE_PARAMS.setCompressionQuality(0.75F);
        DEFAULT_WRITE_PARAMS.setTilingMode(GeoToolsWriteParams.MODE_EXPLICIT);
        DEFAULT_WRITE_PARAMS.setTiling(512, 512);
    }

    private Catalog catalog;

    public ImportProcess(Catalog catalog) {
        this.catalog = catalog;
    }

    @DescribeResult(name = "layerName", description = "Name of the new featuretype, with workspace")
    public String execute(
            @DescribeParameter(name = "features", min = 0, description = "Input feature collection")
                    SimpleFeatureCollection features,
            @DescribeParameter(name = "coverage", min = 0, description = "Input raster")
                    GridCoverage2D coverage,
            @DescribeParameter(
                        name = "workspace",
                        min = 0,
                        description = "Target workspace (default is the system default)"
                    )
                    String workspace,
            @DescribeParameter(
                        name = "store",
                        min = 0,
                        description = "Target store (default is the workspace default)"
                    )
                    String store,
            @DescribeParameter(
                        name = "name",
                        min = 0,
                        description =
                                "Name of the new featuretype/coverage (default is the name of the features in the collection)"
                    )
                    String name,
            @DescribeParameter(
                        name = "srs",
                        min = 0,
                        description =
                                "Target coordinate reference system (default is based on source when possible)"
                    )
                    CoordinateReferenceSystem srs,
            @DescribeParameter(
                        name = "srsHandling",
                        min = 0,
                        description =
                                "Desired SRS handling (default is FORCE_DECLARED, others are REPROJECT_TO_DECLARED or NONE)"
                    )
                    ProjectionPolicy srsHandling,
            @DescribeParameter(
                        name = "styleName",
                        min = 0,
                        description =
                                "Name of the style to be associated with the layer (default is a standard geometry-specific style)"
                    )
                    String styleName,
            ProgressListener listener)
            throws ProcessException {
        // avoid null checks
        if (listener == null) {
            listener = new NullProgressListener();
        }
        listener.started();
        listener.progress(0);

        // first off, decide what is the target store
        WorkspaceInfo ws;
        if (workspace != null) {
            ws = catalog.getWorkspaceByName(workspace);
            if (ws == null) {
                throw new ProcessException("Could not find workspace " + workspace);
            }
        } else {
            ws = catalog.getDefaultWorkspace();
            if (ws == null) {
                throw new ProcessException(
                        "The catalog is empty, could not find a default workspace");
            }
        }

        // create a builder to help build catalog objects
        CatalogBuilder cb = new CatalogBuilder(catalog);
        cb.setWorkspace(ws);

        // ok, find the target store
        StoreInfo storeInfo = null;
        boolean add = false;
        if (store != null) {
            if (features != null) {
                storeInfo = catalog.getDataStoreByName(ws.getName(), store);
            } else if (coverage != null) {
                storeInfo = catalog.getCoverageStoreByName(ws.getName(), store);
            }
            if (storeInfo == null) {
                // mirroring "features != null" below
                if (features != null) {
                    storeInfo = catalog.getDefaultDataStore(ws);
                    if (storeInfo == null) {
                        throw new ProcessException(
                                "Could not find a default store in workspace " + ws.getName());
                    }
                } else if (coverage != null) {
                    // since the store doesn't exist, create it
                    // mirroring "create a new coverage store" below
                    storeInfo = cb.buildCoverageStore((store));
                    add = true;
                    LOGGER.info("Creating store " + store + " since it did not exist");
                }
            }
        } else if (features != null) {
            storeInfo = catalog.getDefaultDataStore(ws);
            if (storeInfo == null) {
                throw new ProcessException(
                        "Could not find a default store in workspace " + ws.getName());
            }
        } else if (coverage != null) {
            // create a new coverage store
            LOGGER.info(
                    "Auto-configuring coverage store: "
                            + (name != null ? name : coverage.getName().toString()));

            storeInfo =
                    cb.buildCoverageStore((name != null ? name : coverage.getName().toString()));
            add = true;
            store = (name != null ? name : coverage.getName().toString());

            if (storeInfo == null) {
                throw new ProcessException(
                        "Could not find a default store in workspace " + ws.getName());
            }
        }

        checkForCancellation(listener);

        // check the target style if any
        StyleInfo targetStyle = null;
        if (styleName != null) {
            targetStyle = catalog.getStyleByName(styleName);
            if (targetStyle == null) {
                throw new ProcessException("Could not find style " + styleName);
            }
        }

        if (features != null) {
            // check if the target layer and the target feature type are not
            // already there (this is a half-assed attempt as we don't have
            // an API telling us how the feature type name will be changed
            // by DataStore.createSchema(...), but better than fully importing
            // the data into the target store to find out we cannot create the layer...)
            String tentativeTargetName = null;
            if (name != null) {
                tentativeTargetName = ws.getName() + ":" + name;
            } else {
                tentativeTargetName = ws.getName() + ":" + features.getSchema().getTypeName();
            }
            if (catalog.getLayer(tentativeTargetName) != null) {
                throw new ProcessException(
                        "Target layer " + tentativeTargetName + " already exists");
            }

            // check the target crs
            String targetSRSCode = null;
            if (srs != null) {
                try {
                    Integer code = CRS.lookupEpsgCode(srs, true);
                    if (code == null) {
                        throw new WPSException("Could not find a EPSG code for " + srs);
                    }
                    targetSRSCode = "EPSG:" + code;
                } catch (Exception e) {
                    throw new ProcessException(
                            "Could not lookup the EPSG code for the provided srs", e);
                }
            } else {
                // check we can extract a code from the original data
                GeometryDescriptor gd = features.getSchema().getGeometryDescriptor();
                if (gd == null) {
                    // data is geometryless, we need a fake SRS
                    targetSRSCode = "EPSG:4326";
                    srsHandling = ProjectionPolicy.FORCE_DECLARED;
                } else {
                    CoordinateReferenceSystem nativeCrs = gd.getCoordinateReferenceSystem();
                    if (nativeCrs == null) {
                        throw new ProcessException(
                                "The original data has no native CRS, "
                                        + "you need to specify the srs parameter");
                    } else {
                        try {
                            Integer code = CRS.lookupEpsgCode(nativeCrs, true);
                            if (code == null) {
                                throw new ProcessException(
                                        "Could not find an EPSG code for data "
                                                + "native spatial reference system: "
                                                + nativeCrs);
                            } else {
                                targetSRSCode = "EPSG:" + code;
                            }
                        } catch (Exception e) {
                            throw new ProcessException(
                                    "Failed to loookup an official EPSG code for "
                                            + "the source data native "
                                            + "spatial reference system",
                                    e);
                        }
                    }
                }
            }

            checkForCancellation(listener);

            // import the data into the target store
            SimpleFeatureType targetType;
            try {
                targetType =
                        importDataIntoStore(features, name, (DataStoreInfo) storeInfo, listener);
            } catch (IOException e) {
                throw new ProcessException("Failed to import data into the target store", e);
            }

            // now import the newly created layer into GeoServer
            try {
                cb.setStore(storeInfo);

                // build the typeInfo and set CRS if necessary
                FeatureTypeInfo typeInfo = cb.buildFeatureType(targetType.getName());
                if (targetSRSCode != null) {
                    typeInfo.setSRS(targetSRSCode);
                }
                if (srsHandling != null) {
                    typeInfo.setProjectionPolicy(srsHandling);
                }
                // compute the bounds
                cb.setupBounds(typeInfo);

                // build the layer and set a style
                LayerInfo layerInfo = cb.buildLayer(typeInfo);
                if (targetStyle != null) {
                    layerInfo.setDefaultStyle(targetStyle);
                }

                checkForCancellation(listener);

                catalog.add(typeInfo);
                catalog.add(layerInfo);

                listener.progress(100);
                listener.complete();

                return layerInfo.prefixedName();
            } catch (Exception e) {
                throw new ProcessException(
                        "Failed to complete the import inside the GeoServer catalog", e);
            }
        } else if (coverage != null) {
            try {
                final Resource directory =
                        catalog.getResourceLoader().get(Paths.path("data", workspace, store));
                final File file = File.createTempFile(store, ".tif", directory.dir());
                ((CoverageStoreInfo) storeInfo).setURL(URLs.fileToUrl(file).toExternalForm());
                ((CoverageStoreInfo) storeInfo).setType("GeoTIFF");

                // check the target crs
                CoordinateReferenceSystem cvCrs = coverage.getCoordinateReferenceSystem();
                if (srs != null) {
                    try {
                        Integer code = CRS.lookupEpsgCode(srs, true);
                        if (code == null) {
                            throw new WPSException("Could not find a EPSG code for " + srs);
                        }
                    } catch (Exception e) {
                        throw new ProcessException(
                                "Could not lookup the EPSG code for the provided srs", e);
                    }
                } else {
                    // check we can extract a code from the original data
                    if (cvCrs == null) {
                        // data is geometryless, we need a fake SRS
                        srs = DefaultGeographicCRS.WGS84;
                    } else {
                        CoordinateReferenceSystem nativeCrs = cvCrs;
                        if (nativeCrs == null) {
                            throw new ProcessException(
                                    "The original data has no native CRS, "
                                            + "you need to specify the srs parameter");
                        } else {
                            try {
                                Integer code = CRS.lookupEpsgCode(nativeCrs, true);
                                if (code == null) {
                                    throw new ProcessException(
                                            "Could not find an EPSG code for data "
                                                    + "native spatial reference system: "
                                                    + nativeCrs);
                                } else {
                                    String targetSRSCode = "EPSG:" + code;
                                    srs = CRS.decode(targetSRSCode, true);
                                }
                            } catch (Exception e) {
                                throw new ProcessException(
                                        "Failed to loookup an official EPSG code for "
                                                + "the source data native "
                                                + "spatial reference system",
                                        e);
                            }
                        }
                    }
                }

                checkForCancellation(listener);

                MathTransform tx = CRS.findMathTransform(cvCrs, srs);

                if (!tx.isIdentity() || !CRS.equalsIgnoreMetadata(cvCrs, srs)) {
                    coverage =
                            WCSUtils.resample(
                                    coverage,
                                    cvCrs,
                                    srs,
                                    null,
                                    Interpolation.getInstance(Interpolation.INTERP_NEAREST));
                }

                GeoTiffWriter writer = new GeoTiffWriter(file);

                // setting the write parameters for this geotiff
                final ParameterValueGroup params = new GeoTiffFormat().getWriteParameters();
                params.parameter(AbstractGridFormat.GEOTOOLS_WRITE_PARAMS.getName().toString())
                        .setValue(DEFAULT_WRITE_PARAMS);
                final GeneralParameterValue[] wps =
                        params.values().toArray(new GeneralParameterValue[1]);

                try {
                    writer.write(coverage, wps);
                } finally {
                    try {
                        writer.dispose();
                    } catch (Exception e) {
                        // we tried, no need to fuss around this one
                    }
                }

                checkForCancellation(listener);

                // add or update the datastore info
                if (add) {
                    catalog.add(storeInfo);
                } else {
                    catalog.save(storeInfo);
                }

                cb.setStore(storeInfo);

                GridCoverage2DReader reader = new GeoTiffReader(file);
                if (reader == null) {
                    throw new ProcessException("Could not aquire reader for coverage.");
                }

                // coverage read params
                final Map customParameters = new HashMap();
                /*
                 * String useJAIImageReadParam = "USE_JAI_IMAGEREAD"; if (useJAIImageReadParam != null) {
                 * customParameters.put(AbstractGridFormat.USE_JAI_IMAGEREAD.getName().toString(), Boolean.valueOf(useJAIImageReadParam)); }
                 */

                CoverageInfo cinfo = cb.buildCoverage(reader, customParameters);

                // check if the name of the coverage was specified
                if (name != null) {
                    cinfo.setName(name);
                }

                checkForCancellation(listener);

                if (!add) {
                    // update the existing
                    CoverageInfo existing =
                            catalog.getCoverageByCoverageStore(
                                    (CoverageStoreInfo) storeInfo,
                                    name != null ? name : coverage.getName().toString());
                    if (existing == null) {
                        // grab the first if there is only one
                        List<CoverageInfo> coverages =
                                catalog.getCoveragesByCoverageStore((CoverageStoreInfo) storeInfo);
                        if (coverages.size() == 1) {
                            existing = coverages.get(0);
                        }
                        if (coverages.size() == 0) {
                            // no coverages yet configured, change add flag and continue on
                            add = true;
                        } else {
                            // multiple coverages, and one to configure not specified
                            throw new ProcessException(
                                    "Unable to determine coverage to configure.");
                        }
                    }

                    if (existing != null) {
                        cb.updateCoverage(existing, cinfo);
                        catalog.save(existing);
                        cinfo = existing;
                    }
                }

                // do some post configuration, if srs is not known or unset, transform to 4326
                if ("UNKNOWN".equals(cinfo.getSRS())) {
                    // CoordinateReferenceSystem sourceCRS =
                    // cinfo.getBoundingBox().getCoordinateReferenceSystem();
                    // CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:4326", true);
                    // ReferencedEnvelope re = cinfo.getBoundingBox().transform(targetCRS, true);
                    cinfo.setSRS("EPSG:4326");
                    // cinfo.setCRS( targetCRS );
                    // cinfo.setBoundingBox( re );
                }

                checkForCancellation(listener);

                // add/save
                LayerInfo layerInfo;
                if (add) {
                    catalog.add(cinfo);

                    layerInfo = cb.buildLayer(cinfo);
                    if (styleName != null && targetStyle != null) {
                        layerInfo.setDefaultStyle(targetStyle);
                    }
                    // JD: commenting this out, these sorts of edits should be handled
                    // with a second PUT request on the created coverage
                    /*
                     * String styleName = form.getFirstValue("style"); if ( styleName != null ) { StyleInfo style = catalog.getStyleByName( styleName
                     * ); if ( style != null ) { layerInfo.setDefaultStyle( style ); if ( !layerInfo.getStyles().contains( style ) ) {
                     * layerInfo.getStyles().add( style ); } } else { LOGGER.warning( "Client specified style '" + styleName +
                     * "'but no such style exists."); } }
                     *
                     * String path = form.getFirstValue( "path"); if ( path != null ) { layerInfo.setPath( path ); }
                     */

                    boolean valid = true;
                    try {
                        if (!catalog.validate(layerInfo, true).isValid()) {
                            valid = false;
                        }
                    } catch (Exception e) {
                        valid = false;
                    }

                    layerInfo.setEnabled(valid);
                    catalog.add(layerInfo);

                    return layerInfo.prefixedName();
                } else {
                    catalog.save(cinfo);

                    layerInfo = catalog.getLayerByName(cinfo.getName());
                    if (styleName != null && targetStyle != null) {
                        layerInfo.setDefaultStyle(targetStyle);
                    }
                }
                listener.progress(100);
                listener.complete();
                return layerInfo.prefixedName();
            } catch (MalformedURLException e) {
                throw new ProcessException("URL Error", e);
            } catch (IOException e) {
                throw new ProcessException("I/O Exception", e);
            } catch (Exception e) {
                e.printStackTrace();
                throw new ProcessException("Exception", e);
            }
        }

        return null;
    }

    private SimpleFeatureType importDataIntoStore(
            SimpleFeatureCollection features,
            String name,
            DataStoreInfo storeInfo,
            ProgressListener listener)
            throws IOException, ProcessException {
        SimpleFeatureType targetType;
        // grab the data store
        DataStore ds = (DataStore) storeInfo.getDataStore(null);

        // decide on the target ft name
        SimpleFeatureType sourceType = features.getSchema();
        if (name != null) {
            SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
            tb.init(sourceType);
            tb.setName(name);
            sourceType = tb.buildFeatureType();
        }

        // create the schema
        ds.createSchema(sourceType);

        // try to get the target feature type (might have slightly different
        // name and structure)
        targetType = ds.getSchema(sourceType.getTypeName());
        if (targetType == null) {
            // ouch, the name was changed... we can only guess now...
            // try with the typical Oracle mangling
            targetType = ds.getSchema(sourceType.getTypeName().toUpperCase());
        }

        if (targetType == null) {
            throw new WPSException(
                    "The target schema was created, but with a name "
                            + "that we cannot relate to the one we provided the data store. Cannot proceeed further");
        } else {
            // check the layer is not already there
            String newLayerName =
                    storeInfo.getWorkspace().getName() + ":" + targetType.getTypeName();
            LayerInfo layer = catalog.getLayerByName(newLayerName);
            // todo: we should not really reach here and know beforehand what the targetType
            // name is, but if we do we should at least get a way to drop it
            if (layer != null) {
                throw new ProcessException(
                        "Target layer " + newLayerName + " already exists in the catalog");
            }
        }

        // try to establish a mapping with old and new attributes. This is again
        // just guesswork until we have a geotools api that will give us the
        // exact mapping to be performed
        Map<String, String> mapping = buildAttributeMapping(sourceType, targetType);

        // start a transaction and fill the target with the input features
        SimpleFeatureStore fstore =
                (SimpleFeatureStore) ds.getFeatureSource(targetType.getTypeName());
        Transaction t = new DefaultTransaction();
        fstore.setTransaction(t);
        boolean complete = false;
        try (SimpleFeatureIterator fi = features.features()) {
            SimpleFeatureBuilder fb = new SimpleFeatureBuilder(targetType);
            while (fi.hasNext()) {
                SimpleFeature source = fi.next();
                fb.reset();
                for (String sname : mapping.keySet()) {
                    fb.set(mapping.get(sname), source.getAttribute(sname));
                }
                SimpleFeature target = fb.buildFeature(null);
                fstore.addFeatures(DataUtilities.collection(target));

                // we do no report progress as we'd need the collection size
                // and the collection might be streaming
                checkForCancellation(listener);
            }
            t.commit();
            complete = true;
        } finally {
            if (!complete) {
                t.rollback();
            }
            t.close();
        }

        return targetType;
    }

    private void checkForCancellation(ProgressListener listener) {
        if (listener.isCanceled()) {
            throw new ProcessException(listener.getTask().toString());
        }
    }

    /**
     * Applies a set of heuristics to find which target attribute corresponds to a certain input
     * attribute
     */
    Map<String, String> buildAttributeMapping(
            SimpleFeatureType sourceType, SimpleFeatureType targetType) {
        // look for the typical manglings. For example, if the target is a
        // shapefile store it will move the geometry and name it the_geom

        // collect the source names
        Set<String> sourceNames = new HashSet<String>();
        for (AttributeDescriptor sd : sourceType.getAttributeDescriptors()) {
            sourceNames.add(sd.getLocalName());
        }

        // first check if we have been kissed by sheer luck and the names are
        // the same
        Map<String, String> result = new HashMap<String, String>();
        for (String name : sourceNames) {
            if (targetType.getDescriptor(name) != null) {
                result.put(name, name);
            }
        }
        sourceNames.removeAll(result.keySet());

        // then check for simple case difference (Oracle case)
        for (String name : sourceNames) {
            for (AttributeDescriptor td : targetType.getAttributeDescriptors()) {
                if (td.getLocalName().equalsIgnoreCase(name)) {
                    result.put(name, td.getLocalName());
                    break;
                }
            }
        }
        sourceNames.removeAll(result.keySet());

        // then check attribute names being cut (another Oracle case)
        for (String name : sourceNames) {
            String loName = name.toLowerCase();
            for (AttributeDescriptor td : targetType.getAttributeDescriptors()) {
                String tdName = td.getLocalName().toLowerCase();
                if (loName.startsWith(tdName)) {
                    result.put(name, td.getLocalName());
                    break;
                }
            }
        }
        sourceNames.removeAll(result.keySet());

        // consider the shapefile geometry descriptor mangling
        if (targetType.getGeometryDescriptor() != null
                && "the_geom".equals(targetType.getGeometryDescriptor().getLocalName())
                && !"the_geom"
                        .equalsIgnoreCase(sourceType.getGeometryDescriptor().getLocalName())) {
            result.put(sourceType.getGeometryDescriptor().getLocalName(), "the_geom");
        }

        // and finally we return with as much as we can match
        if (!sourceNames.isEmpty()) {
            LOGGER.warning(
                    "Could not match the following attributes "
                            + sourceNames
                            + " to the target feature type ones: "
                            + targetType);
        }
        return result;
    }
}

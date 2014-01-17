package org.geoserver.wps.gs;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.wps.WPSException;
import org.geotools.data.DataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.process.ProcessException;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class FeaturesImporter {
    
    static final Logger LOGGER = Logging.getLogger(FeaturesImporter.class);
            
    public Catalog catalog;

    public FeaturesImporter(Catalog catalog) {
        this.catalog = catalog;
    }

    /**
     * Import a SimpleFeatureCollection as a new layer and assign the 'targetStyle' to it.
     * 
     * @param features
     * @param name
     * @param cb
     * @param ws
     * @param storeInfo
     * @param srs
     * @param srsHandling
     * @param targetStyle
     * @return
     * @throws ProcessException
     */
    public String execute(
            SimpleFeatureCollection features, 
            String name,
            CatalogBuilder cb, 
            WorkspaceInfo ws,
            StoreInfo storeInfo, 
            CoordinateReferenceSystem srs, 
            ProjectionPolicy srsHandling, 
            StyleInfo targetStyle
            ) throws ProcessException {
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
        if (this.catalog.getLayer(tentativeTargetName) != null) {
            throw new ProcessException("Target layer " + tentativeTargetName + " already exists");
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
                throw new ProcessException("Could not lookup the EPSG code for the provided srs", e);
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
                    throw new ProcessException("The original data has no native CRS, "
                            + "you need to specify the srs parameter");
                } else {
                    try {
                        Integer code = CRS.lookupEpsgCode(nativeCrs, true);
                        if (code == null) {
                            throw new ProcessException("Could not find an EPSG code for data "
                                    + "native spatial reference system: " + nativeCrs);
                        } else {
                            targetSRSCode = "EPSG:" + code;
                        }
                    } catch (Exception e) {
                        throw new ProcessException("Failed to loookup an official EPSG code for "
                                + "the source data native " + "spatial reference system", e);
                    }
                }
            }
        }

        // import the data into the target store
        SimpleFeatureType targetType;
        try {
            targetType = importDataIntoStore(features, name, (DataStoreInfo) storeInfo);
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

            this.catalog.add(typeInfo);
            this.catalog.add(layerInfo);

            return layerInfo.prefixedName();
        } catch (Exception e) {
            throw new ProcessException(
                    "Failed to complete the import inside the GeoServer catalog", e);
        }
    }
    
    /**
     * 
     * @param features
     * @param name
     * @param storeInfo
     * @return
     * @throws IOException
     * @throws ProcessException
     */
    private SimpleFeatureType importDataIntoStore(SimpleFeatureCollection features, String name,
            DataStoreInfo storeInfo) throws IOException, ProcessException {
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
            String newLayerName = storeInfo.getWorkspace().getName() + ":"
                    + targetType.getTypeName();
            LayerInfo layer = this.catalog.getLayerByName(newLayerName);
            // todo: we should not really reach here and know beforehand what the targetType
            // name is, but if we do we should at least get a way to drop it
            if (layer != null) {
                throw new ProcessException("Target layer " + newLayerName
                        + " already exists in the catalog");
            }
        }

        // try to establish a mapping with old and new attributes. This is again
        // just guesswork until we have a geotools api that will give us the
        // exact mapping to be performed
        Map<String, String> mapping = buildAttributeMapping(sourceType, targetType);

        // start a transaction and fill the target with the input features
        Transaction t = new DefaultTransaction();
        SimpleFeatureStore fstore = (SimpleFeatureStore) ds.getFeatureSource(targetType
                .getTypeName());
        fstore.setTransaction(t);
        SimpleFeatureIterator fi = features.features();
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(targetType);
        while (fi.hasNext()) {
            SimpleFeature source = fi.next();
            fb.reset();
            for (String sname : mapping.keySet()) {
                fb.set(mapping.get(sname), source.getAttribute(sname));
            }
            SimpleFeature target = fb.buildFeature(null);
            fstore.addFeatures(DataUtilities.collection(target));
        }
        t.commit();
        t.close();

        return targetType;
    }
    
    /**
     * Applies a set of heuristics to find which target attribute corresponds to a certain input attribute
     * 
     * @param sourceType
     * @param targetType
     * @return
     */
    Map<String, String> buildAttributeMapping(SimpleFeatureType sourceType,
            SimpleFeatureType targetType) {
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
                && !"the_geom".equalsIgnoreCase(sourceType.getGeometryDescriptor().getLocalName())) {
            result.put(sourceType.getGeometryDescriptor().getLocalName(), "the_geom");
        }

        // and finally we return with as much as we can match
        if (!sourceNames.isEmpty()) {
            LOGGER.warning("Could not match the following attributes " + sourceNames
                    + " to the target feature type ones: " + targetType);
        }
        return result;
    }
}
/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.data.DataAccessFactoryProducer;
import org.geoserver.data.DataStoreFactoryInitializer;
import org.geoserver.feature.retype.RetypingDataStore;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.data.DataAccess;
import org.geotools.data.DataAccessFactory;
import org.geotools.data.DataAccessFactory.Param;
import org.geotools.data.DataAccessFinder;
import org.geotools.data.DataStore;
import org.geotools.data.util.NullProgressListener;
import org.geotools.feature.NameImpl;
import org.geotools.ows.wms.Layer;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;

/**
 * A collecitno of utilties for dealing with GeotTools DataStore.
 *
 * @author Richard Gould, Refractions Research, Inc.
 * @author $Author: cholmesny $ (last modification)
 * @version $Id$
 */
public abstract class DataStoreUtils {

    /** logger */
    static Logger LOGGER = Logging.getLogger("org.geoserver.data");

    /**
     * Looks up the {@link DataAccess} using the given params, verbatim, and then eventually wraps
     * it into a renaming wrapper so that feature type names are good ones from the wfs point of
     * view (that is, no ":" in the type names)
     */
    public static DataAccess<? extends FeatureType, ? extends Feature> getDataAccess(Map params)
            throws IOException {
        DataAccessFactory factory = aquireFactory(params);
        if (factory == null) {
            return null;
        }

        DataAccess<? extends FeatureType, ? extends Feature> store =
                factory.createDataStore(params);
        if (store == null) {
            return null;
        }

        if (store instanceof DataStore) {
            try {
                String[] names = ((DataStore) store).getTypeNames();
                for (int i = 0; i < names.length; i++) {
                    if (names[i].indexOf(":") >= 0) return new RetypingDataStore((DataStore) store);
                }
            } catch (IOException | RuntimeException e) {
                // in case of exception computing the feature types make sure we clean up the store
                store.dispose();
                throw e;
            }
        }
        return store;
    }

    /**
     * When loading from DTO use the params to locate factory.
     *
     * <p>bleck
     */
    public static DataAccessFactory aquireFactory(Map params) {
        for (Iterator i = getAvailableDataStoreFactories().iterator(); i.hasNext(); ) {
            DataAccessFactory factory = (DataAccessFactory) i.next();
            initializeDataStoreFactory(factory);

            if (factory.canProcess(params)) {
                return factory;
            }
        }

        return null;
    }

    /**
     * After user has selected Description can aquire Factory based on display name.
     *
     * <p>Use factory for:
     *
     * <ul>
     *   <li>List of Params (attrb name, help text)
     *   <li>Checking user's input with factory.canProcess( params )
     * </ul>
     */
    public static DataAccessFactory aquireFactory(String displayName) {
        if (displayName == null) {
            return null;
        }
        for (Iterator i = getAvailableDataStoreFactories().iterator(); i.hasNext(); ) {
            DataAccessFactory factory = (DataAccessFactory) i.next();
            initializeDataStoreFactory(factory);

            if (displayName.equals(factory.getDisplayName())) {
                return factory;
            }

            if (displayName.equals(factory.getClass().toString())) {
                return factory;
            }
        }

        return null;
    }

    /**
     * Initializes a newly created data store factory by processing the {@link
     * DataStoreFactoryInitializer} extension point.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    static DataAccessFactory initializeDataStoreFactory(DataAccessFactory factory) {
        List<DataStoreFactoryInitializer> initializers =
                GeoServerExtensions.extensions(DataStoreFactoryInitializer.class);
        for (DataStoreFactoryInitializer initer : initializers) {
            if (initer.getFactoryClass().isAssignableFrom(factory.getClass())) {
                try {
                    initer.initialize(factory);
                } catch (Throwable t) {
                    final Logger LOGGER2 = Logging.getLogger("org.geoserver.platform");
                    String msg =
                            "Error occured processing extension: " + initer.getClass().getName();
                    LOGGER2.log(Level.WARNING, msg, t);
                }
            }
        }
        return factory;
    }

    /** Utility methods for find param by key */
    public static Param find(Param[] params, String key) {
        for (int i = 0; i < params.length; i++) {
            if (key.equalsIgnoreCase(params[i].key)) {
                return params[i];
            }
        }

        return null;
    }

    /**
     * Returns the descriptions for the available DataStores.
     *
     * <p>Arrrg! Put these in the select box.
     *
     * @return Descriptions for user to choose from
     */
    public static List listDataStoresDescriptions() {
        List list = new ArrayList();

        for (Iterator i = getAvailableDataStoreFactories().iterator(); i.hasNext(); ) {
            DataAccessFactory factory = (DataAccessFactory) i.next();
            initializeDataStoreFactory(factory);

            list.add(factory.getDisplayName());
        }

        return list;
    }

    public static Map defaultParams(String description) {
        return defaultParams(aquireFactory(description));
    }

    public static Map defaultParams(DataAccessFactory factory) {
        Map defaults = new HashMap();
        Param[] params = factory.getParametersInfo();

        for (int i = 0; i < params.length; i++) {
            Param param = params[i];
            String key = param.key;
            String value = null;

            // if (param.required ) {
            if (param.sample != null) {
                // Required params may have nice sample values
                //
                value = param.text(param.sample);
            }

            if (value == null) {
                // or not
                value = "";
            }

            // }
            if (value != null) {
                defaults.put(key, value);
            }
        }

        return defaults;
    }

    /**
     * Convert map to real values based on factory Params.
     *
     * <p>The resulting map should still be checked with factory.acceptsMap( map )
     *
     * @return Map with real values that may be acceptable to Factory
     */
    public static Map toConnectionParams(DataAccessFactory factory, Map params) throws IOException {
        Map map = new HashMap(params.size());

        Param[] info = factory.getParametersInfo();

        // Convert Params into the kind of Map we actually need
        for (Iterator i = params.keySet().iterator(); i.hasNext(); ) {
            String key = (String) i.next();

            Object value = find(info, key).lookUp(params);

            if (value != null) {
                map.put(key, value);
            }
        }

        return map;
    }

    public static Collection<DataAccessFactory> getAvailableDataStoreFactories() {
        List<DataAccessFactory> factories = new ArrayList();
        Iterator<DataAccessFactory> it = DataAccessFinder.getAvailableDataStores();
        while (it.hasNext()) {
            factories.add(it.next());
        }

        for (DataAccessFactoryProducer producer :
                GeoServerExtensions.extensions(DataAccessFactoryProducer.class)) {
            try {
                factories.addAll(producer.getDataStoreFactories());
            } catch (Throwable t) {
                LOGGER.log(
                        Level.WARNING,
                        "Error occured loading data access factories. " + "Ignoring producer",
                        t);
            }
        }

        return factories;
    }

    // A utility method for retreiving supported SRS on WFS-NG resource
    public static List<String> getOtherSRSFromWfsNg(FeatureTypeInfo resourceInfo) {
        // do nothing when
        if (resourceInfo.getStore().getType() == null) return Collections.emptyList();
        else if (!resourceInfo.getStore().getType().equalsIgnoreCase("Web Feature Server (NG)"))
            return Collections.emptyList();
        try {
            // featureType.
            FeatureTypeInfo featureType = resourceInfo;
            Name nativeName = new NameImpl(featureType.getNativeName());

            org.geotools.data.wfs.internal.FeatureTypeInfo info =
                    (org.geotools.data.wfs.internal.FeatureTypeInfo)
                            featureType
                                    .getStore()
                                    .getDataStore(null)
                                    .getFeatureSource(nativeName)
                                    .getInfo();
            // read all identifiers of this CRS into a an comma seperated string
            if (info.getOtherSRS() != null) {
                return info.getOtherSRS();
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }

        return Collections.emptyList();
    }

    // A utility method for retreiving supported SRS on WMSLayerInfo resource
    public static List<String> getOtherSRSFromWMSStore(WMSLayerInfo wmsLayerInfo) {
        try {

            Layer wmsLayer = wmsLayerInfo.getWMSLayer(new NullProgressListener());

            Set<String> supportedSRS = wmsLayer.getSrs();
            // check if there are additional srs available
            // if not return an empty list for legacy behavior
            if (supportedSRS.size() == 1) return Collections.emptyList();
            // int index="EPSG:".length();
            List<String> otherSRS = supportedSRS.stream().collect(Collectors.toList());
            return otherSRS;
        } catch (IOException e) {
            LOGGER.log(
                    Level.SEVERE,
                    "Error while reading other SRS from WMS Layer :" + wmsLayerInfo.getNativeName(),
                    e);
        }
        // default to legacy behavior on failure
        return Collections.emptyList();
    }

    // A utility method for retreiving supported SRS on WMTSLayerInfo resource
    public static List<String> getOtherSRSFromWMTSStore(WMTSLayerInfo wmtsLayerInfo) {
        List<String> otherSRS = Collections.emptyList();
        try {
            Layer wmtsLayer = wmtsLayerInfo.getWMTSLayer(new NullProgressListener());
            Set<String> supportedSRS = wmtsLayer.getSrs();
            // do we have additional srs?
            if (!(supportedSRS.size() == 1))
                otherSRS = supportedSRS.stream().collect(Collectors.toList());
        } catch (IOException e) {
            LOGGER.log(
                    Level.SEVERE,
                    "Error while reading other SRS from WMS Layer :"
                            + wmtsLayerInfo.getNativeName(),
                    e);
        }

        return otherSRS;
    }
}

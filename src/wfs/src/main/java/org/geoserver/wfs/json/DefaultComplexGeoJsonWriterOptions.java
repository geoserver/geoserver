/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.json;

import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.StreamSupport;
import org.geotools.data.DataStoreFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.ComplexType;

/**
 * This class provides the options to encode ComplexFeatures when serving them trough an AppSchema
 * store.
 */
public class DefaultComplexGeoJsonWriterOptions implements ComplexGeoJsonWriterOptions {

    static final Logger LOGGER = Logging.getLogger(DefaultComplexGeoJsonWriterOptions.class);

    private static Class NON_FEATURE_TYPE_PROXY;

    static {
        try {
            NON_FEATURE_TYPE_PROXY =
                    Class.forName("org.geotools.data.complex.config.NonFeatureTypeProxy");
        } catch (ClassNotFoundException e) {
            // might be ok if the app-schema datastore is not around
            if (StreamSupport.stream(
                            Spliterators.spliteratorUnknownSize(
                                    DataStoreFinder.getAllDataStores(), Spliterator.ORDERED),
                            false)
                    .anyMatch(
                            f ->
                                    f != null
                                            && f.getClass()
                                                    .getSimpleName()
                                                    .equals("AppSchemaDataAccessFactory"))) {
                LOGGER.log(
                        Level.FINE,
                        "Could not find NonFeatureTypeProxy yet App-schema is around, probably the class changed name, package or does not exist anymore",
                        e);
            }
            NON_FEATURE_TYPE_PROXY = null;
        }
    }

    @Override
    public boolean canHandle(List<FeatureCollection> features) {
        return true;
    }

    @Override
    public boolean encodeComplexAttributeType() {
        return true;
    }

    @Override
    public boolean encodeNestedFeatureAsProperty(ComplexType complexType) {
        return NON_FEATURE_TYPE_PROXY != null && NON_FEATURE_TYPE_PROXY.isInstance(complexType);
    }
}

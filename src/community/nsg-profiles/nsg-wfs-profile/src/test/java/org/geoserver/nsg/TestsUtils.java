/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.nsg;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.nsg.versioning.TimeVersioning;
import org.geoserver.util.IOUtils;
import org.geotools.data.FeatureSource;
import org.geotools.data.util.NullProgressListener;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.util.Converters;
import org.geotools.util.factory.Hints;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.util.ProgressListener;

public final class TestsUtils {

    public static final ProgressListener NULL_PROGRESS_LISTENER = new NullProgressListener();

    public static final Hints EMPTY_HINTS = new Hints();

    private TestsUtils() {}

    public static String readResource(String resourceName) {
        try (InputStream input = TestsUtils.class.getResourceAsStream(resourceName)) {
            return IOUtils.toString(input);
        } catch (Exception exception) {
            throw new RuntimeException(String.format("Error reading resource '%s'.", resourceName));
        }
    }

    public static void updateFeatureTypeTimeVersioning(
            Catalog catalog,
            String featureTypeName,
            boolean enabled,
            String idProperty,
            String timeProperty) {
        FeatureTypeInfo featureType = catalog.getFeatureTypeByName(featureTypeName);
        if (enabled) {
            TimeVersioning.enable(featureType, idProperty, timeProperty);
        } else {
            TimeVersioning.disable(featureType);
        }
        catalog.save(featureType);
    }

    public static List<SimpleFeature> searchFeatures(Catalog catalog, String featureTypeName) {
        FeatureTypeInfo featureType = catalog.getFeatureTypeByName(featureTypeName);
        if (featureType == null) {
            throw new RuntimeException(
                    String.format("Feature type '%s' not found.", featureTypeName));
        }
        FeatureSource source;
        try {
            source = featureType.getFeatureSource(NULL_PROGRESS_LISTENER, EMPTY_HINTS);
        } catch (Exception exception) {
            throw new RuntimeException(
                    String.format(
                            "Error obtaining feature source of feature type '%s'.",
                            featureTypeName),
                    exception);
        }
        FeatureCollection collection;
        try {
            collection = source.getFeatures();
        } catch (Exception exception) {
            throw new RuntimeException(
                    String.format(
                            "Error obtaining feature collection for feature type '%s'.",
                            featureTypeName),
                    exception);
        }
        try (FeatureIterator iterator = collection.features()) {
            List<SimpleFeature> features = new ArrayList<>();
            while (iterator.hasNext()) {
                SimpleFeature feature = (SimpleFeature) iterator.next();
                features.add(feature);
            }
            return features;
        }
    }

    public static List<SimpleFeature> searchFeatures(
            List<SimpleFeature> features,
            String namePropertyName,
            String timePropertyName,
            String expectedName,
            Date expectedTime,
            int toleranceInSeconds) {
        return features.stream()
                .filter(
                        feature -> {
                            String name =
                                    Converters.convert(
                                            feature.getAttribute(namePropertyName), String.class);
                            if (name == null || !name.equals(expectedName)) {
                                return false;
                            }
                            Date time =
                                    Converters.convert(
                                            feature.getAttribute(timePropertyName), Date.class);
                            return dateEqualWitTolerance(time, expectedTime, toleranceInSeconds);
                        })
                .collect(Collectors.toList());
    }

    private static boolean dateEqualWitTolerance(
            Date time, Date expectedTime, int toleranceInSeconds) {
        if (time == null && expectedTime == null) {
            return true;
        }
        if (time == null || expectedTime == null) {
            return false;
        }
        return time.getTime() <= expectedTime.getTime() + toleranceInSeconds * 1000
                && time.getTime() >= expectedTime.getTime() - toleranceInSeconds * 1000;
    }
}

/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.nsg.versioning;

import java.util.Optional;
import org.geoserver.catalog.FeatureTypeInfo;

public final class TimeVersioning {

    public static final String ENABLED_KEY = "TIME_VERSIONING_ENABLED";
    public static final String NAME_PROPERTY_KEY = "TIME_VERSIONING_NAME_PROPERTY";
    public static final String TIME_PROPERTY_KEY = "TIME_VERSIONING_TIME_PROPERTY";

    public static void enable(
            FeatureTypeInfo featureTypeInfo, String nameProperty, String timeProperty) {
        featureTypeInfo.getMetadata().put(ENABLED_KEY, true);
        featureTypeInfo.getMetadata().put(NAME_PROPERTY_KEY, nameProperty);
        featureTypeInfo.getMetadata().put(TIME_PROPERTY_KEY, timeProperty);
    }

    public static void disable(FeatureTypeInfo featureTypeInfo) {
        featureTypeInfo.getMetadata().put(ENABLED_KEY, false);
        featureTypeInfo.getMetadata().put(NAME_PROPERTY_KEY, null);
        featureTypeInfo.getMetadata().put(TIME_PROPERTY_KEY, null);
    }

    public static boolean isEnabled(FeatureTypeInfo featureTypeInfo) {
        return Optional.ofNullable(featureTypeInfo.getMetadata().get(ENABLED_KEY, Boolean.class))
                .orElse(false);
    }

    public static String getNamePropertyName(FeatureTypeInfo featureTypeInfo) {
        String namePropertyName =
                featureTypeInfo.getMetadata().get(NAME_PROPERTY_KEY, String.class);
        if (namePropertyName == null) {
            throw new RuntimeException("No name property name was provided.");
        }
        return namePropertyName;
    }

    public static String getTimePropertyName(FeatureTypeInfo featureTypeInfo) {
        String timePropertyName =
                featureTypeInfo.getMetadata().get(TIME_PROPERTY_KEY, String.class);
        if (timePropertyName == null) {
            throw new RuntimeException("No time property name was provided.");
        }
        return timePropertyName;
    }

    public static void setEnable(FeatureTypeInfo featureTypeInfo, boolean enable) {
        featureTypeInfo.getMetadata().put(ENABLED_KEY, enable);
    }

    public static void setIdAttribute(FeatureTypeInfo featureTypeInfo, String idAttributeName) {
        featureTypeInfo.getMetadata().put(NAME_PROPERTY_KEY, idAttributeName);
    }

    public static void setTimeAttribute(FeatureTypeInfo featureTypeInfo, String timeAttributeName) {
        featureTypeInfo.getMetadata().put(TIME_PROPERTY_KEY, timeAttributeName);
    }
}

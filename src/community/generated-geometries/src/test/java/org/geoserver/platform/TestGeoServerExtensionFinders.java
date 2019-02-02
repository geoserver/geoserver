/*
 *  (c) 2019 Open Source Geospatial Foundation - all rights reserved
 *  This code is licensed under the GPL 2.0 license, available at the root
 *  application directory.
 */

package org.geoserver.platform;

import org.geoserver.generatedgeometries.GeometryGenerationStrategy;

import java.util.Arrays;
import java.util.List;

import static java.util.Collections.emptyList;

public class TestGeoServerExtensionFinders {

    public static GeoServerExtensionFinder emptyFinder() {
        return new GeoServerExtensionFinder() {
            @Override
            public <T> List<T> find(Class<T> clazz) {
                return emptyList();
            }
        };
    }

    public static GeoServerExtensionFinder finderOf(GeometryGenerationStrategy... strategies) {
        return new GeoServerExtensionFinder() {
            @Override
            public <T> List<T> find(Class<T> clazz) {
                return (List<T>) Arrays.asList(strategies);
            }
        };
    }
}

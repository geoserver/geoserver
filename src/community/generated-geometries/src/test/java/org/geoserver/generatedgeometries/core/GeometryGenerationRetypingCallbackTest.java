/*
 * (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.generatedgeometries.core;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.generatedgeometries.core.longitudelatitude.LongLatGeometryGenerationStrategy;
import org.geotools.data.FeatureSource;
import org.geotools.data.simple.SimpleFeatureSource;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;

public class GeometryGenerationRetypingCallbackTest {

    private static final SimpleFeatureType UNUSED = null;

    private LongLatGeometryGenerationStrategy strategy =
            mock(LongLatGeometryGenerationStrategy.class);
    private GeometryGenerationRetypingCallback callback =
            new GeometryGenerationRetypingCallback(strategy);

    @Test
    public void testThatReturnsSameFeatureTypeWhenCannotBuildIt() {
        // given
        FeatureTypeInfo info = mock(FeatureTypeInfo.class);
        given(strategy.canHandle(info, UNUSED)).willReturn(false);
        SimpleFeatureType featureType = mock(SimpleFeatureType.class);

        // when
        FeatureType builtFeatureType = callback.retypeFeatureType(info, featureType);

        // then
        assertSame(featureType, builtFeatureType);
    }

    @Test
    public void testThatReturnsSameFeatureTypeInCaseOfErrorDuringBuilding() {
        // given
        FeatureTypeInfo info = mock(FeatureTypeInfo.class);
        given(strategy.canHandle(info, UNUSED)).willReturn(true);
        SimpleFeatureType featureType = mock(SimpleFeatureType.class);
        given(strategy.defineGeometryAttributeFor(info, featureType))
                .willThrow(GeneratedGeometryConfigurationException.class);

        // when
        FeatureType builtFeatureType = callback.retypeFeatureType(info, featureType);

        // then
        assertSame(featureType, builtFeatureType);
    }

    @Test
    public void testThatBuildsFeatureType() {
        // given
        FeatureTypeInfo info = mock(FeatureTypeInfo.class);
        given(strategy.canHandle(info, UNUSED)).willReturn(true);
        SimpleFeatureType srcFeatureType = mock(SimpleFeatureType.class);
        SimpleFeatureType expectedFeatureType = mock(SimpleFeatureType.class);
        given(strategy.defineGeometryAttributeFor(info, srcFeatureType))
                .willReturn(expectedFeatureType);

        // when
        FeatureType builtFeatureType = callback.retypeFeatureType(info, srcFeatureType);

        // then
        assertSame(expectedFeatureType, builtFeatureType);
    }

    @Test
    public void testThatReturnsSameFeatureSourceWhenCannotWrapIt() {
        // given
        FeatureTypeInfo info = mock(FeatureTypeInfo.class);
        given(strategy.canHandle(info, UNUSED)).willReturn(false);
        FeatureSource<SimpleFeatureType, SimpleFeature> featureSource = mock(FeatureSource.class);

        // when
        FeatureSource<SimpleFeatureType, SimpleFeature> wrapped =
                callback.wrapFeatureSource(info, featureSource);

        // then
        assertSame(featureSource, wrapped);
    }

    @Test
    public void testThatWrapsFeatureSource() {
        // given
        FeatureTypeInfo info = mock(FeatureTypeInfo.class);
        given(strategy.canHandle(info, UNUSED)).willReturn(true);
        FeatureSource<SimpleFeatureType, SimpleFeature> featureSource =
                mock(SimpleFeatureSource.class);

        // when
        FeatureSource<SimpleFeatureType, SimpleFeature> wrapped =
                callback.wrapFeatureSource(info, featureSource);

        // then
        assertNotSame(featureSource, wrapped);
        assertTrue(wrapped instanceof GeometryGenerationFeatureSource);
    }
}

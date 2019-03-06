/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.generatedgeometries.core;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.io.IOException;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;

public class GeometryGenerationFeatureSourceTest {

    private FeatureTypeInfo featureTypeInfo = mock(FeatureTypeInfo.class);
    private SimpleFeatureSource delegate = mock(SimpleFeatureSource.class);
    private GeometryGenerationStrategy strategy = mock(GeometryGenerationStrategy.class);

    private GeometryGenerationFeatureSource featureSource =
            new GeometryGenerationFeatureSource(featureTypeInfo, delegate, strategy);

    @Test
    public void testThatDefinesSchemaAtFirstUse() {
        // given
        SimpleFeatureType src = mock(SimpleFeatureType.class);
        given(delegate.getSchema()).willReturn(src);
        SimpleFeatureType defined = mock(SimpleFeatureType.class);
        given(strategy.defineGeometryAttributeFor(featureTypeInfo, src)).willReturn(defined);

        // when
        SimpleFeatureType schema = featureSource.getSchema();

        // then
        assertSame(schema, defined);
    }

    @Test
    public void testThatReturnsCachedSchemaForSubsequesntCalls() {
        // given
        SimpleFeatureType src = mock(SimpleFeatureType.class);
        given(delegate.getSchema()).willReturn(src);
        SimpleFeatureType defined = mock(SimpleFeatureType.class);
        given(strategy.defineGeometryAttributeFor(featureTypeInfo, src)).willReturn(defined);
        featureSource.getSchema();

        // when
        SimpleFeatureType schema = featureSource.getSchema();

        // then
        assertSame(schema, defined);
        verify(strategy, times(1)).defineGeometryAttributeFor(featureTypeInfo, src);
    }

    @Test
    public void testThatWrapsFeatureCollection() throws IOException {
        // given
        SimpleFeatureCollection collection = mock(SimpleFeatureCollection.class);
        given(delegate.getFeatures()).willReturn(collection);

        // when
        SimpleFeatureCollection features = featureSource.getFeatures();

        // then
        assertThat(features, CoreMatchers.instanceOf(GeometryGenerationFeatureCollection.class));
    }

    @Test
    public void testThatWrapsFeatureCollectionForQuery() throws IOException {
        // given
        Query srcQuery = mock(Query.class);
        Query query = mock(Query.class);
        given(strategy.convertQuery(featureTypeInfo, srcQuery)).willReturn(query);
        SimpleFeatureCollection collection = mock(SimpleFeatureCollection.class);
        given(delegate.getFeatures(query)).willReturn(collection);

        // when
        SimpleFeatureCollection features = featureSource.getFeatures(srcQuery);

        // then
        assertThat(features, CoreMatchers.instanceOf(GeometryGenerationFeatureCollection.class));
    }

    @Test
    public void testThatWrapsFeatureCollectionForFilter() throws IOException {
        // given
        Filter srcFilter = mock(Filter.class);
        Filter filter = mock(Filter.class);
        given(strategy.convertFilter(featureTypeInfo, srcFilter)).willReturn(filter);
        SimpleFeatureCollection collection = mock(SimpleFeatureCollection.class);
        given(delegate.getFeatures(filter)).willReturn(collection);

        // when
        SimpleFeatureCollection features = featureSource.getFeatures(srcFilter);

        // then
        assertThat(features, CoreMatchers.instanceOf(GeometryGenerationFeatureCollection.class));
    }

    @Test
    public void testThatCountsQueryResults() throws IOException {
        // given
        Query srcQuery = mock(Query.class);
        Query query = mock(Query.class);
        given(strategy.convertQuery(featureTypeInfo, srcQuery)).willReturn(query);
        given(delegate.getCount(query)).willReturn(17);

        // when
        int count = featureSource.getCount(srcQuery);

        // then
        assertThat(count, is(17));
    }
}

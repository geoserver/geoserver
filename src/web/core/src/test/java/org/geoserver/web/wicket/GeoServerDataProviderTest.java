/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Iterator;
import java.util.List;
import org.geotools.api.filter.Filter;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.ows.wms.Layer;
import org.junit.Test;

public class GeoServerDataProviderTest {

    private final GeoServerDataProvider<Layer> geoServerLayerDataProviderMock =
            new GeoServerDataProvider<>() {
                @Override
                protected List<Property<Layer>> getProperties() {
                    return List.of(new BeanProperty<>("title", "title"));
                }

                @Override
                protected List<Layer> getItems() {
                    return List.of(new Layer("key"), new Layer("keyword"));
                }
            };

    @Test
    public void fullTextFilter() {
        geoServerLayerDataProviderMock.setKeywords(new String[] {"keyword"});

        Filter filter = geoServerLayerDataProviderMock.getFilter();

        assertEquals("AnyText ILIKE '*keyword*'", ECQL.toCQL(filter));
    }

    @Test
    public void exactTermFilter() {
        geoServerLayerDataProviderMock.setKeywords(new String[] {"'keyword'"});

        Filter filter = geoServerLayerDataProviderMock.getFilter();

        assertEquals("AnyText ILIKE 'keyword'", ECQL.toCQL(filter));
    }

    @Test
    public void mixedFilter() {
        geoServerLayerDataProviderMock.setKeywords(new String[] {"keyword", "'keyword'"});

        Filter filter = geoServerLayerDataProviderMock.getFilter();

        assertEquals("AnyText ILIKE '*keyword*' OR AnyText ILIKE 'keyword'", ECQL.toCQL(filter));
    }

    @Test
    public void emptyFilter() {
        geoServerLayerDataProviderMock.setKeywords(null);

        Filter filter = geoServerLayerDataProviderMock.getFilter();

        assertEquals(Filter.INCLUDE, filter);
    }

    @Test
    public void mismatchedQuotedExactTermFilter() {
        geoServerLayerDataProviderMock.setKeywords(new String[] {"\"keyword'", "'keyword"});

        Filter filter = geoServerLayerDataProviderMock.getFilter();

        assertEquals(
                "AnyText ILIKE '*\"keyword'*' OR AnyText ILIKE '*'keyword*'", ECQL.toCQL(filter));
    }

    @Test
    public void emptyQuotedExactTermFilter() {
        geoServerLayerDataProviderMock.setKeywords(new String[] {"\"\""});

        Filter filter = geoServerLayerDataProviderMock.getFilter();

        assertEquals("AnyText ILIKE '*\"\"*'", ECQL.toCQL(filter));
    }

    @Test
    public void fullTextRegexFilter() {
        geoServerLayerDataProviderMock.setKeywords(new String[] {"key"});

        Iterator<Layer> iterator = geoServerLayerDataProviderMock.iterator(0, Long.MAX_VALUE);

        assertEquals("key", iterator.next().getTitle());
        assertEquals("keyword", iterator.next().getTitle());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void exactTermRegexFilter() {
        geoServerLayerDataProviderMock.setKeywords(new String[] {"'key'"});

        Iterator<Layer> iterator = geoServerLayerDataProviderMock.iterator(0, Long.MAX_VALUE);

        assertEquals("key", iterator.next().getTitle());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void mixedRegexFilter() {
        geoServerLayerDataProviderMock.setKeywords(new String[] {"key", "'key'"});

        Iterator<Layer> iterator = geoServerLayerDataProviderMock.iterator(0, Long.MAX_VALUE);

        assertEquals("key", iterator.next().getTitle());
        assertEquals("keyword", iterator.next().getTitle());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void emptyRegexFilter() {
        geoServerLayerDataProviderMock.setKeywords(new String[] {});

        Iterator<Layer> iterator = geoServerLayerDataProviderMock.iterator(0, Long.MAX_VALUE);

        assertEquals("key", iterator.next().getTitle());
        assertEquals("keyword", iterator.next().getTitle());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void mismatchedQuotedExactTermRegexFilter() {
        geoServerLayerDataProviderMock.setKeywords(new String[] {"\"keyword'", "'keyword"});

        Iterator<Layer> iterator = geoServerLayerDataProviderMock.iterator(0, Long.MAX_VALUE);

        assertFalse(iterator.hasNext());
    }

    @Test
    public void emptyQuotedExactTermRegexFilter() {
        geoServerLayerDataProviderMock.setKeywords(new String[] {"\"\""});

        Iterator<Layer> iterator = geoServerLayerDataProviderMock.iterator(0, Long.MAX_VALUE);

        assertFalse(iterator.hasNext());
    }
}

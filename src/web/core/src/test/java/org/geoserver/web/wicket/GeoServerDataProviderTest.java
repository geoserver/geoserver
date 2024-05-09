package org.geoserver.web.wicket;

import static org.junit.Assert.*;

import java.util.Iterator;
import java.util.List;
import org.geotools.api.filter.Filter;
import org.geotools.ows.wms.Layer;
import org.junit.Test;

public class GeoServerDataProviderTest {

    private final GeoServerDataProvider<Object> geoServerDataProvider =
            new GeoServerDataProvider<>() {
                @Override
                protected List<Property<Object>> getProperties() {
                    return List.of(new BeanProperty<>("title", "title"));
                }

                @Override
                protected List<Object> getItems() {
                    return List.of(new Layer("key"), new Layer("keyword"));
                }
            };

    @Test
    public void fullTextFilter() {
        geoServerDataProvider.setKeywords(new String[] {"keyword"});

        Filter filter = geoServerDataProvider.getFilter();

        assertEquals("[ AnyText is like *keyword* ]", filter.toString());
    }

    @Test
    public void exactTermFilter() {
        geoServerDataProvider.setKeywords(new String[] {"'keyword'"});

        Filter filter = geoServerDataProvider.getFilter();

        assertEquals("[ AnyText is like keyword ]", filter.toString());
    }

    @Test
    public void mixedFilter() {
        geoServerDataProvider.setKeywords(new String[] {"keyword", "'keyword'"});

        Filter filter = geoServerDataProvider.getFilter();

        assertEquals(
                "[[ AnyText is like *keyword* ] OR [ AnyText is like keyword ]]",
                filter.toString());
    }

    @Test
    public void emptyFilter() {
        geoServerDataProvider.setKeywords(null);

        Filter filter = geoServerDataProvider.getFilter();

        assertEquals(Filter.INCLUDE, filter);
    }

    @Test
    public void mismatchedQuotedExactTermFilter() {
        geoServerDataProvider.setKeywords(new String[] {"\"keyword'", "'keyword"});

        Filter filter = geoServerDataProvider.getFilter();

        assertEquals(
                "[[ AnyText is like *\"keyword'* ] OR [ AnyText is like *'keyword* ]]",
                filter.toString());
    }

    @Test
    public void emptyQuotedExactTermFilter() {
        geoServerDataProvider.setKeywords(new String[] {"\"\""});

        Filter filter = geoServerDataProvider.getFilter();

        assertEquals("[ AnyText is like *\"\"* ]", filter.toString());
    }

    @Test
    public void fullTextRegexFilter() {
        geoServerDataProvider.setKeywords(new String[] {"key"});

        Iterator<Object> iterator = geoServerDataProvider.iterator(0, Long.MAX_VALUE);

        assertEquals("key", iterator.next().toString());
        assertEquals("keyword", iterator.next().toString());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void exactTermRegexFilter() {
        geoServerDataProvider.setKeywords(new String[] {"'key'"});

        Iterator<Object> iterator = geoServerDataProvider.iterator(0, Long.MAX_VALUE);

        assertEquals("key", iterator.next().toString());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void mixedRegexFilter() {
        geoServerDataProvider.setKeywords(new String[] {"key", "'key'"});

        Iterator<Object> iterator = geoServerDataProvider.iterator(0, Long.MAX_VALUE);

        assertEquals("key", iterator.next().toString());
        assertEquals("keyword", iterator.next().toString());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void emptyRegexFilter() {
        geoServerDataProvider.setKeywords(new String[] {});

        Iterator<Object> iterator = geoServerDataProvider.iterator(0, Long.MAX_VALUE);

        assertEquals("key", iterator.next().toString());
        assertEquals("keyword", iterator.next().toString());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void mismatchedQuotedExactTermRegexFilter() {
        geoServerDataProvider.setKeywords(new String[] {"\"keyword'", "'keyword"});

        Iterator<Object> iterator = geoServerDataProvider.iterator(0, Long.MAX_VALUE);

        assertFalse(iterator.hasNext());
    }

    @Test
    public void emptyQuotedExactTermRegexFilter() {
        geoServerDataProvider.setKeywords(new String[] {"\"\""});

        Iterator<Object> iterator = geoServerDataProvider.iterator(0, Long.MAX_VALUE);

        assertFalse(iterator.hasNext());
    }
}

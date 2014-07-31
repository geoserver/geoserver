package org.geoserver.wfs;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.NamespaceInfo;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;

/**
 * Justin Deoliveira, Boundless
 */
public class CatalogNamespaceSupportTest {

    @Test
    public void testGetURI() {
        NamespaceInfo ns = createMock(NamespaceInfo.class);
        expect(ns.getURI()).andReturn("http://foo.org");

        Catalog cat = createMock(Catalog.class);
        expect(cat.getNamespaceByPrefix("foo")).andReturn(ns);

        replay(ns, cat);

        CatalogNamespaceSupport nsSupport = new CatalogNamespaceSupport(cat);
        assertEquals("http://foo.org", nsSupport.getURI("foo"));

        verify(ns, cat);
    }

    @Test
    public void testGetDefaultURI() {
        NamespaceInfo ns = createMock(NamespaceInfo.class);
        expect(ns.getURI()).andReturn("http://foo.org");

        Catalog cat = createMock(Catalog.class);
        expect(cat.getDefaultNamespace()).andReturn(ns);

        replay(ns, cat);

        CatalogNamespaceSupport nsSupport = new CatalogNamespaceSupport(cat);
        assertEquals("http://foo.org", nsSupport.getURI(""));

        verify(ns, cat);
    }
}

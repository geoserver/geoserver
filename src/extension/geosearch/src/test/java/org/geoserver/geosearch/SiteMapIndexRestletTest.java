package org.geoserver.geosearch;

import javax.xml.namespace.QName;

import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.test.GeoServerTestSupport;
import org.w3c.dom.Document;

public class SiteMapIndexRestletTest extends GeoServerTestSupport {

    /**
     * Helper function for all these repetitive tests.  The test sequence verifies that the 'Enable GeoSearch' checkbox applies to the specified endpoint, and that the root element is as expected.
     *
     * @param type the QName for the dataset to test against
     * @param path the request path, everything AFTER the servlet context
     * @param rootElement the expected tag name of the root element of 
     *     the returned XML document
     */
    private void assertProtectedEndpoint (
            QName type,
            String fid,
            String path,
            String rootElement) 
        throws Exception {

        path = String.format(
            path,
            type.getPrefix() + ":" + type.getLocalPart(),
            fid
        );

        assertStatusCodeForGet(403, path);
        FeatureTypeInfo ft = getCatalog().getFeatureTypeByName(
            getCatalog().getNamespace(type.getPrefix()), 
            type.getLocalPart()
        );

        ft.getMetadata().put("indexingEnabled", true);
        getCatalog().save(ft);

        ft = getCatalog().getFeatureTypeByName(
            getCatalog().getNamespace(type.getPrefix()), 
            type.getLocalPart()
        );
        assertEquals(ft.getMetadata().get("indexingEnabled"), true);


        Document d = getAsDOM(path);
        assertEquals(
                "In " + path + ": ", 
                rootElement,
                d.getDocumentElement().getTagName()
                );
    }

    public void testSiteMapExists() throws Exception {
        assertProtectedEndpoint(
            MockData.BASIC_POLYGONS,
            null, // Layer-wide, no FID needed
            "/rest/layers/%1$s/sitemap.xml",
            "sitemapindex"
            );
    }

    public void testGotoKML() throws Exception { 
        assertProtectedEndpoint(
            MockData.BASIC_POLYGONS,
            "BasicPolygons.1107531493630",
            "/rest/layers/%1$s/%2$s_goto.kml",
            "kml"
            );
    }

    public void testFeatureKML() throws Exception {
        assertProtectedEndpoint(
                MockData.BASIC_POLYGONS,
                "BasicPolygons.1107531493630",
                "/rest/layers/%1$s/%2$s.kml",
                "kml"
                );
    }

    public void testPagedLayerSiteMap() throws Exception { 
        assertProtectedEndpoint(
                MockData.BASIC_POLYGONS,
                null,
                "/rest/layers/%1$s/sitemap-1.xml",
                "urlset"
                );
    }

    public void testFeatureHTML() throws Exception {
        assertProtectedEndpoint(
                MockData.BASIC_POLYGONS,
                "BasicPolygons.1107531493630",
                "/rest/layers/%1$s/%2$s.html",
                "html"
                );
    }

    public void testLayerHTML() throws Exception {
        assertProtectedEndpoint(
                MockData.GENERICENTITY,
                null,
                "/rest/layers/%1$s.html",
                "html"
        );
    }
}

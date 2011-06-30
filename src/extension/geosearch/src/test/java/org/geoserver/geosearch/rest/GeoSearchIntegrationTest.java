package org.geoserver.geosearch.rest;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.geoserver.data.test.MockData.BASIC_POLYGONS;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import junit.framework.Test;

import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.test.GeoServerTestSupport;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class GeoSearchIntegrationTest extends GeoServerTestSupport {

    static QName[] indexed = { MockData.BASIC_POLYGONS, MockData.BRIDGES };

    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new GeoSearchIntegrationTest());
    }

    @Override
    protected void oneTimeSetUp() throws Exception {
        super.oneTimeSetUp();
        Catalog catalog = getCatalog();
        for (QName name : indexed) {
            String namespaceURI = name.getNamespaceURI();
            String localPart = name.getLocalPart();
            FeatureTypeInfo typeInfo = catalog.getFeatureTypeByName(namespaceURI, localPart);
            LayerInfo layer = catalog.getLayerByName(typeInfo.getPrefixedName());
            layer.getMetadata().put(Properties.INDEXING_ENABLED, Boolean.TRUE);
            catalog.save(layer);
        }

        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("atom", "http://www.w3.org/2005/Atom");
        namespaces.put("geo", "http://www.google.com/geo/schemas/sitemap/1.0");
        namespaces.put("sm", "http://www.sitemaps.org/schemas/sitemap/0.9");
        namespaces.put("kml", "http://www.opengis.net/kml/2.2");
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
    }

    public void testSiteMap() throws Exception {
        Document dom = getAsDOM("/geosearch/sitemap.xml");
        // print(dom);
        assertXpathExists("/sm:urlset", dom);
        assertXpathEvaluatesTo(String.valueOf(indexed.length), "count(/sm:urlset/sm:url/sm:loc)",
                dom);
        assertXpathEvaluatesTo(String.valueOf(indexed.length),
                "count(/sm:urlset/sm:url/geo:geo/geo:format)", dom);

        assertXpathEvaluatesTo("kml", "/sm:urlset/sm:url/geo:geo/geo:format", dom);
    }

    public void testKmlUrls() throws Exception {
        Document sitemap = getAsDOM("/geosearch/sitemap.xml");
        // print(sitemap);

        Set<String> expected = new HashSet<String>();
        expected.add("http://localhost/geoserver/geosearch/cite%3ABasicPolygons.kml");
        expected.add("http://localhost/geoserver/geosearch/cite%3ABridges.kml");

        XpathEngine xp = XMLUnit.newXpathEngine();
        String kmlUrl1 = xp.evaluate("/sm:urlset/sm:url[1]/sm:loc", sitemap);
        String kmlUrl2 = xp.evaluate("/sm:urlset/sm:url[2]/sm:loc", sitemap);
        Set<String> actual = new HashSet<String>();
        actual.add(kmlUrl1);
        actual.add(kmlUrl2);

        assertEquals(expected, actual);
    }

    public void testKml() throws Exception {

        Document kml = getAsDOM("/geosearch/cite%3ABasicPolygons.kml");
        print(kml);

        FeatureTypeInfo ft = getCatalog().getFeatureTypeByName(BASIC_POLYGONS.getNamespaceURI(),
                BASIC_POLYGONS.getLocalPart());

        assertXpathEvaluatesTo(ft.getTitle(), "/kml:kml/kml:Document/kml:name", kml);

        GeoServerInfo global = getGeoServer().getGlobal();

        assertXpathEvaluatesTo(global.getContact().getContactPerson(),
                "/kml:kml/kml:Document/atom:author", kml);

        assertXpathEvaluatesTo(global.getOnlineResource(), "/kml:kml/kml:Document/atom:link/@href",
                kml);

        assertXpathExists("/kml:kml/kml:Document/kml:description", kml);

        assertXpathEvaluatesTo("cite:BasicPolygons",
                "/kml:kml/kml:Document/kml:NetworkLink/kml:name", kml);
    }

    public void testKmlResponseHeaders() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("/geosearch/cite%3ABasicPolygons.kml");
        assertEquals(200, response.getStatusCode());
        assertEquals("application/vnd.google-earth.kml+xml", response.getContentType());
    }
}

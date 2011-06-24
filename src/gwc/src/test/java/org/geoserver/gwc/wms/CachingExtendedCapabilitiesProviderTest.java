package org.geoserver.gwc.wms;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import junit.framework.Test;

import org.geoserver.config.GeoServerInfo;
import org.geoserver.test.GeoServerTestSupport;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;

public class CachingExtendedCapabilitiesProviderTest extends GeoServerTestSupport {

    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new CachingExtendedCapabilitiesProviderTest());
    }

    @Override
    protected void oneTimeSetUp() throws Exception {
        super.oneTimeSetUp();
        GeoServerInfo global = getGeoServer().getGlobal();
        global.setProxyBaseUrl("../wms/src/test/resources/geoserver");
        getGeoServer().save(global);
    }

    public void testCapabilitiesContributedInternalDTD() throws Exception {
        Document dom = dom(get("wms?request=getCapabilities&version=1.1.1&tiled=true"), false);
        DocumentType doctype = dom.getDoctype();
        assertNotNull(doctype);

        assertEquals("WMT_MS_Capabilities", doctype.getName());

        String systemId = doctype.getSystemId();
        assertEquals(
                "../wms/src/test/resources/geoserver/schemas/wms/1.1.1/WMS_MS_Capabilities.dtd",
                systemId);

        String internalSubset = doctype.getInternalSubset();
        assertNotNull(internalSubset);
        assertTrue(internalSubset,
                internalSubset.trim().startsWith("<!ELEMENT VendorSpecificCapabilities"));
        assertTrue(internalSubset, internalSubset.contains("(TileSet*)"));
        assertTrue(
                internalSubset,
                internalSubset
                        .contains("<!ELEMENT TileSet (SRS,BoundingBox?,Resolutions,Width,Height,Format,Layers*,Styles*)>"));
        assertTrue(internalSubset, internalSubset.contains("<!ELEMENT Resolutions (#PCDATA)>"));
        assertTrue(internalSubset, internalSubset.contains("<!ELEMENT Width (#PCDATA)>"));
        assertTrue(internalSubset, internalSubset.contains("<!ELEMENT Height (#PCDATA)>"));
        assertTrue(internalSubset, internalSubset.contains("<!ELEMENT Layers (#PCDATA)>"));
        assertTrue(internalSubset, internalSubset.contains("<!ELEMENT Styles (#PCDATA)>"));
        System.out.println("so?");
    }

    public void testTileSets() throws Exception {
        final int numLayers = getCatalog().getLayers().size();
        final int numCRSs = 2; // 4326 and 900913
        final int numFormats = 2; // png, jpeg
        final int numTileSets = numLayers * numCRSs * numFormats;

        Document dom = dom(get("wms?request=getCapabilities&version=1.1.1&tiled=true"), false);
        // print(dom);

        String tileSetPath = "/WMT_MS_Capabilities/Capability/VendorSpecificCapabilities/TileSet";
        assertXpathEvaluatesTo(String.valueOf(numTileSets), "count(" + tileSetPath + ")", dom);

        assertXpathExists(tileSetPath + "[1]/SRS", dom);
        assertXpathExists(tileSetPath + "[1]/BoundingBox", dom);
        assertXpathExists(tileSetPath + "[1]/Resolutions", dom);
        assertXpathExists(tileSetPath + "[1]/Width", dom);
        assertXpathExists(tileSetPath + "[1]/Height", dom);
        assertXpathExists(tileSetPath + "[1]/Format", dom);
        assertXpathExists(tileSetPath + "[1]/Layers", dom);
        assertXpathExists(tileSetPath + "[1]/Styles", dom);
    }

}

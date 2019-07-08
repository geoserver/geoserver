/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wms;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathNotExists;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.layer.CatalogConfiguration;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;

public class CachingExtendedCapabilitiesProviderTest extends GeoServerSystemTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        GeoServerInfo global = getGeoServer().getGlobal();
        global.getSettings().setProxyBaseUrl("../wms/src/test/resources/geoserver");
        getGeoServer().save(global);
    }

    @Test
    public void testCapabilitiesContributedInternalDTD() throws Exception {

        GWC.get().getConfig().setDirectWMSIntegrationEnabled(false);

        Document dom = dom(get("wms?request=getCapabilities&version=1.1.1&tiled=true"), false);
        DocumentType doctype = dom.getDoctype();
        assertNotNull(doctype);

        assertEquals("WMT_MS_Capabilities", doctype.getName());

        String systemId = doctype.getSystemId();
        assertEquals(
                "../wms/src/test/resources/geoserver/schemas/wms/1.1.1/WMS_MS_Capabilities.dtd",
                systemId);

        String internalSubset = doctype.getInternalSubset();
        assertTrue(internalSubset == null || !internalSubset.contains("TileSet"));

        GWC.get().getConfig().setDirectWMSIntegrationEnabled(true);
        dom = dom(get("wms?request=getCapabilities&version=1.1.1&tiled=true"), false);
        doctype = dom.getDoctype();
        assertNotNull(doctype);
        assertEquals("WMT_MS_Capabilities", doctype.getName());
        systemId = doctype.getSystemId();

        assertEquals(
                "../wms/src/test/resources/geoserver/schemas/wms/1.1.1/WMS_MS_Capabilities.dtd",
                systemId);

        internalSubset = doctype.getInternalSubset();

        assertNotNull(internalSubset);
        assertTrue(
                internalSubset,
                internalSubset.trim().startsWith("<!ELEMENT VendorSpecificCapabilities"));
        assertTrue(internalSubset, internalSubset.contains("(TileSet*)"));
        assertTrue(
                internalSubset,
                internalSubset.contains(
                        "<!ELEMENT TileSet (SRS,BoundingBox?,Resolutions,Width,Height,Format,Layers*,Styles*)>"));
        assertTrue(internalSubset, internalSubset.contains("<!ELEMENT Resolutions (#PCDATA)>"));
        assertTrue(internalSubset, internalSubset.contains("<!ELEMENT Width (#PCDATA)>"));
        assertTrue(internalSubset, internalSubset.contains("<!ELEMENT Height (#PCDATA)>"));
        assertTrue(internalSubset, internalSubset.contains("<!ELEMENT Layers (#PCDATA)>"));
        assertTrue(internalSubset, internalSubset.contains("<!ELEMENT Styles (#PCDATA)>"));
    }

    @Test
    public void testTileSets() throws Exception {
        final int numLayers;
        {
            int validLayers = 0;
            List<LayerInfo> layers = getCatalog().getLayers();
            for (LayerInfo l : layers) {
                if (CatalogConfiguration.isLayerExposable(l)) {
                    ++validLayers;
                }
            }
            numLayers = validLayers;
        }
        final int numCRSs = 2; // 4326 and 900913
        final int numFormats = 2; // png, jpeg
        final int numTileSets = numLayers * numCRSs * numFormats;

        String tileSetPath = "/WMT_MS_Capabilities/Capability/VendorSpecificCapabilities/TileSet";

        GWC.get().getConfig().setDirectWMSIntegrationEnabled(false);
        Document dom = dom(get("wms?request=getCapabilities&version=1.1.1&tiled=true"), false);

        assertXpathNotExists(tileSetPath, dom);

        GWC.get().getConfig().setDirectWMSIntegrationEnabled(true);
        dom = dom(get("wms?request=getCapabilities&version=1.1.1&tiled=true"), false);

        assertXpathExists(tileSetPath, dom);

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

    @Test
    public void testLocalWorkspaceIntegration() throws Exception {

        final String tileSetPath =
                "//WMT_MS_Capabilities/Capability/VendorSpecificCapabilities/TileSet";
        final String localName = MockData.BASIC_POLYGONS.getLocalPart();
        final String qualifiedName = super.getLayerId(MockData.BASIC_POLYGONS);

        Document dom;

        dom = dom(get("wms?request=getCapabilities&version=1.1.1&tiled=true"), false);
        assertXpathExists(tileSetPath + "/Layers[text() = '" + qualifiedName + "']", dom);

        dom = dom(get("cite/wms?request=getCapabilities&version=1.1.1&tiled=true"), false);
        assertXpathExists(tileSetPath + "/Layers[text() = '" + localName + "']", dom);
    }
}

/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_3;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;

import java.util.List;
import java.util.Map;
import org.geoserver.catalog.AuthorityURLInfo;
import org.geoserver.catalog.LayerIdentifierInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.impl.AuthorityURL;
import org.geoserver.catalog.impl.LayerIdentifier;
import org.geoserver.data.test.MockData;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSTestSupport;
import org.junit.Test;
import org.w3c.dom.Document;

public class CapabilitiesAuthorityURLAndIdentifierTest extends WMSTestSupport {

    private void addAuthUrl(final String name, final String url, List<AuthorityURLInfo> target) {
        AuthorityURLInfo auth = new AuthorityURL();
        auth.setName(name);
        auth.setHref(url);
        target.add(auth);
    }

    private void addIdentifier(
            final String authName, final String id, List<LayerIdentifierInfo> target) {
        LayerIdentifierInfo identifier = new LayerIdentifier();
        identifier.setAuthority(authName);
        identifier.setIdentifier(id);
        target.add(identifier);
    }

    @Override
    protected void registerNamespaces(Map<String, String> namespaces) {
        namespaces.put("wms", "http://www.opengis.net/wms");
        namespaces.put("ows", "http://www.opengis.net/ows");
    }

    @Test
    public void testRootLayer() throws Exception {
        WMSInfo serviceInfo = getWMS().getServiceInfo();
        addAuthUrl("rootAuth1", "http://geoserver/wms/auth1", serviceInfo.getAuthorityURLs());
        addAuthUrl("rootAuth2", "http://geoserver/wms/auth2", serviceInfo.getAuthorityURLs());
        addIdentifier("rootAuth1", "rootId1", serviceInfo.getIdentifiers());
        addIdentifier("rootAuth2", "rootId2", serviceInfo.getIdentifiers());
        getGeoServer().save(serviceInfo);

        Document doc = getAsDOM("/wms?service=WMS&request=getCapabilities&version=1.3.0", true);

        assertXpathExists(
                "/wms:WMS_Capabilities/wms:Capability/wms:Layer/wms:AuthorityURL[@name = 'rootAuth1']",
                doc);
        assertXpathEvaluatesTo(
                "http://geoserver/wms/auth1",
                "/wms:WMS_Capabilities/wms:Capability/wms:Layer/wms:AuthorityURL[@name = 'rootAuth1']/wms:OnlineResource/@xlink:href",
                doc);

        assertXpathExists(
                "/wms:WMS_Capabilities/wms:Capability/wms:Layer/wms:AuthorityURL[@name = 'rootAuth2']",
                doc);
        assertXpathEvaluatesTo(
                "http://geoserver/wms/auth2",
                "/wms:WMS_Capabilities/wms:Capability/wms:Layer/wms:AuthorityURL[@name = 'rootAuth2']/wms:OnlineResource/@xlink:href",
                doc);

        assertXpathExists(
                "/wms:WMS_Capabilities/wms:Capability/wms:Layer/wms:Identifier[@authority = 'rootAuth1']",
                doc);
        assertXpathEvaluatesTo(
                "rootId1",
                "/wms:WMS_Capabilities/wms:Capability/wms:Layer/wms:Identifier[@authority = 'rootAuth1']",
                doc);

        assertXpathExists(
                "/wms:WMS_Capabilities/wms:Capability/wms:Layer/wms:Identifier[@authority = 'rootAuth2']",
                doc);
        assertXpathEvaluatesTo(
                "rootId2",
                "/wms:WMS_Capabilities/wms:Capability/wms:Layer/wms:Identifier[@authority = 'rootAuth2']",
                doc);
    }

    @Test
    public void testLayer() throws Exception {

        String layerId = getLayerId(MockData.PRIMITIVEGEOFEATURE);
        LayerInfo layer = getCatalog().getLayerByName(layerId);
        addAuthUrl("layerAuth1", "http://geoserver/wms/auth1", layer.getAuthorityURLs());
        addIdentifier("layerAuth1", "layerId1", layer.getIdentifiers());
        getCatalog().save(layer);

        Document doc =
                getAsDOM(
                        "sf/PrimitiveGeoFeature/wms?service=WMS&request=getCapabilities&version=1.3.0",
                        true);

        String layerName = MockData.PRIMITIVEGEOFEATURE.getLocalPart();
        assertXpathExists(
                "//wms:Layer[wms:Name='" + layerName + "']/wms:AuthorityURL[@name = 'layerAuth1']",
                doc);
        assertXpathEvaluatesTo(
                "http://geoserver/wms/auth1",
                "//wms:Layer[wms:Name='"
                        + layerName
                        + "']/wms:AuthorityURL[@name = 'layerAuth1']/wms:OnlineResource/@xlink:href",
                doc);

        assertXpathExists(
                "//wms:Layer[wms:Name='"
                        + layerName
                        + "']/wms:Identifier[@authority = 'layerAuth1']",
                doc);
        assertXpathEvaluatesTo(
                "layerId1",
                "//wms:Layer[wms:Name='"
                        + layerName
                        + "']/wms:Identifier[@authority = 'layerAuth1']",
                doc);
    }
}

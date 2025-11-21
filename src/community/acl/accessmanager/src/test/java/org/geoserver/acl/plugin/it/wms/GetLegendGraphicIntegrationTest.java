/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * Original from GeoServer 2.24-SNAPSHOT under GPL 2.0 license
 */
package org.geoserver.acl.plugin.it.wms;

import static org.geoserver.acl.domain.rules.CatalogMode.HIDE;
import static org.geoserver.acl.domain.rules.GrantType.ALLOW;
import static org.geoserver.acl.domain.rules.LayerDetails.LayerType.VECTOR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;
import org.geoserver.acl.domain.rules.Rule;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.data.test.MockData;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

@SuppressWarnings("unused")
public class GetLegendGraphicIntegrationTest extends AbstractAclWMSIntegrationTest {

    @Test
    public void testLegendGraphicNestedGroups() throws Exception {
        Rule r1 = support.addRule(1, ALLOW, null, null, null, null, null, null);

        addLakesPlacesLayerGroup(LayerGroupInfo.Mode.SINGLE, "nested");
        addLakesPlacesLayerGroup(LayerGroupInfo.Mode.OPAQUE_CONTAINER, "container");

        LayerGroupInfo group = getRawCatalog().getLayerGroupByName("container");
        LayerGroupInfo nested = getRawCatalog().getLayerGroupByName("nested");
        group.getLayers().add(nested);
        group.getStyles().add(null);
        getRawCatalog().save(group);

        login("anonymousUser", "", "ROLE_ANONYMOUS");
        String url = "wms?service=WMS&version=1.1.1&request=GetLegendGraphic"
                + "&layer="
                + group.getName()
                + "&style="
                + "&format=image/png&width=20&height=20";
        MockHttpServletResponse response = getAsServletResponse(url);
        MediaType actual = MediaType.parseMediaType(response.getContentType());
        assertTrue(MediaType.IMAGE_PNG.isCompatibleWith(actual));
    }

    @Test
    public void testLegendGraphicLayerGroupStyle() throws Exception {
        final String disallowedStyleName = "forests_style";
        LayerGroupInfo group;
        {
            final String layerGroupName = "lakes_and_places_legend";
            addLakesPlacesLayerGroup(LayerGroupInfo.Mode.SINGLE, layerGroupName);

            final Catalog rawCatalog = getRawCatalog();
            group = rawCatalog.getLayerGroupByName(layerGroupName);

            // not among the allowed styles, adding it to a layergroup style
            StyleInfo polygonStyle = rawCatalog.getStyleByName("polygon");
            LayerInfo forest = rawCatalog.getLayerByName(getLayerId(MockData.FORESTS));
            forest.getStyles().add(polygonStyle);
            rawCatalog.save(forest);

            addLayerGroupStyle(group, disallowedStyleName, List.of(forest), List.of(polygonStyle));

            Rule r1 = support.addRule(1, ALLOW, null, "ROLE_ANONYMOUS", "WMS", null, "cite", "Forests");
            Rule r2 = support.addRule(2, ALLOW, null, null, null, null, null, null);

            final Set<String> allowedStyles = Set.of("Lakes", "NamedPlaces");
            support.setLayerDetails(r1, allowedStyles, Set.of(), HIDE, null, null, VECTOR);
        }

        login("anonymousUser", "", "ROLE_ANONYMOUS");

        final String urlFormat = "wms?service=WMS&version=1.1.1&request=GetLegendGraphic&layer="
                + group.getName()
                + "&style=%s"
                + "&format=image/png&width=20&height=20";

        String url = urlFormat.formatted(""); // default style
        MockHttpServletResponse response = getAsServletResponse(url);
        // default lg style should not fail
        MediaType actual = MediaType.parseMediaType(response.getContentType());
        assertTrue(MediaType.IMAGE_PNG.isCompatibleWith(actual));

        url = "wms?service=WMS&version=1.1.1&request=GetLegendGraphic"
                + "&layer="
                + group.getName()
                + "&style="
                + disallowedStyleName
                + "&format=image/png&width=20&height=20";
        response = getAsServletResponse(url);
        // should fail the forests_style contains the not allowed polygon style
        MediaType expected = MediaType.parseMediaType("application/vnd.ogc.se_xml");
        actual = MediaType.parseMediaType(response.getContentType());
        assertEquals(expected.getType(), actual.getType());
        assertEquals(expected.getSubtype(), actual.getSubtype());
        assertTrue(response.getContentAsString().contains("style is not available on this layer"));
    }
}

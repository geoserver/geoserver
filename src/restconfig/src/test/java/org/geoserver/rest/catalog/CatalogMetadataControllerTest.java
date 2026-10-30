/* (c) 2026 Open Source Geospatial Foundation
 * This code is licensed under the GPL 2.0 license.
 */
package org.geoserver.rest.catalog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.VectorAccessLimits;
import org.geoserver.security.WrapperPolicy;
import org.geoserver.security.decorators.SecuredFeatureTypeInfo;
import org.geoserver.security.decorators.SecuredLayerInfo;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.data.DataUtilities;
import org.geotools.factory.CommonFactoryFinder;
import org.junit.After;
import org.junit.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public class CatalogMetadataControllerTest {

    @After
    public void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void testRestrictedAttributesAreOmitted() throws Exception {

        // Simulate a published feature type with three attributes.
        SimpleFeatureType fullSchema =
                DataUtilities.createType("Bridges", "id:Integer,public_name:String,private_note:String");

        assertNotNull(fullSchema.getDescriptor("private_note"));

        FeatureTypeInfo rawFeatureType = mock(FeatureTypeInfo.class);
        NamespaceInfo namespace = mock(NamespaceInfo.class);

        when(namespace.getPrefix()).thenReturn("cite");
        when(rawFeatureType.getNamespace()).thenReturn(namespace);
        when(rawFeatureType.getFeatureType()).thenReturn(fullSchema);

        LayerInfo rawLayer = mock(LayerInfo.class);

        when(rawLayer.getName()).thenReturn("Bridges");

        // Only id and public_name are readable.
        FilterFactory filterFactory = CommonFactoryFinder.getFilterFactory();

        VectorAccessLimits limits = new VectorAccessLimits(
                CatalogMode.HIDE,
                List.of(filterFactory.property("id"), filterFactory.property("public_name")),
                Filter.INCLUDE,
                null,
                Filter.INCLUDE);

        // Wrap the feature type using GeoServer's real security decorator.
        FeatureTypeInfo securedFeatureType =
                new SecuredFeatureTypeInfo(rawFeatureType, WrapperPolicy.readOnlyHide(limits));

        // The mock layer now returns the security-filtered feature type.
        when(rawLayer.getResource()).thenReturn(securedFeatureType);

        // The controller receives a catalog containing our test layer.
        Catalog catalog = mock(Catalog.class);

        when(catalog.getLayers()).thenReturn(List.of(rawLayer));
        // Authenticate as an authorized metadata reader.
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "metadatareader", "ignored", List.of(new SimpleGrantedAuthority("ROLE_METADATA_READER")));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        CatalogMetadataController controller = new CatalogMetadataController(catalog);

        // Request metadata through the actual controller.
        Map<String, Object> response = controller.featureType("cite", "Bridges");

        // Extract the returned attribute names.
        Object attributesObject = response.get("attributes");

        assertTrue(attributesObject instanceof List<?>);

        List<?> attributes = (List<?>) attributesObject;

        List<String> attributeNames = attributes.stream()
                .map(value -> (Map<?, ?>) value)
                .map(value -> (String) value.get("name"))
                .toList();

        // The permitted attributes must be visible.
        assertTrue(attributeNames.contains("id"));
        assertTrue(attributeNames.contains("public_name"));

        // The restricted attribute must not be disclosed.
        assertFalse(attributeNames.contains("private_note"));

        // Verify no other attributes were returned.
        assertEquals(List.of("id", "public_name"), attributeNames);
    }

    @Test
    public void testWorkspaceDiscoveryUsesReadableLayers() throws Exception {

        Catalog catalog = mock(Catalog.class);

        // A published, readable layer in the topp workspace.
        NamespaceInfo namespace = mock(NamespaceInfo.class);
        when(namespace.getPrefix()).thenReturn("topp");

        FeatureTypeInfo resource = mock(FeatureTypeInfo.class);
        when(resource.getNamespace()).thenReturn(namespace);

        LayerInfo visibleLayer = mock(LayerInfo.class);
        when(visibleLayer.getName()).thenReturn("states");
        when(visibleLayer.getResource()).thenReturn(resource);

        // A second layer that permits metadata visibility, but not READ.
        LayerInfo metadataOnlyLayer = new SecuredLayerInfo(mock(LayerInfo.class), WrapperPolicy.metadata(null));

        when(catalog.getLayers()).thenReturn(List.of(visibleLayer, metadataOnlyLayer));

        // Simulate the empty workspace listing we observed over HTTP.
        when(catalog.getWorkspaces()).thenReturn(List.of());

        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        "metadatareader", "ignored", List.of(new SimpleGrantedAuthority("ROLE_METADATA_READER"))));

        CatalogMetadataController controller = new CatalogMetadataController(catalog);

        assertEquals(List.of("topp"), controller.workspaces().get("workspaces"));

        assertEquals(List.of("topp:states"), controller.layers().get("layers"));
    }
}

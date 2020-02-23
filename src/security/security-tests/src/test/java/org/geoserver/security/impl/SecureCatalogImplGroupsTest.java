/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.*;

import java.io.StringReader;
import java.util.Arrays;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerGroupInfo.Mode;
import org.geoserver.ows.Dispatcher;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.security.ResourceAccessManager;
import org.geoserver.security.SecureCatalogImpl;
import org.junit.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * A variant to {@link SecureCatalogImplTest} allowing a per test setup of group nesting and rules
 *
 * @author Andrea Aime - GeoSolutions
 */
public class SecureCatalogImplGroupsTest extends AbstractAuthorizationTest {

    private static final String NAMED_GROUP_NAME = "named";

    private static final String OPAQUE_GROUP_NAME = "opaque";

    private static final String SINGLE_GROUP_NAME = "single";

    private static final String NESTED_GROUP_NAME = "nested";

    private static final String[] DEFAULT_RULES = new String[] {"*.*.r=*", "*.*.w=*"};

    @Override
    public void setUp() throws Exception {
        super.setUp();

        SecurityContextHolder.getContext().setAuthentication(null);
        Dispatcher.REQUEST.remove();
    }

    protected ResourceAccessManager buildManager(String... theRules) throws Exception {
        Properties props = new Properties();
        props.load(new StringReader(Stream.of(theRules).collect(Collectors.joining("\n"))));
        DefaultResourceAccessManager manager =
                new DefaultResourceAccessManager(
                        new MemoryDataAccessRuleDAO(catalog, props), catalog);

        sc =
                new SecureCatalogImpl(catalog, manager) {

                    @Override
                    protected boolean isAdmin(Authentication authentication) {
                        return false;
                    }
                };
        GeoServerExtensionsHelper.singleton("secureCatalog", sc, SecureCatalogImpl.class);

        return manager;
    }

    private LayerGroupInfo prepareStandaloneOpaqueGroup() throws Exception {
        // setup group
        LayerGroupInfo opaque =
                buildLayerGroup(
                        OPAQUE_GROUP_NAME, Mode.OPAQUE_CONTAINER, null, statesLayer, roadsLayer);
        layerGroups = Arrays.asList(opaque);
        populateCatalog();

        // setup security
        buildManager(DEFAULT_RULES);
        SecurityContextHolder.getContext().setAuthentication(roUser);
        return opaque;
    }

    @Test
    public void testWmsStandaloneOpaqueGroup() throws Exception {
        setupRequestThreadLocal("WMS");
        LayerGroupInfo opaque = prepareStandaloneOpaqueGroup();

        // direct access to layers not allowed
        assertNull(sc.getLayerByName(statesLayer.prefixedName()));
        assertNull(sc.getLayerByName(roadsLayer.prefixedName()));

        // however we can access the group and the layers through it
        LayerGroupInfo securedGroup = sc.getLayerGroupByName(opaque.prefixedName());
        assertNotNull(securedGroup);
        assertEquals(2, securedGroup.getLayers().size());
    }

    @Test
    public void testWfsStandaloneOpaqueGroup() throws Exception {
        setupRequestThreadLocal("WFS");
        LayerGroupInfo opaque = prepareStandaloneOpaqueGroup();

        // direct access to layers is allowed in this case
        assertNotNull(sc.getLayerByName(statesLayer.prefixedName()));
        assertNotNull(sc.getLayerByName(roadsLayer.prefixedName()));

        // and we can access the group and the layers through it
        LayerGroupInfo securedGroup = sc.getLayerGroupByName(opaque.prefixedName());
        assertNotNull(securedGroup);
        assertEquals(2, securedGroup.getLayers().size());
    }

    private LayerGroupInfo prepareNamedAndOpaqueGroup() throws Exception {
        // setup group
        LayerGroupInfo opaque =
                buildLayerGroup(
                        OPAQUE_GROUP_NAME, Mode.OPAQUE_CONTAINER, null, statesLayer, roadsLayer);
        LayerGroupInfo named =
                buildLayerGroup(NAMED_GROUP_NAME, Mode.NAMED, null, statesLayer, roadsLayer);
        layerGroups = Arrays.asList(named, opaque);
        populateCatalog();

        // setup security
        buildManager(DEFAULT_RULES);
        SecurityContextHolder.getContext().setAuthentication(roUser);
        return opaque;
    }

    @Test
    public void testWmsNamedOpaqueGroup() throws Exception {
        setupRequestThreadLocal("WMS");
        LayerGroupInfo opaque = prepareNamedAndOpaqueGroup();

        // direct access to layers allowed because of the named group
        assertNotNull(sc.getLayerByName(statesLayer.prefixedName()));
        assertNotNull(sc.getLayerByName(roadsLayer.prefixedName()));

        // we can access the group and the layers through it
        LayerGroupInfo securedGroup = sc.getLayerGroupByName(opaque.prefixedName());
        assertNotNull(securedGroup);
        assertEquals(2, securedGroup.getLayers().size());
    }

    @Test
    public void testWfsNamedOpaqueGroup() throws Exception {
        setupRequestThreadLocal("WFS");
        LayerGroupInfo opaque = prepareNamedAndOpaqueGroup();

        // direct access to layers allowed
        assertNotNull(sc.getLayerByName(statesLayer.prefixedName()));
        assertNotNull(sc.getLayerByName(roadsLayer.prefixedName()));

        // and we can access the group and the layers through it
        LayerGroupInfo securedGroup = sc.getLayerGroupByName(opaque.prefixedName());
        assertNotNull(securedGroup);
        assertEquals(2, securedGroup.getLayers().size());
    }

    @Test
    public void testWmsSingleAndOpaqueGroup() throws Exception {
        setupRequestThreadLocal("WMS");
        // setup groups
        LayerGroupInfo opaque =
                buildLayerGroup(
                        OPAQUE_GROUP_NAME, Mode.OPAQUE_CONTAINER, null, statesLayer, roadsLayer);
        LayerGroupInfo single =
                buildLayerGroup(SINGLE_GROUP_NAME, Mode.SINGLE, null, statesLayer, roadsLayer);
        layerGroups = Arrays.asList(single, opaque);
        populateCatalog();

        // setup security
        buildManager(DEFAULT_RULES);
        SecurityContextHolder.getContext().setAuthentication(roUser);

        // direct access to layers not allowed because the only container is in opaque mode
        assertNull(sc.getLayerByName(statesLayer.prefixedName()));
        assertNull(sc.getLayerByName(roadsLayer.prefixedName()));

        // we can access the group and the layers through it
        LayerGroupInfo opaqueSecuredGroup = sc.getLayerGroupByName(opaque.prefixedName());
        assertNotNull(opaqueSecuredGroup);
        assertEquals(2, opaqueSecuredGroup.getLayers().size());

        LayerGroupInfo securedSingleGroup = sc.getLayerGroupByName(single.prefixedName());
        assertNotNull(securedSingleGroup);
        assertEquals(2, securedSingleGroup.getLayers().size());
    }

    @Test
    public void testWmsMilitaryNamedAndPublicOpaqueGroup() throws Exception {
        setupRequestThreadLocal("WMS");
        // setup groups
        LayerGroupInfo opaque =
                buildLayerGroup(
                        OPAQUE_GROUP_NAME, Mode.OPAQUE_CONTAINER, null, statesLayer, roadsLayer);
        LayerGroupInfo named =
                buildLayerGroup(NAMED_GROUP_NAME, Mode.NAMED, null, statesLayer, roadsLayer);
        layerGroups = Arrays.asList(named, opaque);
        populateCatalog();

        // setup security
        buildManager(new String[] {"named.r=MILITARY"});

        // try the ro user
        SecurityContextHolder.getContext().setAuthentication(roUser);

        // ... direct access to layers not allowed because the named group is not visible
        assertNull(sc.getLayerByName(statesLayer.prefixedName()));
        assertNull(sc.getLayerByName(roadsLayer.prefixedName()));

        // ... but we can access the opaque group and the layers through it
        LayerGroupInfo securedGroup = sc.getLayerGroupByName(OPAQUE_GROUP_NAME);
        assertNotNull(securedGroup);
        assertEquals(2, securedGroup.getLayers().size());

        // now try with the military one
        SecurityContextHolder.getContext().setAuthentication(milUser);

        // ... direct access to layers allowed because the named group is visible to the mil user
        assertNotNull(sc.getLayerByName(statesLayer.prefixedName()));
        assertNotNull(sc.getLayerByName(roadsLayer.prefixedName()));
    }

    @Test
    public void testWmsPublicSingleAndSecuredOpaqueGroup() throws Exception {
        setupRequestThreadLocal("WMS");
        // setup groups
        LayerGroupInfo opaque =
                buildLayerGroup(
                        OPAQUE_GROUP_NAME, Mode.OPAQUE_CONTAINER, null, forestsLayer, roadsLayer);
        LayerGroupInfo single =
                buildLayerGroup(SINGLE_GROUP_NAME, Mode.SINGLE, null, statesLayer, roadsLayer);
        layerGroups = Arrays.asList(single, opaque);
        populateCatalog();

        // setup security, single allowed to everybody, opaque to military only
        buildManager(SINGLE_GROUP_NAME + ".r=*", OPAQUE_GROUP_NAME + ".r=MILITARY");
        SecurityContextHolder.getContext().setAuthentication(roUser);

        // try the ro user
        SecurityContextHolder.getContext().setAuthentication(roUser);

        // ... the only directly available layer should be states, contained in single
        assertNotNull(sc.getLayerByName(statesLayer.prefixedName()));
        assertNull(sc.getLayerByName(forestsLayer.prefixedName()));
        assertNull(sc.getLayerByName(roadsLayer.prefixedName()));

        // ... and even via groups it's the same
        assertNull(sc.getLayerGroupByName(opaque.prefixedName()));
        LayerGroupInfo securedSingleGroup = sc.getLayerGroupByName(single.prefixedName());
        assertNotNull(securedSingleGroup);
        assertEquals(1, securedSingleGroup.getLayers().size());

        // however switching to mil user
        SecurityContextHolder.getContext().setAuthentication(milUser);

        // ... same as above for direct access
        assertNotNull(sc.getLayerByName(statesLayer.prefixedName()));
        assertNull(sc.getLayerByName(forestsLayer.prefixedName()));
        assertNull(sc.getLayerByName(roadsLayer.prefixedName()));

        // but the opaque group is now visible along with its layers
        securedSingleGroup = sc.getLayerGroupByName(single.prefixedName());
        assertNotNull(securedSingleGroup);
        assertEquals(2, securedSingleGroup.getLayers().size());
        LayerGroupInfo securedOpaqueGroup = sc.getLayerGroupByName(opaque.prefixedName());
        assertNotNull(securedOpaqueGroup);
        assertEquals(2, securedOpaqueGroup.getLayers().size());
    }

    @Test
    public void testWmsSecuredSingleAndPublicOpaqueGroup() throws Exception {
        setupRequestThreadLocal("WMS");
        // setup groups
        LayerGroupInfo opaque =
                buildLayerGroup(
                        OPAQUE_GROUP_NAME, Mode.OPAQUE_CONTAINER, null, forestsLayer, roadsLayer);
        LayerGroupInfo single =
                buildLayerGroup(SINGLE_GROUP_NAME, Mode.SINGLE, null, statesLayer, roadsLayer);
        layerGroups = Arrays.asList(single, opaque);
        populateCatalog();

        // setup security, single allowed to everybody, opaque to military only
        buildManager(SINGLE_GROUP_NAME + ".r=MILITARY", OPAQUE_GROUP_NAME + ".r=*");
        SecurityContextHolder.getContext().setAuthentication(roUser);

        // try the ro user
        SecurityContextHolder.getContext().setAuthentication(roUser);

        // ... states should be visible since "single" auth does not cascade to contained layers
        assertNotNull(sc.getLayerByName(statesLayer.prefixedName()));
        assertNull(sc.getLayerByName(forestsLayer.prefixedName()));
        assertNull(sc.getLayerByName(roadsLayer.prefixedName()));

        // ... the single group is not available, the opaque allows access to all its layers, even
        // the ones shared with the single
        assertNull(sc.getLayerGroupByName(single.prefixedName()));
        LayerGroupInfo securedOpaqueGroup = sc.getLayerGroupByName(opaque.prefixedName());
        assertNotNull(securedOpaqueGroup);
        assertEquals(2, securedOpaqueGroup.getLayers().size());

        // however switching to mil user
        SecurityContextHolder.getContext().setAuthentication(milUser);

        // ... now states becomes visible
        assertNotNull(sc.getLayerByName(statesLayer.prefixedName()));
        assertNull(sc.getLayerByName(forestsLayer.prefixedName()));
        assertNull(sc.getLayerByName(roadsLayer.prefixedName()));

        // but the opaque group is now visible along with its layers
        LayerGroupInfo securedSingleGroup = sc.getLayerGroupByName(single.prefixedName());
        assertNotNull(securedSingleGroup);
        assertEquals(2, securedSingleGroup.getLayers().size());
        securedOpaqueGroup = sc.getLayerGroupByName(opaque.prefixedName());
        assertNotNull(securedOpaqueGroup);
        assertEquals(2, securedOpaqueGroup.getLayers().size());
    }

    @Test
    public void testNestedOpaqueGroup() throws Exception {
        setupRequestThreadLocal("WMS");
        // setup groups
        LayerGroupInfo opaque =
                buildLayerGroup(
                        OPAQUE_GROUP_NAME, Mode.OPAQUE_CONTAINER, null, statesLayer, roadsLayer);
        LayerGroupInfo named =
                buildLayerGroup(NAMED_GROUP_NAME, Mode.NAMED, null, forestsLayer, opaque);

        layerGroups = Arrays.asList(named, opaque);
        populateCatalog();

        // setup security
        buildManager(DEFAULT_RULES);

        // direct access forests allowed but not states and roads
        assertNotNull(sc.getLayerByName(forestsLayer.prefixedName()));
        assertNull(sc.getLayerByName(statesLayer.prefixedName()));
        assertNull(sc.getLayerByName(roadsLayer.prefixedName()));

        // however via group access we can see all layers
        LayerGroupInfo securedNamedGroup = sc.getLayerGroupByName(NAMED_GROUP_NAME);
        assertEquals(3, securedNamedGroup.layers().size());
        LayerGroupInfo securedOpaqueGroup = sc.getLayerGroupByName(OPAQUE_GROUP_NAME);
        assertEquals(2, securedOpaqueGroup.layers().size());
    }

    @Test
    public void testNestedOpaqueDenyNestedGroup() throws Exception {
        setupRequestThreadLocal("WMS");
        // setup groups
        LayerGroupInfo opaque =
                buildLayerGroup(
                        OPAQUE_GROUP_NAME, Mode.OPAQUE_CONTAINER, null, statesLayer, roadsLayer);
        LayerGroupInfo named =
                buildLayerGroup(NAMED_GROUP_NAME, Mode.NAMED, null, forestsLayer, opaque);

        layerGroups = Arrays.asList(named, opaque);
        populateCatalog();

        // setup security, disallow nested group
        buildManager(new String[] {OPAQUE_GROUP_NAME + ".r=MILITARY"});

        // try the ro user
        SecurityContextHolder.getContext().setAuthentication(roUser);

        // direct access forests allowed but not states and roads
        assertNotNull(sc.getLayerByName(forestsLayer.prefixedName()));
        assertNull(sc.getLayerByName(statesLayer.prefixedName()));
        assertNull(sc.getLayerByName(roadsLayer.prefixedName()));

        // and via group access we cannot reach the nested one either
        LayerGroupInfo securedNamedGroup = sc.getLayerGroupByName(NAMED_GROUP_NAME);
        assertEquals(1, securedNamedGroup.layers().size());
        assertEquals(forestsLayer.getName(), securedNamedGroup.getLayers().get(0).getName());
        // nested nor accessible directly either
        assertNull(sc.getLayerGroupByName(OPAQUE_GROUP_NAME));
    }

    @Test
    public void testNestedOpaqueDenyContainerGroup() throws Exception {
        setupRequestThreadLocal("WMS");
        // setup groups
        LayerGroupInfo opaque =
                buildLayerGroup(
                        OPAQUE_GROUP_NAME, Mode.OPAQUE_CONTAINER, null, statesLayer, roadsLayer);
        LayerGroupInfo named =
                buildLayerGroup(NAMED_GROUP_NAME, Mode.NAMED, null, forestsLayer, opaque);

        layerGroups = Arrays.asList(named, opaque);
        populateCatalog();

        // setup security, disallow nested group
        buildManager(new String[] {NAMED_GROUP_NAME + ".r=MILITARY"});

        // try the ro user
        SecurityContextHolder.getContext().setAuthentication(roUser);

        // ... direct access not available for any
        assertNull(sc.getLayerByName(forestsLayer.prefixedName()));
        assertNull(sc.getLayerByName(statesLayer.prefixedName()));
        assertNull(sc.getLayerByName(roadsLayer.prefixedName()));

        // ... and via group access we cannot reach the nested one either
        assertNull(sc.getLayerGroupByName(NAMED_GROUP_NAME));
        // ... nested nor accessible directly either
        assertNull(sc.getLayerGroupByName(OPAQUE_GROUP_NAME));

        // switch to the mil user
        SecurityContextHolder.getContext().setAuthentication(milUser);

        // ... direct access available for forests
        assertNotNull(sc.getLayerByName(forestsLayer.prefixedName()));
        assertNull(sc.getLayerByName(statesLayer.prefixedName()));
        assertNull(sc.getLayerByName(roadsLayer.prefixedName()));

        // ... however as mil via group access we can see all layers
        LayerGroupInfo securedNamedGroup = sc.getLayerGroupByName(NAMED_GROUP_NAME);
        assertEquals(3, securedNamedGroup.layers().size());
        LayerGroupInfo securedOpaqueGroup = sc.getLayerGroupByName(OPAQUE_GROUP_NAME);
        assertEquals(2, securedOpaqueGroup.layers().size());
    }

    /**
     * Same as {@link #testWmsStandaloneOpaqueGroup()} but with a nested group as the testing target
     */
    @Test
    public void testWmsNestedInStandaloneOpaqueGroup() throws Exception {
        setupRequestThreadLocal("WMS");
        // setup group
        LayerGroupInfo nested = buildLayerGroup(NESTED_GROUP_NAME, Mode.NAMED, null, statesLayer);
        LayerGroupInfo opaque =
                buildLayerGroup(OPAQUE_GROUP_NAME, Mode.OPAQUE_CONTAINER, null, nested, roadsLayer);
        layerGroups = Arrays.asList(nested, opaque);
        populateCatalog();

        // setup security
        buildManager(DEFAULT_RULES);
        SecurityContextHolder.getContext().setAuthentication(roUser);

        // direct access to nested group not allowed, nor to its layers
        assertNull(sc.getLayerGroupByName(NESTED_GROUP_NAME));
        assertNull(sc.getLayerByName(statesLayer.prefixedName()));
        assertNull(sc.getLayerByName(roadsLayer.prefixedName()));

        // however we can access the group as part of the opaque one
        LayerGroupInfo securedGroup = sc.getLayerGroupByName(opaque.prefixedName());
        assertNotNull(securedGroup);
        assertEquals(2, securedGroup.getLayers().size());
        assertThat(securedGroup.getLayers(), contains(nested, roadsLayer));
        assertThat(securedGroup.layers(), contains(statesLayer, roadsLayer));
    }

    /** Same as {@link #testWmsNamedOpaqueGroup()} but with a nested group as the testing target */
    @Test
    public void testWmsNestedInNamedOpaqueGroup() throws Exception {
        setupRequestThreadLocal("WMS");
        // setup group
        LayerGroupInfo nested = buildLayerGroup(NESTED_GROUP_NAME, Mode.NAMED, null, statesLayer);
        LayerGroupInfo opaque =
                buildLayerGroup(OPAQUE_GROUP_NAME, Mode.OPAQUE_CONTAINER, null, nested, roadsLayer);
        LayerGroupInfo named =
                buildLayerGroup(NAMED_GROUP_NAME, Mode.NAMED, null, nested, roadsLayer);
        layerGroups = Arrays.asList(nested, named, opaque);
        populateCatalog();

        // setup security
        buildManager(DEFAULT_RULES);
        SecurityContextHolder.getContext().setAuthentication(roUser);

        // direct access to layers and nested group allowed because of the named group
        assertNotNull(sc.getLayerGroupByName(nested.prefixedName()));
        assertNotNull(sc.getLayerByName(statesLayer.prefixedName()));
        assertNotNull(sc.getLayerByName(roadsLayer.prefixedName()));

        // we can access the group and the layers through it
        LayerGroupInfo securedOpaqueGroup = sc.getLayerGroupByName(opaque.prefixedName());
        assertNotNull(securedOpaqueGroup);
        assertEquals(2, securedOpaqueGroup.getLayers().size());
        assertThat(securedOpaqueGroup.getLayers(), contains(nested, roadsLayer));

        // and same for named
        LayerGroupInfo securedNamedGroup = sc.getLayerGroupByName(named.prefixedName());
        assertNotNull(securedNamedGroup);
        assertEquals(2, securedNamedGroup.getLayers().size());
        assertThat(securedNamedGroup.getLayers(), contains(nested, roadsLayer));
    }

    /**
     * Same as {@link #testWmsSingleAndOpaqueGroup()} but with a nested group as the testing target
     */
    @Test
    public void testWmsNestedInSingleAndOpaqueGroup() throws Exception {
        setupRequestThreadLocal("WMS");

        // setup groups
        LayerGroupInfo nested = buildLayerGroup(NESTED_GROUP_NAME, Mode.NAMED, null, statesLayer);
        LayerGroupInfo opaque =
                buildLayerGroup(OPAQUE_GROUP_NAME, Mode.OPAQUE_CONTAINER, null, nested, roadsLayer);
        LayerGroupInfo single =
                buildLayerGroup(SINGLE_GROUP_NAME, Mode.SINGLE, null, nested, roadsLayer);
        layerGroups = Arrays.asList(nested, single, opaque);
        populateCatalog();

        // setup security
        buildManager(DEFAULT_RULES);
        SecurityContextHolder.getContext().setAuthentication(roUser);

        // direct access to layers and nested group not allowed because the only container is in
        // opaque mode
        assertNull(sc.getLayerGroupByName(nested.prefixedName()));
        assertNull(sc.getLayerByName(statesLayer.prefixedName()));
        assertNull(sc.getLayerByName(roadsLayer.prefixedName()));

        // we can access the group and the layers through it
        LayerGroupInfo opaqueSecuredGroup = sc.getLayerGroupByName(opaque.prefixedName());
        assertNotNull(opaqueSecuredGroup);
        assertEquals(2, opaqueSecuredGroup.getLayers().size());
        assertThat(opaqueSecuredGroup.getLayers(), contains(nested, roadsLayer));

        LayerGroupInfo securedSingleGroup = sc.getLayerGroupByName(single.prefixedName());
        assertNotNull(securedSingleGroup);
        assertEquals(2, securedSingleGroup.getLayers().size());
        assertThat(securedSingleGroup.getLayers(), contains(nested, roadsLayer));
    }

    /**
     * Same as {@link #testWmsMilitaryNamedAndPublicOpaqueGroup()} but with a nested group as the
     * testing target
     */
    @Test
    public void testWmsNestedInMilitaryNamedAndPublicOpaqueGroup() throws Exception {
        setupRequestThreadLocal("WMS");
        // setup groups
        LayerGroupInfo nested = buildLayerGroup(NESTED_GROUP_NAME, Mode.NAMED, null, statesLayer);
        LayerGroupInfo opaque =
                buildLayerGroup(OPAQUE_GROUP_NAME, Mode.OPAQUE_CONTAINER, null, nested, roadsLayer);
        LayerGroupInfo named =
                buildLayerGroup(NAMED_GROUP_NAME, Mode.NAMED, null, nested, roadsLayer);
        layerGroups = Arrays.asList(nested, opaque, named);
        populateCatalog();

        // setup security
        buildManager(new String[] {"named.r=MILITARY"});

        // try the ro user
        SecurityContextHolder.getContext().setAuthentication(roUser);

        // ... direct access to layers not allowed because the named group is not visible
        assertNull(sc.getLayerGroupByName(nested.prefixedName()));
        assertNull(sc.getLayerByName(statesLayer.prefixedName()));
        assertNull(sc.getLayerByName(roadsLayer.prefixedName()));

        // ... but we can access the opaque group and the layers through it
        LayerGroupInfo securedGroup = sc.getLayerGroupByName(OPAQUE_GROUP_NAME);
        assertNotNull(securedGroup);
        assertEquals(2, securedGroup.getLayers().size());
        assertThat(securedGroup.getLayers(), contains(nested, roadsLayer));

        // now try with the military one
        SecurityContextHolder.getContext().setAuthentication(milUser);

        // ... direct access to layers allowed because the named group is visible to the mil user
        assertNotNull(sc.getLayerGroupByName(nested.prefixedName()));
        assertNotNull(sc.getLayerByName(statesLayer.prefixedName()));
        assertNotNull(sc.getLayerByName(roadsLayer.prefixedName()));
    }

    /**
     * Same as {@link #testWmsPublicSingleAndSecuredOpaqueGroup} but with a nested group as the
     * testing target
     */
    @Test
    public void testWmsNestedInPublicSingleAndSecuredOpaqueGroup() throws Exception {
        setupRequestThreadLocal("WMS");
        // setup groups
        LayerGroupInfo nested = buildLayerGroup(NESTED_GROUP_NAME, Mode.NAMED, null, statesLayer);
        LayerGroupInfo opaque =
                buildLayerGroup(
                        OPAQUE_GROUP_NAME, Mode.OPAQUE_CONTAINER, null, forestsLayer, nested);
        LayerGroupInfo single =
                buildLayerGroup(SINGLE_GROUP_NAME, Mode.SINGLE, null, roadsLayer, nested);
        layerGroups = Arrays.asList(nested, single, opaque);
        populateCatalog();

        // setup security, single allowed to everybody, opaque to military only
        buildManager(SINGLE_GROUP_NAME + ".r=*", OPAQUE_GROUP_NAME + ".r=MILITARY");
        SecurityContextHolder.getContext().setAuthentication(roUser);

        // try the ro user
        SecurityContextHolder.getContext().setAuthentication(roUser);

        // ... the only directly available layer should be roads, contained in single
        assertNotNull(sc.getLayerByName(roadsLayer.prefixedName()));
        assertNull(sc.getLayerByName(forestsLayer.prefixedName()));
        assertNull(sc.getLayerByName(statesLayer.prefixedName()));
        assertNull(sc.getLayerGroupByName(nested.prefixedName()));

        // ... and even via groups it's the same
        assertNull(sc.getLayerGroupByName(opaque.prefixedName()));
        LayerGroupInfo securedSingleGroup = sc.getLayerGroupByName(single.prefixedName());
        assertNotNull(securedSingleGroup);
        assertEquals(1, securedSingleGroup.getLayers().size());

        // however switching to mil user
        SecurityContextHolder.getContext().setAuthentication(milUser);

        // ... same as above for direct access
        assertNotNull(sc.getLayerByName(roadsLayer.prefixedName()));
        assertNull(sc.getLayerByName(forestsLayer.prefixedName()));
        assertNull(sc.getLayerByName(statesLayer.prefixedName()));
        assertNull(sc.getLayerGroupByName(nested.prefixedName()));

        // but the opaque group is now visible along with its layers
        securedSingleGroup = sc.getLayerGroupByName(single.prefixedName());
        assertNotNull(securedSingleGroup);
        assertEquals(2, securedSingleGroup.getLayers().size());
        assertThat(securedSingleGroup.getLayers(), contains(roadsLayer, nested));
        LayerGroupInfo nestedInSingle = (LayerGroupInfo) securedSingleGroup.getLayers().get(1);
        assertEquals(1, nestedInSingle.getLayers().size());
        assertThat(nestedInSingle.getLayers(), contains(statesLayer));

        LayerGroupInfo securedOpaqueGroup = sc.getLayerGroupByName(opaque.prefixedName());
        assertNotNull(securedOpaqueGroup);
        assertEquals(2, securedOpaqueGroup.getLayers().size());
        assertThat(securedOpaqueGroup.getLayers(), contains(forestsLayer, nested));
        LayerGroupInfo nestedInOpaque = (LayerGroupInfo) securedSingleGroup.getLayers().get(1);
        assertEquals(1, nestedInOpaque.getLayers().size());
        assertThat(nestedInOpaque.getLayers(), contains(statesLayer));
    }
}

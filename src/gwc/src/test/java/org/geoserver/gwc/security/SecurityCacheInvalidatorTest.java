/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.security;

import static org.geoserver.gwc.security.SecurityParameterFilter.ACCESS_LIMITS_KEY;
import static org.geoserver.gwc.security.SecurityParameterFilter.SECURITY_TAGS_KEY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.security.impl.LayerGroupContainmentCache;
import org.geoserver.security.impl.LayerGroupContainmentCache.LayerGroupSummary;
import org.geowebcache.filter.parameters.ParametersUtils;
import org.geowebcache.layer.TileLayerDispatcher;
import org.geowebcache.storage.StorageBroker;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link SecurityCacheInvalidator}. Uses a mock {@link StorageBroker} to verify that the correct
 * parameter sets are deleted for each invalidation scenario.
 *
 * <p>Layer "farm:fields" simulates a secured layer with several cached parameter combinations representing different
 * users' access profiles. Each combination carries {@code ACCESS_LIMITS_KEY} (content fingerprint) and optionally
 * {@code SECURITY_TAGS_KEY} (field ownership tags).
 */
public class SecurityCacheInvalidatorTest {

    static final String LAYER = "farm:fields";

    // cached parameter maps representing access profiles for different field combinations
    static final Map<String, String> PARAMS_FIELD1 = params("{\"readFilter\":\"FIELD_ID IN (1)\"}", "field:1");
    static final Map<String, String> PARAMS_FIELD2 = params("{\"readFilter\":\"FIELD_ID IN (2)\"}", "field:2");
    static final Map<String, String> PARAMS_FIELD1_2 =
            params("{\"readFilter\":\"FIELD_ID IN (1, 2)\"}", "field:1,field:2");
    static final Map<String, String> PARAMS_FIELD2_3 =
            params("{\"readFilter\":\"FIELD_ID IN (2, 3)\"}", "field:2,field:3");
    // unrestricted - no ACCESS_LIMITS_KEY
    static final Map<String, String> PARAMS_UNRESTRICTED = Map.of("STYLES", "");

    private StorageBroker mockBroker;
    private TileLayerDispatcher mockDispatcher;
    private Catalog mockCatalog;
    private LayerGroupContainmentCache mockContainmentCache;
    private PrecisionAgricultureRAM ram;
    private SecurityCacheInvalidator invalidator;

    @Before
    public void setUp() throws Exception {
        mockBroker = mock(StorageBroker.class);
        mockDispatcher = mock(TileLayerDispatcher.class);
        when(mockDispatcher.getLayerNames()).thenReturn(Set.of(LAYER));
        mockCatalog = mock(Catalog.class);
        mockContainmentCache = mock(LayerGroupContainmentCache.class);
        ram = new PrecisionAgricultureRAM();
        invalidator = new SecurityCacheInvalidator(mockBroker, mockDispatcher, mockCatalog, mockContainmentCache);
        ram.register(invalidator);
    }

    @Test
    public void testSingleFieldUserRevoke() throws Exception {
        // farmer_alice has field 1 cached; revoke fires event for "field:1" -> that tile set deleted
        when(mockBroker.getCachedParameters(LAYER)).thenReturn(Set.of(PARAMS_FIELD1, PARAMS_UNRESTRICTED));
        ram.addField("farmer_alice", 1);
        ram.removeField("farmer_alice", 1);
        // two events fired (add + remove), each should delete PARAMS_FIELD1
        verify(mockBroker, times(2)).deleteByParametersId(LAYER, ParametersUtils.getId(PARAMS_FIELD1));
        verify(mockBroker, never()).deleteByParametersId(eq(LAYER), eq(ParametersUtils.getId(PARAMS_UNRESTRICTED)));
    }

    @Test
    public void testEventMatchesTileWithMultipleTags() throws Exception {
        // tile tagged "field:1,field:2" - event for field:1 must delete it
        when(mockBroker.getCachedParameters(LAYER)).thenReturn(Set.of(PARAMS_FIELD1_2));
        ram.addField("manager", 1);
        verify(mockBroker).deleteByParametersId(LAYER, ParametersUtils.getId(PARAMS_FIELD1_2));
    }

    @Test
    public void testEventDoesNotDeleteUnrelatedField() throws Exception {
        // tiles for field 2 and field 2+3 should not be touched by a field:1 event
        when(mockBroker.getCachedParameters(LAYER)).thenReturn(Set.of(PARAMS_FIELD2, PARAMS_FIELD2_3));
        ram.addField("farmer_alice", 1);
        verify(mockBroker, never()).deleteByParametersId(eq(LAYER), eq(ParametersUtils.getId(PARAMS_FIELD2)));
        verify(mockBroker, never()).deleteByParametersId(eq(LAYER), eq(ParametersUtils.getId(PARAMS_FIELD2_3)));
    }

    @Test
    public void testNullTagDeletesAllSecurityKeyedTiles() throws Exception {
        // event with targetTag=null drops all entries carrying ACCESS_LIMITS_KEY
        when(mockBroker.getCachedParameters(LAYER))
                .thenReturn(Set.of(PARAMS_FIELD1, PARAMS_FIELD2, PARAMS_FIELD1_2, PARAMS_UNRESTRICTED));
        SecurityConfigurationChangeEvent event = new SecurityConfigurationChangeEvent(null, null);
        invalidator.onSecurityConfigChange(event);
        verify(mockBroker).deleteByParametersId(LAYER, ParametersUtils.getId(PARAMS_FIELD1));
        verify(mockBroker).deleteByParametersId(LAYER, ParametersUtils.getId(PARAMS_FIELD2));
        verify(mockBroker).deleteByParametersId(LAYER, ParametersUtils.getId(PARAMS_FIELD1_2));
        verify(mockBroker, never()).deleteByParametersId(eq(LAYER), eq(ParametersUtils.getId(PARAMS_UNRESTRICTED)));
    }

    @Test
    public void testUnrestrictedTilesNeverDeleted() throws Exception {
        // tiles without ACCESS_LIMITS_KEY are never touched, regardless of targetTag
        when(mockBroker.getCachedParameters(LAYER)).thenReturn(Set.of(PARAMS_UNRESTRICTED));
        SecurityConfigurationChangeEvent event = new SecurityConfigurationChangeEvent(null, null);
        invalidator.onSecurityConfigChange(event);
        verify(mockBroker, never()).deleteByParametersId(eq(LAYER), eq(ParametersUtils.getId(PARAMS_UNRESTRICTED)));
    }

    @Test
    public void testNullAffectedLayersSweepsAll() throws Exception {
        // a null scope means "unknown" -> every tile layer is swept
        String otherLayer = "other:layer";
        when(mockDispatcher.getLayerNames()).thenReturn(Set.of(LAYER, otherLayer));
        when(mockBroker.getCachedParameters(LAYER)).thenReturn(Set.of(PARAMS_FIELD1));
        when(mockBroker.getCachedParameters(otherLayer)).thenReturn(Set.of(PARAMS_FIELD1));

        invalidator.onSecurityConfigChange(new SecurityConfigurationChangeEvent(null, Set.of("field:1")));

        verify(mockBroker).deleteByParametersId(LAYER, ParametersUtils.getId(PARAMS_FIELD1));
        verify(mockBroker).deleteByParametersId(otherLayer, ParametersUtils.getId(PARAMS_FIELD1));
    }

    @Test
    public void testScopedAffectedLayersExcludesOthers() throws Exception {
        // a non-null scope must invalidate only the listed layers, never an unrelated one
        String otherLayer = "other:layer";
        when(mockDispatcher.getLayerNames()).thenReturn(Set.of(LAYER, otherLayer));
        LayerInfo affected = mock(LayerInfo.class);
        when(affected.getId()).thenReturn("layer-1");
        when(affected.prefixedName()).thenReturn(LAYER);
        when(mockBroker.getCachedParameters(LAYER)).thenReturn(Set.of(PARAMS_FIELD1));

        invalidator.onSecurityConfigChange(new SecurityConfigurationChangeEvent(Set.of(affected), null));

        verify(mockBroker).deleteByParametersId(LAYER, ParametersUtils.getId(PARAMS_FIELD1));
        verify(mockBroker, never()).getCachedParameters(otherLayer);
    }

    @Test
    public void testMultiFieldUserRevokeSingleField() throws Exception {
        // manager has fields 1 and 2; cached tiles: field:1 only, field:2 only, field:1,2 combined
        when(mockBroker.getCachedParameters(LAYER)).thenReturn(Set.of(PARAMS_FIELD1, PARAMS_FIELD2, PARAMS_FIELD1_2));
        ram.addField("manager", 1);
        ram.addField("manager", 2);
        // each add event fires for its tag; after two adds: field:1 event fired once, field:2 event once
        // field:1 event should delete PARAMS_FIELD1 and PARAMS_FIELD1_2 (both carry field:1 tag)
        // field:2 event should delete PARAMS_FIELD2 and PARAMS_FIELD1_2 (both carry field:2 tag)
        verify(mockBroker, times(1)).deleteByParametersId(LAYER, ParametersUtils.getId(PARAMS_FIELD1));
        verify(mockBroker, times(1)).deleteByParametersId(LAYER, ParametersUtils.getId(PARAMS_FIELD2));
        // PARAMS_FIELD1_2 matches both events so deleted twice
        verify(mockBroker, times(2)).deleteByParametersId(LAYER, ParametersUtils.getId(PARAMS_FIELD1_2));
    }

    @Test
    public void testRevokeFieldFromManagerLeavesOtherFieldTilesIntact() throws Exception {
        // pre-populate manager with fields 1+2 without firing events, then revoke field 1 only
        ram.setInitialFields("manager", 1, 2);
        when(mockBroker.getCachedParameters(LAYER)).thenReturn(Set.of(PARAMS_FIELD1, PARAMS_FIELD2, PARAMS_FIELD1_2));

        ram.removeField("manager", 1); // fires field:1 event only

        // field:1 event must delete tiles carrying field:1 tag
        verify(mockBroker).deleteByParametersId(LAYER, ParametersUtils.getId(PARAMS_FIELD1));
        verify(mockBroker).deleteByParametersId(LAYER, ParametersUtils.getId(PARAMS_FIELD1_2));
        // PARAMS_FIELD2 (tagged only "field:2") must not be touched
        verify(mockBroker, never()).deleteByParametersId(eq(LAYER), eq(ParametersUtils.getId(PARAMS_FIELD2)));
    }

    @Test
    public void testEmptyCachedParameters() throws Exception {
        // no cached params - no deletions, no exceptions
        when(mockBroker.getCachedParameters(LAYER)).thenReturn(Set.of());
        SecurityConfigurationChangeEvent event = new SecurityConfigurationChangeEvent(null, Set.of("field:1"));
        invalidator.onSecurityConfigChange(event);
        verify(mockBroker, never()).deleteByParametersId(eq(LAYER), eq(ParametersUtils.getId(PARAMS_FIELD1)));
    }

    @Test
    public void testMemberLayerEventInvalidatesContainingGroup() throws Exception {
        // a member-layer change must also sweep the group tile-layer, whose tiles embed the member's limits.
        // the containing groups come from LayerGroupContainmentCache, keyed by the member's resource
        String groupName = "farm:overview";
        ResourceInfo resource = mock(ResourceInfo.class);
        LayerInfo affected = mock(LayerInfo.class);
        when(affected.getId()).thenReturn("layer-1");
        when(affected.prefixedName()).thenReturn(LAYER);
        when(affected.getResource()).thenReturn(resource);
        LayerGroupSummary group = mock(LayerGroupSummary.class);
        when(group.prefixedName()).thenReturn(groupName);
        when(mockContainmentCache.getContainerGroupsFor(resource, true)).thenReturn(List.of(group));
        when(mockDispatcher.getLayerNames()).thenReturn(Set.of(LAYER, groupName));
        when(mockBroker.getCachedParameters(LAYER)).thenReturn(Set.of(PARAMS_FIELD1));
        when(mockBroker.getCachedParameters(groupName)).thenReturn(Set.of(PARAMS_FIELD1));

        invalidator.onSecurityConfigChange(new SecurityConfigurationChangeEvent(Set.of(affected), Set.of("field:1")));

        verify(mockBroker).deleteByParametersId(LAYER, ParametersUtils.getId(PARAMS_FIELD1));
        verify(mockBroker).deleteByParametersId(groupName, ParametersUtils.getId(PARAMS_FIELD1));
    }

    @Test
    public void testMultiTagEventDeletesAnyMatch() throws Exception {
        // a single event carrying several tags must delete every tile matching any of them
        when(mockBroker.getCachedParameters(LAYER))
                .thenReturn(Set.of(PARAMS_FIELD1, PARAMS_FIELD2, PARAMS_FIELD1_2, PARAMS_FIELD2_3));
        ram.setInitialFields("manager", 1, 3);
        ram.removeFields("manager", 1, 3); // one event with tags {field:1, field:3}

        verify(mockBroker).deleteByParametersId(LAYER, ParametersUtils.getId(PARAMS_FIELD1)); // field:1
        verify(mockBroker).deleteByParametersId(LAYER, ParametersUtils.getId(PARAMS_FIELD1_2)); // field:1
        verify(mockBroker).deleteByParametersId(LAYER, ParametersUtils.getId(PARAMS_FIELD2_3)); // field:3
        // PARAMS_FIELD2 (only field:2) matches neither
        verify(mockBroker, never()).deleteByParametersId(eq(LAYER), eq(ParametersUtils.getId(PARAMS_FIELD2)));
    }

    @Test
    public void testContainsAnyTagHelper() {
        assertTrue(SecurityCacheInvalidator.containsAnyTag("field:1", Set.of("field:1")));
        assertTrue(SecurityCacheInvalidator.containsAnyTag("field:1,field:2", Set.of("field:1")));
        assertTrue(SecurityCacheInvalidator.containsAnyTag("field:1,field:2", Set.of("field:3", "field:2")));
        assertFalse(SecurityCacheInvalidator.containsAnyTag("field:2", Set.of("field:1")));
        assertFalse(SecurityCacheInvalidator.containsAnyTag(null, Set.of("field:1")));
        assertFalse(SecurityCacheInvalidator.containsAnyTag("", Set.of("field:1")));
        assertFalse(SecurityCacheInvalidator.containsAnyTag("field:10", Set.of("field:1"))); // no partial match
    }

    @Test
    public void testEventTargetTagWithCommaRejected() {
        // a comma in a tag would never match a stored tag (split on commas), leaving stale tiles behind
        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> new SecurityConfigurationChangeEvent(null, Set.of("field:1,field:2")));
        assertThat(e.getMessage(), containsString("field:1,field:2"));
    }

    private static Map<String, String> params(String accessKey, String tags) {
        Map<String, String> m = new HashMap<>();
        m.put(ACCESS_LIMITS_KEY, accessKey);
        if (tags != null) m.put(SECURITY_TAGS_KEY, tags);
        return m;
    }
}

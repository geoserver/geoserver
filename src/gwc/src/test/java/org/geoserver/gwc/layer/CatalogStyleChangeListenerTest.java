package org.geoserver.gwc.layer;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;

import junit.framework.TestCase;

import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.event.impl.CatalogModifyEventImpl;
import org.geoserver.catalog.event.impl.CatalogPostModifyEventImpl;

public class CatalogStyleChangeListenerTest extends TestCase {

    private final String STYLE_NAME = "highways";

    private String STYLE_NAME_MODIFIED = STYLE_NAME + "_modified";

    private final String PREFIXED_RESOURCE_NAME = "mock:Layer";

    private CatalogConfiguration mockConfiguration;

    private ResourceInfo mockResourceInfo;

    private LayerInfo mockLayerInfo;

    private StyleInfo mockStyle;

    private GeoServerTileLayer mockTileLayer;

    private GeoServerTileLayerInfo mockTileLayerInfo;

    private CatalogModifyEventImpl styleNameModifyEvent;

    private CatalogStyleChangeListener listener;

    protected void setUp() throws Exception {
        mockConfiguration = mock(CatalogConfiguration.class);
        mockStyle = mock(StyleInfo.class);
        when(mockStyle.getName()).thenReturn(STYLE_NAME);

        mockResourceInfo = mock(FeatureTypeInfo.class);
        when(mockResourceInfo.getPrefixedName()).thenReturn(PREFIXED_RESOURCE_NAME);

        mockLayerInfo = mock(LayerInfo.class);
        when(mockLayerInfo.getResource()).thenReturn(mockResourceInfo);

        mockTileLayer = mock(GeoServerTileLayer.class);
        mockTileLayerInfo = mock(GeoServerTileLayerInfo.class);
        when(mockTileLayer.getInfo()).thenReturn(mockTileLayerInfo);
        when(mockTileLayer.getName()).thenReturn(PREFIXED_RESOURCE_NAME);
        when(mockConfiguration.getTileLayersForStyle(eq(STYLE_NAME))).thenReturn(
                Collections.singletonList(mockTileLayer));

        listener = new CatalogStyleChangeListener(mockConfiguration);

        styleNameModifyEvent = new CatalogModifyEventImpl();
        styleNameModifyEvent.setSource(mockStyle);
        styleNameModifyEvent.setPropertyNames(Arrays.asList("name"));
        styleNameModifyEvent.setOldValues(Arrays.asList(STYLE_NAME));
        styleNameModifyEvent.setNewValues(Arrays.asList(STYLE_NAME_MODIFIED));
    }

    public void testIgnorableChange() throws Exception {

        // not a name change
        styleNameModifyEvent.setPropertyNames(Arrays.asList("fileName"));
        listener.handleModifyEvent(styleNameModifyEvent);

        // name didn't change at all
        styleNameModifyEvent.setPropertyNames(Arrays.asList("name"));
        styleNameModifyEvent.setOldValues(Arrays.asList(STYLE_NAME));
        styleNameModifyEvent.setNewValues(Arrays.asList(STYLE_NAME));
        listener.handleModifyEvent(styleNameModifyEvent);

        // not a style change
        styleNameModifyEvent.setSource(mock(LayerInfo.class));
        listener.handleModifyEvent(styleNameModifyEvent);

        // a change in the name of the default style should not cause a truncate
        verify(mockConfiguration, never()).truncate(anyString(), anyString());
        // nor a save, as the default style name is dynamic
        verify(mockConfiguration, never()).save((GeoServerTileLayer) anyObject());

        verify(mockTileLayer, never()).getInfo();
        verify(mockTileLayerInfo, never()).getCachedStyles();
    }

    public void testRenameDefaultStyle() throws Exception {
        // this is another case of an ignorable change. Renaming the default style shall have no
        // impact.
        listener.handleModifyEvent(styleNameModifyEvent);
        // a change in the name of the default style should not cause a truncate
        verify(mockConfiguration, never()).truncate(anyString(), anyString());
        // nor a save, as the default style name is dynamic
        verify(mockConfiguration, never()).save((GeoServerTileLayer) anyObject());

        verify(mockTileLayer, atLeastOnce()).getInfo();
        verify(mockTileLayerInfo, atLeastOnce()).getCachedStyles();
    }

    public void testRenameAlternateStyle() throws Exception {
        when(mockTileLayerInfo.getCachedStyles()).thenReturn(Collections.singleton(STYLE_NAME));

        listener.handleModifyEvent(styleNameModifyEvent);

        verify(mockTileLayerInfo, times(1)).setCachedStyles(
                eq(Collections.singleton(STYLE_NAME_MODIFIED)));
        verify(mockTileLayer, times(1)).resetParameterFilters();
        verify(mockConfiguration, times(1)).truncate(eq(PREFIXED_RESOURCE_NAME), eq(STYLE_NAME));
        verify(mockConfiguration, times(1)).save(same(mockTileLayer));
    }

    @SuppressWarnings("unchecked")
    public void testLayerInfoDefaultOrAlternateStyleChanged() throws Exception {
        when(mockConfiguration.getLayerInfosFor(same(mockStyle))).thenReturn(
                Collections.singleton(mockLayerInfo));
        when(mockConfiguration.getLayerGroupsFor(same(mockStyle))).thenReturn(
                Collections.EMPTY_LIST);

        CatalogPostModifyEventImpl postModifyEvent = new CatalogPostModifyEventImpl();
        postModifyEvent.setSource(mockStyle);
        listener.handlePostModifyEvent(postModifyEvent);

        verify(mockConfiguration, times(1)).truncate(eq(PREFIXED_RESOURCE_NAME), eq(STYLE_NAME));
    }

    @SuppressWarnings("unchecked")
    public void testLayerGroupInfoImplicitOrExplicitStyleChanged() throws Exception {
        LayerGroupInfo mockGroup = mock(LayerGroupInfo.class);
        when(mockGroup.getName()).thenReturn("mockGroup");

        when(mockConfiguration.getLayerInfosFor(same(mockStyle)))
                .thenReturn(Collections.EMPTY_LIST);
        when(mockConfiguration.getLayerGroupsFor(same(mockStyle))).thenReturn(
                Collections.singleton(mockGroup));

        CatalogPostModifyEventImpl postModifyEvent = new CatalogPostModifyEventImpl();
        postModifyEvent.setSource(mockStyle);
        listener.handlePostModifyEvent(postModifyEvent);

        verify(mockConfiguration, times(1)).truncate(eq("mockGroup"));
    }
}

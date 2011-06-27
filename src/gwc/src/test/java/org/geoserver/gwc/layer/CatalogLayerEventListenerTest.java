/**
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package org.geoserver.gwc.layer;

import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.impl.CatalogAddEventImpl;
import org.geoserver.catalog.event.impl.CatalogRemoveEventImpl;
import org.mockito.Mockito;

/**
 * @author groldan
 * 
 */
public class CatalogLayerEventListenerTest extends TestCase {

    private static final String NAMESPACE_PREFIX = "mock";

    private static final String RESOURCE_NAME = "Layer";

    private static final String PREFIXED_RESOURCE_NAME = "mock:Layer";

    private static final String LAYER_GROUP_NAME = "LayerGroupName";

    private CatalogConfiguration mockConfiguration;

    private LayerInfo mockLayerInfo;

    private ResourceInfo mockResourceInfo;

    private NamespaceInfo mockNamespaceInfo;

    private LayerGroupInfo mockLayerGroupInfo;

    private CatalogLayerEventListener listener;

    /**
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        mockConfiguration = mock(CatalogConfiguration.class);
        mockLayerInfo = mock(LayerInfo.class);
        mockLayerGroupInfo = mock(LayerGroupInfo.class);
        mockResourceInfo = mock(FeatureTypeInfo.class);
        mockNamespaceInfo = mock(NamespaceInfo.class);

        when(mockLayerGroupInfo.getName()).thenReturn(LAYER_GROUP_NAME);
        when(mockResourceInfo.getPrefixedName()).thenReturn(PREFIXED_RESOURCE_NAME);
        when(mockResourceInfo.getName()).thenReturn(RESOURCE_NAME);
        when(mockResourceInfo.getNamespace()).thenReturn(mockNamespaceInfo);
        when(mockNamespaceInfo.getPrefix()).thenReturn(NAMESPACE_PREFIX);
        when(mockLayerInfo.getResource()).thenReturn(mockResourceInfo);

        listener = new CatalogLayerEventListener(mockConfiguration);
    }

    public void testLayerInfoAdded() throws Exception {
        CatalogAddEventImpl event = new CatalogAddEventImpl();
        event.setSource(mockLayerInfo);

        listener.handleAddEvent(event);

        verify(mockConfiguration).createLayer(Mockito.same(mockLayerInfo));
    }

    public void testLayerGroupInfoAdded() throws Exception {

        CatalogAddEventImpl event = new CatalogAddEventImpl();
        event.setSource(mockLayerGroupInfo);

        listener.handleAddEvent(event);

        verify(mockConfiguration).createLayer(Mockito.same(mockLayerGroupInfo));
    }

    public void testLayerInfoRemoved() throws Exception {
        CatalogRemoveEventImpl event = new CatalogRemoveEventImpl();
        event.setSource(mockLayerInfo);

        listener.handleRemoveEvent(event);

        verify(mockConfiguration).removeLayer(eq(mockResourceInfo.getPrefixedName()));
    }

    public void testLayerGroupInfoRemoved() throws Exception {
        CatalogRemoveEventImpl event = new CatalogRemoveEventImpl();
        event.setSource(mockLayerGroupInfo);

        listener.handleRemoveEvent(event);

        verify(mockConfiguration).removeLayer(eq(mockLayerGroupInfo.getName()));
    }

    public void testResourceInfoRenamed() throws Exception {

        final String renamedResouceName = RESOURCE_NAME + "_Renamed";
        final String renamedPrefixedResouceName = PREFIXED_RESOURCE_NAME + "_Renamed";

        // rename mockResourceInfo
        when(mockResourceInfo.getName()).thenReturn(renamedResouceName);
        when(mockResourceInfo.getPrefixedName()).thenReturn(renamedPrefixedResouceName);

        CatalogModifyEvent modifyEvent = mock(CatalogModifyEvent.class);
        when(modifyEvent.getSource()).thenReturn(mockResourceInfo);
        when(modifyEvent.getPropertyNames()).thenReturn(Arrays.asList("name"));
        when(modifyEvent.getOldValues()).thenReturn(Arrays.asList((Object) RESOURCE_NAME));
        when(modifyEvent.getNewValues()).thenReturn(Arrays.asList((Object) renamedResouceName));

        listener.handleModifyEvent(modifyEvent);

        CatalogPostModifyEvent postModifyEvent = mock(CatalogPostModifyEvent.class);
        when(postModifyEvent.getSource()).thenReturn(mockResourceInfo);

        listener.handlePostModifyEvent(postModifyEvent);

        verify(mockConfiguration).renameTileLayer(eq(PREFIXED_RESOURCE_NAME),
                eq(renamedPrefixedResouceName));
    }

    public void testLayerGroupInfoRenamed() throws Exception {
        final String renamedGroupName = LAYER_GROUP_NAME + "_Renamed";

        // rename mockResourceInfo
        when(mockLayerGroupInfo.getName()).thenReturn(renamedGroupName);

        CatalogModifyEvent modifyEvent = mock(CatalogModifyEvent.class);
        when(modifyEvent.getSource()).thenReturn(mockLayerGroupInfo);
        when(modifyEvent.getPropertyNames()).thenReturn(Arrays.asList("name"));
        when(modifyEvent.getOldValues()).thenReturn(Arrays.asList((Object) LAYER_GROUP_NAME));
        when(modifyEvent.getNewValues()).thenReturn(Arrays.asList((Object) renamedGroupName));

        listener.handleModifyEvent(modifyEvent);

        CatalogPostModifyEvent postModifyEvent = mock(CatalogPostModifyEvent.class);
        when(postModifyEvent.getSource()).thenReturn(mockLayerGroupInfo);

        listener.handlePostModifyEvent(postModifyEvent);

        verify(mockConfiguration).renameTileLayer(eq(LAYER_GROUP_NAME), eq(renamedGroupName));
    }

    public void testResourceInfoNamespaceChanged() throws Exception {
        NamespaceInfo newNamespace = mock(NamespaceInfo.class);
        when(newNamespace.getPrefix()).thenReturn("newMock");

        final String newPrefixedName = newNamespace.getPrefix() + ":" + mockResourceInfo.getName();

        // set the new namespace
        when(mockResourceInfo.getNamespace()).thenReturn(newNamespace);
        when(mockResourceInfo.getPrefixedName()).thenReturn(newPrefixedName);

        CatalogModifyEvent modifyEvent = mock(CatalogModifyEvent.class);
        when(modifyEvent.getSource()).thenReturn(mockResourceInfo);
        when(modifyEvent.getPropertyNames()).thenReturn(Arrays.asList("namespace"));
        when(modifyEvent.getOldValues()).thenReturn(Arrays.asList((Object) mockNamespaceInfo));
        when(modifyEvent.getNewValues()).thenReturn(Arrays.asList((Object) newNamespace));

        listener.handleModifyEvent(modifyEvent);

        CatalogPostModifyEvent postModifyEvent = mock(CatalogPostModifyEvent.class);
        when(postModifyEvent.getSource()).thenReturn(mockResourceInfo);

        listener.handlePostModifyEvent(postModifyEvent);

        verify(mockConfiguration).renameTileLayer(eq(PREFIXED_RESOURCE_NAME), eq(newPrefixedName));
    }

    public void testLayerGroupInfoLayersChanged() throws Exception {
        CatalogModifyEvent modifyEvent = mock(CatalogModifyEvent.class);
        when(modifyEvent.getSource()).thenReturn(mockLayerGroupInfo);
        when(modifyEvent.getPropertyNames()).thenReturn(Arrays.asList("layers"));

        List<LayerInfo> oldLayers = Collections.emptyList();
        List<LayerInfo> newLayers = Collections.singletonList(mockLayerInfo);

        when(modifyEvent.getOldValues()).thenReturn(Collections.singletonList((Object) oldLayers));
        when(modifyEvent.getNewValues()).thenReturn(Collections.singletonList((Object) newLayers));

        listener.handleModifyEvent(modifyEvent);

        CatalogPostModifyEvent postModifyEvent = mock(CatalogPostModifyEvent.class);
        when(postModifyEvent.getSource()).thenReturn(mockLayerGroupInfo);

        listener.handlePostModifyEvent(postModifyEvent);

        verify(mockConfiguration).truncate(eq(LAYER_GROUP_NAME));
    }

    public void testLayerGroupInfoStylesChanged() throws Exception {

        CatalogModifyEvent modifyEvent = mock(CatalogModifyEvent.class);
        when(modifyEvent.getSource()).thenReturn(mockLayerGroupInfo);
        when(modifyEvent.getPropertyNames()).thenReturn(Arrays.asList("styles"));
        List<StyleInfo> oldStyles = Collections.emptyList();
        StyleInfo newStyle = mock(StyleInfo.class);
        List<StyleInfo> newStyles = Collections.singletonList(newStyle);
        when(modifyEvent.getOldValues()).thenReturn(Collections.singletonList((Object) oldStyles));
        when(modifyEvent.getNewValues()).thenReturn(Collections.singletonList((Object) newStyles));

        listener.handleModifyEvent(modifyEvent);

        CatalogPostModifyEvent postModifyEvent = mock(CatalogPostModifyEvent.class);
        when(postModifyEvent.getSource()).thenReturn(mockLayerGroupInfo);

        listener.handlePostModifyEvent(postModifyEvent);

        verify(mockConfiguration).truncate(eq(LAYER_GROUP_NAME));
    }

    public void testLayerInfoDefaultStyleChanged() throws Exception {
        final String oldName = "oldStyle";
        final String newName = "newStyle";

        StyleInfo oldStyle = mock(StyleInfo.class);
        when(oldStyle.getName()).thenReturn(oldName);
        StyleInfo newStyle = mock(StyleInfo.class);
        when(newStyle.getName()).thenReturn(newName);

        when(mockLayerInfo.getDefaultStyle()).thenReturn(newStyle);

        CatalogModifyEvent modifyEvent = mock(CatalogModifyEvent.class);
        when(modifyEvent.getSource()).thenReturn(mockLayerInfo);
        when(modifyEvent.getPropertyNames()).thenReturn(Arrays.asList("defaultStyle"));
        when(modifyEvent.getOldValues()).thenReturn(Collections.singletonList((Object) oldStyle));
        when(modifyEvent.getNewValues()).thenReturn(Collections.singletonList((Object) newStyle));

        GeoServerTileLayer tileLayer = mock(GeoServerTileLayer.class);
        when(mockConfiguration.getTileLayer(eq(PREFIXED_RESOURCE_NAME))).thenReturn(tileLayer);

        listener.handleModifyEvent(modifyEvent);

        CatalogPostModifyEvent postModifyEvent = mock(CatalogPostModifyEvent.class);
        when(postModifyEvent.getSource()).thenReturn(mockLayerInfo);

        listener.handlePostModifyEvent(postModifyEvent);

        verify(mockConfiguration).truncate(eq(PREFIXED_RESOURCE_NAME), eq(oldName));
        verify(mockConfiguration).save(same(tileLayer));
    }

    public void testLayerInfoAlternateStylesChanged() throws Exception {

        StyleInfo removedStyle = mock(StyleInfo.class);
        when(removedStyle.getName()).thenReturn("removedStyleName");
        final Set<StyleInfo> oldStyles = Collections.singleton(removedStyle);

        StyleInfo addedStyle = mock(StyleInfo.class);
        when(addedStyle.getName()).thenReturn("addedStyleName");
        final Set<StyleInfo> newStyles = Collections.singleton(addedStyle);

        CatalogModifyEvent modifyEvent = mock(CatalogModifyEvent.class);
        when(modifyEvent.getSource()).thenReturn(mockLayerInfo);
        when(modifyEvent.getPropertyNames()).thenReturn(Arrays.asList("styles"));
        when(modifyEvent.getOldValues()).thenReturn(Collections.singletonList((Object) oldStyles));
        when(modifyEvent.getNewValues()).thenReturn(Collections.singletonList((Object) newStyles));

        GeoServerTileLayerInfo info = mock(GeoServerTileLayerInfo.class);
        when(info.getCachedStyles()).thenReturn(
                new HashSet<String>(Arrays.asList("remainingStyle", "removedStyleName")));
        when(info.isAutoCacheStyles()).thenReturn(true);

        GeoServerTileLayer tileLayer = mock(GeoServerTileLayer.class);
        when(tileLayer.getInfo()).thenReturn(info);

        when(mockConfiguration.getTileLayer(eq(PREFIXED_RESOURCE_NAME))).thenReturn(tileLayer);

        listener.handleModifyEvent(modifyEvent);

        CatalogPostModifyEvent postModifyEvent = mock(CatalogPostModifyEvent.class);
        when(postModifyEvent.getSource()).thenReturn(mockLayerInfo);

        listener.handlePostModifyEvent(postModifyEvent);

        verify(mockConfiguration).truncate(eq(PREFIXED_RESOURCE_NAME), eq("removedStyleName"));
        verify(mockConfiguration).save(same(tileLayer));
    }
}

/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.notification;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.beanutils.BeanToPropertyValueTransformer;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.notification.common.Notification;
import org.geoserver.notification.common.Notification.Action;
import org.geoserver.notification.common.Notification.Type;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class NotificationCatalogListener extends NotificationListener
        implements INotificationCatalogListener {

    private Boolean filterEvent(CatalogInfo source) {
        return (source instanceof WorkspaceInfo
                || source instanceof NamespaceInfo
                || source instanceof FeatureTypeInfo
                || source instanceof CoverageInfo
                || source instanceof WMSLayerInfo
                || source instanceof StoreInfo
                || source instanceof LayerInfo
                || source instanceof LayerGroupInfo);
    }

    @Override
    public void handleRemoveEvent(CatalogRemoveEvent event) throws CatalogException {
        if (filterEvent(event.getSource())) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String user = (auth != null) ? auth.getName() : null;
            CatalogInfo info = ModificationProxy.unwrap(event.getSource());
            Notification notification =
                    new NotificationImpl(
                            Type.Catalog,
                            event.getSource().getId(),
                            Action.Remove,
                            info,
                            null,
                            user);
            notify(notification);
        }
    }

    @Override
    public void handleModifyEvent(CatalogModifyEvent event) throws CatalogException {
        if (filterEvent(event.getSource())) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String user = (auth != null) ? auth.getName() : null;
            CatalogInfo info = ModificationProxy.unwrap(event.getSource());
            Notification notification =
                    new NotificationImpl(
                            Type.Catalog,
                            event.getSource().getId(),
                            Action.Update,
                            info,
                            handleModifiedProperties(event),
                            user);
            notify(notification);
        }
    }

    @Override
    public void handleAddEvent(CatalogAddEvent event) throws CatalogException {
        if (filterEvent(event.getSource())) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String user = (auth != null) ? auth.getName() : null;
            CatalogInfo info = ModificationProxy.unwrap(event.getSource());
            Notification notification =
                    new NotificationImpl(
                            Type.Catalog, event.getSource().getId(), Action.Add, info, null, user);
            notify(notification);
        }
    }

    private Map<String, Object> handleModifiedProperties(CatalogModifyEvent event) {
        final Map<String, Object> properties = new HashMap<String, Object>();
        final CatalogInfo source = event.getSource();
        final List<String> changedProperties = event.getPropertyNames();
        final List<Object> oldValues = event.getOldValues();
        final List<Object> newValues = event.getNewValues();
        if (source instanceof FeatureTypeInfo
                || source instanceof CoverageInfo
                || source instanceof WMSLayerInfo
                || source instanceof LayerGroupInfo) {
            if (changedProperties.contains("name")
                    || changedProperties.contains("namespace")
                    || changedProperties.contains("workspace")) {
                handleRename(properties, source, changedProperties, oldValues, newValues);
            }
        } else if (source instanceof WorkspaceInfo) {
            if (changedProperties.contains("name")) {
                handleWorkspaceRename(properties, source, changedProperties, oldValues, newValues);
            }
        }
        if (source instanceof LayerInfo) {
            final LayerInfo li = (LayerInfo) source;
            handleLayerInfoChange(properties, changedProperties, oldValues, newValues, li);
        } else if (source instanceof LayerGroupInfo) {
            LayerGroupInfo lgInfo = (LayerGroupInfo) source;
            handleLayerGroupInfoChange(properties, changedProperties, oldValues, newValues, lgInfo);
        }
        return properties;
    }

    private void handleLayerGroupInfoChange(
            Map<String, Object> properties,
            final List<String> changedProperties,
            final List<Object> oldValues,
            final List<Object> newValues,
            final LayerGroupInfo lgInfo) {

        if (changedProperties.contains("layers")) {
            final int layersIndex = changedProperties.indexOf("layers");
            Object oldLayers = oldValues.get(layersIndex);
            Object newLayers = newValues.get(layersIndex);
        }

        if (changedProperties.contains("styles")) {
            final int stylesIndex = changedProperties.indexOf("styles");
            BeanToPropertyValueTransformer transformer = new BeanToPropertyValueTransformer("name");
            String oldStyles =
                    StringUtils.join(
                            CollectionUtils.collect(
                                            (Set<StyleInfo>) oldValues.get(stylesIndex),
                                            transformer)
                                    .toArray());
            String newStyles =
                    StringUtils.join(
                            CollectionUtils.collect(
                                            (Set<StyleInfo>) newValues.get(stylesIndex),
                                            transformer)
                                    .toArray());
            if (!oldStyles.equals(newStyles)) {
                properties.put("styles", newStyles);
            }
        }
    }

    private void handleLayerInfoChange(
            Map<String, Object> properties,
            final List<String> changedProperties,
            final List<Object> oldValues,
            final List<Object> newValues,
            final LayerInfo li) {

        if (changedProperties.contains("defaultStyle")) {
            final int propIndex = changedProperties.indexOf("defaultStyle");
            final StyleInfo oldStyle = (StyleInfo) oldValues.get(propIndex);
            final StyleInfo newStyle = (StyleInfo) newValues.get(propIndex);

            final String oldStyleName = oldStyle.prefixedName();
            final String newStyleName = newStyle.prefixedName();
            if (!oldStyleName.equals(newStyleName)) {
                properties.put("defaultStyle", newStyleName);
            }
        }

        if (changedProperties.contains("styles")) {
            final int stylesIndex = changedProperties.indexOf("styles");
            BeanToPropertyValueTransformer transformer = new BeanToPropertyValueTransformer("name");
            String oldStyles =
                    StringUtils.join(
                            CollectionUtils.collect(
                                            (Set<StyleInfo>) oldValues.get(stylesIndex),
                                            transformer)
                                    .toArray());
            String newStyles =
                    StringUtils.join(
                            CollectionUtils.collect(
                                            (Set<StyleInfo>) newValues.get(stylesIndex),
                                            transformer)
                                    .toArray());
            if (!oldStyles.equals(newStyles)) {
                properties.put("styles", newStyles);
            }
        }
    }

    private void handleWorkspaceRename(
            Map<String, Object> properties,
            final CatalogInfo source,
            final List<String> changedProperties,
            final List<Object> oldValues,
            final List<Object> newValues) {
        final int nameIndex = changedProperties.indexOf("name");
        final String oldWorkspaceName = (String) oldValues.get(nameIndex);
        final String newWorkspaceName = (String) newValues.get(nameIndex);
    }

    private void handleRename(
            Map<String, Object> properties,
            final CatalogInfo source,
            final List<String> changedProperties,
            final List<Object> oldValues,
            final List<Object> newValues) {

        final int nameIndex = changedProperties.indexOf("name");
        final int namespaceIndex = changedProperties.indexOf("namespace");

        String oldLayerName;
        String newLayerName;
        if (source instanceof ResourceInfo) { // covers LayerInfo, CoverageInfo, and WMSLayerInfo
            // must cover prefix:name
            final ResourceInfo resourceInfo = (ResourceInfo) source;
            final NamespaceInfo currNamespace = resourceInfo.getNamespace();
            final NamespaceInfo oldNamespace;
            if (namespaceIndex > -1) {
                oldNamespace = (NamespaceInfo) oldValues.get(namespaceIndex);
            } else {
                oldNamespace = currNamespace;
            }

            newLayerName = resourceInfo.prefixedName();
            if (nameIndex > -1) {
                oldLayerName = (String) oldValues.get(nameIndex);
            } else {
                oldLayerName = resourceInfo.getName();
            }
            oldLayerName = oldNamespace.getPrefix() + ":" + oldLayerName;
        }
    }

    @Override
    public void setMessageMultiplexer(MessageMultiplexer messageMultiplexer) {
        this.messageMultiplexer = messageMultiplexer;
    }

    @Override
    public MessageMultiplexer getMessageMultiplexer() {
        return messageMultiplexer;
    }

    @Override
    public void handlePostModifyEvent(CatalogPostModifyEvent event) throws CatalogException {
        int a = 1;
    }

    @Override
    public void reloaded() {}
}

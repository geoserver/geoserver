/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.notification.geonode;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.net.InetAddress;
import java.rmi.server.UID;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import org.apache.commons.beanutils.BeanToPropertyValueTransformer;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.notification.common.Bounds;
import org.geoserver.notification.common.Notification;
import org.geoserver.notification.common.NotificationEncoder;
import org.geoserver.notification.geonode.kombu.KombuCoverageInfo;
import org.geoserver.notification.geonode.kombu.KombuFeatureTypeInfo;
import org.geoserver.notification.geonode.kombu.KombuLayerGroupInfo;
import org.geoserver.notification.geonode.kombu.KombuLayerInfo;
import org.geoserver.notification.geonode.kombu.KombuLayerSimpleInfo;
import org.geoserver.notification.geonode.kombu.KombuMessage;
import org.geoserver.notification.geonode.kombu.KombuNamespaceInfo;
import org.geoserver.notification.geonode.kombu.KombuResourceInfo;
import org.geoserver.notification.geonode.kombu.KombuStoreInfo;
import org.geoserver.notification.geonode.kombu.KombuWMSLayerInfo;
import org.geoserver.notification.geonode.kombu.KombuWorkspaceInfo;

public class GeoNodeJsonEncoder implements NotificationEncoder {

    @Override
    public byte[] encode(Notification notification) throws Exception {
        byte[] ret = null;

        ObjectMapper mapper = new ObjectMapper();
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'"));
        mapper.setVisibility(PropertyAccessor.ALL, Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
        mapper.setSerializationInclusion(Include.NON_NULL);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        KombuMessage message = new KombuMessage();

        message.setId(new UID().toString());
        message.setType(notification.getType() != null ? notification.getType().name() : null);
        message.setAction(
                notification.getAction() != null ? notification.getAction().name() : null);
        message.setTimestamp(new Date());
        message.setUser(notification.getUser());
        message.setOriginator(InetAddress.getLocalHost().getHostAddress());
        message.setProperties(notification.getProperties());
        if (notification.getObject() instanceof NamespaceInfo) {
            NamespaceInfo obj = (NamespaceInfo) notification.getObject();
            KombuNamespaceInfo source = new KombuNamespaceInfo();
            source.setId(obj.getId());
            source.setType("NamespaceInfo");
            source.setName(obj.getName());
            source.setNamespaceURI(obj.getURI());
            message.setSource(source);
        }
        if (notification.getObject() instanceof WorkspaceInfo) {
            WorkspaceInfo obj = (WorkspaceInfo) notification.getObject();
            KombuWorkspaceInfo source = new KombuWorkspaceInfo();
            source.setId(obj.getId());
            source.setType("WorkspaceInfo");
            source.setName(obj.getName());
            source.setNamespaceURI("");
            message.setSource(source);
        }
        if (notification.getObject() instanceof LayerInfo) {
            LayerInfo obj = (LayerInfo) notification.getObject();
            KombuLayerInfo source = new KombuLayerInfo();
            source.setId(obj.getId());
            source.setType("LayerInfo");
            source.setName(obj.getName());
            source.setResourceType(obj.getType() != null ? obj.getType().name() : "");
            BeanToPropertyValueTransformer transformer = new BeanToPropertyValueTransformer("name");
            Collection<String> styleNames = CollectionUtils.collect(obj.getStyles(), transformer);
            source.setStyles(StringUtils.join(styleNames.toArray()));
            source.setDefaultStyle(
                    obj.getDefaultStyle() != null ? obj.getDefaultStyle().getName() : "");
            ResourceInfo res = obj.getResource();
            source.setWorkspace(
                    res.getStore() != null
                            ? res.getStore().getWorkspace() != null
                                    ? res.getStore().getWorkspace().getName()
                                    : ""
                            : "");
            if (res.getNativeBoundingBox() != null) {
                source.setBounds(new Bounds(res.getNativeBoundingBox()));
            }
            if (res.getLatLonBoundingBox() != null) {
                source.setGeographicBunds(new Bounds(res.getLatLonBoundingBox()));
            }
            message.setSource(source);
        }
        if (notification.getObject() instanceof LayerGroupInfo) {
            LayerGroupInfo obj = (LayerGroupInfo) notification.getObject();
            KombuLayerGroupInfo source = new KombuLayerGroupInfo();
            source.setId(obj.getId());
            source.setType("LayerGroupInfo");
            source.setName(obj.getName());
            source.setWorkspace(obj.getWorkspace() != null ? obj.getWorkspace().getName() : "");
            source.setMode(obj.getType().name());
            String rootStyle =
                    obj.getRootLayerStyle() != null ? obj.getRootLayerStyle().getName() : "";
            source.setRootLayerStyle(rootStyle);
            source.setRootLayer(obj.getRootLayer() != null ? obj.getRootLayer().getPath() : "");
            for (PublishedInfo pl : obj.getLayers()) {
                KombuLayerSimpleInfo kl = new KombuLayerSimpleInfo();
                if (pl instanceof LayerInfo) {
                    LayerInfo li = (LayerInfo) pl;
                    kl.setName(li.getName());
                    String lstyle =
                            li.getDefaultStyle() != null ? li.getDefaultStyle().getName() : "";
                    if (!lstyle.equals(rootStyle)) {
                        kl.setStyle(lstyle);
                    }
                    source.addLayer(kl);
                }
            }
            message.setSource(source);
        }
        if (notification.getObject() instanceof ResourceInfo) {
            ResourceInfo obj = (ResourceInfo) notification.getObject();
            KombuResourceInfo source = null;
            if (notification.getObject() instanceof FeatureTypeInfo) {
                source = new KombuFeatureTypeInfo();
                source.setType("FeatureTypeInfo");
            }
            if (notification.getObject() instanceof CoverageInfo) {
                source = new KombuCoverageInfo();
                source.setType("CoverageInfo");
            }
            if (notification.getObject() instanceof WMSLayerInfo) {
                source = new KombuWMSLayerInfo();
                source.setType("WMSLayerInfo");
            }
            if (source != null) {
                source.setId(obj.getId());
                source.setName(obj.getName());
                source.setWorkspace(
                        obj.getStore() != null
                                ? obj.getStore().getWorkspace() != null
                                        ? obj.getStore().getWorkspace().getName()
                                        : ""
                                : "");
                source.setNativeName(obj.getNativeName());
                source.setStore(obj.getStore() != null ? obj.getStore().getName() : "");
                if (obj.getNativeBoundingBox() != null) {
                    source.setGeographicBunds(new Bounds(obj.getNativeBoundingBox()));
                }
                if (obj.boundingBox() != null) {
                    source.setBounds(new Bounds(obj.boundingBox()));
                }
            }
            message.setSource(source);
        }
        if (notification.getObject() instanceof StoreInfo) {
            StoreInfo obj = (StoreInfo) notification.getObject();
            KombuStoreInfo source = new KombuStoreInfo();
            source.setId(obj.getId());
            source.setType("StoreInfo");
            source.setName(obj.getName());
            source.setWorkspace(obj.getWorkspace() != null ? obj.getWorkspace().getName() : "");
            message.setSource(source);
        }
        ret = mapper.writeValueAsBytes(message);
        return ret;
    }
}

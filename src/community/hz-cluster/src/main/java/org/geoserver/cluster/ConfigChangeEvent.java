/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 Boundless
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster;

import static com.google.common.base.Objects.equal;

import com.google.common.base.Objects;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.geoserver.catalog.AttributionInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.AttributionInfoImpl;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.impl.CoverageInfoImpl;
import org.geoserver.catalog.impl.CoverageStoreInfoImpl;
import org.geoserver.catalog.impl.DataStoreInfoImpl;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.catalog.impl.LayerGroupInfoImpl;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.catalog.impl.MetadataLinkInfoImpl;
import org.geoserver.catalog.impl.NamespaceInfoImpl;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geoserver.catalog.impl.WMSLayerInfoImpl;
import org.geoserver.catalog.impl.WMSStoreInfoImpl;
import org.geoserver.catalog.impl.WMTSLayerInfoImpl;
import org.geoserver.catalog.impl.WMTSStoreInfoImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.config.ContactInfo;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.impl.ContactInfoImpl;
import org.geoserver.config.impl.GeoServerInfoImpl;
import org.geoserver.config.impl.LoggingInfoImpl;
import org.geoserver.config.impl.SettingsInfoImpl;

/**
 * Event for
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class ConfigChangeEvent extends Event {

    static Map<Class<? extends Info>, Class<? extends Info>> INTERFACES =
            new HashMap<Class<? extends Info>, Class<? extends Info>>();

    static {
        INTERFACES.put(GeoServerInfoImpl.class, GeoServerInfo.class);
        INTERFACES.put(SettingsInfoImpl.class, SettingsInfo.class);
        INTERFACES.put(LoggingInfoImpl.class, LoggingInfo.class);
        INTERFACES.put(ContactInfoImpl.class, ContactInfo.class);
        INTERFACES.put(AttributionInfoImpl.class, AttributionInfo.class);

        // catalog
        INTERFACES.put(CatalogImpl.class, Catalog.class);
        INTERFACES.put(NamespaceInfoImpl.class, NamespaceInfo.class);
        INTERFACES.put(WorkspaceInfoImpl.class, WorkspaceInfo.class);
        INTERFACES.put(DataStoreInfoImpl.class, DataStoreInfo.class);
        INTERFACES.put(WMSStoreInfoImpl.class, WMSStoreInfo.class);
        INTERFACES.put(WMTSStoreInfoImpl.class, WMTSStoreInfo.class);
        INTERFACES.put(CoverageStoreInfoImpl.class, CoverageStoreInfo.class);
        INTERFACES.put(StyleInfoImpl.class, StyleInfo.class);
        INTERFACES.put(FeatureTypeInfoImpl.class, FeatureTypeInfo.class);
        INTERFACES.put(CoverageInfoImpl.class, CoverageInfo.class);
        INTERFACES.put(WMSLayerInfoImpl.class, WMSLayerInfo.class);
        INTERFACES.put(WMTSLayerInfoImpl.class, WMTSLayerInfo.class);
        INTERFACES.put(MetadataLinkInfoImpl.class, MetadataLinkInfo.class);
        INTERFACES.put(LayerInfoImpl.class, LayerInfo.class);
        INTERFACES.put(LayerGroupInfoImpl.class, LayerGroupInfo.class);
    }

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    public enum Type {
        ADD,
        REMOVE,
        MODIFY,
        POST_MODIFY
    }

    /** id of object */
    String id;

    /** name of object */
    String name;

    /** name of workspace qualifying the object */
    String workspaceId;

    /** id of Store object if the modified object was a Resource */
    String storeId;

    /** class of object */
    Class<? extends Info> clazz;

    /** type of config change */
    Type type;

    List<String> propertyNames;

    List<Object> oldValues;

    List<Object> newValues;

    private String nativeName;

    public ConfigChangeEvent(String id, String name, Class<? extends Info> clazz, Type type) {
        super();
        this.id = id;
        this.name = name;
        this.clazz = clazz;
        this.type = type;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(String.valueOf(type)).append(" ");

        Serializable source = getSource();
        if (source != null) {
            sb.append('(').append(source).append(") ");
        }

        sb.append("[uuid:")
                .append(getUUID())
                .append(", object id:")
                .append(id)
                .append(", name:")
                .append(name)
                .append("]");
        return sb.toString();
    }

    /**
     * Equals is based on {@link #getObjectId() id}, {@link #getObjectName() name}, and {@link
     * #getChangeType() changeType}. {@link #getObjectClass() class} is left off because it can be a
     * proxy class and id/name/type are good enough anyways (given ids are unique, no two objects of
     * different class can have the same id).
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ConfigChangeEvent)) {
            return false;
        }
        ConfigChangeEvent e = (ConfigChangeEvent) o;
        return equal(id, e.id) && equal(type, e.type);
    }

    /**
     * Hash code is based on {@link #getObjectId() id}, {@link #getObjectName() name}, and {@link
     * #getChangeType() changeType}. {@link #getObjectClass() class} is left off because it can be a
     * proxy class and id/name/type are good enough anyways (given ids are unique, no two objects of
     * different class can have the same id).
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(ConfigChangeEvent.class, id, name, type);
    }

    public String getObjectId() {
        return id;
    }

    public String getObjectName() {
        return name;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public Class<? extends Info> getObjectClass() {
        return clazz;
    }

    public Class<? extends Info> getObjectInterface() {
        Class<? extends Info> clazz = INTERFACES.get(getObjectClass());

        // There are several different ServiceInfo subtypes and it's an extension point
        // so don't check for specific classes
        if (clazz == null && ServiceInfo.class.isAssignableFrom(getObjectClass())) {
            clazz = ServiceInfo.class;
        }

        // Fall back, mostly here to support EasyMock test objects in unit tests.
        if (clazz == null) {
            for (Class<? extends Info> realClazz : INTERFACES.values()) {
                if (realClazz.isAssignableFrom(getObjectClass())) {
                    clazz = realClazz;
                    break;
                }
            }
        }

        return clazz;
    }

    public Type getChangeType() {
        return type;
    }

    public void setNativeName(String nativeName) {
        this.nativeName = nativeName;
    }

    @Nullable
    public String getNativeName() {
        return nativeName;
    }

    public List<String> getPropertyNames() {
        return propertyNames;
    }

    public void setPropertyNames(List<String> propertyNames) {
        this.propertyNames = propertyNames;
    }

    public List<Object> getOldValues() {
        return oldValues;
    }

    public void setOldValues(List<Object> oldValues) {
        this.oldValues = oldValues;
    }

    public List<Object> getNewValues() {
        return newValues;
    }

    public void setNewValues(List<Object> newValues) {
        this.newValues = newValues;
    }
}

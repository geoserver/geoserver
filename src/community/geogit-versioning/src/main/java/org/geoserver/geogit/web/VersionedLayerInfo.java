/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geogit.web;

import java.io.Serializable;

import org.geoserver.catalog.FeatureTypeInfo;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.feature.NameImpl;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.GeometryType;
import org.opengis.feature.type.Name;

import com.vividsolutions.jts.geom.Geometry;

public class VersionedLayerInfo implements Comparable<VersionedLayerInfo>, Serializable {

    private static final long serialVersionUID = 7737632010950106656L;

    private final String ns;

    private final String name;

    private boolean published;

    private boolean readOnly;

    private Class<? extends Geometry> geometryType;

    private boolean enabled;

    private String errorMessage;

    public VersionedLayerInfo(final Name typeName) {
        this.ns = typeName.getNamespaceURI();
        this.name = typeName.getLocalPart();
        this.geometryType = null;
        this.enabled = false;
        this.errorMessage = "Feature type does not exist in Catalog";
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public VersionedLayerInfo(final FeatureTypeInfo featureType) {
        final Name qualifiedName = featureType.getQualifiedName();
        this.ns = qualifiedName.getNamespaceURI();
        this.name = qualifiedName.getLocalPart();
        this.enabled = featureType.enabled();
        if (!this.enabled) {
            errorMessage = "FeatureType is disabled in Catalog";
        }
        GeometryDescriptor geometryDescriptor;
        if (enabled) {
            try {
                geometryDescriptor = featureType.getFeatureType().getGeometryDescriptor();
                FeatureSource featureSource = featureType.getFeatureSource(null, null);
                this.readOnly = !(featureSource instanceof FeatureStore);
                if (geometryDescriptor != null) {
                    GeometryType type = geometryDescriptor.getType();
                    this.geometryType = (Class<? extends Geometry>) type.getBinding();
                }
            } catch (Exception e) {
                enabled = false;
                errorMessage = e.getMessage();
            }
        }
    }

    /**
     * @return the isInError
     */
    public boolean isInError() {
        return enabled;
    }

    /**
     * @return the errorMessage
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    public Name getName() {
        return new NameImpl(ns, name);
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    /**
     * Read only feature types (those that don't resolve to a {@link FeatureStore}) can't be
     * versioned.
     * 
     * @return whether it's a read only FeatureType
     */
    public boolean isReadOnly() {
        return readOnly;
    }

    /**
     * @param readOnly
     *            the readOnly to set
     */
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    /**
     * @return the geometryType, or {@code null} if the FeatureType is geometry-less
     */
    public Class<? extends Geometry> getGeometryType() {
        return geometryType;
    }

    public int compareTo(VersionedLayerInfo o) {
        // unpublished resources first
        if (published && !o.published)
            return -1;
        else if (!published && o.published)
            return 1;
        // the compare by local name, as it's unlikely the users will see the
        // namespace URI (and the prefix is not available in Name)
        return name.compareTo(o.name);
    }

    public String toString() {
        return getClass().getSimpleName() + "[" + name + "]";
    }
}
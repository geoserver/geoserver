/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import org.geotools.api.data.Query;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.factory.CommonFactoryFinder;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;

/**
 * Describes the access limits on a vector layer
 *
 * @author Andrea Aime - GeoSolutions
 */
public class VectorAccessLimits extends DataAccessLimits {
    @Serial
    private static final long serialVersionUID = 1646981660625898503L;

    private static FilterFactory FF = CommonFactoryFinder.getFilterFactory(null);

    /** The list of attributes the user is allowed to read (will be band names for raster data) */
    transient List<PropertyName> readAttributes;

    /** The set of attributes the user is allowed to write on */
    transient List<PropertyName> writeAttributes;

    /** Limits the features that can actually be written */
    transient Filter writeFilter;

    Geometry clipVectorFilter;

    Geometry intersectVectorFilter;

    /**
     * Builds a new vector access limits
     *
     * @param readAttributes The list of attributes that can be read
     * @param readFilter Only matching features will be returned to the user
     * @param writeAttributes The list of attributes that can be modified
     * @param writeFilter Only matching features will be allowed to be created/modified/deleted
     */
    public VectorAccessLimits(
            CatalogMode mode,
            List<PropertyName> readAttributes,
            Filter readFilter,
            List<PropertyName> writeAttributes,
            Filter writeFilter,
            MultiPolygon clipVectorFilter) {
        super(mode, readFilter);
        this.readAttributes = readAttributes;
        this.writeAttributes = writeAttributes;
        this.writeFilter = writeFilter;
        this.clipVectorFilter = clipVectorFilter;
    }

    public VectorAccessLimits(
            CatalogMode mode,
            List<PropertyName> readAttributes,
            Filter readFilter,
            List<PropertyName> writeAttributes,
            Filter writeFilter) {
        this(mode, readAttributes, readFilter, writeAttributes, writeFilter, null);
    }

    /** The list of attributes the user is allowed to read */
    public List<PropertyName> getReadAttributes() {
        return readAttributes;
    }

    /**
     * The list of attributes the user is allowed to read
     *
     * @param readAttributes the list of attributes the user is allowed to read
     */
    public void setReadAttributes(List<PropertyName> readAttributes) {
        this.readAttributes = readAttributes;
    }

    /** The list of attributes the user is allowed to write */
    public List<PropertyName> getWriteAttributes() {
        return writeAttributes;
    }

    /**
     * Sets the list of attributes the user is allowed to write
     *
     * @param writeAttributes
     */
    public void setWriteAttributes(List<PropertyName> writeAttributes) {
        this.writeAttributes = writeAttributes;
    }

    /**
     * Sets the filter that limits the features the user can write onto
     *
     * @param writeFilter
     */
    public void setWriteFilter(Filter writeFilter) {
        this.writeFilter = writeFilter;
    }

    /** Identifies the features the user can write onto */
    public Filter getWriteFilter() {
        return writeFilter;
    }

    /** Returns a GeoTools query wrapping the read attributes and the read filter */
    public Query getReadQuery() {
        return buildQuery(readAttributes, readFilter);
    }

    /** Returns a GeoTools query wrapping the write attributes and the write filter */
    public Query getWriteQuery() {
        return buildQuery(writeAttributes, writeFilter);
    }

    /** Returns a GeoTools query build with the provided attributes and filters */
    private Query buildQuery(List<PropertyName> attributes, Filter filter) {
        if (attributes == null && (filter == null || filter == Filter.INCLUDE)) {
            return Query.ALL;
        } else {
            Query q = new Query();
            q.setFilter(filter);
            // TODO: switch this to property names when possible
            q.setPropertyNames(flattenNames(attributes));
            return q;
        }
    }

    /** Turns a list of {@link PropertyName} into a list of {@link String} */
    List<String> flattenNames(List<PropertyName> names) {
        if (names == null) {
            return null;
        }

        List<String> result = new ArrayList<>(names.size());
        for (PropertyName name : names) {
            result.add(name.getPropertyName());
        }

        return result;
    }

    @Override
    public String toString() {
        return "VectorAccessLimits [readAttributes="
                + readAttributes
                + ", writeAttributes="
                + writeAttributes
                + ", writeFilter="
                + writeFilter
                + ", readFilter="
                + readFilter
                + ", mode="
                + mode
                + "]";
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        readAttributes = readProperties(in);
        readFilter = readFilter(in);
        writeAttributes = readProperties(in);
        writeFilter = readFilter(in);
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        writeProperties(readAttributes, out);
        writeFilter(readFilter, out);
        writeProperties(writeAttributes, out);
        writeFilter(writeFilter, out);
    }

    private void writeProperties(List<PropertyName> attributes, ObjectOutputStream oos) throws IOException {
        if (attributes == null) {
            oos.writeInt(-1);
        } else {
            oos.writeInt(attributes.size());
            for (PropertyName property : attributes) {
                oos.writeObject(property.getPropertyName());
                // TODO: write out the namespace support as well
            }
        }
    }

    private List<PropertyName> readProperties(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        int size = ois.readInt();
        if (size == -1) {
            return null;
        } else {
            List<PropertyName> properties = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                String name = (String) ois.readObject();
                properties.add(FF.property(name));
                // TODO: read out the namespace support as well
            }
            return properties;
        }
    }

    public Geometry getClipVectorFilter() {
        return clipVectorFilter;
    }

    public void setClipVectorFilter(Geometry clipVectorFilter) {
        this.clipVectorFilter = clipVectorFilter;
    }

    public Geometry getIntersectVectorFilter() {
        return intersectVectorFilter;
    }

    public void setIntersectVectorFilter(Geometry intersectVectorFilter) {
        this.intersectVectorFilter = intersectVectorFilter;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((readAttributes == null) ? 0 : readAttributes.hashCode());
        result = prime * result + ((writeAttributes == null) ? 0 : writeAttributes.hashCode());
        result = prime * result + ((writeFilter == null) ? 0 : writeFilter.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (getClass() != obj.getClass()) return false;
        VectorAccessLimits other = (VectorAccessLimits) obj;
        if (readAttributes == null) {
            if (other.readAttributes != null) return false;
        } else if (!readAttributes.equals(other.readAttributes)) return false;
        if (writeAttributes == null) {
            if (other.writeAttributes != null) return false;
        } else if (!writeAttributes.equals(other.writeAttributes)) return false;
        if (writeFilter == null) {
            if (other.writeFilter != null) return false;
        } else if (!writeFilter.equals(other.writeFilter)) return false;
        return true;
    }
}

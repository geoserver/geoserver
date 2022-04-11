/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import org.geotools.filter.text.ecql.ECQL;
import org.opengis.filter.Filter;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Base class for all AccessLimits declared by a {@link ResourceAccessManager}
 *
 * @author Andrea Aime - GeoSolutions
 */
public class DataAccessLimits extends AccessLimits {

    private static final long serialVersionUID = 2594922992934373705L;

    /**
     * Used for vector reading, for raster if there is a read param taking an OGC filter, and in WMS
     * if the remote server supports CQL filters and on feature info requests. For workspaces it
     * will be just INCLUDE or EXCLUDE to allow or deny access to the workspace
     */
    transient Filter readFilter;

    /**
     * Builds a generic DataAccessLimits
     *
     * @param readFilter This filter will be merged with the request read filters to limit the
     *     features/tiles that can be actually read
     */
    public DataAccessLimits(CatalogMode mode, Filter readFilter) {
        super(mode);
        this.readFilter = readFilter;
    }

    /**
     * This filter will be merged with the request read filters to limit the features/tiles that can
     * be actually read
     */
    public Filter getReadFilter() {
        return readFilter;
    }

    /** The catalog mode for this layer */
    @Override
    public CatalogMode getMode() {
        return mode;
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        readFilter = readFilter(in);
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        writeFilter(readFilter, out);
    }

    /**
     * Writes the non Serializable Filter object to the ObjectOutputStream using ECQL
     * encoding conversion
     */
    protected void writeFilter(Filter filter, ObjectOutputStream out) throws IOException {
        if (filter != null) {
            out.writeObject(ECQL.toCQL(filter));
        } else {
            out.writeObject(null);
        }
    }

    /**
     * Reads from the object input stream a string representing a filter in OGC XML encoding and
     * parses it back to a Filter object
     */
    protected Filter readFilter(ObjectInputStream in) throws IOException, ClassNotFoundException {
        String serializedReadFilter = (String) in.readObject();
        if (serializedReadFilter != null) {
            try {
                return ECQL.toFilter(serializedReadFilter);
            } catch (Exception e) {
                throw (IOException) new IOException("Failed to parse filter").initCause(e);
            }
        } else {
            return null;
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((readFilter == null) ? 0 : readFilter.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (getClass() != obj.getClass()) return false;
        DataAccessLimits other = (DataAccessLimits) obj;
        if (readFilter == null) {
            if (other.readFilter != null) return false;
        } else if (!readFilter.equals(other.readFilter)) return false;
        return true;
    }
}

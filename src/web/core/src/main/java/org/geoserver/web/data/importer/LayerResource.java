/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.importer;

import java.io.Serializable;
import org.geotools.feature.NameImpl;
import org.opengis.feature.type.Name;

/**
 * The bean to be rendered in the wms mass publisher page
 *
 * @author Andrea Aime - OpenGeo
 */
public class LayerResource implements Comparable<LayerResource>, Serializable {

    private static final long serialVersionUID = 7584589248746230483L;

    enum LayerStatus {
        ERROR,
        NEWLY_PUBLISHED,
        UPDATED,
        NEW,
        PUBLISHED
    };

    /** The resource name */
    String name;

    String uri;

    /** Status of the resource in the workflow */
    LayerStatus status = LayerStatus.NEW;

    /** The eventual import error */
    String error;

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public LayerResource(Name name) {
        super();
        this.name = name.getLocalPart();
        this.uri = name.getURI();
    }

    public String getLocalName() {
        return name;
    }

    public Name getName() {
        return new NameImpl(uri, name);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((error == null) ? 0 : error.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((status == null) ? 0 : status.hashCode());
        result = prime * result + ((uri == null) ? 0 : uri.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        LayerResource other = (LayerResource) obj;
        if (error == null) {
            if (other.error != null) return false;
        } else if (!error.equals(other.error)) return false;
        if (name == null) {
            if (other.name != null) return false;
        } else if (!name.equals(other.name)) return false;
        if (status == null) {
            if (other.status != null) return false;
        } else if (!status.equals(other.status)) return false;
        if (uri == null) {
            if (other.uri != null) return false;
        } else if (!uri.equals(other.uri)) return false;
        return true;
    }

    public int compareTo(LayerResource o) {
        // unpublished resources first
        if (status.compareTo(o.status) != 0) {
            return status.compareTo(o.status);
        }
        // the compare by local name, as it's unlikely the users will see the
        // namespace URI (and the prefix is not available in Name)
        return name.compareTo(o.name);
    }

    public LayerStatus getStatus() {
        return status;
    }

    public void setStatus(LayerStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return name + "(" + status + ")";
    }
}

/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.records;

import org.geoserver.csw.util.PropertyPath;

/**
 * A helper that builds CSW Dublin core records as GeoTools features
 *
 * @author Andrea Aime - GeoSolutions
 */
public class CSWRecordBuilder extends GenericRecordBuilder {

    public CSWRecordBuilder() {
        super(CSWRecordDescriptor.getInstance());
    }

    /** Adds an element to the current record */
    public void addElement(PropertyPath name, String... values) {
        super.addElement(name.add("value"), (Object[]) values);
    }

    /** Adds an element to the current record with scheme */
    public void addElementWithScheme(PropertyPath name, String scheme, String value) {
        super.addElement(name.add("value"), value);
        super.addElement(name.add("scheme"), scheme);
    }

    @Deprecated
    public void addElement(String name, String... values) {
        addElement(PropertyPath.fromDotPath(name), values);
    }

    @Deprecated
    public void addElementWithScheme(String name, String scheme, String value) {
        addElementWithScheme(PropertyPath.fromDotPath(name), scheme, value);
    }
}

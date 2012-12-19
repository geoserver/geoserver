/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.records;

/**
 * A helper that builds CSW Dublin core records as GeoTools features
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class CSWRecordBuilder extends GenericRecordBuilder{

    public CSWRecordBuilder() {
        super(CSWRecordDescriptor.getInstance());
    }
   
    /**
     * Adds an element to the current record
     * 
     * @param name
     * @param values
     */
    public void addElement(String name, String... values) {
        super.addElement(name + ".value", values);
    }

    /**
     * Adds an element to the current record with scheme
     * 
     * @param name
     * @param scheme
     * @param values
     */
    public void addElementWithScheme(String name, String scheme, String value) {
        super.addElement(name + ".value", value);
        super.addElement(name + ".scheme", scheme);
    }

}

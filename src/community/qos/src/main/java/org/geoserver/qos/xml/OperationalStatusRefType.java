/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.xml;

public class OperationalStatusRefType extends ReferenceType {

    public OperationalStatusRefType() {
        super();
    }

    public OperationalStatusRefType(String href, String title) {
        super(href, title);
    }

    @Override
    public void setTitle(String title) {
        super.setTitle(title);
        setHref(OperationalStatus.URL + title);
    }
}

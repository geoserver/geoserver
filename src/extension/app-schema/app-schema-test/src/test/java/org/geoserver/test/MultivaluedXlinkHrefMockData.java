/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

/**
 * Mock data for a multivalued xlink:href ClientProperty mapping without feature chaining.
 *
 * @author Ben Caradoc-Davies (Transient Software Limited)
 */
public class MultivaluedXlinkHrefMockData extends AbstractAppSchemaMockData {

    public MultivaluedXlinkHrefMockData() {
        super(GML32_NAMESPACES);
    }

    @Override
    public void addContent() {
        addFeatureType(
                GSML_PREFIX,
                "GeologicUnit",
                "MultivaluedXlinkHref.xml",
                "MultivaluedXlinkHref.properties");
    }
}

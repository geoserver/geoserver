/* (c) 2014-2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.response;

import static org.junit.Assert.assertEquals;

import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.impl.NamespaceInfoImpl;
import org.geoserver.wfs.WFSTestSupport;
import org.junit.Test;

public class CSVOutputFormatTest extends WFSTestSupport {

    @Test
    public void testResolvePrefixedAttributeNames() {
        NamespaceInfo nsInfo = new NamespaceInfoImpl();
        nsInfo.setPrefix("test-ns");
        nsInfo.setURI("http://test-ns/core");
        getGeoServer().getCatalog().add(nsInfo);
        CSVOutputFormat csvFormat = new CSVOutputFormat(getGeoServer());
        assertEquals(
                "test-ns:attributeName", csvFormat.resolveNamespacePrefixName("http://test-ns/core:attributeName"));
    }

    @Test
    public void testDontResolvePrefixedAttributeNames() {
        NamespaceInfo nsInfo = new NamespaceInfoImpl();
        nsInfo.setPrefix("test-ns2");
        nsInfo.setURI("http://test-ns2/core");
        getGeoServer().getCatalog().add(nsInfo);
        CSVOutputFormat csvFormat = new CSVOutputFormat(getGeoServer());
        assertEquals("test-ns2:attributeName", csvFormat.resolveNamespacePrefixName("test-ns2:attributeName"));
    }

    @Test
    public void testUnvalidResolvePrefixedAttributeNames() {
        CSVOutputFormat csvFormat = new CSVOutputFormat(getGeoServer());
        assertEquals("test:attributeName:", csvFormat.resolveNamespacePrefixName("test:attributeName:"));
    }
}

/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.capabilities;

import java.util.HashMap;

import junit.framework.TestCase;

import org.geoserver.wms.GetCapabilitiesRequest;
import org.geoserver.wms.WMS;

@SuppressWarnings("rawtypes")
public class CapabilitiesKvpReaderTest extends TestCase {

    private CapabilitiesKvpReader reader;

    private HashMap kvp;

    private HashMap rawKvp;

    public void setUp() {
        this.reader = new CapabilitiesKvpReader(new WMS(null));
        this.kvp = new HashMap();
        this.rawKvp = new HashMap();
    }

    @SuppressWarnings("unchecked")
    public void testDefault() throws Exception {
        rawKvp.put("request", "getcapabilities");
        kvp.put("request", "getcapabilities");
        GetCapabilitiesRequest read = reader.read(reader.createRequest(), kvp, rawKvp);
        assertNotNull(read);
        assertEquals("getcapabilities", read.getRequest().toLowerCase());
        assertNull(read.getBaseUrl());
        assertNull(read.getNamespace());
    }

    /**
     * 1.0 "WMTVER" parameter supplied instead of "VERSION"? Version negotiation should agree on
     * 1.1.1
     * 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public void testWMTVER() throws Exception {
        rawKvp.put("WMTVER", "1.0");

        GetCapabilitiesRequest read = reader.read(reader.createRequest(), kvp, rawKvp);
        assertNotNull(read);
        assertEquals("1.1.1", read.getVersion());
    }

    @SuppressWarnings("unchecked")
    public void testVersion() throws Exception {
        kvp.put("Version", "1.1.1");
        GetCapabilitiesRequest read = reader.read(reader.createRequest(), kvp, rawKvp);
        assertNotNull(read);
        assertEquals("1.1.1", read.getVersion());
    }

    @SuppressWarnings("unchecked")
    public void testNamespace() throws Exception {
        kvp.put("namespace", "og");
        GetCapabilitiesRequest read = reader.read(reader.createRequest(), kvp, rawKvp);
        assertNotNull(read);
        assertEquals("og", read.getNamespace());
    }

    @SuppressWarnings("unchecked")
    public void testUpdateSequence() throws Exception {
        kvp.put("updateSequence", "1000");
        GetCapabilitiesRequest read = reader.read(reader.createRequest(), kvp, rawKvp);
        assertNotNull(read);
        assertEquals("1000", read.getUpdateSequence());
    }
}

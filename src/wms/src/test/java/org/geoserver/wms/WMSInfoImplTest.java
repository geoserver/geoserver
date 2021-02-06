/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;

public class WMSInfoImplTest {

    @Test
    public void testSerialize() {
        WMSInfoImpl info = new WMSInfoImpl();
        info.setAbstract("test");

        byte[] data = SerializationUtils.serialize(info);
        Object o = SerializationUtils.deserialize(data);
        assertTrue(o instanceof WMSInfoImpl);
        assertEquals("test", ((WMSInfoImpl) o).getAbstract());
    }
}

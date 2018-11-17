/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import static org.easymock.EasyMock.*;

import java.io.InputStream;
import org.geoserver.wms.WMSMapContent;
import org.junit.Test;

public class RawMapTest {

    @Test
    public void testInputStream() throws Exception {
        InputStream stream = createMock(InputStream.class);
        expect(stream.read((byte[]) anyObject())).andReturn(-1).once();
        replay(stream);

        WMSMapContent map = createNiceMock(WMSMapContent.class);
        replay(map);

        new RawMap(map, stream, "text/plain").writeTo(null);
        verify(stream);
    }
}

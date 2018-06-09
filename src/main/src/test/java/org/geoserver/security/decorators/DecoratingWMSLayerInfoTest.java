package org.geoserver.security.decorators;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import junit.framework.TestCase;
import org.geoserver.catalog.WMSLayerInfo;

public class DecoratingWMSLayerInfoTest extends TestCase {
    public void testPrefixName() {
        // build up the mock
        WMSLayerInfo li = createNiceMock(WMSLayerInfo.class);

        expect(li.prefixedName()).andReturn("PREFIX");
        replay(li);

        DecoratingWMSLayerInfo ro = new DecoratingWMSLayerInfo(li);
        assertEquals("PREFIX", ro.prefixedName());
    }
}

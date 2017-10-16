
package org.geoserver.security.decorators;

import junit.framework.TestCase;
import org.geoserver.catalog.WMSLayerInfo;

import static org.easymock.EasyMock.*;

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

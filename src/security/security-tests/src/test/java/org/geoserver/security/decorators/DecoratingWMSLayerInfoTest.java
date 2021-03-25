/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.decorators;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import org.geoserver.catalog.WMSLayerInfo;
import org.junit.Assert;
import org.junit.Test;

public class DecoratingWMSLayerInfoTest {
    @Test
    public void testPrefixName() {
        // build up the mock
        WMSLayerInfo li = createNiceMock(WMSLayerInfo.class);

        expect(li.prefixedName()).andReturn("PREFIX");
        replay(li);

        DecoratingWMSLayerInfo ro = new DecoratingWMSLayerInfo(li);
        Assert.assertEquals("PREFIX", ro.prefixedName());
    }
}

/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */package org.geoserver.security.decorators;

import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.PublishedType;
import org.junit.Test;

import java.io.IOException;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;

public class DecoratingLayerGroupInfoTest {

    @Test
    public void testDelegateGetType() throws IOException {
        // build up the mock
        LayerGroupInfo lg = createNiceMock(LayerGroupInfo.class);
    
        expect(lg.getType()).andReturn(PublishedType.GROUP);
        replay(lg);
    
        DecoratingLayerGroupInfo decorator = new DecoratingLayerGroupInfo(lg);
        assertEquals(PublishedType.GROUP, decorator.getType());
    }
    
}

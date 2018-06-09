/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.easymock.EasyMock;
import org.geoserver.platform.ExtensionFilter;
import org.geoserver.platform.ExtensionProvider;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

/**
 * Unit test for {@link WMSExtensions}
 *
 * @author Gabriel Roldan (TOPP)
 * @version $Id$
 */
public class WMSExtensionsTest {

    @Test
    public void testFindMapProducers() {
        GetMapOutputFormat mockProducer1 = createMock(GetMapOutputFormat.class);
        GetMapOutputFormat mockProducer2 = createMock(GetMapOutputFormat.class);

        ApplicationContext mockContext = EasyMock.createMock(ApplicationContext.class);
        // I'm not so pleasant with this block of code as it implies knowing how
        // the internals of GeoServerExtensions work
        expect(mockContext.getBeanNamesForType(ExtensionFilter.class)).andReturn(new String[0]);
        expect(mockContext.getBeanNamesForType(GetMapOutputFormat.class))
                .andReturn(new String[] {"producer1", "producer2"});
        expect(mockContext.getBeanNamesForType(ExtensionProvider.class)).andReturn(new String[0]);
        expect(mockContext.getBeanNamesForType(ExtensionFilter.class)).andReturn(new String[0]);
        expect(mockContext.getBean("producer1")).andReturn(mockProducer1);
        expect(mockContext.getBean("producer2")).andReturn(mockProducer2);
        expect(mockContext.isSingleton((String) anyObject())).andReturn(false).anyTimes();
        // end of unpleasant block

        replay(mockContext);

        List<GetMapOutputFormat> mapProducers = WMSExtensions.findMapProducers(mockContext);
        assertNotNull(mapProducers);
        assertEquals(2, mapProducers.size());
        assertTrue(mapProducers.contains(mockProducer1));
        assertTrue(mapProducers.contains(mockProducer2));
    }

    @Test
    public void testFindMapProducersEmpty() {
        ApplicationContext mockContext = EasyMock.createMock(ApplicationContext.class);
        // I'm not so pleasant with this block of code as it implies knowing how
        // the internals of GeoServerExtensions work
        expect(mockContext.getBeanNamesForType(ExtensionFilter.class)).andReturn(new String[0]);
        expect(mockContext.getBeanNamesForType(GetMapOutputFormat.class))
                .andReturn(new String[] {});
        expect(mockContext.getBeanNamesForType(ExtensionProvider.class)).andReturn(new String[0]);
        expect(mockContext.getBeanNamesForType(ExtensionFilter.class)).andReturn(new String[0]);
        expect(mockContext.isSingleton((String) anyObject())).andReturn(false).anyTimes();
        // end of unpleasant block

        replay(mockContext);

        List<GetMapOutputFormat> mapProducers = WMSExtensions.findMapProducers(mockContext);
        assertNotNull(mapProducers);
        assertEquals(0, mapProducers.size());
    }

    @Test
    public void testFindMapProducer() {
        GetMapOutputFormat mockProducer = createMock(GetMapOutputFormat.class);

        ApplicationContext mockContext = EasyMock.createMock(ApplicationContext.class);
        // I'm not so pleasant with this block of code as it implies knowing how
        // the internals of GeoServerExtensions work
        expect(mockContext.isSingleton((String) anyObject())).andReturn(false).anyTimes();
        expect(mockContext.getBeanNamesForType(ExtensionFilter.class)).andReturn(new String[0]);
        expect(mockContext.getBeanNamesForType(GetMapOutputFormat.class))
                .andReturn(new String[] {"producer1"}); // call#1
        expect(mockContext.getBeanNamesForType(ExtensionProvider.class)).andReturn(new String[0]);
        expect(mockContext.getBeanNamesForType(ExtensionFilter.class)).andReturn(new String[0]);
        expect(mockContext.getBean("producer1")).andReturn(mockProducer); // call#1

        expect(mockContext.getBeanNamesForType(ExtensionFilter.class)).andReturn(new String[0]);
        expect(mockContext.getBeanNamesForType(GetMapOutputFormat.class))
                .andReturn(new String[] {"producer1"}); // call#2
        expect(mockContext.getBeanNamesForType(ExtensionProvider.class)).andReturn(new String[0]);
        expect(mockContext.getBeanNamesForType(ExtensionFilter.class)).andReturn(new String[0]);
        expect(mockContext.getBean("producer1")).andReturn(mockProducer); // call#2

        // end of unpleasant block

        Set<String> testFormatNames = new HashSet<String>();
        testFormatNames.add("image/fakeformat");
        testFormatNames.add("image/dummy");

        expect(mockProducer.getOutputFormatNames()).andReturn(testFormatNames).anyTimes();

        replay(mockContext);
        replay(mockProducer);

        // note the lookup shall be case insensitive..
        GetMapOutputFormat producer;
        producer = WMSExtensions.findMapProducer("ImaGe/FaKeForMat", mockContext); // call#1
        assertSame(mockProducer, producer);

        producer = WMSExtensions.findMapProducer("notARegisteredFormat", mockContext); // call#2
        assertNull(producer);

        verify(mockProducer);
        verify(mockContext);
    }
}

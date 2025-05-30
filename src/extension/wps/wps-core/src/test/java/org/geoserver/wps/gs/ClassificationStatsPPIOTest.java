/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.mock;
import static org.easymock.EasyMock.notNull;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;
import it.geosolutions.jaiext.stats.Statistics;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.LinkedHashSet;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.custommonkey.xmlunit.XMLAssert;
import org.easymock.EasyMock;
import org.geotools.process.classify.ClassificationStats;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.ContentHandler;

public class ClassificationStatsPPIOTest {

    @Test
    public void testSanity() throws Exception {
        ClassificationStats classStats = newStats();

        ClassificationStatsPPIO ppio = new ClassificationStatsPPIO();
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ppio.encode(classStats, bout);

        Document doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(new ByteArrayInputStream(bout.toByteArray()));

        assertEquals("Results", doc.getDocumentElement().getNodeName());
        XMLAssert.assertXpathExists("/Results/Class[@lowerBound='0.0']", doc);
        XMLAssert.assertXpathExists("/Results/Class[@lowerBound='10.0']", doc);
    }

    @Test
    public void testNamespacesNotNull() throws Exception {
        ContentHandler h = createNiceMock(ContentHandler.class);
        h.startElement(notNull(), notNull(), eq("Results"), anyObject());
        expectLastCall().times(1);

        replay(h);

        new ClassificationStatsPPIO().encode(newStats(), h);

        verify(h);
    }

    ClassificationStats newStats() {
        Range r0 = RangeFactory.create(0d, true, 10d, false);
        Range r1 = RangeFactory.create(10d, true, 20d, true);

        ClassificationStats result = mock(ClassificationStats.class);
        EasyMock.expect(result.size()).andReturn(2).anyTimes();
        EasyMock.expect(result.range(0)).andReturn(r0).anyTimes();
        EasyMock.expect(result.range(1)).andReturn(r1).anyTimes();
        EasyMock.expect(result.count(0)).andReturn(1L).anyTimes();
        EasyMock.expect(result.count(1)).andReturn(1L).anyTimes();
        EasyMock.expect(result.value(0, Statistics.StatsType.MEAN))
                .andReturn(10d)
                .anyTimes();
        EasyMock.expect(result.value(1, Statistics.StatsType.MEAN))
                .andReturn(10d)
                .anyTimes();
        EasyMock.expect(result.getStats())
                .andReturn(new LinkedHashSet<>(List.of(Statistics.StatsType.MEAN)))
                .anyTimes();
        EasyMock.replay(result);
        return result;
    }
}

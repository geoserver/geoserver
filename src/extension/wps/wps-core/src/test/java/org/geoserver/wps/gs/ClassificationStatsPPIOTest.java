/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.notNull;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.custommonkey.xmlunit.XMLAssert;
import org.geotools.process.classify.ClassificationStats;
import org.geotools.process.vector.FeatureClassStats;
import org.jaitools.numeric.Range;
import org.jaitools.numeric.Statistic;
import org.jaitools.numeric.StreamingSampleStats;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.ContentHandler;

public class ClassificationStatsPPIOTest {

    @Test
    public void testSanity() throws Exception {
        List<Range<Double>> ranges =
                Arrays.asList(
                        Range.create(0d, true, 10d, false), Range.create(10d, true, 20d, true));

        StreamingSampleStats s1 = new StreamingSampleStats();
        s1.setStatistic(Statistic.MEAN);
        s1.addRange(ranges.get(0));
        s1.offer(10d);

        StreamingSampleStats s2 = new StreamingSampleStats();
        s2.setStatistic(Statistic.MEAN);
        s2.addRange(ranges.get(0));
        s2.offer(10d);

        StreamingSampleStats[] stats = new StreamingSampleStats[] {s1, s2};

        ClassificationStats classStats = new FeatureClassStats.Results(ranges, stats);

        ClassificationStatsPPIO ppio = new ClassificationStatsPPIO();
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ppio.encode(classStats, bout);

        Document doc =
                DocumentBuilderFactory.newInstance()
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
        List<Range<Double>> ranges =
                Arrays.asList(
                        Range.create(0d, true, 10d, false), Range.create(10d, true, 20d, true));

        StreamingSampleStats s1 = new StreamingSampleStats();
        s1.setStatistic(Statistic.MEAN);
        s1.addRange(ranges.get(0));
        s1.offer(10d);

        StreamingSampleStats s2 = new StreamingSampleStats();
        s2.setStatistic(Statistic.MEAN);
        s2.addRange(ranges.get(0));
        s2.offer(10d);

        StreamingSampleStats[] stats = new StreamingSampleStats[] {s1, s2};

        return new FeatureClassStats.Results(ranges, stats);
    }
}

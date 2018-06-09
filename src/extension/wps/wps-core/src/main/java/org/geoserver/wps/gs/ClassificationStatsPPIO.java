/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import java.io.InputStream;
import javax.xml.namespace.QName;
import org.geoserver.wps.ppio.XMLPPIO;
import org.geotools.process.classify.ClassificationStats;
import org.jaitools.numeric.Range;
import org.jaitools.numeric.Statistic;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;

/**
 * A PPIO for encoding the results of the {@link org.geotools.process.vector.FeatureClassStats} and
 * {@link org.geotools.process.raster.CoverageClassStats} processes.
 */
public class ClassificationStatsPPIO extends XMLPPIO {

    protected ClassificationStatsPPIO() {
        super(ClassificationStats.class, ClassificationStats.class, new QName("Results"));
    }

    @Override
    public void encode(Object object, ContentHandler h) throws Exception {
        ClassificationStats results = (ClassificationStats) object;

        h.startDocument();
        h.startElement("", "", "Results", null);

        for (int i = 0; i < results.size(); i++) {
            Range range = results.range(i);

            AttributesImpl atts = new AttributesImpl();
            atts.addAttribute("", "", "lowerBound", null, range.getMin().toString());
            atts.addAttribute("", "", "upperBound", null, range.getMax().toString());
            atts.addAttribute("", "", "count", null, results.count(i).toString());

            h.startElement("", "", "Class", atts);
            for (Statistic stat : results.getStats()) {
                h.startElement("", "", stat.name(), null);

                String value = String.valueOf(results.value(i, stat));
                h.characters(value.toCharArray(), 0, value.length());

                h.endElement("", "", stat.name());
            }
            h.endElement("", "", "Class");
        }

        h.endElement("", "", "Results");
        h.endDocument();
    }

    @Override
    public Object decode(InputStream input) throws Exception {
        throw new UnsupportedOperationException();
    }
}

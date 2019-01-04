/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import com.thoughtworks.xstream.mapper.MapperWrapper;
import javax.xml.namespace.QName;
import org.geoserver.config.util.SecureXStream;
import org.geoserver.wps.ppio.XStreamPPIO;
import org.geotools.process.vector.AggregateProcess;

/**
 * A PPIO to generate good looking xml for the aggreagate process results
 *
 * @author Andrea Aime - GeoSolutions
 */
public class AggregateProcessPPIO extends XStreamPPIO {

    static final QName AggregationResults = new QName("AggregationResults");

    protected AggregateProcessPPIO() {
        super(AggregateProcess.Results.class, AggregationResults);
    }

    @Override
    protected SecureXStream buildXStream() {
        SecureXStream xstream =
                new SecureXStream() {
                    protected MapperWrapper wrapMapper(MapperWrapper next) {
                        return new UppercaseTagMapper(next);
                    };
                };
        xstream.allowTypes(new Class[] {AggregateProcess.Results.class});
        xstream.omitField(AggregateProcess.Results.class, "aggregateAttribute");
        xstream.omitField(AggregateProcess.Results.class, "functions");
        xstream.omitField(AggregateProcess.Results.class, "groupByAttributes");
        xstream.omitField(AggregateProcess.Results.class, "results");
        xstream.alias(AggregationResults.getLocalPart(), AggregateProcess.Results.class);
        xstream.omitField(AggregateProcess.Results.class, "aggregateAttribute");
        xstream.omitField(AggregateProcess.Results.class, "functions");
        xstream.omitField(AggregateProcess.Results.class, "groupByAttributes");
        xstream.omitField(AggregateProcess.Results.class, "results");
        return xstream;
    }
}

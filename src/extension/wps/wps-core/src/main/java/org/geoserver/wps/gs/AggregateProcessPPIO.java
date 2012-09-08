/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import org.geoserver.wps.ppio.XStreamPPIO;
import org.geotools.process.vector.AggregateProcess;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.mapper.MapperWrapper;

/**
 * A PPIO to generate good looking xml for the aggreagate process results
 * @author Andrea Aime - GeoSolutions
 */
public class AggregateProcessPPIO extends XStreamPPIO {

    protected AggregateProcessPPIO() {
        super(AggregateProcess.Results.class);
    }
    
    @Override
    protected XStream buildXStream() {
        XStream xstream = new XStream() {
            protected MapperWrapper wrapMapper(MapperWrapper next) {
                return new UppercaseTagMapper(next);
            };
        };
        xstream.alias("AggregationResults", AggregateProcess.Results.class);
        return xstream;
    }
    
    

}

/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.kvp;

import java.util.Collection;
import java.util.Date;
import java.util.logging.Logger;

import net.opengis.wcs11.TimeSequenceType;
import net.opengis.wcs11.Wcs111Factory;

import org.geoserver.ows.KvpParser;
import org.geoserver.ows.kvp.TimeKvpParser;
import org.geotools.util.logging.Logging;

public class TimeSequenceKvpParser extends KvpParser {
    Logger LOGGER = Logging.getLogger(TimeSequenceKvpParser.class);

    public TimeSequenceKvpParser() {
        super("TimeSequence", TimeSequenceType.class);
        
    }

    @Override
    public Object parse(String value) throws Exception {
        TimeSequenceType timeSequence = Wcs111Factory.eINSTANCE.createTimeSequenceType();
        TimeKvpParser parser = new TimeKvpParser("WCS1_1");
        
        Collection<Date> timePositions = (Collection<Date>)parser.parse(value);
        for (Date tp : timePositions) {
            timeSequence.getTimePosition().add(tp);
        }
        
        return timeSequence;
    }

}

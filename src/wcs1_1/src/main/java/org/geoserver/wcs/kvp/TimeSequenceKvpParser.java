/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.kvp;

import net.opengis.wcs11.TimeSequenceType;
import net.opengis.wcs11.Wcs111Factory;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.KvpParser;
import org.geoserver.ows.kvp.TimeParser;
import org.geoserver.wcs.WCSInfo;

import java.util.Collection;
import java.util.Date;

public class TimeSequenceKvpParser extends KvpParser {
    private final GeoServer geoServer;

    public TimeSequenceKvpParser(GeoServer geoServer) {
        super("TimeSequence", TimeSequenceType.class);
        this.geoServer = geoServer;
    }

    @Override
    public Object parse(String value) throws Exception {
        TimeSequenceType timeSequence = Wcs111Factory.eINSTANCE.createTimeSequenceType();

        WCSInfo info = geoServer.getService(WCSInfo.class);
        int maxRequestedDimensionValues = info.getMaxRequestedDimensionValues();
        TimeParser parser = new TimeParser(maxRequestedDimensionValues);
        Collection<Date> timePositions = (Collection<Date>) parser.parse(value);
        for (Date tp : timePositions) {
            timeSequence.getTimePosition().add(tp);
        }
        
        return timeSequence;
    }

}

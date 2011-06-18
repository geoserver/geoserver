/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.kml;

import java.util.Collections;
import java.util.HashMap;

import javax.xml.namespace.QName;

import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.data.test.MockData;
import org.geoserver.test.GeoServerTestSupport;

/**
 * Base class for functional testing of the regionating code; sets up a proper testing enviroment
 * with a real data dir and a connection to a postgis data store
 * 
 * @author David Winslow <dwinslow@openplans.org>
 * 
 */
public abstract class RegionatingTestSupport extends GeoServerTestSupport {
    public static QName STACKED_FEATURES = new QName(MockData.SF_URI, "Stacked", MockData.SF_PREFIX);
    public static QName DISPERSED_FEATURES = new QName(MockData.SF_URI, "Dispersed", MockData.SF_PREFIX);
    public static QName TILE_TESTS = new QName(MockData.SF_URI, "Tiles", MockData.SF_PREFIX);
    public static QName CENTERED_POLY = new QName(MockData.SF_URI, "CenteredPoly", MockData.SF_PREFIX);

    public void populateDataDirectory(MockData data) throws Exception{
        super.populateDataDirectory(data);

        data.addPropertiesType(
                STACKED_FEATURES,
                getClass().getResource("Stacked.properties"),
                Collections.EMPTY_MAP
                );
        data.addPropertiesType(
                DISPERSED_FEATURES,
                getClass().getResource("Dispersed.properties"),
                Collections.EMPTY_MAP
                );
        
        HashMap extra = new HashMap();
        extra.put(MockData.KEY_SRS_HANDLINGS, ProjectionPolicy.FORCE_DECLARED.getCode());
        
        data.addPropertiesType(
                TILE_TESTS,
                getClass().getResource("TileTests.properties"),
                extra
                );
        
        data.addPropertiesType(
                CENTERED_POLY,
                getClass().getResource("CenteredPoly.properties"),
                Collections.EMPTY_MAP
                );
    }

}


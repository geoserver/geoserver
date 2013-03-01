/* Copyright (c) 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml;

import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WebMap;

import de.micromata.opengis.kml.v_2_2_0.Kml;

public class KMLMap extends WebMap {
    
    Kml kml;

    public KMLMap(WMSMapContent context, Kml kml) {
        super(context);
        this.kml = kml;
    }

    public Kml getKml() {
        return kml;
    }
   

}

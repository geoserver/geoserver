/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.python.format;

import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WebMap;

public class PythonWebMap extends WebMap {

    PythonFormatAdapter adapter;
    
    public PythonWebMap(WMSMapContent context, PythonFormatAdapter adapter) {
        super(context);
        this.adapter = adapter;
    }

    public WMSMapContent getMapContext() {
        return mapContent;
    }
    
    public PythonFormatAdapter getAdapter() {
        return adapter;
    }
}

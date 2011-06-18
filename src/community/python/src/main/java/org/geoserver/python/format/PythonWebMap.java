package org.geoserver.python.format;

import org.geoserver.wms.WMSMapContext;
import org.geoserver.wms.WebMap;

public class PythonWebMap extends WebMap {

    PythonFormatAdapter adapter;
    
    public PythonWebMap(WMSMapContext context, PythonFormatAdapter adapter) {
        super(context);
        this.adapter = adapter;
    }

    public WMSMapContext getMapContext() {
        return mapContext;
    }
    
    public PythonFormatAdapter getAdapter() {
        return adapter;
    }
}

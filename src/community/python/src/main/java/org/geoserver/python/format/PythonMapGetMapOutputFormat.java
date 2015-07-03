/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.python.format;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Set;

import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetMapOutputFormat;
import org.geoserver.wms.MapProducerCapabilities;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WebMap;

public class PythonMapGetMapOutputFormat implements GetMapOutputFormat {
    
    PythonMapFormatAdapter adapter;
    WMSMapContent context;
    
    public PythonMapGetMapOutputFormat(PythonMapFormatAdapter adapter) {
        this.adapter = adapter;
    }
    
    public String getMimeType() {
        return adapter.getMimeType();
    }
    
    public Set<String> getOutputFormatNames() {
        return Collections.singleton(adapter.getName());
    }

    public WebMap produceMap(WMSMapContent mapContent) throws ServiceException, IOException {
        return new PythonWebMap(mapContent, adapter);
    }
    
    public void writeTo(OutputStream out) throws ServiceException, IOException {
        try {
            adapter.write(context, out);
        } 
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    public MapProducerCapabilities getCapabilities(String format) {
        return new MapProducerCapabilities(false, false, false, true, null);
    }

}

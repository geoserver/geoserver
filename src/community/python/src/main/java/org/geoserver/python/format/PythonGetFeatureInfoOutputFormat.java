/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.python.format;

import java.io.IOException;
import java.io.OutputStream;

import net.opengis.wfs.FeatureCollectionType;

import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wms.GetFeatureInfoRequest;
import org.geoserver.wms.featureinfo.GetFeatureInfoOutputFormat;

public class PythonGetFeatureInfoOutputFormat extends GetFeatureInfoOutputFormat {

    PythonVectorFormatAdapter adapter;
    
    public PythonGetFeatureInfoOutputFormat(PythonVectorFormatAdapter adapter) {
        super(adapter.getMimeType());
        this.adapter = adapter;
        
        //supportedFormats = Arrays.asList(format, adapter.getName());
    }
    
    @Override
    public void write(FeatureCollectionType results, GetFeatureInfoRequest request, OutputStream out)
            throws ServiceException, IOException {
        
        try {
            adapter.write(FeatureCollectionResponse.adapt(results), out);
        } 
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return new PythonGetFeatureInfoOutputFormat(adapter);
    }
    
}

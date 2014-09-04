/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.python.format;

import java.io.IOException;
import java.io.OutputStream;

import net.opengis.wfs.FeatureCollectionType;

import org.geoserver.config.GeoServer;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.request.FeatureCollectionResponse;

public class PythonGetFeatureOutputFormat extends WFSGetFeatureOutputFormat {

    PythonVectorFormatAdapter adapter;
    
    public PythonGetFeatureOutputFormat(PythonVectorFormatAdapter adapter, GeoServer gs) {
        super(gs, adapter.getName());
        this.adapter = adapter;
        
    }
    
    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return adapter.getMimeType();
    }

    @Override
    protected void write(FeatureCollectionResponse featureCollection, OutputStream output,
            Operation getFeature) throws IOException, ServiceException {
        try {
            adapter.write(featureCollection, output);
        } 
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }

}

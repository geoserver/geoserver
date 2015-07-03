/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package ${groupId};

import java.io.IOException;
import java.io.OutputStream;

import net.opengis.wfs.FeatureCollectionType;

import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;

public class MyOutputFormat extends WFSGetFeatureOutputFormat {

    public MyOutputFormat() {
        //this is the name of your output format, it is the string
        // that will be used when requesting the format in a 
        // GEtFeature request: 
        // ie ;.../geoserver/wfs?request=getfeature&outputFormat=myOutputFormat
        super("myOutputFormat");
    }

    @Override
    public String getMimeType(Object value, Operation operation)
            throws ServiceException {
        // return the mime type of the format here, the parent 
        // class returns 'text/xml'
        return super.getMimeType(value, operation);
    }
    
    @Override
    protected boolean canHandleInternal(Operation operation) {
        //any additional checks that need to be performed to 
        // determine when the output format should be "engaged" 
        // should go here
        return super.canHandleInternal(operation);
    }
    
    @Override
    protected void write(FeatureCollectionType featureCollection,
            OutputStream output, Operation getFeature) throws IOException,
            ServiceException {
        //write out content here
    }

}

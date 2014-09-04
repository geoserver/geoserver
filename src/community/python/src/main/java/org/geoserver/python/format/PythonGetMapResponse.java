/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.python.format;

import java.io.IOException;
import java.io.OutputStream;

import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.map.AbstractMapResponse;


public class PythonGetMapResponse extends AbstractMapResponse {

    public PythonGetMapResponse(PythonFormatAdapter adapter) {
        super(PythonWebMap.class, adapter.getMimeType());
    }
    
    @Override
    public void write(Object value, OutputStream output, Operation operation) throws IOException,
            ServiceException {
        
        PythonWebMap map = (PythonWebMap) value;
        PythonFormatAdapter adapter = map.getAdapter();
       
        try {
            if (adapter instanceof PythonVectorFormatAdapter) {
                ((PythonVectorFormatAdapter)adapter).write(map.getMapContext(), output);
            }
            else if (adapter instanceof PythonMapFormatAdapter) {
                ((PythonMapFormatAdapter)adapter).write(map.getMapContext(), output);
            }
            else {
                throw new IllegalStateException();
            }
        }
        catch(Exception e) {
            throw new ServiceException(e);
        }
        
    }

    
}

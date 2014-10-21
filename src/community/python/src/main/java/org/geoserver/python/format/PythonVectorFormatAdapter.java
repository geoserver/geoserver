/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.python.format;

import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.geoserver.python.Python;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wms.WMSMapContent;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.python.core.Py;
import org.python.core.PyObject;

public class PythonVectorFormatAdapter extends PythonFormatAdapter {

    public PythonVectorFormatAdapter(File module, Python py) {
        super(module, py);
    }
    
    @Override
    protected String getMarker() {
        return "__vector_format__";
    }
    
    public void write(FeatureCollectionResponse features, OutputStream output) throws Exception {
        write(features.getFeature(), output);
    }
    
    public void write(List<FeatureCollection> features, OutputStream output) throws Exception {
        PyObject obj = pyObject();
        obj.__call__(Py.javas2pys(features, output));
        
        output.flush();
    }
    
    public void write(WMSMapContent mapContent, OutputStream output) throws Exception {
        List features = new ArrayList();
        for (Layer l : mapContent.layers()) {
            if (l instanceof FeatureLayer) {
                FeatureSource source = l.getFeatureSource();
                features.add(source.getFeatures(l.getQuery()));
            }
        }

        if (features.isEmpty()) {
            throw new IllegalArgumentException("No feature data in map context");
        }

        PyObject obj = pyObject();
        obj.__call__(Py.javas2pys(features, output));
        
        output.flush();
    }

}

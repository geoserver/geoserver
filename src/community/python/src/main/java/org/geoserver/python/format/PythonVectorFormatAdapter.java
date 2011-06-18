package org.geoserver.python.format;

import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import net.opengis.wfs.FeatureCollectionType;

import org.geoserver.python.Python;
import org.geoserver.wms.WMSMapContext;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.map.FeatureLayer;
import org.geotools.map.MapLayer;
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
    
    public void write(FeatureCollectionType features, OutputStream output) throws Exception {
        write(features.getFeature(), output);
    }
    
    public void write(List<FeatureCollection> features, OutputStream output) throws Exception {
        PyObject obj = pyObject();
        obj.__call__(Py.javas2pys(features, output));
        
        output.flush();
    }
    
    public void write(WMSMapContext mapContext, OutputStream output) throws Exception {
        List features = new ArrayList();
        for (int i = 0; i < mapContext.getLayerCount(); i++) {
            MapLayer l = mapContext.getLayer(i);
            if (l.toLayer() instanceof FeatureLayer) {
                FeatureSource source = mapContext.getLayer(i).getFeatureSource();
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

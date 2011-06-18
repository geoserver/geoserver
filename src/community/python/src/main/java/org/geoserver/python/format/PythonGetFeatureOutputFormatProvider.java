package org.geoserver.python.format;

import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.python.Python;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;

public class PythonGetFeatureOutputFormatProvider extends PythonOutputFormatProvider<WFSGetFeatureOutputFormat> {

    
    
    public PythonGetFeatureOutputFormatProvider(Python py) {
        super(py);
    }
    
    public Class<WFSGetFeatureOutputFormat> getExtensionPoint() {
        return WFSGetFeatureOutputFormat.class;
    }
    
    @Override
    protected WFSGetFeatureOutputFormat createOutputFormat(PythonFormatAdapter adapter) {
        //workaround, because this is an extension provider it can not depend on GeoServer or 
        // a circular bean reference is created so we lazily obtain a reference to gs 
        GeoServer gs = GeoServerExtensions.bean(GeoServer.class);
        return new PythonGetFeatureOutputFormat((PythonVectorFormatAdapter) adapter, gs);
    }

}

package org.geoserver.python.format;

import org.geoserver.python.Python;
import org.geoserver.wms.GetMapOutputFormat;

public class PythonGetMapOutputFormatProvider extends PythonVectorOutputFormatProvider<GetMapOutputFormat> {

    public PythonGetMapOutputFormatProvider(Python py) {
        super(py);
    }
    
    public Class<GetMapOutputFormat> getExtensionPoint() {
        return GetMapOutputFormat.class;
    }

    @Override
    protected GetMapOutputFormat createOutputFormat(PythonVectorFormatAdapter adapter) {
        return new PythonGetMapOutputFormat(adapter);
    }

}

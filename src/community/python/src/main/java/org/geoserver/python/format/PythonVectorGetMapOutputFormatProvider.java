package org.geoserver.python.format;

import org.geoserver.python.Python;
import org.geoserver.wms.GetMapOutputFormat;

public class PythonVectorGetMapOutputFormatProvider extends PythonOutputFormatProvider<GetMapOutputFormat> {

    public PythonVectorGetMapOutputFormatProvider(Python py) {
        super(py);
    }
    
    public Class<GetMapOutputFormat> getExtensionPoint() {
        return GetMapOutputFormat.class;
    }

    @Override
    protected GetMapOutputFormat createOutputFormat(PythonFormatAdapter adapter) {
        return new PythonVectorGetMapOutputFormat((PythonVectorFormatAdapter) adapter);
    }

}

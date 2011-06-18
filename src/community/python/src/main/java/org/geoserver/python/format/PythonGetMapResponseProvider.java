package org.geoserver.python.format;

import org.geoserver.ows.Response;
import org.geoserver.python.Python;

public class PythonGetMapResponseProvider extends PythonOutputFormatProvider<Response> {

    public PythonGetMapResponseProvider(Python py) {
        super(py);
    }

    public Class<Response> getExtensionPoint() {
        return Response.class;
    }

    @Override
    protected Response createOutputFormat(PythonFormatAdapter adapter) {
        return new PythonGetMapResponse(adapter);
    }

}

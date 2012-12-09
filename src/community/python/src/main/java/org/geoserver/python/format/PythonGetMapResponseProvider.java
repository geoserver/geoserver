/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
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

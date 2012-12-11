/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.python.format;

import java.io.File;
import java.io.OutputStream;

import org.geoserver.python.Python;
import org.geoserver.wms.WMSMapContent;
import org.python.core.Py;
import org.python.core.PyObject;

public class PythonMapFormatAdapter extends PythonFormatAdapter {

    public PythonMapFormatAdapter(File module, Python py) {
        super(module, py);
    }

    @Override
    protected String getMarker() {
        return "__map_format__";
    }

    public void write(WMSMapContent mapContent, OutputStream output) throws Exception {
        PyObject obj = pyObject();
        obj.__call__(Py.javas2pys(mapContent, output));
        
        output.flush();
    }

}

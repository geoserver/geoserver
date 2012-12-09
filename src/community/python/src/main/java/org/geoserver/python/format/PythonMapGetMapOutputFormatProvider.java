/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.python.format;

import org.geoserver.python.Python;
import org.geoserver.wms.GetMapOutputFormat;

public class PythonMapGetMapOutputFormatProvider extends PythonOutputFormatProvider<GetMapOutputFormat> {

    public PythonMapGetMapOutputFormatProvider(Python py) {
        super(py);
    }

    public Class<GetMapOutputFormat> getExtensionPoint() {
        return GetMapOutputFormat.class;
    }
    
    @Override
    protected GetMapOutputFormat createOutputFormat(PythonFormatAdapter adapter) {
        return new PythonMapGetMapOutputFormat((PythonMapFormatAdapter) adapter);
    }

}

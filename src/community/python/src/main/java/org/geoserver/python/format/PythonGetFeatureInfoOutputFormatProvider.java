/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.python.format;

import org.geoserver.python.Python;
import org.geoserver.wms.featureinfo.GetFeatureInfoOutputFormat;

public class PythonGetFeatureInfoOutputFormatProvider 
    extends PythonOutputFormatProvider<GetFeatureInfoOutputFormat> {

    public PythonGetFeatureInfoOutputFormatProvider(Python py) {
        super(py);
    }

    public Class<GetFeatureInfoOutputFormat> getExtensionPoint() {
        return GetFeatureInfoOutputFormat.class;
    }
    
    @Override
    protected GetFeatureInfoOutputFormat createOutputFormat(PythonFormatAdapter adapter) {
        return new PythonGetFeatureInfoOutputFormat((PythonVectorFormatAdapter) adapter);
    }

}

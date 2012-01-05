/* Copyright (c) 2010 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms;

import java.nio.charset.Charset;

import org.geoserver.platform.ServiceException;
import org.geoserver.wms.describelayer.DescribeLayerTransformer;

/**
 * DescribeLayer WMS operation default implementation.
 * 
 * @author Gabriel Roldan
 */
public class DescribeLayer {

    private WMS wms;

    public DescribeLayer(final WMS wms) {
        this.wms = wms;
    }

    /**
     * @see org.geoserver.wms.DescribeLayer#run(org.geoserver.wms.DescribeLayerRequest)
     */
    public DescribeLayerTransformer run(DescribeLayerRequest request) throws ServiceException {
        String baseURL = request.getBaseUrl();

        DescribeLayerTransformer transformer;
        transformer = new DescribeLayerTransformer(baseURL);
        Charset encoding = wms.getCharSet();
        transformer.setEncoding(encoding);
        if (wms.getGeoServer().getSettings().isVerbose()) {
            transformer.setIndentation(2);
        }
        return transformer;
    }

}

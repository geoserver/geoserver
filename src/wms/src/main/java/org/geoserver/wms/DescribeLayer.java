/* Copyright (c) 2010 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.geoserver.platform.ServiceException;
import org.geoserver.wms.describelayer.DescribeLayerModel;
import org.geotools.data.ows.LayerDescription;
import org.geotools.util.logging.Logging;

/**
 * DescribeLayer WMS operation default implementation.
 * 
 * @author carlo cancellieri
 */
public class DescribeLayer {

private static final Logger LOGGER = Logging.getLogger(DescribeLayerModel.class);
    
    public DescribeLayer() {
    	
    }

	private final List<LayerDescription> layerDescriptions=new ArrayList<LayerDescription>();

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static DescribeLayerModel run(final DescribeLayerRequest request) throws ServiceException {

        return new DescribeLayerModel(request);

    }


}

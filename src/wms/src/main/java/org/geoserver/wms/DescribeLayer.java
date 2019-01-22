/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import org.geoserver.platform.ServiceException;
import org.geoserver.wms.describelayer.DescribeLayerModel;

/**
 * DescribeLayer WMS operation default implementation.
 *
 * @author carlo cancellieri
 */
public class DescribeLayer {

    public DescribeLayer() {}

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static DescribeLayerModel run(final DescribeLayerRequest request)
            throws ServiceException {

        return new DescribeLayerModel(request);
    }
}

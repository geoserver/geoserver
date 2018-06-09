/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps.kvp;

import java.util.Map;
import net.opengis.wps10.DescribeProcessType;

/**
 * DescribeProcess KVP request reader
 *
 * @author Lucas Reed, Refractions Research Inc
 */
public class DescribeProcessKvpRequestReader extends WPSKvpRequestReader {
    public DescribeProcessKvpRequestReader() {
        super(DescribeProcessType.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object read(Object request, Map kvp, Map rawKvp) throws Exception {
        request = super.read(request, kvp, rawKvp);

        // Version arbitration could be done at this point

        return request;
    }
}

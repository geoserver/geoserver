/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfsv.kvp;

import java.util.Map;

import net.opengis.wfsv.DescribeVersionedFeatureTypeType;

public class DescribeVersionedFeatureTypeKvpRequestReader extends WFSVKvpRequestReader {

    public DescribeVersionedFeatureTypeKvpRequestReader() {
        super(DescribeVersionedFeatureTypeType.class);
    }

    public Object read(Object request, Map kvp, Map rawKvp) throws Exception {
        // let super do its thing
        request = super.read(request, kvp, rawKvp);

        // do an additional check for outputFormat, because the default
        // in wfs 1.1 is not the default for wfs 1.0
        DescribeVersionedFeatureTypeType describeFeatureType = (DescribeVersionedFeatureTypeType) request;

        if (!describeFeatureType.isSetOutputFormat()) {
            if (describeFeatureType.getVersion().startsWith("1.1")) {
                // set 1.1 default
                describeFeatureType
                        .setOutputFormat("text/xml; subtype=gml/3.1.1");
            } else {
                // set 1.0 default
                describeFeatureType.setOutputFormat("XMLSCHEMA");
            }
        }

        return request;
    }
}

/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss.impl.discovery;

import org.geoserver.gss.impl.GSS;
import org.geoserver.gss.service.GetCapabilities;

public class GetCapabilitiesOp {

    public Object execute(GetCapabilities request, GSS gss) {
        return new GetCapabilitiesTransformer(gss);
    }

}

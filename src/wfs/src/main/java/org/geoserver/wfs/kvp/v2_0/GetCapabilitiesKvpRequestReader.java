/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.kvp.v2_0;

import net.opengis.wfs20.GetCapabilitiesType;
import net.opengis.wfs20.Wfs20Factory;

public class GetCapabilitiesKvpRequestReader extends org.geoserver.wfs.kvp.GetCapabilitiesKvpRequestReader {
    public GetCapabilitiesKvpRequestReader() {
        super(GetCapabilitiesType.class, Wfs20Factory.eINSTANCE);
    }
}

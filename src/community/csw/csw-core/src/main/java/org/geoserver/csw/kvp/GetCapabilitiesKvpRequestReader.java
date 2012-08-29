/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.csw.kvp;

import net.opengis.ows10.GetCapabilitiesType;

/**
 * GetCapabilities KVP request reader
 * 
 * @author Andrea Aime, GeoSolutions
 */
public class GetCapabilitiesKvpRequestReader extends CSWKvpRequestReader {
    public GetCapabilitiesKvpRequestReader() {
        super(GetCapabilitiesType.class);
    }

}

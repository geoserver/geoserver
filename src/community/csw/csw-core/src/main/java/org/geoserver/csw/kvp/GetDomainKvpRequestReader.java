/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.csw.kvp;

import net.opengis.cat.csw20.GetDomainType;

/**
 * GetDomain KVP request reader
 * 
 * @author Andrea Aime, GeoSolutions
 */
public class GetDomainKvpRequestReader extends CSWKvpRequestReader {
    public GetDomainKvpRequestReader() {
        super(GetDomainType.class);
    }

}

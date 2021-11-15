/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.nsg.pagination.random;

import org.geotools.util.Version;

public class ResultTypeKvpParser extends org.geoserver.wfs.kvp.v2_0.ResultTypeKvpParser {

    public ResultTypeKvpParser() {
        super();
        setVersion(new Version("2.0.2"));
    }
}

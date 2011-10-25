/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.kvp.v2_0;

import net.opengis.wfs20.ResultTypeType;

import org.geoserver.ows.KvpParser;
import org.geotools.util.Version;

public class ResultTypeKvpParser extends KvpParser {

    public ResultTypeKvpParser() {
        super("resultType", ResultTypeType.class);
        setService("WFS");
        setVersion(new Version("2.0.0"));
    }

    @Override
    public Object parse(String value) throws Exception {
        return ResultTypeType.get(value.toLowerCase());
    }

}

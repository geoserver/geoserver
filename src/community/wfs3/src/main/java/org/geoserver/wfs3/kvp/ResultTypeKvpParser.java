/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3.kvp;

import net.opengis.wfs20.ResultTypeType;
import org.geoserver.ows.KvpParser;
import org.geotools.util.Version;

/**
 * ResultType parser for WFS 3.0.
 *
 * @author Fernando Mino - Geosolutions
 */
public class ResultTypeKvpParser extends KvpParser {

    public ResultTypeKvpParser() {
        super("resultType", ResultTypeType.class);
        setService("WFS");
        setVersion(new Version("3.0.0"));
    }

    @Override
    public Object parse(String value) throws Exception {
        return ResultTypeType.get(value.toLowerCase());
    }
}

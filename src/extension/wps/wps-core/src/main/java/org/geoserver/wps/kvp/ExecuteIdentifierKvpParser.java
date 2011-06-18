/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps.kvp;

import net.opengis.ows11.CodeType;

import org.geoserver.ows.KvpParser;
import org.geoserver.ows.Ows11Util;

/**
 * Identifier attribute KVP parser
 *
 * @author Andrea Aime - OpenGeo
 */
public class ExecuteIdentifierKvpParser extends KvpParser {
    public ExecuteIdentifierKvpParser() {
        super("identifier", CodeType.class);

        this.setService("wps");
        this.setRequest("Execute");
    }

    @SuppressWarnings("unchecked")
    public Object parse(String value) throws Exception {
        return Ows11Util.code(value);
    }
}
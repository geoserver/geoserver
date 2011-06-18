/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.ows.kvp;

import java.util.List;

import net.opengis.ows11.AcceptVersionsType;
import net.opengis.ows11.Ows11Factory;

import org.geoserver.ows.KvpParser;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.ows.util.RequestUtils;
import org.geotools.util.Version;

/**
 * Parses the OWS 1.1 capabilities negotiation related AcceptVersion parameter
 * 
 * @author Andrea Aime - TOPP
 * 
 */
public class OWS11AcceptVersionsKvpParser extends KvpParser {

    public OWS11AcceptVersionsKvpParser() {
        super("AcceptVersions", AcceptVersionsType.class);
        setVersion( new Version( "1.1.1" ) );
    }

    public Object parse(String value) throws Exception {
        List<String> versions = KvpUtils.readFlat(value);
        for (String version : versions) {
            RequestUtils.checkVersionNumber(version, "AcceptVersions");
        }
        AcceptVersionsType accepts = Ows11Factory.eINSTANCE.createAcceptVersionsType();
        accepts.getVersion().addAll(versions);
        return accepts;
    }
}

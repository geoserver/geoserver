/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wcs2_0.kvp;

import java.util.Collection;
import net.opengis.ows20.AcceptVersionsType;
import net.opengis.ows20.Ows20Factory;

import org.eclipse.emf.ecore.EObject;
import org.geoserver.ows.kvp.AcceptVersionsKvpParser;
import org.geoserver.ows.util.KvpUtils;
import org.geotools.util.Version;
import org.geotools.xml.EMFUtils;

/**
 * Parses the OWS 2.0 capabilities negotiation related AcceptVersion parameter
 * 
 * @author Emanuele Tajariol (etj) - GeoSolutions
 * 
 */
public class WCS20AcceptVersionsKvpParser extends AcceptVersionsKvpParser {

    public final static String VERSION = "2.0";

    public WCS20AcceptVersionsKvpParser() {
        super(AcceptVersionsType.class);
        setService( "wcs" );
        setVersion( new Version( VERSION ) );
    }

//    public Object parse(String value) throws Exception {
//        EObject acceptVersions = createObject();
//        ((Collection)EMFUtils.get(acceptVersions, "version")).addAll(KvpUtils.readFlat(value, KvpUtils.INNER_DELIMETER));
//        return acceptVersions;
//    }

    @Override
    protected AcceptVersionsType createObject() {
        return Ows20Factory.eINSTANCE.createAcceptVersionsType();
    }
   
}

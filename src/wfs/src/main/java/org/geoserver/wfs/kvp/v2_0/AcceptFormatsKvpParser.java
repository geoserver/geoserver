/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfs.kvp.v2_0;

import net.opengis.ows11.AcceptFormatsType;
import net.opengis.ows11.Ows11Factory;

import org.eclipse.emf.ecore.EObject;


/**
 * Parses a kvp of the form "acceptFormats=format1,format2,...,formatN" into
 * an instance of {@link net.opengis.ows.v1_1_0.AcceptFormatsType}.
 *
 * @author Justin Deoliveira, The Open Planning Project
 *
 */
public class AcceptFormatsKvpParser extends org.geoserver.ows.kvp.AcceptFormatsKvpParser {
    public AcceptFormatsKvpParser() {
        super(AcceptFormatsType.class);
        //make this parser the default
        setVersion(null);
    }

    protected EObject createObject() {
        return Ows11Factory.eINSTANCE.createAcceptFormatsType();
    }
}

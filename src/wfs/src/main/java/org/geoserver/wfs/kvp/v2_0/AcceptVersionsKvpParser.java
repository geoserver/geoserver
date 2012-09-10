/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfs.kvp.v2_0;

import net.opengis.ows11.AcceptVersionsType;
import net.opengis.ows11.Ows11Factory;

import org.eclipse.emf.ecore.EObject;

/**
 * Parses a kvp of the form "acceptVersions=version1,version2,...,versionN" into
 * an instance of {@link net.opengis.ows.v1_1_0.AcceptVersionsType}.
 *
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class AcceptVersionsKvpParser extends org.geoserver.ows.kvp.AcceptVersionsKvpParser {
    public AcceptVersionsKvpParser() {
        super(AcceptVersionsType.class);

        //make this the default
        setVersion(null);
    }

    @Override
    protected EObject createObject() {
        return Ows11Factory.eINSTANCE.createAcceptVersionsType();
    }
}

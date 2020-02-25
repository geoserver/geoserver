/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.kvp;

import net.opengis.ows11.AcceptVersionsType;
import net.opengis.ows11.Ows11Factory;
import org.eclipse.emf.ecore.EObject;
import org.geotools.util.Version;

/**
 * Parses the OWS 1.1 capabilities negotiation related AcceptVersion parameter
 *
 * @author Andrea Aime - TOPP
 */
public class AcceptVersionsKvpParser extends org.geoserver.ows.kvp.AcceptVersionsKvpParser {

    public AcceptVersionsKvpParser() {
        super(AcceptVersionsType.class);
        setService("wcs");
        setVersion(new Version("1.1.1"));
    }

    @Override
    protected EObject createObject() {
        return Ows11Factory.eINSTANCE.createAcceptVersionsType();
    }
}

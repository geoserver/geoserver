/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.csw.kvp;

import net.opengis.ows10.Ows10Factory;
import net.opengis.ows11.AcceptFormatsType;
import org.eclipse.emf.ecore.EObject;

/**
 * Parses a kvp of the form "acceptFormats=format1,format2,...,formatN" into an instance of {@link
 * net.opengis.ows.v1_1_0.AcceptFormatsType}.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public class AcceptFormatsKvpParser extends org.geoserver.ows.kvp.AcceptFormatsKvpParser {
    public AcceptFormatsKvpParser() {
        super(AcceptFormatsType.class);
        setService("csw");
    }

    protected EObject createObject() {
        return Ows10Factory.eINSTANCE.createAcceptFormatsType();
    }
}

/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.kvp;

import net.opengis.ows10.AcceptVersionsType;
import net.opengis.ows10.Ows10Factory;
import org.eclipse.emf.ecore.EObject;
import org.geoserver.wfs.WFSInfo;

/**
 * Parses a kvp of the form "acceptVersions=version1,version2,...,versionN" into an instance of
 * {@link net.opengis.ows.v1_0_0.AcceptVersionsType}.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public class AcceptVersionsKvpParser extends org.geoserver.ows.kvp.AcceptVersionsKvpParser {

    public AcceptVersionsKvpParser() {
        super(AcceptVersionsType.class);
        setVersion(WFSInfo.Version.V_11.getVersion());
    }

    protected EObject createObject() {
        return Ows10Factory.eINSTANCE.createAcceptVersionsType();
    }
}

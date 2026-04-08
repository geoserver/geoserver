/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.kvp;

import net.opengis.ows11.Ows11Factory;
import net.opengis.ows11.SectionsType;
import org.eclipse.emf.ecore.EObject;

/**
 * Parses the "sections" GetCapabilities kvp argument
 *
 * @author Andrea Aime - TOPP
 */
public class SectionsKvpParser extends org.geoserver.ows.kvp.SectionsKvpParser {

    public SectionsKvpParser() {
        super(SectionsType.class);
    }

    @Override
    protected EObject createObject() {
        return Ows11Factory.eINSTANCE.createSectionsType();
    }
}

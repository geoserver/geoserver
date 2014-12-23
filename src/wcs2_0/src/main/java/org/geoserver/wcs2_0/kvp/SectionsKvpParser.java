/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.kvp;

import net.opengis.ows20.Ows20Factory;
import net.opengis.ows20.SectionsType;

import org.eclipse.emf.ecore.EObject;

/**
 * Parses the "sections" GetCapabilities kvp argument in WCS 2.0
 * 
 * @author Nicola Lagomarsini GeoSolutions
 */
public class SectionsKvpParser extends org.geoserver.ows.kvp.SectionsKvpParser {

    public SectionsKvpParser() {
        super(SectionsType.class);
    }

    @Override
    protected EObject createObject() {
        return Ows20Factory.eINSTANCE.createSectionsType();
    }
}

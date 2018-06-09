/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.kvp;

import net.opengis.ows10.Ows10Factory;
import net.opengis.ows10.SectionsType;
import org.eclipse.emf.ecore.EObject;

/** Parses the "sections" GetCapabilities kvp argument */
public class SectionsKvpParser extends org.geoserver.ows.kvp.SectionsKvpParser {

    public SectionsKvpParser() {
        super(SectionsType.class);
        setService("WFS");
    }

    @Override
    protected EObject createObject() {
        return Ows10Factory.eINSTANCE.createSectionsType();
    }
}

/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.kvp.v2_0;

import net.opengis.ows10.SectionsType;
import net.opengis.ows11.Ows11Factory;
import org.eclipse.emf.ecore.EObject;
import org.geotools.util.Version;

/** Parses the "sections" GetCapabilities kvp argument */
public class SectionsKvpParser extends org.geoserver.ows.kvp.SectionsKvpParser {

    public SectionsKvpParser() {
        super(SectionsType.class);
        setService("WFS");
        setVersion(new Version("2.0.0"));
    }

    @Override
    protected EObject createObject() {
        return Ows11Factory.eINSTANCE.createSectionsType();
    }
}

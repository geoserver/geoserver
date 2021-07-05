/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.kvp;

import net.opengis.ows20.AcceptLanguagesType;
import net.opengis.ows20.Ows20Factory;
import org.eclipse.emf.ecore.EObject;
import org.geoserver.ows.kvp.AcceptLanguagesKvpParser;
import org.geotools.util.Version;

public class WCS20AcceptLanguagesKvpParser extends AcceptLanguagesKvpParser {

    public static final String VERSION = "2.0";

    public WCS20AcceptLanguagesKvpParser() {
        super(AcceptLanguagesType.class);
        setService("wcs");
        setVersion(new Version(VERSION));
    }

    @Override
    protected EObject createObject() {
        return Ows20Factory.eINSTANCE.createAcceptLanguagesType();
    }
}

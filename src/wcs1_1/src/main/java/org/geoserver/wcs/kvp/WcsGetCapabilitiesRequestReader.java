/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.kvp;

import java.util.Map;
import net.opengis.ows11.AcceptVersionsType;
import net.opengis.wcs11.GetCapabilitiesType;
import net.opengis.wcs11.Wcs111Factory;
import org.eclipse.emf.ecore.EObject;
import org.geoserver.ows.kvp.EMFKvpRequestReader;

/**
 * Parses a GetCapabilities request for WCS into the correspondent model object
 *
 * @author Andrea Aime - TOPP
 */
public class WcsGetCapabilitiesRequestReader extends EMFKvpRequestReader {
    public WcsGetCapabilitiesRequestReader() {
        super(GetCapabilitiesType.class, Wcs111Factory.eINSTANCE);
    }

    public Object read(Object request, Map kvp, Map rawKvp) throws Exception {
        // make sure we get the right accepts versions param -> workaround for GEOS-1719
        if (rawKvp.containsKey("acceptVersions")) {
            AcceptVersionsKvpParser avp = new AcceptVersionsKvpParser();
            AcceptVersionsType avt =
                    (AcceptVersionsType) avp.parse((String) rawKvp.get("acceptVersions"));
            kvp.put("acceptVersions", avt);
        }
        // make sure we get the right Sections-Type param -> workaround for GEOS-6807
        if (rawKvp.containsKey("sections")) {
            SectionsKvpParser parser = new SectionsKvpParser();
            String value = (String) rawKvp.get("sections");
            EObject sections = (EObject) parser.parse(value);
            kvp.put("sections", sections);
        }
        request = super.read(request, kvp, rawKvp);

        return request;
    }
}

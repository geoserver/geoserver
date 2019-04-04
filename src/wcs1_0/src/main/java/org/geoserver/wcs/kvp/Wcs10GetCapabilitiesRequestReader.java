/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.kvp;

import java.util.Collection;
import java.util.Map;
import net.opengis.ows10.Ows10Factory;
import net.opengis.wcs10.GetCapabilitiesType;
import net.opengis.wcs10.Wcs10Factory;
import org.eclipse.emf.ecore.EObject;
import org.geoserver.ows.kvp.EMFKvpRequestReader;
import org.geoserver.ows.util.KvpUtils;
import org.geotools.xsd.EMFUtils;

/**
 * Parses a GetCapabilities request for WCS into the correspondent model object
 *
 * @author Andrea Aime - TOPP
 */
public class Wcs10GetCapabilitiesRequestReader extends EMFKvpRequestReader {
    public Wcs10GetCapabilitiesRequestReader() {
        super(GetCapabilitiesType.class, Wcs10Factory.eINSTANCE);
    }

    public Object read(Object request, Map kvp, Map rawKvp) throws Exception {
        request = super.read(request, kvp, rawKvp);

        // set the version attribute on the request
        if (kvp.containsKey("version")) {
            String ver = (String) kvp.get("version");
            if (ver != null && "".equals(ver)) {
                ver = null;
            }

            GetCapabilitiesType getCapabilities = (GetCapabilitiesType) request;
            getCapabilities.setVersion(ver);
        }
        if (rawKvp.containsKey("acceptVersions")) {
            String value = (String) rawKvp.get("acceptVersions");
            EObject acceptVersions = Ows10Factory.eINSTANCE.createAcceptVersionsType();
            ((Collection) EMFUtils.get(acceptVersions, "version"))
                    .addAll(KvpUtils.readFlat(value, KvpUtils.INNER_DELIMETER));
            kvp.put("acceptVersions", acceptVersions);
        }
        // make sure we get the right Sections-Type param -> workaround for GEOS-6807
        if (rawKvp.containsKey("sections")) {
            String value = (String) rawKvp.get("sections");
            LOGGER.info("Sections: " + value);
            EObject sections = Ows10Factory.eINSTANCE.createSectionsType();
            ((Collection) EMFUtils.get(sections, "section"))
                    .addAll(KvpUtils.readFlat(value, KvpUtils.INNER_DELIMETER));
            kvp.put("sections", sections);
        }

        return request;
    }
}

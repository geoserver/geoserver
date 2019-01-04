/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.kvp;

import java.util.Collection;
import java.util.Map;
import net.opengis.ows20.AcceptVersionsType;
import net.opengis.ows20.Ows20Factory;
import net.opengis.wcs20.GetCapabilitiesType;
import net.opengis.wcs20.Wcs20Factory;
import org.eclipse.emf.ecore.EObject;
import org.geoserver.ows.kvp.AcceptVersionsKvpParser;
import org.geoserver.ows.kvp.EMFKvpRequestReader;
import org.geoserver.ows.util.KvpUtils;
import org.geotools.xsd.EMFUtils;

/**
 * Parses a GetCapabilities request for WCS into the correspondent model object
 *
 * @author Emanuele Tajariol (etj) - GeoSolutions
 */
@SuppressWarnings(value = {"rawtypes", "unchecked"})
public class WCS20GetCapabilitiesRequestReader extends EMFKvpRequestReader {
    public WCS20GetCapabilitiesRequestReader() {
        super(GetCapabilitiesType.class, Wcs20Factory.eINSTANCE);
    }

    public Object read(Object request, Map kvp, Map rawKvp) throws Exception {

        if (rawKvp.containsKey("acceptVersions")) {
            AcceptVersionsKvpParser avp = new WCS20AcceptVersionsKvpParser();
            String value = (String) rawKvp.get("acceptVersions");
            LOGGER.info("acceptVersions: " + value);
            AcceptVersionsType avt = (AcceptVersionsType) avp.parse(value);
            kvp.put("acceptVersions", avt);
        }
        // make sure we get the right Sections-Type param -> workaround for GEOS-6807
        if (rawKvp.containsKey("sections")) {
            String value = (String) rawKvp.get("sections");
            LOGGER.info("Sections: " + value);
            EObject sections = Ows20Factory.eINSTANCE.createSectionsType();
            ((Collection) EMFUtils.get(sections, "section"))
                    .addAll(KvpUtils.readFlat(value, KvpUtils.INNER_DELIMETER));
            kvp.put("sections", sections);
        }
        request = super.read(request, kvp, rawKvp);

        return request;
    }
}

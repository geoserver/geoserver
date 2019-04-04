/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.kvp;

import static org.vfny.geoserver.wcs.WcsException.WcsExceptionCode.InvalidParameterValue;

import net.opengis.wcs10.CapabilitiesSectionType;
import org.geoserver.ows.KvpParser;
import org.vfny.geoserver.wcs.WcsException;

/**
 * Parses the "sections" GetCapabilities kvp argument
 *
 * @author Andrea Aime - TOPP
 * @author Alessio Fabiani, GeoSolutions
 */
public class SectionKvpParser extends KvpParser {

    public SectionKvpParser() {
        super("section", CapabilitiesSectionType.class);
        setService("WCS");
    }

    @Override
    public Object parse(String value) throws Exception {
        if (CapabilitiesSectionType.get(value) == null)
            throw new WcsException(
                    "Could not find section '" + value + "'", InvalidParameterValue, "section");

        return CapabilitiesSectionType.get(value);
    }
}

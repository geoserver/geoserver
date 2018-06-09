/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.kvp;

import net.opengis.ows20.AcceptVersionsType;
import net.opengis.ows20.Ows20Factory;
import org.geoserver.ows.kvp.AcceptVersionsKvpParser;
import org.geotools.util.Version;

/**
 * Parses the OWS 2.0 capabilities negotiation related AcceptVersion parameter
 *
 * @author Emanuele Tajariol (etj) - GeoSolutions
 */
public class WCS20AcceptVersionsKvpParser extends AcceptVersionsKvpParser {

    public static final String VERSION = "2.0";

    public WCS20AcceptVersionsKvpParser() {
        super(AcceptVersionsType.class);
        setService("wcs");
        setVersion(new Version(VERSION));
    }

    //    public Object parse(String value) throws Exception {
    //        EObject acceptVersions = createObject();
    //        ((Collection)EMFUtils.get(acceptVersions, "version")).addAll(KvpUtils.readFlat(value,
    // KvpUtils.INNER_DELIMETER));
    //        return acceptVersions;
    //    }

    @Override
    protected AcceptVersionsType createObject() {
        return Ows20Factory.eINSTANCE.createAcceptVersionsType();
    }
}

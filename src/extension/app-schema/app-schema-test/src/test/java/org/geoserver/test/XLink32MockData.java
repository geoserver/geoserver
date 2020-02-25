/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2009 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import org.geoserver.data.test.MockData;
import org.geotools.data.complex.AppSchemaDataAccess;

/**
 * Mock data for testing integration of {@link AppSchemaDataAccess} with GeoServer.
 *
 * <p>Inspired by {@link MockData}.
 *
 * @author Niels Charlier
 */
public class XLink32MockData extends AbstractAppSchemaMockData {

    /** @see org.geoserver.test.AbstractAppSchemaMockData#addContent() */
    @Override
    public void addContent() {

        putNamespace("wfs", "http://www.opengis.net/wfs/2.0");
        putNamespace("gml", "http://www.opengis.net/gml/3.2");
        putNamespace("ows", "http://www.opengis.net/ows/1.1");
        putNamespace(GSML_PREFIX, "urn:cgi:xmlns:CGI:GeoSciML-Core:3.0.0");

        addFeatureType(
                GSML_PREFIX,
                "MappedFeature",
                "MappedFeatureXlink32.xml",
                "MappedFeaturePropertyfile.properties");
        addFeatureType(
                GSML_PREFIX, "GeologicUnit", "GeologicUnitXLink32.xml", "GeologicUnit.properties");
        addFeatureType(
                GSML_PREFIX,
                "CompositionPart",
                "CompositionPartXLink32.xml",
                "CompositionPart.properties");
    }
}

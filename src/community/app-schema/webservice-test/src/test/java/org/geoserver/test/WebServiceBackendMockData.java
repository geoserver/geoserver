/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import org.geoserver.data.test.MockData;
import org.geotools.data.complex.AppSchemaDataAccess;

/**
 * Mock data for testing integration of {@link AppSchemaDataAccess} with web service back end.
 *
 * <p>Inspired by {@link MockData}.
 *
 * @author Rini Angreani, CSIRO Earth Science and Resource Engineering
 */
public class WebServiceBackendMockData extends AbstractAppSchemaMockData {
    /** @see org.geoserver.test.AbstractAppSchemaMockData#addContent() */
    @Override
    public void addContent() {
        addFeatureType(
                GSML_PREFIX,
                "MappedFeature",
                "MappedFeatureWebServiceChain.xml",
                "MappedFeatureWebServiceChain.properties",
                "guWithWebService.xml",
                "guXmlResponse.xml");
        addFeatureType(
                GSML_PREFIX,
                "GeologicUnit",
                "guContainer.xml",
                "guXmlResponse.xml",
                "ObservationMethod.xml",
                "ObservationMethod.properties",
                "cpXmlResponse.xml",
                "cpWithWebService.xml");
    }
}

/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import org.geoserver.data.test.MockData;

/**
 * Mock data for testing TimeSeries with list value in app-schema {@link TimeSeriesWfsTest}
 *
 * <p>Inspired by {@link MockData}.
 *
 * @author Rini Angreani (CSIRO Earth Science and Resource Engineering)
 */
public class TimeSeriesMockData extends AbstractAppSchemaMockData {

    /** Prefix for csml namespace. */
    protected static final String CSML_PREFIX = "csml";

    /** URI for csml namespace. */
    protected static final String CSML_URI = "http://ndg.nerc.ac.uk/csml";

    public TimeSeriesMockData() {
        super(GML32_NAMESPACES);
    }

    /** @see org.geoserver.test.AbstractAppSchemaMockData#addContent() */
    @Override
    public void addContent() {
        putNamespace(CSML_PREFIX, CSML_URI);
        addFeatureType(
                CSML_PREFIX,
                "PointSeriesFeature",
                "TimeSeries.xml",
                "timeseries.properties",
                "schemas/csml/csmlMain.xsd");
    }
}

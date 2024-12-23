/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.maps;

import java.util.Collections;
import java.util.TimeZone;
import javax.xml.namespace.QName;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.impl.DimensionInfoImpl;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ogcapi.OGCApiTestSupport;
import org.junit.BeforeClass;

public class MapsTestSupport extends OGCApiTestSupport {
    protected static final QName TIMESERIES = new QName(MockData.SF_URI, "timeseries", MockData.SF_PREFIX);
    static final QName TIME_WITH_START_END = new QName(MockData.SF_URI, "TimeWithStartEnd", MockData.SF_PREFIX);

    static final QName TIME_WITH_START_END_DATE =
            new QName(MockData.SF_URI, "TimeWithStartEndDate", MockData.SF_PREFIX);

    @BeforeClass
    public static void setupTimeZone() throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        testData.setUpDefault();
        testData.setUpDefaultRasterLayers();
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        // add temporal layer
        testData.addRasterLayer(TIMESERIES, "timeseries.zip", null, getCatalog());
        testData.addVectorLayer(
                TIME_WITH_START_END,
                Collections.emptyMap(),
                "TimeElevationWithStartEnd.properties",
                getClass(),
                getCatalog());
        testData.addVectorLayer(
                TIME_WITH_START_END_DATE,
                Collections.emptyMap(),
                "TimeElevationWithStartEndDate.properties",
                getClass(),
                getCatalog());
    }

    protected void setupStartEndTimeDimension(QName typeName, String dimension, String start, String end) {
        FeatureTypeInfo info = getCatalog().getFeatureTypeByName(typeName.getLocalPart());
        DimensionInfo di = new DimensionInfoImpl();
        di.setEnabled(true);
        di.setAttribute(start);
        di.setEndAttribute(end);
        di.setPresentation(DimensionPresentation.LIST);
        info.getMetadata().put(dimension, di);
        getCatalog().save(info);
    }
}

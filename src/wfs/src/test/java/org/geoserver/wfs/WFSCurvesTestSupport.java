/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import java.util.Collections;
import javax.xml.namespace.QName;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.junit.Before;

/**
 * Base class for tests dealing with curve geometry support in WFS
 *
 * @author Andrea Aime - GeoSolutions
 */
public class WFSCurvesTestSupport extends WFSTestSupport {

    protected QName CURVELINES = new QName(MockData.CITE_URI, "curvelines", MockData.CITE_PREFIX);

    protected QName CURVEMULTILINES =
            new QName(MockData.CITE_URI, "curvemultilines", MockData.CITE_PREFIX);

    protected QName CURVEPOLYGONS =
            new QName(MockData.CITE_URI, "curvepolygons", MockData.CITE_PREFIX);

    protected XpathEngine xpath;

    @Override
    protected void setUpInternal(SystemTestData testData) throws Exception {
        // TODO Auto-generated method stub
        super.setUpInternal(testData);

        testData.addWorkspace(MockData.CITE_PREFIX, MockData.CITE_URI, getCatalog());
        testData.addVectorLayer(
                CURVELINES,
                Collections.EMPTY_MAP,
                "curvelines.properties",
                MockData.class,
                getCatalog());
        testData.addVectorLayer(
                CURVEMULTILINES,
                Collections.EMPTY_MAP,
                "curvemultilines.properties",
                MockData.class,
                getCatalog());
        testData.addVectorLayer(
                CURVEPOLYGONS,
                Collections.EMPTY_MAP,
                "curvepolygons.properties",
                MockData.class,
                getCatalog());

        FeatureTypeInfo curveLines = getCatalog().getFeatureTypeByName(getLayerId(CURVELINES));
        curveLines.setCircularArcPresent(true);
        curveLines.setLinearizationTolerance(null);
        getCatalog().save(curveLines);

        FeatureTypeInfo curveMultiLines =
                getCatalog().getFeatureTypeByName(getLayerId(CURVEMULTILINES));
        curveMultiLines.setCircularArcPresent(true);
        curveMultiLines.setLinearizationTolerance(null);
        getCatalog().save(curveMultiLines);
    }

    @Before
    public void setXPath() {
        xpath = XMLUnit.newXpathEngine();
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // do not call super, we only need the curved data sets
    }
}

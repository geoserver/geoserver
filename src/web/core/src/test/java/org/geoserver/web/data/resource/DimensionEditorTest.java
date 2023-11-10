/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.resource;

import static org.junit.Assert.assertEquals;

import java.util.Date;
import javax.xml.namespace.QName;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.web.FormTestPage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Before;
import org.junit.Test;

public class DimensionEditorTest extends GeoServerWicketTestSupport {

    private static final QName V_TIME_ELEVATION =
            new QName(MockData.SF_URI, "TimeElevation", MockData.SF_PREFIX);

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        testData.addVectorLayer(V_TIME_ELEVATION, getCatalog());
    }

    @Before
    public void resetDimensions() {
        // setup time dimension with nearest match
        setupVectorDimension(
                V_TIME_ELEVATION.getLocalPart(),
                ResourceInfo.TIME,
                "time",
                DimensionPresentation.LIST,
                null,
                null,
                null);
        setupNearestMatch(
                V_TIME_ELEVATION,
                ResourceInfo.TIME,
                true,
                "PT10M",
                DimensionInfo.NearestFailBehavior.EXCEPTION,
                false);
        setupVectorDimension(
                V_TIME_ELEVATION.getLocalPart(),
                ResourceInfo.ELEVATION,
                "elevation",
                DimensionPresentation.LIST,
                null,
                null,
                null);
    }

    @Test
    public void testEditVectorTime() throws Exception {
        FeatureTypeInfo ft = getCatalog().getFeatureTypeByName(getLayerId(V_TIME_ELEVATION));
        DimensionInfo time = ft.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
        Model<DimensionInfo> timeModel = new Model<>(time);
        tester.startPage(
                new FormTestPage(
                        id -> new DimensionEditor(id, timeModel, ft, Date.class, true, false)));
        print(tester.getLastRenderedPage(), true, true);

        // check the form
        String prefix = "form:panel:configContainer:configs:";
        tester.assertModelValue(prefix + "attributeContainer:attribute", "time");
        tester.assertModelValue(prefix + "attributeContainer:endAttribute", null);
        tester.assertModelValue(prefix + "presentation", DimensionPresentation.LIST);
        tester.assertInvisible(prefix + "resolutionContainer:resolutions");
        tester.assertModelValue(prefix + "nearestMatchContainer:nearestMatchEnabled", true);
        tester.assertModelValue(
                prefix + "nearestMatchContainer:acceptableIntervalEditor:acceptableInterval",
                "PT10M");
        tester.assertModelValue(
                prefix + "nearestMatchContainer:failedMatchBehaviorContainer:nearestFailBehavior",
                DimensionInfo.NearestFailBehavior.EXCEPTION);

        // try editing the nearest match fail mode
        FormTester form = tester.newFormTester("form");
        String formPrefix = "panel:configContainer:configs:";
        form.select(
                formPrefix
                        + "nearestMatchContainer:failedMatchBehaviorContainer:nearestFailBehavior",
                0);
        form.submit();

        // check the edit worked
        assertEquals(DimensionInfo.NearestFailBehavior.IGNORE, time.getNearestFailBehavior());
    }

    @Test
    public void testEditVectorElevation() throws Exception {
        FeatureTypeInfo ft = getCatalog().getFeatureTypeByName(getLayerId(V_TIME_ELEVATION));
        DimensionInfo elevation = ft.getMetadata().get(ResourceInfo.ELEVATION, DimensionInfo.class);
        Model<DimensionInfo> elevationModel = new Model<>(elevation);
        tester.startPage(
                // elevation does not support nearest match yet, thus "false" at the end
                new FormTestPage(
                        id ->
                                new DimensionEditor(
                                        id, elevationModel, ft, Double.class, false, false)));
        print(tester.getLastRenderedPage(), true, true);

        // check the form
        String prefix = "form:panel:configContainer:configs:";
        tester.assertModelValue(prefix + "attributeContainer:attribute", "elevation");
        tester.assertModelValue(prefix + "attributeContainer:endAttribute", null);
        tester.assertModelValue(prefix + "presentation", DimensionPresentation.LIST);
        tester.assertInvisible(prefix + "resolutionContainer:resolutions");
        // should not be visible
        tester.assertInvisible(prefix + "nearestMatchContainer");
    }
}

package org.geoserver.web.data.resource;

import static org.junit.Assert.assertEquals;

import javax.xml.namespace.QName;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.model.Model;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.web.FormTestPage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Test;

public class CoverageBandsConfigurationPanelTest extends GeoServerWicketTestSupport {

    public static QName HYPER =
            new QName(SystemTestData.WCS_URI, "Hyper", SystemTestData.WCS_PREFIX);

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // nothing
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // one layer with "normal" bands
        testData.addDefaultRasterLayer(SystemTestData.TASMANIA_BM, getCatalog());
        // one with 300+ bands
        testData.addRasterLayer(
                HYPER, "hyper.tiff", "tiff", null, SystemTestData.class, getCatalog());
    }

    @Test
    public void testBands() throws Exception {
        CoverageInfo coverage =
                getCatalog().getCoverageByName(getLayerId(SystemTestData.TASMANIA_BM));
        Model<CoverageInfo> model = new Model<>(coverage);

        FormTestPage page = new FormTestPage(id -> new CoverageBandsConfigurationPanel(id, model));
        tester.startPage(page);

        MarkupContainer container =
                (MarkupContainer)
                        tester.getComponentFromLastRenderedPage(
                                "form:panel:bands:listContainer:items");
        assertEquals(3, container.size());
    }

    @Test
    public void testHyperspectral() throws Exception {
        CoverageInfo coverage = getCatalog().getCoverageByName(getLayerId(HYPER));
        Model<CoverageInfo> model = new Model<>(coverage);

        FormTestPage page = new FormTestPage(id -> new CoverageBandsConfigurationPanel(id, model));
        tester.startPage(page);

        MarkupContainer container =
                (MarkupContainer)
                        tester.getComponentFromLastRenderedPage(
                                "form:panel:bands:listContainer:items");

        // set to non pageable, we don't have support for editing a paging table right now
        assertEquals(326, container.size());
    }
}

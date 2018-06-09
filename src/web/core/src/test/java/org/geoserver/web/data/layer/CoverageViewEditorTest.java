/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layer;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import javax.xml.namespace.QName;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.CoverageView;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Test;

public class CoverageViewEditorTest extends GeoServerWicketTestSupport {

    private static QName TIME_RANGES =
            new QName(MockData.DEFAULT_URI, "timeranges", MockData.DEFAULT_PREFIX);

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        // add raster file to perform some tests
        testData.addRasterLayer(
                TIME_RANGES, "timeranges.zip", null, null, SystemTestData.class, getCatalog());
        testData.addDefaultRasterLayer(SystemTestData.TASMANIA_BM, getCatalog());
    }

    @Test
    public void testSingleBandsIndexIsNotVisible() throws Exception {
        // perform the login as administrator
        login();
        // opening the new coverage view page
        CoverageViewNewPage newPage =
                new CoverageViewNewPage(MockData.DEFAULT_PREFIX, "timeranges", null, null);
        tester.startPage(newPage);
        tester.assertComponent("form:coverages:outputBandsChoice", ListMultipleChoice.class);
        // let's see if we have the correct components instantiated
        tester.assertComponent("form", Form.class);
        tester.assertComponent("form:name", TextField.class);
        tester.assertComponent("form:coverages", CoverageViewEditor.class);
        tester.assertComponent("form:coverages:coveragesChoice", ListMultipleChoice.class);
        tester.assertComponent("form:coverages:outputBandsChoice", ListMultipleChoice.class);
        tester.assertComponent("form:coverages:addBand", Button.class);
        // check the available bands names without any selected band
        CoverageViewEditor coverageViewEditor =
                (CoverageViewEditor) tester.getComponentFromLastRenderedPage("form:coverages");
        coverageViewEditor.setModelObject(null);
        ListMultipleChoice availableBands =
                (ListMultipleChoice)
                        tester.getComponentFromLastRenderedPage("form:coverages:coveragesChoice");
        ListMultipleChoice selectedBands =
                (ListMultipleChoice)
                        tester.getComponentFromLastRenderedPage("form:coverages:outputBandsChoice");
        // select the first band
        FormTester formTester = tester.newFormTester("form");
        formTester.selectMultiple("coverages:coveragesChoice", new int[] {0});
        tester.executeAjaxEvent("form:coverages:addBand", "click");
        // check that the coverage name contains the band index
        assertThat(availableBands.getChoices().size(), is(1));
        assertThat(availableBands.getChoices().get(0), is("time_domainsRanges"));
        assertThat(selectedBands.getChoices().size(), is(1));
        CoverageView.CoverageBand selectedBand =
                (CoverageView.CoverageBand) selectedBands.getChoices().get(0);
        assertThat(selectedBand.getDefinition(), is("time_domainsRanges"));
        // set a name and submit
        formTester.setValue("name", "bands_index_coverage_test");
        formTester.submit("save");
    }

    @Test
    public void testMultiBandsIndexIsVisible() throws Exception {
        // perform the login as administrator
        login();
        // opening the new coverage view page
        CoverageViewNewPage newPage =
                new CoverageViewNewPage(
                        MockData.TASMANIA_BM.getPrefix(),
                        MockData.TASMANIA_BM.getLocalPart(),
                        null,
                        null);
        tester.startPage(newPage);
        tester.assertComponent("form:coverages:outputBandsChoice", ListMultipleChoice.class);
        // check the available bands names without any selected band
        CoverageViewEditor coverageViewEditor =
                (CoverageViewEditor) tester.getComponentFromLastRenderedPage("form:coverages");
        coverageViewEditor.setModelObject(null);
        ListMultipleChoice availableBands =
                (ListMultipleChoice)
                        tester.getComponentFromLastRenderedPage("form:coverages:coveragesChoice");
        ListMultipleChoice selectedBands =
                (ListMultipleChoice)
                        tester.getComponentFromLastRenderedPage("form:coverages:outputBandsChoice");
        // select the first band
        FormTester formTester = tester.newFormTester("form");
        formTester.selectMultiple("coverages:coveragesChoice", new int[] {0});
        tester.executeAjaxEvent("form:coverages:addBand", "click");
        // check that the coverage name contains the band index
        assertThat(availableBands.getChoices().size(), is(3));
        assertThat(availableBands.getChoices().get(0), is("tazbm@0"));
        assertThat(selectedBands.getChoices().size(), is(1));
        CoverageView.CoverageBand selectedBand =
                (CoverageView.CoverageBand) selectedBands.getChoices().get(0);
        assertThat(selectedBand.getDefinition(), is("tazbm@0"));
        // set a name and submit
        formTester.setValue("name", "bands_index_coverage_test");
        formTester.submit("save");
    }
}

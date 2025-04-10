/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.util.List;
import javax.xml.namespace.QName;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.CoverageView;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Test;

public class CoverageViewEditorTest extends GeoServerWicketTestSupport {

    private static QName TIME_RANGES = new QName(MockData.DEFAULT_URI, "timeranges", MockData.DEFAULT_PREFIX);

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        // add raster file to perform some tests
        testData.addRasterLayer(TIME_RANGES, "timeranges.zip", null, null, SystemTestData.class, getCatalog());
        testData.addDefaultRasterLayer(SystemTestData.TASMANIA_BM, getCatalog());
    }

    @Test
    public void testSingleBandsIndexIsNotVisible() throws Exception {
        // perform the login as administrator
        login();
        // opening the new coverage view page
        CoverageViewNewPage newPage = new CoverageViewNewPage(MockData.DEFAULT_PREFIX, "timeranges", null, null);
        tester.startPage(newPage);
        tester.assertComponent("form:coverages:bandChoiceContainer:outputBandsChoice", ListMultipleChoice.class);
        // let's see if we have the correct components instantiated
        tester.assertComponent("form", Form.class);
        tester.assertComponent("form:name", TextField.class);
        tester.assertComponent("form:coverages", CoverageViewEditor.class);
        tester.assertComponent("form:coverages:bandChoiceContainer:coveragesChoice", ListMultipleChoice.class);
        tester.assertComponent("form:coverages:bandChoiceContainer:outputBandsChoice", ListMultipleChoice.class);
        tester.assertComponent("form:coverages:bandChoiceContainer:addBand", Button.class);
        // check the available bands names without any selected band
        CoverageViewEditor coverageViewEditor =
                (CoverageViewEditor) tester.getComponentFromLastRenderedPage("form:coverages");
        coverageViewEditor.setModelObject(null);
        ListMultipleChoice availableBands = (ListMultipleChoice)
                tester.getComponentFromLastRenderedPage("form:coverages:bandChoiceContainer:coveragesChoice");
        ListMultipleChoice selectedBands = (ListMultipleChoice)
                tester.getComponentFromLastRenderedPage("form:coverages:bandChoiceContainer:outputBandsChoice");
        // select the first band
        FormTester formTester = tester.newFormTester("form");
        formTester.selectMultiple("coverages:bandChoiceContainer:coveragesChoice", new int[] {0});
        tester.executeAjaxEvent("form:coverages:bandChoiceContainer:addBand", "click");
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
        CoverageViewNewPage newPage = new CoverageViewNewPage(
                MockData.TASMANIA_BM.getPrefix(), MockData.TASMANIA_BM.getLocalPart(), null, null);
        tester.startPage(newPage);
        tester.assertComponent("form:coverages:bandChoiceContainer:outputBandsChoice", ListMultipleChoice.class);
        // check the available bands names without any selected band
        CoverageViewEditor coverageViewEditor =
                (CoverageViewEditor) tester.getComponentFromLastRenderedPage("form:coverages");
        coverageViewEditor.setModelObject(null);
        ListMultipleChoice availableBands = (ListMultipleChoice)
                tester.getComponentFromLastRenderedPage("form:coverages:bandChoiceContainer:coveragesChoice");
        ListMultipleChoice selectedBands = (ListMultipleChoice)
                tester.getComponentFromLastRenderedPage("form:coverages:bandChoiceContainer:outputBandsChoice");
        // select the first band
        FormTester formTester = tester.newFormTester("form");
        formTester.selectMultiple("coverages:bandChoiceContainer:coveragesChoice", new int[] {0});
        tester.executeAjaxEvent("form:coverages:bandChoiceContainer:addBand", "click");
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

    @Test
    public void testJiffleModeCreatesCorrectBands() throws Exception {
        login();

        // Open the CoverageViewNewPage for a multi-band coverage
        CoverageViewNewPage newPage = new CoverageViewNewPage(
                MockData.TASMANIA_BM.getPrefix(), MockData.TASMANIA_BM.getLocalPart(), null, null);
        tester.startPage(newPage);

        // Set editor mode to JIFFLE
        FormTester formTester = tester.newFormTester("form");
        formTester.select("coverages:compositionMode", 1); // assuming 1 = JIFFLE
        tester.executeAjaxEvent("form:coverages:compositionMode", "change");

        // Input Jiffle script
        String jiffleScript = "res[0] = tazbm[0];\nres[1] = tazbm[1];\nres[2] = (tazbm[2] + 10);";

        @SuppressWarnings("unchecked")
        TextField<String> jiffleOutput = (TextField<String>)
                tester.getComponentFromLastRenderedPage("form:coverages:jiffleEditorContainer:jiffleOutputName");
        jiffleOutput.setModelObject("res");
        @SuppressWarnings("unchecked")
        TextArea<String> jiffleFormulaField = (TextArea<String>)
                tester.getComponentFromLastRenderedPage("form:coverages:jiffleEditorContainer:jiffleFormula");
        jiffleFormulaField.setModelObject(jiffleScript);

        // Set a coverage view name
        formTester.setValue("name", "jiffle_based_view");
        CoverageViewEditor coverageViewEditor =
                (CoverageViewEditor) tester.getComponentFromLastRenderedPage("form:coverages");
        coverageViewEditor.validateAndSave();

        // Submit form
        formTester.submit("save");

        List<String> availableCoverages = coverageViewEditor.availableCoverages;
        assertThat(availableCoverages.get(0), is("tazbm@0"));
        assertThat(availableCoverages.get(1), is("tazbm@1"));
        assertThat(availableCoverages.get(2), is("tazbm@2"));

        List<? extends CoverageView.CoverageBand> bands = coverageViewEditor.outputBandsChoice.getChoices();

        // Check output band names and definitions
        assertThat(bands.get(0).getDefinition(), is("res@0"));
        assertThat(bands.get(1).getDefinition(), is("res@1"));
        assertThat(bands.get(2).getDefinition(), is("res@2"));
    }
}

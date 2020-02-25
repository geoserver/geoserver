/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.web;

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.apache.wicket.markup.repeater.OddEvenItem;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wps.ProcessGroupInfo;
import org.geoserver.wps.ProcessInfo;
import org.geoserver.wps.ProcessInfoImpl;
import org.geoserver.wps.WPSInfo;
import org.geoserver.wps.validator.MaxSizeValidator;
import org.geoserver.wps.validator.MultiplicityValidator;
import org.geoserver.wps.validator.NumberRangeValidator;
import org.geoserver.wps.web.FilteredProcessesProvider.FilteredProcess;
import org.geoserver.wps.web.ProcessLimitsPage.InputLimit;
import org.geotools.feature.NameImpl;
import org.geotools.process.raster.RasterProcessFactory;
import org.geotools.util.NumberRange;
import org.junit.Test;

public class ProcessLimitsPageTest extends WPSPagesTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // add some limits to the processes
        WPSInfo wps = getGeoServer().getService(WPSInfo.class);

        ProcessGroupInfo rasterGroup = getRasterGroup(wps.getProcessGroups());

        // for the buffer process
        ProcessInfo contour = new ProcessInfoImpl();
        contour.setEnabled(true);
        contour.setName(new NameImpl("ras", "Contour"));
        contour.getValidators().put("data", new MaxSizeValidator(1));
        contour.getValidators()
                .put(
                        "levels",
                        new NumberRangeValidator(
                                new NumberRange<Double>(Double.class, -8000d, 8000d)));
        contour.getValidators().put("levels", new MultiplicityValidator(3));
        rasterGroup.getFilteredProcesses().add(contour);

        // save
        getGeoServer().save(wps);
    }

    @Test
    public void test() throws Exception {
        login();
        WPSInfo wps = getGeoServerApplication().getGeoServer().getService(WPSInfo.class);
        ProcessGroupInfo rasterGroup = getRasterGroup(wps.getProcessGroups());
        ProcessInfo pi = getProcess(rasterGroup.getFilteredProcesses(), "Contour");

        // start the pages
        WPSAccessRulePage accessRulePage =
                (WPSAccessRulePage) tester.startPage(new WPSAccessRulePage());
        ProcessSelectionPage selectionPage =
                (ProcessSelectionPage)
                        tester.startPage(new ProcessSelectionPage(accessRulePage, rasterGroup));
        FilteredProcess filteredProcess = new FilteredProcess(pi.getName(), "");
        filteredProcess.setValidators(pi.getValidators());
        ProcessLimitsPage limitsPage =
                (ProcessLimitsPage)
                        tester.startPage(new ProcessLimitsPage(selectionPage, filteredProcess));

        // print(limitsPage, true, true);

        // grab the table and check its contents (the order should be stable, we are iterating over
        // the process inputs)
        OddEvenItem item =
                (OddEvenItem)
                        tester.getComponentFromLastRenderedPage("form:table:listContainer:items:1");
        // max input size
        InputLimit il = (InputLimit) item.getDefaultModelObject();
        assertEquals("data", il.getName());
        assertEquals(
                Integer.valueOf(1),
                item.get("itemProperties:2:component:text").getDefaultModelObject());
        // levels range validator
        item =
                (OddEvenItem)
                        tester.getComponentFromLastRenderedPage("form:table:listContainer:items:3");
        il = (InputLimit) item.getDefaultModelObject();
        assertEquals("levels", il.getName());
        assertEquals(
                new NumberRange(Double.class, -8000d, 8000d),
                item.get("itemProperties:2:component:range").getDefaultModelObject());
        // multiplicity validator
        item =
                (OddEvenItem)
                        tester.getComponentFromLastRenderedPage("form:table:listContainer:items:4");
        il = (InputLimit) item.getDefaultModelObject();
        assertEquals("levels", il.getName());
        assertEquals(
                Integer.valueOf(3),
                item.get("itemProperties:2:component:text").getDefaultModelObject());
    }

    private ProcessInfo getProcess(List<ProcessInfo> filteredProcesses, String name) {
        for (ProcessInfo pi : filteredProcesses) {
            if (pi.getName().getLocalPart().equals(name)) {
                return pi;
            }
        }

        return null;
    }

    private ProcessGroupInfo getRasterGroup(List<ProcessGroupInfo> processGroups) {
        for (ProcessGroupInfo pgi : processGroups) {
            if (pgi.getFactoryClass().equals(RasterProcessFactory.class)) {
                return pgi;
            }
        }

        return null;
    }
}

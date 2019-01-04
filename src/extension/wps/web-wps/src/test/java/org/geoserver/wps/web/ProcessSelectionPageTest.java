/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.web;

import static org.junit.Assert.assertEquals;

import java.util.Iterator;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.markup.repeater.OddEvenItem;
import org.apache.wicket.markup.repeater.data.DataView;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wps.ProcessGroupInfo;
import org.geoserver.wps.ProcessInfo;
import org.geoserver.wps.ProcessInfoImpl;
import org.geoserver.wps.WPSInfo;
import org.geoserver.wps.validator.MaxSizeValidator;
import org.geoserver.wps.validator.NumberRangeValidator;
import org.geoserver.wps.web.FilteredProcessesProvider.FilteredProcess;
import org.geotools.feature.NameImpl;
import org.geotools.process.geometry.GeometryProcessFactory;
import org.geotools.util.NumberRange;
import org.junit.Test;

public class ProcessSelectionPageTest extends WPSPagesTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // add some limits to the processes
        WPSInfo wps = getGeoServer().getService(WPSInfo.class);

        ProcessGroupInfo geoGroup = getGeoGroup(wps.getProcessGroups());

        // for the buffer process
        ProcessInfo buffer = new ProcessInfoImpl();
        buffer.setEnabled(true);
        buffer.setName(new NameImpl("geo", "buffer"));
        buffer.getValidators().put("geom", new MaxSizeValidator(1));
        buffer.getValidators()
                .put(
                        "distance",
                        new NumberRangeValidator(new NumberRange<Double>(Double.class, 0d, 100d)));
        buffer.getValidators()
                .put(
                        "quadrantSegments",
                        new NumberRangeValidator(new NumberRange<Integer>(Integer.class, 2, 20)));
        geoGroup.getFilteredProcesses().add(buffer);

        // save
        getGeoServer().save(wps);
    }

    @Test
    public void test() throws Exception {
        login();
        WPSInfo wps = getGeoServerApplication().getGeoServer().getService(WPSInfo.class);
        ProcessGroupInfo pgi = getGeoGroup(wps.getProcessGroups());

        // start the page
        WPSAccessRulePage accessRulePage =
                (WPSAccessRulePage) tester.startPage(new WPSAccessRulePage());
        ProcessSelectionPage selectionPage =
                (ProcessSelectionPage)
                        tester.startPage(new ProcessSelectionPage(accessRulePage, pgi));

        // print(selectionPage, true, true);

        // grab the table and check its contents
        DataView datas =
                (DataView)
                        tester.getComponentFromLastRenderedPage(
                                "form:selectionTable:listContainer:items");
        for (Iterator it = datas.getItems(); it.hasNext(); ) {
            OddEvenItem item = (OddEvenItem) it.next();
            FilteredProcess fp = (FilteredProcess) item.getDefaultModelObject();

            Component validatedLabel = item.get("itemProperties:5:component");
            if (fp.getName().getLocalPart().equals("buffer")) {
                assertEquals("*", validatedLabel.getDefaultModelObject());
            } else {
                assertEquals("", validatedLabel.getDefaultModelObject());
            }
        }
    }

    private ProcessGroupInfo getGeoGroup(List<ProcessGroupInfo> processGroups) {
        for (ProcessGroupInfo pgi : processGroups) {
            if (pgi.getFactoryClass().equals(GeometryProcessFactory.class)) {
                return pgi;
            }
        }

        return null;
    }
}

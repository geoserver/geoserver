/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.wicket.Select2DropDownChoice;
import org.geotools.data.wfs.WFSDataStoreFactory;
import org.junit.Test;

public class WFSStoreNewPageTest extends GeoServerWicketTestSupport {

    @Test
    public void testSelect2UniqueIdentifiers() {
        // a store that will generate multiple select2 dropdowns
        WFSDataStoreFactory factory = new WFSDataStoreFactory();
        final String factoryName = factory.getDisplayName();

        // start the page
        login();
        final AbstractDataAccessPage page = new DataAccessNewPage(factoryName);
        tester.startPage(page);
        tester.assertRenderedPage(DataAccessNewPage.class);

        // check the identifiers are indeed unique
        Set<String> identifiers = new HashSet<>();
        tester.getLastRenderedPage()
                .visitChildren(
                        Select2DropDownChoice.class,
                        (component, visit) -> {
                            String markupId = component.getMarkupId();
                            if (!identifiers.add(markupId))
                                fail("Duplicate identifier " + markupId);
                        });

        // and are in the expected number
        long parametersWithOptions =
                Arrays.stream(factory.getParametersInfo())
                        .filter(p -> new ParamInfo(p).getOptions() != null)
                        .count();
        assertEquals(parametersWithOptions, identifiers.size());
    }
}

/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.demo;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Test;

public class SRSDescriptionPageTest extends GeoServerWicketTestSupport {

    @Test
    public void testCodeEscaped() {
        String code = "','',[],0);foo('";
        PageParameters parameters = new PageParameters();
        parameters.add("code", code);
        tester.startPage(SRSDescriptionPage.class, parameters);
        tester.assertRenderedPage(SRSDescriptionPage.class);
        String html = tester.getLastResponseAsString();
        assertThat(html, not(containsString(code)));
        assertThat(html, containsString("\\',\\'\\',[],0);foo(\\'"));
    }

    @Test
    public void testIAUCode() {
        PageParameters parameters = new PageParameters();
        parameters.add("code", "IAU:1000");
        tester.startPage(SRSDescriptionPage.class, parameters);
        tester.assertRenderedPage(SRSDescriptionPage.class);

        // the SRS has no area of validity defined, the components should now show up
        tester.assertInvisible("areaOfValidityText");
        tester.assertInvisible("areaOfValidityMap");
    }
}

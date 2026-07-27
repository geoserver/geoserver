/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;

import java.util.List;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.TagTester;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Test;

public class GsIconTest extends GeoServerWicketTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // no data needed
    }

    /** GsIcon must render an {@code <i>} element, not an {@code <img>}. */
    @Test
    public void testRendersAsITag() {
        tester.startComponentInPage(new GsIcon("icon", "gs-icon-accept"));

        List<TagTester> iTags = TagTester.createTags(
                tester.getLastResponseAsString(),
                tag -> "i".equalsIgnoreCase(tag.getName())
                        && tag.getAttribute("class") != null
                        && tag.getAttribute("class").toString().contains("gs-icon-accept"),
                false);
        assertThat("GsIcon must render an <i> element", iTags, not(empty()));
        assertEquals("i", iTags.get(0).getName());
    }

    /** The rendered element must carry both the {@code gs-icon} base class and the icon-specific class. */
    @Test
    public void testClassAttribute() {
        tester.startComponentInPage(new GsIcon("icon", "gs-icon-wrench"));

        assertThat(tester.getLastResponseAsString(), containsString("class=\"gs-icon gs-icon-wrench\""));
    }

    /** GsIcon must never produce an {@code <img>} element. */
    @Test
    public void testNoImgTags() {
        tester.startComponentInPage(new GsIcon("icon", "gs-icon-add"));

        List<TagTester> imgTags = TagTester.createTags(
                tester.getLastResponseAsString(), tag -> "img".equalsIgnoreCase(tag.getName()), false);
        assertThat("GsIcon must not render any <img> elements", imgTags, empty());
    }

    /** The {@code IModel<String>} constructor must produce the same output as the {@code String} constructor. */
    @Test
    public void testIModelConstructor() {
        tester.startComponentInPage(new GsIcon("icon", Model.of("gs-icon-tick")));

        assertThat(tester.getLastResponseAsString(), containsString("class=\"gs-icon gs-icon-tick\""));
    }

    /** When the model object changes the re-rendered output must reflect the new class. */
    @Test
    public void testDynamicModel() {
        Model<String> model = Model.of("gs-icon-accept");
        GsIcon icon = new GsIcon("icon", model);

        tester.startComponentInPage(icon);
        assertThat(tester.getLastResponseAsString(), containsString("gs-icon-accept"));

        model.setObject("gs-icon-error");
        tester.startComponentInPage(icon);
        assertThat(tester.getLastResponseAsString(), containsString("gs-icon-error"));
        assertThat(tester.getLastResponseAsString(), not(containsString("gs-icon-accept")));
    }
}

/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.layer;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.AbstractTextComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.web.ComponentBuilder;
import org.geoserver.web.FormTestPage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geowebcache.filter.parameters.IntegerParameterFilter;
import org.junit.Before;
import org.junit.Test;

public class IntegerParameterFilterSubformTest extends GeoServerWicketTestSupport {

    private IModel<IntegerParameterFilter> model;

    private IntegerParameterFilter pf;

    @Before
    public void setUpInternal() throws Exception {
        pf = new IntegerParameterFilter();
        pf.setKey("TEST");
        model = Model.of(pf);
    }

    @Test
    public void testPageLoad() {
        startPage();

        tester.assertComponent("form:panel:defaultValue", AbstractTextComponent.class);
        tester.assertComponent("form:panel:values", AbstractTextComponent.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testLoadDefaultValues() {
        startPage();

        AbstractTextComponent<String> defaultValue =
                (AbstractTextComponent<String>)
                        tester.getComponentFromLastRenderedPage("form:panel:defaultValue");
        AbstractTextComponent<List<Integer>> values =
                (AbstractTextComponent<List<Integer>>)
                        tester.getComponentFromLastRenderedPage("form:panel:values");

        assertThat(defaultValue.getValue(), equalTo(""));
        assertThat(values.getValue(), equalTo(""));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testLoadSpecifiedValues() {
        pf.setDefaultValue("testDefault");
        pf.setValues(Arrays.asList(1, 2));
        startPage();

        AbstractTextComponent<String> defaultValue =
                (AbstractTextComponent<String>)
                        tester.getComponentFromLastRenderedPage("form:panel:defaultValue");
        AbstractTextComponent<List<Integer>> values =
                (AbstractTextComponent<List<Integer>>)
                        tester.getComponentFromLastRenderedPage("form:panel:values");

        assertThat(defaultValue.getValue(), equalTo("testDefault"));
        assertThat(values.getValue(), equalTo("1\r\n2"));
    }

    @Test
    public void testChange() {
        startPage();

        FormTester formTester = tester.newFormTester("form");
        formTester.setValue("panel:defaultValue", "testDefault");
        formTester.setValue("panel:values", "1\r\n2");
        formTester.submit();

        assertThat(pf.getDefaultValue(), equalTo("testDefault"));
        assertThat(pf.getValues(), contains(1, 2));
    }

    private void startPage() {
        tester.startPage(
                new FormTestPage(
                        new ComponentBuilder() {
                            /** serialVersionUID */
                            private static final long serialVersionUID = 1L;

                            public Component buildComponent(final String id) {
                                return new IntegerParameterFilterSubform(id, model);
                            }
                        }));
    }
}

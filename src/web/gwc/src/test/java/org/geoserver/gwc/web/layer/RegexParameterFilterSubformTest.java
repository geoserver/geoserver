/* (c) 2015 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.layer;

import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Locale;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.AbstractSingleSelectChoice;
import org.apache.wicket.markup.html.form.AbstractTextComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.web.ComponentBuilder;
import org.geoserver.web.FormTestPage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geowebcache.filter.parameters.CaseNormalizer;
import org.geowebcache.filter.parameters.CaseNormalizer.Case;
import org.geowebcache.filter.parameters.RegexParameterFilter;
import org.junit.Before;
import org.junit.Test;

public class RegexParameterFilterSubformTest extends GeoServerWicketTestSupport {

    private IModel<RegexParameterFilter> model;

    private RegexParameterFilter pf;

    @Before
    public void setUpInternal() throws Exception {
        pf = new RegexParameterFilter();
        pf.setKey("TEST");
        model = Model.of(pf);
    }

    @Test
    public void testPageLoad() {
        startPage();

        tester.assertComponent("form:panel:defaultValue", AbstractTextComponent.class);
        tester.assertComponent("form:panel:regex", AbstractTextComponent.class);
        tester.assertComponent("form:panel:normalize", CaseNormalizerSubform.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testLoadDefaultValues() {
        startPage();

        AbstractTextComponent<String> defaultValue =
                (AbstractTextComponent<String>)
                        tester.getComponentFromLastRenderedPage("form:panel:defaultValue");
        AbstractTextComponent<String> regex =
                (AbstractTextComponent<String>)
                        tester.getComponentFromLastRenderedPage("form:panel:regex");
        AbstractSingleSelectChoice<Case> kase =
                (AbstractSingleSelectChoice<Case>)
                        tester.getComponentFromLastRenderedPage("form:panel:normalize:case");
        AbstractSingleSelectChoice<Locale> locale =
                (AbstractSingleSelectChoice<Locale>)
                        tester.getComponentFromLastRenderedPage("form:panel:normalize:locale");

        assertThat(defaultValue.getValue(), equalTo(""));
        assertThat(regex.getValue(), equalTo(""));
        assertThat(kase.getValue(), equalTo("NONE"));
        assertThat(locale.getValue(), equalTo(""));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testLoadSpecifiedValues() {
        pf.setDefaultValue("testDefault");
        pf.setRegex("testRegex");
        pf.setNormalize(new CaseNormalizer(Case.UPPER, Locale.CANADA));
        startPage();

        AbstractTextComponent<String> defaultValue =
                (AbstractTextComponent<String>)
                        tester.getComponentFromLastRenderedPage("form:panel:defaultValue");
        AbstractTextComponent<String> regex =
                (AbstractTextComponent<String>)
                        tester.getComponentFromLastRenderedPage("form:panel:regex");
        AbstractSingleSelectChoice<Case> kase =
                (AbstractSingleSelectChoice<Case>)
                        tester.getComponentFromLastRenderedPage("form:panel:normalize:case");
        AbstractSingleSelectChoice<Locale> locale =
                (AbstractSingleSelectChoice<Locale>)
                        tester.getComponentFromLastRenderedPage("form:panel:normalize:locale");

        assertThat(defaultValue.getValue(), equalTo("testDefault"));
        assertThat(regex.getValue(), equalTo("testRegex"));
        assertThat(kase.getValue(), equalTo("UPPER"));
        assertThat(locale.getValue(), equalTo("en_CA"));
    }

    @Test
    public void testChange() {
        startPage();

        FormTester formTester = tester.newFormTester("form");
        formTester.setValue("panel:defaultValue", "testDefault");
        formTester.setValue("panel:regex", "testRegex");
        formTester.setValue("panel:normalize:case", "UPPER");
        formTester.setValue("panel:normalize:locale", "en_CA");
        formTester.submit();

        assertThat(pf.getDefaultValue(), equalTo("testDefault"));
        assertThat(pf.getRegex(), equalTo("testRegex"));
        assertThat(
                pf.getNormalize(),
                both(hasProperty("case", is(Case.UPPER)))
                        .and(hasProperty("locale", is(Locale.CANADA))));
    }

    private void startPage() {
        tester.startPage(
                new FormTestPage(
                        new ComponentBuilder() {
                            /** serialVersionUID */
                            private static final long serialVersionUID = 1L;

                            public Component buildComponent(final String id) {
                                return new RegexParameterFilterSubform(id, model);
                            }
                        }));
    }
}

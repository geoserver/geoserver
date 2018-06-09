/* (c) 2015 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.layer;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.Locale;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.AbstractSingleSelectChoice;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.web.ComponentBuilder;
import org.geoserver.web.FormTestPage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geowebcache.filter.parameters.CaseNormalizer;
import org.geowebcache.filter.parameters.CaseNormalizer.Case;
import org.junit.Before;
import org.junit.Test;

public class CaseNormalizerSubformTest extends GeoServerWicketTestSupport {

    private IModel<CaseNormalizer> model;

    private CaseNormalizer cn;

    @Before
    public void setUpInternal() throws Exception {
        cn = new CaseNormalizer();
        model = Model.of(cn);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPageLoad() {
        startPage();

        tester.assertComponent("form:panel:case", AbstractSingleSelectChoice.class);
        tester.assertComponent("form:panel:locale", AbstractSingleSelectChoice.class);

        AbstractSingleSelectChoice<Case> kase =
                (AbstractSingleSelectChoice<Case>)
                        tester.getComponentFromLastRenderedPage("form:panel:case");

        AbstractSingleSelectChoice<Locale> locale =
                (AbstractSingleSelectChoice<Locale>)
                        tester.getComponentFromLastRenderedPage("form:panel:locale");

        assertThat(kase.isNullValid(), is(false));
        assertThat(locale.isNullValid(), is(true));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testLoadDefaultValues() {
        startPage();

        AbstractSingleSelectChoice<Case> kase =
                (AbstractSingleSelectChoice<Case>)
                        tester.getComponentFromLastRenderedPage("form:panel:case");

        AbstractSingleSelectChoice<Locale> locale =
                (AbstractSingleSelectChoice<Locale>)
                        tester.getComponentFromLastRenderedPage("form:panel:locale");

        assertThat(kase.getValue(), equalTo("NONE"));
        assertThat(locale.getValue(), equalTo(""));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testLoadSpecifiedValues() {
        cn = new CaseNormalizer(Case.UPPER, Locale.CANADA);
        model = Model.of(cn);
        startPage();

        AbstractSingleSelectChoice<Case> kase =
                (AbstractSingleSelectChoice<Case>)
                        tester.getComponentFromLastRenderedPage("form:panel:case");

        AbstractSingleSelectChoice<Locale> locale =
                (AbstractSingleSelectChoice<Locale>)
                        tester.getComponentFromLastRenderedPage("form:panel:locale");

        assertThat(kase.getValue(), equalTo("UPPER"));
        assertThat(locale.getValue(), equalTo("en_CA"));
    }

    @Test
    public void testChangeFromDefault() {
        startPage();

        FormTester formTester = tester.newFormTester("form");
        formTester.setValue("panel:case", "UPPER");
        formTester.setValue("panel:locale", "en_CA");
        formTester.submit();

        assertThat(cn.getCase(), is(Case.UPPER));
        assertThat(cn.getConfiguredLocale(), is(Locale.CANADA));
    }

    @Test
    public void testChange() {
        cn = new CaseNormalizer(Case.LOWER, Locale.TAIWAN);
        model = Model.of(cn);
        startPage();

        FormTester formTester = tester.newFormTester("form");
        formTester.setValue("panel:case", "UPPER");
        formTester.setValue("panel:locale", "en_CA");
        formTester.submit();

        assertThat(cn.getCase(), is(Case.UPPER));
        assertThat(cn.getConfiguredLocale(), is(Locale.CANADA));
    }

    @Test
    public void testChangeToDefault() {
        cn = new CaseNormalizer(Case.LOWER, Locale.TAIWAN);
        model = Model.of(cn);
        startPage();

        FormTester formTester = tester.newFormTester("form");
        formTester.setValue("panel:case", "NONE");
        formTester.setValue("panel:locale", "-1");
        formTester.submit();

        assertThat(cn.getCase(), is(Case.NONE));
        assertThat(cn.getConfiguredLocale(), nullValue());
        assertThat(cn.getLocale(), instanceOf(Locale.class));
    }

    private void startPage() {
        tester.startPage(
                new FormTestPage(
                        new ComponentBuilder() {
                            /** serialVersionUID */
                            private static final long serialVersionUID = 1L;

                            public Component buildComponent(final String id) {
                                return new CaseNormalizerSubform(id, model);
                            }
                        }));
    }
}

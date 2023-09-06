/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import static org.junit.Assert.assertEquals;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;

public class CRSPanelTestPage extends WebPage {

    public CRSPanelTestPage() {
        Form form = new Form("form");
        add(form);

        form.add(new CRSPanel("crs", new CRSModel(null)));
    }

    public CRSPanelTestPage(String expectedSRS) {
        Form form = new Form("form");
        add(form);

        form.add(
                new CRSPanel("crs", new CRSModel(null)) {
                    @Override
                    protected void onSRSUpdated(String srs, AjaxRequestTarget target) {
                        assertEquals(expectedSRS, srs);
                    }
                });
    }

    public CRSPanelTestPage(Object o) {
        Form<Object> form = new Form<>("form", new CompoundPropertyModel<>(o));
        add(form);

        form.add(new CRSPanel("crs"));
    }

    public CRSPanelTestPage(IModel<CoordinateReferenceSystem> model) {
        Form<Object> form = new Form<>("form");
        add(form);

        form.add(new CRSPanel("crs", model));
    }

    public CRSPanelTestPage(CoordinateReferenceSystem crs) {
        Form form = new Form("form");
        add(form);

        form.add(new CRSPanel("crs", crs));
    }
}

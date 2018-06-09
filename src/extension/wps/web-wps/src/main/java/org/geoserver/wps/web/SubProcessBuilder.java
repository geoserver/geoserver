/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.web;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;

public class SubProcessBuilder extends WebPage {

    public SubProcessBuilder(ExecuteRequest request, final ModalWindow window) {
        Form form = new Form("form");
        add(form);

        final WPSRequestBuilderPanel builder = new WPSRequestBuilderPanel("builder", request);
        form.add(builder);

        form.add(
                new AjaxSubmitLink("apply") {

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form form) {
                        window.close(target);
                    }

                    @Override
                    protected void onError(AjaxRequestTarget target, Form form) {
                        super.onError(target, form);
                        target.add(builder.getFeedbackPanel());
                    }
                });
    }
}

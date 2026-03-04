/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.web;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.geoserver.web.wicket.GSModalWindow;

// TODO WICKET8 - Verify this page works OK
public class SubProcessBuilder extends Panel {

    private static final boolean isCssEmpty = org.geoserver.web.util.WebUtils.IsWicketCssFileEmpty(
            java.lang.invoke.MethodHandles.lookup().lookupClass());

    @Override
    public void renderHead(org.apache.wicket.markup.head.IHeaderResponse response) {
        super.renderHead(response);
        // if the panel-specific CSS file contains actual css then have the browser load the css
        if (!isCssEmpty) {
            response.render(org.apache.wicket.markup.head.CssHeaderItem.forReference(
                    new org.apache.wicket.request.resource.PackageResourceReference(
                            getClass(), getClass().getSimpleName() + ".css")));
        }
    }

    public SubProcessBuilder(ExecuteRequest request, final GSModalWindow window) {
        super(window.getContentId());
        Form form = new Form<>("form");
        add(form);

        final WPSRequestBuilderPanel builder = new WPSRequestBuilderPanel("builder", request);
        form.add(builder);

        form.add(new AjaxSubmitLink("apply") {

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                window.close(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                super.onError(target);
                target.add(builder.getFeedbackPanel());
            }
        });
    }
}

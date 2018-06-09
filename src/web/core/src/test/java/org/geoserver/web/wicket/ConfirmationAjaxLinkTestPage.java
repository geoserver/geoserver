/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;

public class ConfirmationAjaxLinkTestPage extends WebPage {

    public ConfirmationAjaxLinkTestPage() {
        Form form = new Form("form");
        add(form);

        ConfirmationAjaxLink<String> link =
                new ConfirmationAjaxLink<String>(
                        "confirmationLink",
                        new Model<String>("model"),
                        new Model("label"),
                        new Model("'confirmation'")) {

                    @Override
                    protected void onClick(AjaxRequestTarget target) {
                        // nothing to do

                    }
                };
        form.add(link);
    }
}

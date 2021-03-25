/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.admin;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.geoserver.config.ContactInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.web.GeoserverAjaxSubmitLink;

public class ContactPage extends ServerAdminPage {

    private final IModel<GeoServer> geoServerModel;
    private final IModel<ContactInfo> contactModel;

    public ContactPage() {
        geoServerModel = getGeoServerModel();
        contactModel = getContactInfoModel();

        Form<ContactInfo> form = new Form<>("form", new CompoundPropertyModel<>(contactModel));
        add(form);

        form.add(new ContactPanel("contact", contactModel));
        form.add(
                new Button("submit") {
                    @Override
                    public void onSubmit() {
                        save(true);
                    }
                });
        form.add(applyLink(form));
        form.add(
                new Button("cancel") {
                    @Override
                    public void onSubmit() {
                        doReturn();
                    }
                });
    }

    public void save(boolean doReturn) {
        GeoServer gs = geoServerModel.getObject();
        GeoServerInfo global = gs.getGlobal();
        global.getSettings().setContact(contactModel.getObject());
        gs.save(global);
        if (doReturn) {
            doReturn();
        }
    }

    private GeoserverAjaxSubmitLink applyLink(Form<?> form) {
        return new GeoserverAjaxSubmitLink("apply", form, this) {

            @Override
            protected void onError(AjaxRequestTarget target, Form form) {
                super.onError(target, form);
                target.add(form);
            }

            @Override
            protected void onSubmitInternal(AjaxRequestTarget target, Form<?> form) {
                try {
                    save(false);
                } catch (IllegalArgumentException e) {
                    form.error(e.getMessage());
                    target.add(form);
                }
            }
        };
    }
}

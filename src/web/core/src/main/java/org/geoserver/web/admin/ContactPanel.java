/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.admin;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.config.ContactInfo;
import org.geoserver.web.EmailAddressValidator;
import org.geoserver.web.InternationalStringPanel;
import org.geoserver.web.StringAndInternationalStringPanel;

public class ContactPanel extends Panel {

    public ContactPanel(String id, final IModel<ContactInfo> model) {
        super(id, model);
        add(new StringAndInternationalStringPanel("contactOrganization", model, this));
        add(new StringAndInternationalStringPanel("onlineResource", model, this));

        // setup the "welcome" text as either a textarea (non-international)
        // or as a set of textareas (international).
        WebMarkupContainer abstractLabelContainer = new WebMarkupContainer("welcomeLabel");
        abstractLabelContainer.add(
                new Label("welcomeLabel", new StringResourceModel("welcome", this)));
        add(abstractLabelContainer);
        TextArea<String> area = new TextArea<>("welcome", new PropertyModel<>(model, "welcome"));
        add(area);
        InternationalStringPanel<TextArea<String>> internationalStringPanelAbstract =
                new InternationalStringPanel<TextArea<String>>(
                        "internationalWelcome",
                        new PropertyModel<>(model, "internationalWelcome"),
                        area,
                        abstractLabelContainer) {
                    @Override
                    protected TextArea<String> getTextComponent(String id, IModel<String> model) {
                        return new TextArea<>(id, model);
                    }
                };
        add(internationalStringPanelAbstract);

        add(new StringAndInternationalStringPanel("contactPerson", model, this));
        add(new StringAndInternationalStringPanel("contactPosition", model, this));
        add(new StringAndInternationalStringPanel("addressType", model, this));
        add(new StringAndInternationalStringPanel("address", model, this));
        add(new StringAndInternationalStringPanel("addressDeliveryPoint", model, this));
        add(new StringAndInternationalStringPanel("addressCity", model, this));
        add(new StringAndInternationalStringPanel("addressState", model, this));
        add(new StringAndInternationalStringPanel("addressPostalCode", model, this));
        add(new StringAndInternationalStringPanel("addressCountry", model, this));
        add(new StringAndInternationalStringPanel("contactVoice", model, this));
        add(new StringAndInternationalStringPanel("contactFacsimile", model, this));

        String contactEmail = "contactEmail";
        add(
                new StringAndInternationalStringPanel(
                        contactEmail,
                        model,
                        contactEmail,
                        contactEmail,
                        "internationalContactEmail",
                        this,
                        EmailAddressValidator.getInstance()));
    }
}

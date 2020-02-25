/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.admin;

import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.geoserver.config.ContactInfo;

public class ContactPanel extends Panel {

    public ContactPanel(String id, final IModel<ContactInfo> model) {
        super(id, model);

        add(new TextField<String>("contactPerson"));
        add(new TextField<String>("contactOrganization"));
        add(new TextField<String>("contactPosition"));
        // address
        add(new TextField<String>("addressType"));
        add(new TextField<String>("address"));
        add(new TextField<String>("addressDeliveryPoint"));
        add(new TextField<String>("addressCity"));
        add(new TextField<String>("addressState"));
        add(new TextField<String>("addressPostalCode"));
        add(new TextField<String>("addressCountry"));
        // phone
        add(new TextField<String>("contactVoice"));
        add(new TextField<String>("contactFacsimile"));
        // email
        add(new TextField<String>("contactEmail"));
    }
}

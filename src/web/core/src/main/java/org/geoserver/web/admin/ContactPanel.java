/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
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

        add(new TextField("contactPerson" ));
        add(new TextField("contactOrganization"));
        add(new TextField("contactPosition"));
        add(new TextField("addressType"));
        add(new TextField("address")); 
        add(new TextField("addressDeliveryPoint"));
        add(new TextField("addressCity"));
        add(new TextField("addressState")); 
        add(new TextField("addressPostalCode"));
        add(new TextField("addressCountry"));
        add(new TextField("addressElectronicMailAddress"));
        add(new TextField("contactVoice"));
        add(new TextField("contactFacsimile"));
        add(new TextField("contactEmail"));
    }
}

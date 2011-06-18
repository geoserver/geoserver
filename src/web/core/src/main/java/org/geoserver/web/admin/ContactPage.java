package org.geoserver.web.admin;

import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.geoserver.config.ContactInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.web.GeoServerHomePage;

public class ContactPage extends ServerAdminPage {
    public ContactPage(){
        final IModel geoServerModel = getGeoServerModel();
        final IModel contactModel = getContactInfoModel();

        Form form = new Form("form", new CompoundPropertyModel(contactModel));

        add(form);
        form.add(new TextField("contactPerson" ));
        form.add(new TextField("contactOrganization"));
        form.add(new TextField("contactPosition"));
        form.add(new TextField("addressType"));
        form.add(new TextField("address")); 
        form.add(new TextField("addressCity"));
        form.add(new TextField("addressState")); 
        form.add(new TextField("addressPostalCode"));
        form.add(new TextField("addressCountry"));
        form.add(new TextField("contactVoice"));
        form.add(new TextField("contactFacsimile"));
        form.add(new TextField("contactEmail"));
        form.add(new Button("submit") {
            @Override
            public void onSubmit() {
                GeoServer gs = (GeoServer)geoServerModel.getObject();
                GeoServerInfo global = gs.getGlobal();
                global.setContact((ContactInfo)contactModel.getObject());
                gs.save(global);
                setResponsePage(GeoServerHomePage.class);
            }
        });
        form.add(new Button("cancel") {
            @Override
            public void onSubmit() {
                setResponsePage(GeoServerHomePage.class);
            }
        });
    }
}

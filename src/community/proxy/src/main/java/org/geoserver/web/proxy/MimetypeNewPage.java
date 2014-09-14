/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.proxy;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;
import org.geoserver.proxy.ProxyConfig;
import org.geoserver.web.GeoServerSecuredPage;

/**
 * Allows for editing a new style, includes file upload
 */
@SuppressWarnings("serial")
public class MimetypeNewPage extends GeoServerSecuredPage {

    TextField nameTextField;
    
    public MimetypeNewPage() {
        final TextField mimetype = new TextField("mimetype", new Model(""));
        
        final Form form = new Form("form") {
            @Override
            protected void onSubmit() {
                String newMimetype = mimetype.getDefaultModelObjectAsString();
                ProxyConfig config = ProxyConfig.loadConfFromDisk();
                config.mimetypeWhitelist.add(newMimetype);
                ProxyConfig.writeConfigToDisk(config);
                setResponsePage(ProxyAdminPage.class);
            }
        };
        
        form.add(mimetype);
        form.setMarkupId("mainForm");
        add(form);

        
        AjaxLink cancelLink = new AjaxLink( "cancel" ) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                setResponsePage( ProxyAdminPage.class );
            }
        };
        add( cancelLink );
    }
}

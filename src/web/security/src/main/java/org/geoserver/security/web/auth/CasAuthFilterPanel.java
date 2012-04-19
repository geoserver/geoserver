/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.auth;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.security.cas.CasAuthenticationFilterConfig;
import org.geoserver.security.cas.GeoServerCasProxiedAuthenticationFilter;
import org.geoserver.security.web.usergroup.UserGroupServiceChoice;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.wicket.HelpLink;

/**
 * Configuration panel for {@link GeoServerCasProxiedAuthenticationFilter}.
 * 
 * @author mcr
 */
public class CasAuthFilterPanel 
    extends AuthenticationFilterPanel<CasAuthenticationFilterConfig>  {

    
    private static final long serialVersionUID = 1;
    CasConnectionPanel<CasAuthenticationFilterConfig> connectionPanel;

    public CasAuthFilterPanel(String id, IModel<CasAuthenticationFilterConfig> model) {
        super(id, model);
        add (connectionPanel=new CasConnectionPanel<CasAuthenticationFilterConfig>("cas",model)) ;
        add(new TextField<String>("urlInCasLogoutPage"));
        add(new HelpLink("urlInLogoutPageHelp",this).setDialog(dialog));
        
        
        add(new UserGroupServiceChoice("userGroupServiceName"));

        
        add(new AjaxSubmitLink("urlInLogoutPageTest") {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                try {
                    testURL("urlInCasLogoutPage");
                    info(new StringResourceModel("urlInLogoutPageSuccessful",CasAuthFilterPanel.this, null).getObject());
                }
                catch(Exception e) {
                    error(e);
                    LOGGER.log(Level.WARNING, "Cas connection error ", e);
                }
                finally {
                    System.out.println("PageClass: "+getPage().getClass().getName());
                    target.addComponent(connectionPanel.getFeedbackPanel());
                }
            }
        }.setDefaultFormProcessing(false));

        
    }

    public void testURL(String wicketId) throws Exception {
        //since this wasn't a regular form submission, we need to manually update component
        // models
        ((FormComponent)get(wicketId)).processInput();
        String urlString = get(wicketId).getDefaultModelObjectAsString();
        URL url = new URL(urlString);        
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.getInputStream().close();
    }

}

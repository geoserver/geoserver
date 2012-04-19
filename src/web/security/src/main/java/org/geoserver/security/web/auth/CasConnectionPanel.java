/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.auth;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.security.cas.CasAuthenticationProperties;
import org.geoserver.security.cas.GeoServerCasConstants;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.HelpLink;
import org.geotools.util.logging.Logging;

/**
 * Reusable form component for CAS configurations
 * 
 * @author Christian Mueller
 *
 */
public class CasConnectionPanel<T extends CasAuthenticationProperties> extends FormComponentPanel<T> {

    private static final long serialVersionUID = 1L;

    static Logger LOGGER = Logging.getLogger("org.geoserver.security");


    GeoServerDialog dialog;
    
    public CasConnectionPanel(String id, IModel<T> model) {
        super(id, new Model());
                
        dialog = new GeoServerDialog("dialog");
        add(dialog);
                        
        add(new TextField<String>("casServerUrlPrefix"));
        add(new HelpLink("casServerUrlPrefixHelp",this).setDialog(dialog));
        add(new TextField<String>("service"));
        add(new HelpLink("serviceHelp",this).setDialog(dialog));
        add(new CheckBox("sendRenew"));
        add(new TextField<String>("proxyCallbackUrlPrefix").setRequired(false));
        add(new HelpLink("proxyCallbackUrlPrefixHelp",this).setDialog(dialog));
        
        add(new AjaxSubmitLink("casServerTest") {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                try {
                    testURL("casServerUrlPrefix",GeoServerCasConstants.LOGOUT_URI);
                    info(new StringResourceModel("casConnectionSuccessful",CasConnectionPanel.this, null).getObject());
                }
                catch(Exception e) {
                    error(e);
                    LOGGER.log(Level.WARNING, "Cas connection error ", e);
                }
                finally {
                    target.addComponent(feedbackPanel);
                }
            }
        }.setDefaultFormProcessing(false));
        
        add(new AjaxSubmitLink("casServiceTest") {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                try {
                    testURL("service",null);
                    info(new StringResourceModel("serviceConnectionSuccessful",CasConnectionPanel.this, null).getObject());
                }
                catch(Exception e) {
                    error(e);
                    LOGGER.log(Level.WARNING, "Cas connection error ", e);
                }
                finally {
                    target.addComponent(feedbackPanel);
                }
            }
        }.setDefaultFormProcessing(false));
        
        
        add(new AjaxSubmitLink("proxyCallbackTest") {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                try {
                    testURL("proxyCallbackUrlPrefix",null);
                    info(new StringResourceModel("casProxyCallbackSuccessful",CasConnectionPanel.this, null).getObject());
                }
                catch(Exception e) {
                    error(e);
                    LOGGER.log(Level.WARNING, "CAs proxy callback  error ", e);
                }
                finally {
                    target.addComponent(feedbackPanel);
                }
            }
        }.setDefaultFormProcessing(false));


        add(feedbackPanel = new FeedbackPanel("feedback"));
        feedbackPanel.setOutputMarkupId(true);
    }

    public void testURL(String wicketId, String uri) throws Exception {
        //since this wasn't a regular form submission, we need to manually update component
        // models
        ((FormComponent)get(wicketId)).processInput();
        String urlString = get(wicketId).getDefaultModelObjectAsString();
        if (uri!=null) 
            urlString+=uri;
        URL url = new URL(urlString);        
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.getInputStream().close();
    }
    
    FeedbackPanel feedbackPanel;
    public FeedbackPanel getFeedbackPanel() {
        return feedbackPanel;
    }
    

}

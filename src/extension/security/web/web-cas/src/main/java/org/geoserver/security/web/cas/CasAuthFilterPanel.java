/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.cas;

import static org.geoserver.security.cas.CasAuthenticationFilterConfig.CasSpecificRoleSource.CustomAttribute;

import java.io.Serial;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.security.cas.CasAuthenticationFilterConfig;
import org.geoserver.security.cas.GeoServerCasAuthenticationFilter;
import org.geoserver.security.cas.GeoServerCasConstants;
import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig.PreAuthenticatedUserNameRoleSource;
import org.geoserver.security.config.RoleSource;
import org.geoserver.security.web.auth.PreAuthenticatedUserNameFilterPanel;
import org.geoserver.security.web.auth.RoleSourceChoiceRenderer;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.HelpLink;
import org.geotools.util.logging.Logging;

/**
 * Configuration panel for {@link GeoServerCasAuthenticationFilter}.
 *
 * @author mcr
 */
// TODO WICKET8 - Verify this page works OK
public class CasAuthFilterPanel extends PreAuthenticatedUserNameFilterPanel<CasAuthenticationFilterConfig> {

    @Serial
    private static final long serialVersionUID = 1;

    static Logger LOGGER = Logging.getLogger("org.geoserver.security");
    GeoServerDialog dialog;

    public CasAuthFilterPanel(String id, IModel<CasAuthenticationFilterConfig> model) {
        super(id, model);

        dialog = (GeoServerDialog) get("dialog");

        add(new HelpLink("connectionParametersHelp", this).setDialog(dialog));
        add(new HelpLink("singleSignOnParametersHelp", this).setDialog(dialog));
        add(new HelpLink("singleSignOutParametersHelp", this).setDialog(dialog));
        add(new HelpLink("proxyTicketParametersHelp", this).setDialog(dialog));

        add(new TextField<>("casServerUrlPrefix"));
        add(new CheckBox("sendRenew"));
        add(new TextField<String>("proxyCallbackUrlPrefix").setRequired(false));

        add(
                new AjaxSubmitLink("casServerTest") {
                    @Override
                    protected void onSubmit(AjaxRequestTarget target) {
                        try {
                            testURL("casServerUrlPrefix", GeoServerCasConstants.LOGOUT_URI);
                            info(new StringResourceModel("casConnectionSuccessful", CasAuthFilterPanel.this, null)
                                    .getObject());
                        } catch (Exception e) {
                            error(e);
                            ((GeoServerBasePage) getPage()).addFeedbackPanels(target); // to display message
                            LOGGER.log(Level.WARNING, "CAS connection error ", e);
                        }
                    }
                }.setDefaultFormProcessing(false));

        add(
                new AjaxSubmitLink("proxyCallbackTest") {
                    @Override
                    protected void onSubmit(AjaxRequestTarget target) {
                        try {
                            testURL("proxyCallbackUrlPrefix", null);
                            info(new StringResourceModel("casProxyCallbackSuccessful", CasAuthFilterPanel.this, null)
                                    .getObject());
                        } catch (Exception e) {
                            error(e);
                            ((GeoServerBasePage) getPage()).addFeedbackPanels(target); // to display message
                            LOGGER.log(Level.WARNING, "CAS proxy callback  error ", e);
                        }
                    }
                }.setDefaultFormProcessing(false));

        CheckBox createSession = new CheckBox("singleSignOut");
        add(createSession);

        add(new TextField<>("urlInCasLogoutPage"));
        add(
                new AjaxSubmitLink("urlInCasLogoutPageTest") {
                    @Override
                    protected void onSubmit(AjaxRequestTarget target) {
                        try {
                            testURL("urlInCasLogoutPage", null);
                            info(new StringResourceModel("urlInCasLogoutPageSuccessful", CasAuthFilterPanel.this, null)
                                    .getObject());
                        } catch (Exception e) {
                            error(e);
                            ((GeoServerBasePage) getPage()).addFeedbackPanels(target); // to display message
                            LOGGER.log(Level.WARNING, "CAs url in logout page error ", e);
                        }
                    }
                }.setDefaultFormProcessing(false));
    }

    public void testURL(String wicketId, String uri) throws Exception {
        // since this wasn't a regular form submission, we need to manually update component
        // models
        ((FormComponent) get(wicketId)).processInput();
        String urlString = get(wicketId).getDefaultModelObjectAsString();
        if (uri != null) urlString += uri;
        URL url = new URL(urlString);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.getInputStream().close();
    }

    @Override
    protected Panel getRoleSourcePanel(RoleSource model) {
        if (CustomAttribute.equals(model)) {
            return new CustomAttributePanel("panel");
        }
        return super.getRoleSourcePanel(model);
    }

    @Override
    protected DropDownChoice<RoleSource> createRoleSourceDropDown() {
        List<RoleSource> sources = new ArrayList<>(Arrays.asList(PreAuthenticatedUserNameRoleSource.values()));
        sources.addAll(Arrays.asList(CasAuthenticationFilterConfig.CasSpecificRoleSource.values()));
        return new DropDownChoice<>("roleSource", sources, new RoleSourceChoiceRenderer());
    }

    static class CustomAttributePanel extends Panel {
        public CustomAttributePanel(String id) {
            super(id, new Model<>());
            add(new TextField<String>("customAttributeName").setRequired(true));
        }
    }
}

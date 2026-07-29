/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.keycloak;

import java.util.Arrays;
import java.util.List;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.security.keycloak.KeycloakRESTClient;
import org.geoserver.security.keycloak.KeycloakRoleService;
import org.geoserver.security.keycloak.KeycloakRoleServiceConfig;
import org.geoserver.security.web.role.RoleServicePanel;

/** Configuration panel for {@link KeycloakRoleService}. */
public class KeycloakRoleServicePanel extends RoleServicePanel<KeycloakRoleServiceConfig> {

    public KeycloakRoleServicePanel(String id, IModel<KeycloakRoleServiceConfig> model) {
        super(id, model);

        FeedbackPanel feedback = new FeedbackPanel("feedback");
        feedback.setOutputMarkupId(true);
        add(feedback);

        TextField<String> serverURL = new TextField<>("serverURL");
        serverURL.setRequired(true);
        add(serverURL);

        TextField<String> realm = new TextField<>("realm");
        realm.setRequired(true);
        add(realm);

        TextField<String> clientID = new TextField<>("clientID");
        clientID.setRequired(true);
        add(clientID);

        TextField<String> clientSecret = new TextField<>("clientSecret");
        clientSecret.setRequired(true);
        add(clientSecret);

        TextField<String> idsOfClientsList = new TextField<>("idsOfClientsList");
        idsOfClientsList.setRequired(false);
        add(idsOfClientsList);

        add(
                new AjaxSubmitLink("testConnection") {
                    @Override
                    protected void onSubmit(AjaxRequestTarget target) {
                        serverURL.processInput();
                        realm.processInput();
                        clientID.processInput();
                        clientSecret.processInput();
                        idsOfClientsList.processInput();

                        if (serverURL.hasErrorMessage()
                                || realm.hasErrorMessage()
                                || clientID.hasErrorMessage()
                                || clientSecret.hasErrorMessage()) {
                            target.add(feedback);
                            return;
                        }

                        List<String> clientIds = null;
                        String ids = idsOfClientsList.getConvertedInput();
                        if (ids != null && !ids.isBlank()) {
                            clientIds = Arrays.asList(ids.split(","));
                        }

                        try {
                            KeycloakRESTClient client = new KeycloakRESTClient(
                                    serverURL.getConvertedInput(),
                                    realm.getConvertedInput(),
                                    clientID.getConvertedInput(),
                                    clientSecret.getConvertedInput(),
                                    clientIds);
                            KeycloakRESTClient.ConnectionTestResult result = client.testConnection();
                            info(new StringResourceModel("testConnectionSuccessful", KeycloakRoleServicePanel.this)
                                    .setParameters(result.realmRoleCount(), result.clientRoleCount())
                                    .getObject());
                        } catch (Exception e) {
                            error(new StringResourceModel("testConnectionFailed", KeycloakRoleServicePanel.this)
                                    .setParameters(e.getMessage())
                                    .getObject());
                        }
                        target.add(feedback);
                    }
                }.setDefaultFormProcessing(false));
    }
}

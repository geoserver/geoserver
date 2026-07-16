/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.keycloak;

import org.apache.wicket.Component;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.geoserver.security.keycloak.KeycloakRoleServiceConfig;
import org.geoserver.security.web.AbstractSecurityWicketTestSupport;
import org.geoserver.web.ComponentBuilder;
import org.geoserver.web.FormTestPage;
import org.junit.Test;

public class KeycloakRoleServicePanelTest extends AbstractSecurityWicketTestSupport {

    private void setupPanel() {
        KeycloakRoleServiceConfig config = new KeycloakRoleServiceConfig();
        config.setName("test");
        tester.startPage(new FormTestPage(
                new ComponentBuilder() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public Component buildComponent(String id) {
                        return new KeycloakRoleServicePanel(id, new Model<>(config));
                    }
                },
                new CompoundPropertyModel<>(config)));
    }

    @Test
    public void testConnectionWithoutRequiredFieldsShowsValidationErrorsInsteadOfCrashing() throws Exception {
        setupPanel();

        // clicking "Test Connection" with every field left blank used to throw a
        // NullPointerException from deep inside KeycloakUrlBuilder; it should now
        // report the standard per-field required-validation errors instead.
        tester.executeAjaxEvent("form:panel:testConnection", "click");

        tester.assertErrorMessages(
                "Field 'Keycloak URL' is required.",
                "Field 'Realm name (human-readable)' is required.",
                "Field 'Client ID (human-readable)' is required.",
                "Field 'Client Secret' is required.");
    }
}

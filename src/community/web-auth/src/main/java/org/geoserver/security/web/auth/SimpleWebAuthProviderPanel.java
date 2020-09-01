/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.auth;

import java.util.logging.Level;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.security.auth.web.SimpleWebAuthenticationConfig;
import org.geoserver.security.web.role.RoleServiceChoice;

public class SimpleWebAuthProviderPanel
        extends AuthenticationProviderPanel<SimpleWebAuthenticationConfig> {

    WebMarkupContainer webAuthorizationContainer =
            new WebMarkupContainer("webAuthorizationContainer");
    WebMarkupContainer roleAuthorizationContainer =
            new WebMarkupContainer("roleAuthorizationContainer");

    public SimpleWebAuthProviderPanel(String id, IModel<SimpleWebAuthenticationConfig> model) {
        super(id, model);
        // add checkbox to switch between regex and role service
        add(new TextField<String>("connectionURL"));
        add(new TextField<Integer>("readTimeoutOut"));
        add(new TextField<Integer>("connectionTimeOut"));
        add(new CheckBox("useHeader"));

        // authorization controls
        webAuthorizationContainer.setOutputMarkupId(true);
        webAuthorizationContainer.setOutputMarkupPlaceholderTag(true);
        roleAuthorizationContainer.setOutputMarkupId(true);
        roleAuthorizationContainer.setOutputMarkupPlaceholderTag(true);

        // set visibility as per selected authorization option
        webAuthorizationContainer.setVisible(
                model.getObject()
                        .getAuthorizationOption()
                        .equalsIgnoreCase(
                                SimpleWebAuthenticationConfig.AUTHORIZATION_RADIO_OPTION_WEB));
        roleAuthorizationContainer.setVisible(
                model.getObject()
                        .getAuthorizationOption()
                        .equalsIgnoreCase(
                                SimpleWebAuthenticationConfig.AUTHORIZATION_RADIO_OPTION_SERVICE));

        webAuthorizationContainer.add(new TextField<String>("roleRegex"));
        add(webAuthorizationContainer);
        roleAuthorizationContainer.add(new RoleServiceChoice("roleServiceName"));
        add(roleAuthorizationContainer);
        add(initAuthorizationRadioChoice(model));
    }

    private RadioGroup initAuthorizationRadioChoice(IModel<SimpleWebAuthenticationConfig> model) {
        RadioGroup sl = new RadioGroup("authorizationOption");
        // form.add(sl);
        sl.add(
                new Radio(
                        "roleService",
                        new Model(
                                SimpleWebAuthenticationConfig.AUTHORIZATION_RADIO_OPTION_SERVICE)));
        sl.add(
                new Radio(
                        "webResponse",
                        new Model(SimpleWebAuthenticationConfig.AUTHORIZATION_RADIO_OPTION_WEB)));

        //        final RadioChoice<String> radioChoice =
        //                new RadioChoice<String>(
        //                        "authorizationOption",
        //                        new PropertyModel<String>(model, "authorizationOption"),
        //                        SimpleWebAuthenticationConfig.AUTHORIZATION_RADIO_OPTIONS);

        sl.add(
                new AjaxFormChoiceComponentUpdatingBehavior() {

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        final String selectedValue = getComponent().getDefaultModelObjectAsString();

                        if (LOGGER.isLoggable(Level.FINE)) {
                            LOGGER.fine("Authorization Radio Selection : " + selectedValue);
                        }

                        // reset visibility of controls
                        webAuthorizationContainer.setVisible(
                                selectedValue.equalsIgnoreCase(
                                        SimpleWebAuthenticationConfig
                                                .AUTHORIZATION_RADIO_OPTION_WEB));
                        roleAuthorizationContainer.setVisible(
                                selectedValue.equalsIgnoreCase(
                                        SimpleWebAuthenticationConfig
                                                .AUTHORIZATION_RADIO_OPTION_SERVICE));
                        target.add(webAuthorizationContainer);
                        target.add(roleAuthorizationContainer);
                    }
                });

        return sl;
    }
}

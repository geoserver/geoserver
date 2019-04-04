/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.AuthenticationKeyFilterConfig;
import org.geoserver.security.AuthenticationKeyMapper;
import org.geoserver.security.GeoServerAuthenticationKeyFilter;
import org.geoserver.security.web.auth.AuthenticationFilterPanel;
import org.geoserver.security.web.usergroup.UserGroupServiceChoice;
import org.geoserver.web.util.MapModel;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.HelpLink;
import org.geotools.util.logging.Logging;

/**
 * Configuration panel for {@link GeoServerAuthenticationKeyFilter}.
 *
 * @author mcr
 */
public class AuthenticationKeyFilterPanel
        extends AuthenticationFilterPanel<AuthenticationKeyFilterConfig> {

    private static final long serialVersionUID = 1;

    static Logger LOGGER = Logging.getLogger("org.geoserver.security");
    GeoServerDialog dialog;

    IModel<AuthenticationKeyFilterConfig> model;

    public AuthenticationKeyFilterPanel(String id, IModel<AuthenticationKeyFilterConfig> model) {
        super(id, model);

        dialog = (GeoServerDialog) get("dialog");
        this.model = model;

        add(new HelpLink("authKeyParametersHelp", this).setDialog(dialog));

        add(new TextField<String>("authKeyParamName"));

        Map<String, String> parameters = model.getObject().getMapperParameters();
        final ParamsPanel paramsPanel =
                createParamsPanel(
                        "authKeyMapperParamsPanel",
                        model.getObject().getAuthKeyMapperName(),
                        parameters);

        AuthenticationKeyMapperChoice authenticationKeyMapperChoice =
                new AuthenticationKeyMapperChoice("authKeyMapperName");

        authenticationKeyMapperChoice.add(
                new AjaxFormComponentUpdatingBehavior("change") {
                    protected void onUpdate(AjaxRequestTarget target) {
                        String newSelection = (String) getFormComponent().getConvertedInput();
                        Map<String, String> parameters = getMapperParameters(newSelection);
                        AuthenticationKeyFilterPanel.this
                                .model
                                .getObject()
                                .setMapperParameters(parameters);
                        paramsPanel.updateParameters(newSelection, parameters);
                        target.add(paramsPanel);
                    }
                });

        add(authenticationKeyMapperChoice);
        add(new UserGroupServiceChoice("userGroupServiceName"));

        add(
                new WebMarkupContainer("authKeyMapperParamsContainer")
                        .add(paramsPanel)
                        .setOutputMarkupId(true));

        add(
                new AjaxSubmitLink("synchronize") {
                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        try {
                            // AuthenticationKeyFilterPanel.this.updateModel();
                            AuthenticationKeyFilterConfig config =
                                    AuthenticationKeyFilterPanel.this.model.getObject();

                            getSecurityManager().saveFilter(config);
                            AuthenticationKeyMapper mapper =
                                    (AuthenticationKeyMapper)
                                            GeoServerExtensions.bean(config.getAuthKeyMapperName());
                            mapper.setSecurityManager(getSecurityManager());
                            mapper.setUserGroupServiceName(config.getUserGroupServiceName());
                            int numberOfNewKeys = mapper.synchronize();
                            info(
                                    new StringResourceModel(
                                                    "synchronizeSuccessful",
                                                    AuthenticationKeyFilterPanel.this)
                                            .setParameters(numberOfNewKeys)
                                            .getObject());
                        } catch (Exception e) {
                            error(e);
                            LOGGER.log(Level.WARNING, "Authentication key  error ", e);
                        } finally {
                            target.add(getPage().get("topFeedback"));
                        }
                    }
                }.setDefaultFormProcessing(true));
    }

    class ParamsPanel extends FormComponentPanel {

        public ParamsPanel(String id, String authMapperName, Map<String, String> parameters) {
            super(id, new Model());
            updateParameters(authMapperName, parameters);
        }

        private void updateParameters(
                final String authMapperName, final Map<String, String> parameters) {

            removeAll();
            add(
                    new ListView<String>(
                            "parametersList",
                            new Model(new ArrayList<String>(parameters.keySet()))) {
                        @Override
                        protected void populateItem(ListItem<String> item) {
                            item.add(
                                    new Label(
                                            "parameterName",
                                            new StringResourceModel(
                                                    "AuthenticationKeyFilterPanel."
                                                            + authMapperName
                                                            + "."
                                                            + item.getModel().getObject(),
                                                    this,
                                                    null)));
                            item.add(
                                    new TextField<String>(
                                            "parameterField",
                                            new MapModel(parameters, item.getModel().getObject())));
                        }
                    });
        }

        public void resetModel() {}
    }

    private ParamsPanel createParamsPanel(
            String id, String authKeyMapperName, Map<String, String> parameters) {
        ParamsPanel paramsPanel = new ParamsPanel(id, authKeyMapperName, parameters);
        paramsPanel.setOutputMarkupId(true);
        return paramsPanel;
    }

    private Map<String, String> getMapperParameters(String authKeyMapperName) {
        if (authKeyMapperName != null) {
            AuthenticationKeyMapper mapper =
                    (AuthenticationKeyMapper) GeoServerExtensions.bean(authKeyMapperName);
            if (mapper != null) {
                return mapper.getMapperConfiguration();
            }
        }
        return new HashMap<String, String>();
    }
}

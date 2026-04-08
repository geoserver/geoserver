/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * Original from GeoServer 2.24-SNAPSHOT under GPL 2.0 license (org.geoserver.geofence.web.GeofencePage)
 */
package org.geoserver.acl.plugin.web.config;

import java.util.logging.Level;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.AbstractSubmitLink;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.acl.plugin.accessmanager.AuthorizationServiceConfig;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.model.ExtPropertyModel;

/**
 * ACL wicket administration UI for GeoServer.
 *
 * @author "Mauro Bartolomeoli - mauro.bartolomeoli@geo-solutions.it" - Originally as part of GeoFence's GeoServer
 *     extension
 * @author Gabriel Roldan - Camptocamp
 */
@SuppressWarnings("serial")
public class ACLServiceConfigPage extends GeoServerSecuredPage {

    private ACLServiceConfigPageModel pageModel;

    public ACLServiceConfigPage() {
        pageModel = ACLServiceConfigPageModel.newInstance();

        Form<AuthorizationServiceConfig> form = new Form<>("form", pageModel.getConfigModel());
        form.setOutputMarkupId(true);
        super.add(form);

        // TODO: allow to configure the url, user, and pwd?
        form.add(serviceURLField());
        form.add(testConnectionLink());
    }

    private TextField<String> serviceURLField() {
        final boolean isInternal = pageModel.isInternal();
        ExtPropertyModel<String> serviceUrl = pageModel.getServiceUrl().setReadOnly(isInternal);
        TextField<String> serviceURLField = new TextField<>("servicesUrl", serviceUrl);
        serviceURLField.setRequired(true);
        serviceURLField.setEnabled(false);
        return serviceURLField;
    }

    private AbstractSubmitLink testConnectionLink() {
        return new AjaxSubmitLink("test") {
            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                try {
                    FormComponent<?> url = (FormComponent<?>) super.getForm().get("servicesUrl");
                    url.processInput();
                    pageModel.testConnection();
                    info(new StringResourceModel(ACLServiceConfigPage.class.getSimpleName() + ".connectionSuccessful")
                            .getObject());
                } catch (Exception e) {
                    error(e);
                    LOGGER.log(Level.WARNING, e.getMessage(), e);
                }

                if (getPage() instanceof GeoServerBasePage) {
                    ((GeoServerBasePage) getPage()).addFeedbackPanels(target);
                }
            }
        }.setDefaultFormProcessing(false);
    }
}

/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker.web;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.updateschecker.Config;
import org.geoserver.updateschecker.ConfigPersister;
import org.geoserver.updateschecker.FeedClient;
import org.geoserver.updateschecker.Messages;
import org.geoserver.updateschecker.UpdatesCheckerService;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.HelpLink;
import org.geotools.util.logging.Logging;

/** The settings page, with the Status section above the settings. */
public class UpdatesCheckerPage extends GeoServerSecuredPage {

    private static final Logger LOGGER = Logging.getLogger(UpdatesCheckerService.LOGGER_NAME);

    public UpdatesCheckerPage() {
        GeoServerDialog dialog = new GeoServerDialog("dialog");
        add(dialog);
        UpdatesCheckerService service = GeoServerExtensions.bean(UpdatesCheckerService.class);

        add(new StatusPanel("status", service.getStatus()));

        Config config = GeoServerExtensions.bean(ConfigPersister.class).getConfig();

        Form<Config> form = new Form<>("form", new CompoundPropertyModel<>(config));
        add(form);

        form.add(new CheckBox("enabled"));

        CheckBox logUpdateChecksCheckbox = new CheckBox("logUpdateChecks");
        logUpdateChecksCheckbox.setOutputMarkupId(true);
        form.add(logUpdateChecksCheckbox);

        // only means something while logging is on
        CheckBox hideVersionNumbersInLogsCheckbox = new CheckBox("hideVersionNumbersInLogs");
        hideVersionNumbersInLogsCheckbox.setOutputMarkupId(true);
        hideVersionNumbersInLogsCheckbox.add(new Behavior() {
            @Override
            public void onConfigure(Component component) {
                super.onConfigure(component);
                component.setEnabled(logUpdateChecksCheckbox.getModelObject());
            }
        });
        form.add(hideVersionNumbersInLogsCheckbox);

        logUpdateChecksCheckbox.add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(hideVersionNumbersInLogsCheckbox);
            }
        });

        form.add(new Button("submit") {
            @Override
            public void onSubmit() {
                try {
                    GeoServerExtensions.bean(ConfigPersister.class).save(config);
                } catch (RuntimeException e) {
                    // for example a read-only data directory
                    LOGGER.log(Level.WARNING, Messages.base("log.configSaveFailed"), e);
                    error(getLocalizer().getString("UpdatesCheckerPage.saveFailed", UpdatesCheckerPage.this));
                    return;
                }
                doReturn();
            }
        });

        form.add(new Button("cancel") {
            @Override
            public void onSubmit() {
                doReturn();
            }
        });

        String feedUrl = GeoServerExtensions.bean(FeedClient.class).getFeedUri().toString();
        TextField<String> feedUrlField = new TextField<>("feedUrl", Model.of(feedUrl));
        feedUrlField.add(AttributeModifier.replace("readonly", "readonly"));
        form.add(feedUrlField);

        // set by whoever starts the server, so never editable here
        String suppressed = GeoServerExtensions.bean(UpdatesCheckerService.class)
                .getSuppression()
                .toString();
        TextField<String> suppressedField = new TextField<>("suppressedCves", Model.of(suppressed));
        suppressedField.add(AttributeModifier.replace("readonly", "readonly"));
        suppressedField.add(
                AttributeModifier.replace("placeholder", new ResourceModel("UpdatesCheckerPage.suppressedCves.none")));
        form.add(suppressedField);
        form.add(new HelpLink("suppressedCvesHelp").setDialog(dialog));
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        // a stylesheet, because GeoServer's content security policy drops inline style attributes
        response.render(CssHeaderItem.forReference(
                new PackageResourceReference(UpdatesCheckerPage.class, "UpdatesCheckerPage.css")));
    }
}

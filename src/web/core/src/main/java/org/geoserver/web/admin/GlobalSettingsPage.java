/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.admin;

import static org.geoserver.filters.LoggingFilter.LOG_BODIES_ENABLED;
import static org.geoserver.filters.LoggingFilter.LOG_HEADERS_ENABLED;
import static org.geoserver.filters.LoggingFilter.LOG_REQUESTS_ENABLED;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.ListChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.validator.RangeValidator;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ResourceErrorHandling;
import org.geoserver.config.SettingsInfo;
import org.geoserver.logging.LoggingUtils;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.LockProvider;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.Resources;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoserverAjaxSubmitLink;
import org.geoserver.web.data.resource.LocalesDropdown;
import org.geoserver.web.data.settings.SettingsPluginPanelInfo;
import org.geoserver.web.util.MetadataMapModel;
import org.geoserver.web.wicket.LocalizedChoiceRenderer;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.Select2DropDownChoice;
import org.springframework.context.ApplicationContext;

public class GlobalSettingsPage extends ServerAdminPage {

    private static final long serialVersionUID = 4716657682337915996L;

    public static final ArrayList<String> AVAILABLE_CHARSETS =
            new ArrayList<>(Charset.availableCharsets().keySet());
    private final IModel<GeoServerInfo> globalInfoModel;
    private final IModel<LoggingInfo> loggingInfoModel;

    public GlobalSettingsPage() {
        globalInfoModel = getGlobalInfoModel();
        loggingInfoModel = getLoggingInfoModel();

        CompoundPropertyModel<GeoServerInfo> globalModel =
                new CompoundPropertyModel<>(globalInfoModel);
        PropertyModel<SettingsInfo> settingsModel = new PropertyModel<>(globalModel, "settings");
        PropertyModel<MetadataMap> metadataModel = new PropertyModel<>(globalInfoModel, "metadata");
        Form<GeoServerInfo> form = new Form<>("form", globalModel);

        add(form);

        form.add(new CheckBox("verbose", new PropertyModel<>(settingsModel, "verbose")));
        form.add(
                new CheckBox(
                        "verboseExceptions",
                        new PropertyModel<>(settingsModel, "verboseExceptions")));
        form.add(new CheckBox("globalServices"));
        form.add(
                new TextField<Integer>(
                                "numDecimals", new PropertyModel<>(settingsModel, "numDecimals"))
                        .add(RangeValidator.minimum(0)));
        form.add(
                new Select2DropDownChoice<>(
                        "charset",
                        new PropertyModel<>(settingsModel, "charset"),
                        AVAILABLE_CHARSETS));
        form.add(
                new Select2DropDownChoice<>(
                        "resourceErrorHandling",
                        Arrays.asList(ResourceErrorHandling.values()),
                        new ResourceErrorHandlingRenderer()));
        form.add(
                new TextField<String>(
                        "proxyBaseUrl", new PropertyModel<>(settingsModel, "proxyBaseUrl")));
        form.add(new CheckBox("useHeadersProxyURL"));

        logLevelsAppend(form, loggingInfoModel);
        form.add(
                new CheckBox(
                        "stdOutLogging", new PropertyModel<>(loggingInfoModel, "stdOutLogging")));
        form.add(
                new TextField<>(
                        "loggingLocation", new PropertyModel<>(loggingInfoModel, "location")));

        TextField<String> xmlPostRequestLogBufferSize =
                new TextField<>(
                        "xmlPostRequestLogBufferSize",
                        new PropertyModel<>(globalInfoModel, "xmlPostRequestLogBufferSize"));
        xmlPostRequestLogBufferSize.add(RangeValidator.minimum(0));
        form.add(xmlPostRequestLogBufferSize);
        CheckBox logBodiesCheckBox =
                new CheckBox(
                        "requestLoggingBodies",
                        new MetadataMapModel<>(metadataModel, LOG_BODIES_ENABLED, Boolean.class));
        form.add(logBodiesCheckBox);
        CheckBox logHeadersCheckBox =
                new CheckBox(
                        "requestLoggingHeaders",
                        new MetadataMapModel<>(metadataModel, LOG_HEADERS_ENABLED, Boolean.class));
        WebMarkupContainer wmc = new WebMarkupContainer("requestLoggingSub");
        wmc.setOutputMarkupId(true);
        wmc.add(logBodiesCheckBox);
        wmc.add(logHeadersCheckBox);
        wmc.add(xmlPostRequestLogBufferSize);
        MetadataMapModel<Boolean> requestCheckModel =
                new MetadataMapModel<Boolean>(metadataModel, LOG_REQUESTS_ENABLED, Boolean.class) {
                    @Override
                    public void setObject(Boolean object) {
                        super.setObject(object);
                    }
                };
        wmc.setEnabled(Boolean.TRUE.equals(requestCheckModel.getObject()));
        form.add(wmc);

        AjaxCheckBox requestCheckBox =
                new AjaxCheckBox("requestLogging", requestCheckModel) {

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        wmc.setEnabled(Boolean.TRUE.equals(requestCheckModel.getObject()));
                        logBodiesCheckBox.getModel().setObject(false);
                        logHeadersCheckBox.getModel().setObject(false);
                        target.add(wmc);
                    }
                };

        form.add(requestCheckBox);

        form.add(new CheckBox("xmlExternalEntitiesEnabled"));

        form.add(new TextField<Integer>("featureTypeCacheSize").add(RangeValidator.minimum(0)));

        IModel<String> lockProviderModel = new PropertyModel<>(globalInfoModel, "lockProviderName");
        ApplicationContext applicationContext = GeoServerApplication.get().getApplicationContext();
        List<String> providers =
                new ArrayList<>(
                        Arrays.asList(applicationContext.getBeanNamesForType(LockProvider.class)));
        providers.remove("lockProvider"); // remove the global lock provider
        Collections.sort(providers);

        DropDownChoice<String> lockProviderChoice =
                new Select2DropDownChoice<>(
                        "lockProvider",
                        lockProviderModel,
                        providers,
                        new LocalizedChoiceRenderer(this));

        form.add(lockProviderChoice);

        IModel<GeoServerInfo.WebUIMode> webUIModeModel =
                new PropertyModel<>(globalInfoModel, "webUIMode");
        if (webUIModeModel.getObject() == null) {
            webUIModeModel.setObject(GeoServerInfo.WebUIMode.DEFAULT);
        }
        DropDownChoice<GeoServerInfo.WebUIMode> webUIModeChoice =
                new Select2DropDownChoice<>(
                        "webUIMode",
                        webUIModeModel,
                        Arrays.asList(GeoServerInfo.WebUIMode.values()));

        form.add(webUIModeChoice);

        form.add(
                new CheckBox(
                        "allowStoredQueriesPerWorkspace",
                        new PropertyModel<>(globalInfoModel, "allowStoredQueriesPerWorkspace")));

        // Extension plugin for Global Settings
        // Loading of the settings from the Global Info
        ListView extensions =
                SettingsPluginPanelInfo.createExtensions(
                        "extensions", settingsModel, getGeoServerApplication());
        form.add(extensions);

        form.add(
                new CheckBox(
                        "showCreatedTimeCols",
                        new PropertyModel<>(settingsModel, "showCreatedTimeColumnsInAdminList")));

        form.add(
                new CheckBox(
                        "showModifiedTimeCols",
                        new PropertyModel<>(settingsModel, "showModifiedTimeColumnsInAdminList")));

        form.add(
                new LocalesDropdown(
                        "defaultLocale", new PropertyModel<>(settingsModel, "defaultLocale")));
        Button submit =
                new Button("submit") {
                    @Override
                    public void onSubmit() {
                        onSave(true);
                    }
                };
        form.add(submit);

        form.add(applyLink(form));

        Button cancel =
                new Button("cancel") {
                    @Override
                    public void onSubmit() {
                        doReturn();
                    }
                };
        form.add(cancel);
    }

    private GeoserverAjaxSubmitLink applyLink(Form form) {
        return new GeoserverAjaxSubmitLink("apply", form, this) {

            @Override
            protected void onError(AjaxRequestTarget target, Form form) {
                super.onError(target, form);
                target.add(form);
            }

            @Override
            protected void onSubmitInternal(AjaxRequestTarget target, Form<?> form) {
                try {
                    onSave(false);
                } catch (IllegalArgumentException e) {
                    form.error(e.getMessage());
                    target.add(form);
                }
            }
        };
    }

    public void onSave(boolean doReturn) {
        GeoServer gs = getGeoServer();
        gs.save(globalInfoModel.getObject());
        gs.save(loggingInfoModel.getObject());
        if (doReturn) {
            doReturn();
        }
    }

    private void logLevelsAppend(Form<GeoServerInfo> form, IModel<LoggingInfo> loggingInfoModel) {
        // search for *LOGGING xml and properties files in the data directory
        GeoServerResourceLoader loader =
                GeoServerApplication.get().getBeanOfType(GeoServerResourceLoader.class);
        List<String> logProfiles = null;
        try {
            Resource logsDirectory = loader.get("logs");
            if (logsDirectory.getType() == Type.DIRECTORY) {
                logProfiles = new ArrayList<>();
                List<Resource> xmlFiles =
                        Resources.list(
                                logsDirectory,
                                obj -> obj.name().toLowerCase().endsWith("_logging.xml"));
                for (Resource res : xmlFiles) {
                    logProfiles.add(Paths.sidecar(res.name(), null));
                }

                List<Resource> propertiesFiles =
                        Resources.list(
                                logsDirectory,
                                obj -> obj.name().toLowerCase().endsWith("_logging.properties"));
                for (Resource res : propertiesFiles) {
                    logProfiles.add(res.name());
                }

                Collections.sort(logProfiles, String.CASE_INSENSITIVE_ORDER);
            }
        } catch (Exception e) {
            LOGGER.log(
                    Level.WARNING,
                    "Could not load the list of log configurations from the data directory",
                    e);
        }
        // if none is found use the default set
        if (logProfiles == null || logProfiles.isEmpty()) {
            logProfiles = Arrays.asList(LoggingUtils.STANDARD_LOGGING_CONFIGURATIONS);
        }
        // fix optional properties suffix
        String level = loggingInfoModel.getObject().getLevel();
        if (level != null && !logProfiles.contains(level)) {
            for (String profile : logProfiles) {
                if (profile.startsWith(level)) {
                    loggingInfoModel.getObject().setLevel(profile);
                    break;
                }
                if (profile.startsWith(Paths.sidecar(level, null))) {
                    loggingInfoModel.getObject().setLevel(profile);
                    break;
                }
            }
        }
        form.add(
                new ListChoice<>(
                        "log4jConfigFile",
                        new PropertyModel<>(loggingInfoModel, "level"),
                        logProfiles));
    }

    class ResourceErrorHandlingRenderer extends ChoiceRenderer<ResourceErrorHandling> {
        private static final long serialVersionUID = 4183327535180465575L;

        @Override
        public Object getDisplayValue(ResourceErrorHandling object) {
            return new ParamResourceModel(object.name(), GlobalSettingsPage.this).getString();
        }

        @Override
        public String getIdValue(ResourceErrorHandling object, int index) {
            return object.name();
        }

        @Override
        public ResourceErrorHandling getObject(
                String id, IModel<? extends List<? extends ResourceErrorHandling>> choices) {
            return id == null || "".equals(id) ? null : ResourceErrorHandling.valueOf(id);
        }
    }
}

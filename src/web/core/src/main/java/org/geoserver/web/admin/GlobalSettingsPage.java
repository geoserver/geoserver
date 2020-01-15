/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.admin;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
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
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ResourceErrorHandling;
import org.geoserver.config.SettingsInfo;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.LockProvider;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.Resources;
import org.geoserver.util.Filter;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.data.settings.SettingsPluginPanelInfo;
import org.geoserver.web.wicket.LocalizedChoiceRenderer;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.Select2DropDownChoice;
import org.springframework.context.ApplicationContext;

public class GlobalSettingsPage extends ServerAdminPage {

    private static final long serialVersionUID = 4716657682337915996L;

    static final List<String> DEFAULT_LOG_PROFILES =
            Arrays.asList(
                    "DEFAULT_LOGGING.properties",
                    "VERBOSE_LOGGING.properties",
                    "PRODUCTION_LOGGING.properties",
                    "GEOTOOLS_DEVELOPER_LOGGING.properties",
                    "GEOSERVER_DEVELOPER_LOGGING.properties");

    public static final ArrayList<String> AVAILABLE_CHARSETS =
            new ArrayList<String>(Charset.availableCharsets().keySet());

    public GlobalSettingsPage() {
        final IModel<GeoServerInfo> globalInfoModel = getGlobalInfoModel();
        final IModel<LoggingInfo> loggingInfoModel = getLoggingInfoModel();

        CompoundPropertyModel<GeoServerInfo> model = new CompoundPropertyModel<>(globalInfoModel);
        PropertyModel<SettingsInfo> settingsModel = new PropertyModel<>(model, "settings");
        Form<GeoServerInfo> form = new Form<>("form", model);

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
                        "stdOutLogging",
                        new PropertyModel<Boolean>(loggingInfoModel, "stdOutLogging")));
        form.add(
                new TextField<String>(
                        "loggingLocation",
                        new PropertyModel<String>(loggingInfoModel, "location")));

        TextField<String> xmlPostRequestLogBufferSize =
                new TextField<String>(
                        "xmlPostRequestLogBufferSize",
                        new PropertyModel<String>(globalInfoModel, "xmlPostRequestLogBufferSize"));
        xmlPostRequestLogBufferSize.add(RangeValidator.minimum(0));
        form.add(xmlPostRequestLogBufferSize);

        form.add(new CheckBox("xmlExternalEntitiesEnabled"));

        form.add(new TextField<Integer>("featureTypeCacheSize").add(RangeValidator.minimum(0)));

        IModel<String> lockProviderModel =
                new PropertyModel<String>(globalInfoModel, "lockProviderName");
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
                new PropertyModel<GeoServerInfo.WebUIMode>(globalInfoModel, "webUIMode");
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
                        new PropertyModel<Boolean>(
                                globalInfoModel, "allowStoredQueriesPerWorkspace")));

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

        Button submit =
                new Button("submit") {
                    @Override
                    public void onSubmit() {
                        GeoServer gs = getGeoServer();
                        gs.save((GeoServerInfo) globalInfoModel.getObject());
                        gs.save((LoggingInfo) loggingInfoModel.getObject());
                        doReturn();
                    }
                };
        form.add(submit);

        Button cancel =
                new Button("cancel") {
                    @Override
                    public void onSubmit() {
                        doReturn();
                    }
                };
        form.add(cancel);
    }

    private void logLevelsAppend(Form<GeoServerInfo> form, IModel<LoggingInfo> loggingInfoModel) {
        // search for *LOGGING.properties files in the data directory
        GeoServerResourceLoader loader =
                GeoServerApplication.get().getBeanOfType(GeoServerResourceLoader.class);
        List<String> logProfiles = null;
        try {
            Resource logsDirectory = loader.get("logs");
            if (logsDirectory.getType() == Type.DIRECTORY) {
                List<Resource> propFiles =
                        Resources.list(
                                logsDirectory,
                                new Filter<Resource>() {
                                    @Override
                                    public boolean accept(Resource obj) {
                                        return obj.name()
                                                .toLowerCase()
                                                .endsWith("logging.properties");
                                    }
                                });
                logProfiles = new ArrayList<String>();
                for (Resource res : propFiles) {
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
        if (logProfiles == null || logProfiles.size() == 0) logProfiles = DEFAULT_LOG_PROFILES;

        form.add(
                new ListChoice<String>(
                        "log4jConfigFile",
                        new PropertyModel<String>(loggingInfoModel, "level"),
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

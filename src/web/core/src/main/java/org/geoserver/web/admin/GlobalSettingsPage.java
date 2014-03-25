/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.admin;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.ListChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.validation.validator.MinimumValidator;
import org.apache.wicket.validation.validator.UrlValidator;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ResourceErrorHandling;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.LockProvider;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.LocalizedChoiceRenderer;
import org.geoserver.web.wicket.ParamResourceModel;
import org.springframework.context.ApplicationContext;

public class GlobalSettingsPage extends ServerAdminPage {


    private static final long serialVersionUID = 4716657682337915996L;

    static final List<String> DEFAULT_LOG_PROFILES = Arrays.asList("DEFAULT_LOGGING.properties",
            "VERBOSE_LOGGING.properties", "PRODUCTION_LOGGING.properties",
            "GEOTOOLS_DEVELOPER_LOGGING.properties", "GEOSERVER_DEVELOPER_LOGGING.properties");

    public static final ArrayList<String> AVAILABLE_CHARSETS = new ArrayList<String>(Charset.availableCharsets().keySet());

    public GlobalSettingsPage() {
        final IModel globalInfoModel = getGlobalInfoModel();
        final IModel loggingInfoModel = getLoggingInfoModel();
        
        Form form = new Form("form", new CompoundPropertyModel(globalInfoModel));

        add(form);

        form.add(new CheckBox("verbose"));
        form.add(new CheckBox("verboseExceptions"));
        form.add(new CheckBox("globalServices"));
        form.add(new TextField<Integer>("numDecimals").add(new MinimumValidator<Integer>(0)));
        form.add(new DropDownChoice("charset", AVAILABLE_CHARSETS));
        form.add(new DropDownChoice<ResourceErrorHandling>("resourceErrorHandling", Arrays.asList(ResourceErrorHandling.values()),
                new ResourceErrorHandlingRenderer()));
        form.add(new TextField("proxyBaseUrl").add(new UrlValidator()));
        
        logLevelsAppend(form, loggingInfoModel);
        form.add(new CheckBox("stdOutLogging", new PropertyModel( loggingInfoModel, "stdOutLogging")));
        form.add(new TextField("loggingLocation", new PropertyModel( loggingInfoModel, "location")) );

        TextField xmlPostRequestLogBufferSize = new TextField("xmlPostRequestLogBufferSize", new PropertyModel(
                globalInfoModel, "xmlPostRequestLogBufferSize"));
        xmlPostRequestLogBufferSize.add(new MinimumValidator<Integer>(0));
        form.add(xmlPostRequestLogBufferSize);

        form.add(new CheckBox("xmlExternalEntitiesEnabled"));    
        
        form.add(new TextField<Integer>("featureTypeCacheSize").add(new MinimumValidator<Integer>(0)));
       
        IModel<String> lockProviderModel = new PropertyModel<String>(globalInfoModel, "lockProviderName");
        ApplicationContext applicationContext = GeoServerApplication.get().getApplicationContext();
        List<String> providers = new ArrayList<String>( Arrays.asList(applicationContext.getBeanNamesForType( LockProvider.class )));
        providers.remove("lockProvider"); // remove the global lock provider
        Collections.sort(providers);;
        
        DropDownChoice<String> lockProviderChoice = new DropDownChoice<String>("lockProvider", lockProviderModel, providers, new LocalizedChoiceRenderer(this));
        
        form.add( lockProviderChoice );
        
        Button submit = new Button("submit", new StringResourceModel("submit", this, null)) {
            @Override
            public void onSubmit() {
                GeoServer gs = getGeoServer();
                gs.save( (GeoServerInfo) globalInfoModel.getObject() );
                gs.save( (LoggingInfo) loggingInfoModel.getObject() );
                doReturn();
            }
        };
        form.add(submit);
        
        Button cancel = new Button("cancel") {
            @Override
            public void onSubmit() {
                doReturn();
            }
        };
        form.add(cancel);
    }

    private void logLevelsAppend(Form form, IModel loggingInfoModel) {
        // search for *LOGGING.properties files in the data directory
        GeoServerResourceLoader loader = GeoServerApplication.get().getBeanOfType(
                GeoServerResourceLoader.class);
        List<String> logProfiles = null;
        try {
            File logsDirectory = loader.find("logs");
            if(logsDirectory.exists() && logsDirectory.isDirectory()) {
                String[] propFiles = logsDirectory.list(new FilenameFilter() {
                    
                    public boolean accept(File dir, String name) {
                        return name.toLowerCase().endsWith("logging.properties");
                    }
                });
                logProfiles = Arrays.asList(propFiles);
                Collections.sort(logProfiles, String.CASE_INSENSITIVE_ORDER);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING,
                    "Could not load the list of log configurations from the data directory", e);
        }
        // if none is found use the default set
        if(logProfiles == null || logProfiles.size() == 0)
            logProfiles = DEFAULT_LOG_PROFILES;

        form.add(new ListChoice("log4jConfigFile", new PropertyModel(loggingInfoModel,
                "level"), logProfiles));
    }
    
    class ResourceErrorHandlingRenderer implements IChoiceRenderer<ResourceErrorHandling> {

        @Override
        public Object getDisplayValue(ResourceErrorHandling object) {
            return new ParamResourceModel(object.name(), GlobalSettingsPage.this).getString();
        }

        @Override
        public String getIdValue(ResourceErrorHandling object, int index) {
            return object.name();
        }

    }
}

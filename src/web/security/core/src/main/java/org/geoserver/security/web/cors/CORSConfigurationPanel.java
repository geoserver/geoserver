/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 *
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.cors;

import java.util.Arrays;
import java.util.List;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.SettingsInfo;
import org.geoserver.security.cors.CORSConfiguration;
import org.geoserver.web.util.MetadataMapModel;

public class CORSConfigurationPanel extends Panel {

    public CORSConfigurationPanel(String id, IModel<SettingsInfo> settingsInfoIModel) {
        super(id, settingsInfoIModel);
        final PropertyModel<MetadataMap> metadata = new PropertyModel<>(settingsInfoIModel, "metadata");

        MetadataMap metadataMap = metadata.getObject();

        // Initialize CORS Configuration settings if missing
        if (metadataMap != null && !metadataMap.containsKey(CORSConfiguration.CORS_CONFIGURATION_METADATA_KEY)) {
            CORSConfiguration corsConfiguration = new CORSConfiguration();
            metadataMap.put(CORSConfiguration.CORS_CONFIGURATION_METADATA_KEY, corsConfiguration);
        }
        MetadataMapModel<Object> model = new MetadataMapModel<>(
                metadata, CORSConfiguration.CORS_CONFIGURATION_METADATA_KEY, CORSConfiguration.class);

        add(new CheckBox("enabled", new PropertyModel<>(model, "enabled")));
        TextField<String> allowedOriginPatterns =
                new TextField<>("allowedOriginPatterns", new PropertyModel<>(model, "allowedOriginPatterns"));
        add(allowedOriginPatterns);
        // AllowedMethods multiselect
        List<String> methodChoices = Arrays.asList("GET", "POST", "HEAD", "OPTIONS");

        ListMultipleChoice<String> allowedMethods =
                new ListMultipleChoice<>("allowedMethods", new PropertyModel<>(model, "allowedMethods"), methodChoices);
        add(allowedMethods);
        // AllowedHeaders multiselect
        List<String> headerChoices = Arrays.asList("X-Requested-With", "Content-Type", "Accept", "Origin");

        ListMultipleChoice<String> allowedHeaders =
                new ListMultipleChoice<>("allowedHeaders", new PropertyModel<>(model, "allowedHeaders"), headerChoices);
        add(allowedHeaders);

        NumberTextField<Integer> maxAge =
                new NumberTextField<>("maxAge", new PropertyModel<>(model, "maxAge"), Integer.class);
        maxAge.setMinimum(0);
        maxAge.setMaximum(9999999);
        add(maxAge);

        add(new CheckBox("supportsCredentials", new PropertyModel<>(model, "supportsCredentials")));
    }
}

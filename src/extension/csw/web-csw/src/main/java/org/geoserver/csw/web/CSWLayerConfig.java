/* (c) 2015 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.web;

import java.io.Serial;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.validator.RangeValidator;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.GeoServer;
import org.geoserver.csw.CSWInfo;
import org.geoserver.csw.DirectDownloadSettings;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.web.publish.PublishedConfigurationPanel;
import org.geoserver.web.util.MetadataMapModel;

/** A configuration panel for CoverageInfo properties that related to CSW publication */
public class CSWLayerConfig extends PublishedConfigurationPanel<LayerInfo> {

    @Serial
    private static final long serialVersionUID = 6204512572932860227L;

    protected final CheckBox directDownloadEnabled;

    protected final TextField<Long> maxDownloadSize;

    public CSWLayerConfig(String id, IModel<LayerInfo> model) {
        super(id, model);

        PropertyModel<MetadataMap> settingsMap = new PropertyModel<>(model, "resource.metadata");

        DirectDownloadSettings settings = DirectDownloadSettings.getSettingsFromMetadata(settingsMap.getObject(), null);
        if (settings == null) {
            settingsMap
                    .getObject()
                    .put(
                            DirectDownloadSettings.DIRECTDOWNLOAD_KEY,
                            setDefaultSettings(
                                    GeoServerExtensions.bean(GeoServer.class).getService(CSWInfo.class)));
        }
        IModel<DirectDownloadSettings> directDownloadModel = new MetadataMapModel<>(
                settingsMap, DirectDownloadSettings.DIRECTDOWNLOAD_KEY, DirectDownloadSettings.class);

        directDownloadEnabled = new CheckBox(
                "directDownloadEnabled", new PropertyModel<>(directDownloadModel, "directDownloadEnabled"));
        add(directDownloadEnabled);

        maxDownloadSize =
                new TextField<>("maxDownloadSize", new PropertyModel<>(directDownloadModel, "maxDownloadSize"));
        maxDownloadSize.add(RangeValidator.minimum(0l));
        add(maxDownloadSize);
    }

    /** Get DefaultSettings from {@link CSWInfo} config or default value. */
    private DirectDownloadSettings setDefaultSettings(CSWInfo info) {
        if (info != null) {
            MetadataMap serviceInfoMetadata = info.getMetadata();
            DirectDownloadSettings infoSettings =
                    DirectDownloadSettings.getSettingsFromMetadata(serviceInfoMetadata, null);
            // create a copy of the CSWInfo settings
            if (infoSettings != null) {
                return new DirectDownloadSettings(infoSettings);
            }
        }
        return new DirectDownloadSettings();
    }
}

/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wps;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.validator.RangeValidator;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.services.AdminPagePanel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.wps.gs.download.DownloadServiceConfiguration;
import org.geoserver.wps.gs.download.DownloadServiceConfigurationWatcher;
import org.geotools.util.logging.Logging;

/** Extension to the WPS admin panel, allowing to edit the download limits */
public class DownloadLimitsPanel extends AdminPagePanel {

    static final Logger LOGGER = Logging.getLogger(DownloadLimitsPanel.class);

    public DownloadLimitsPanel(String id, IModel<?> model) {
        super(id, model);

        ConfigurationModel cm = new ConfigurationModel();
        setDefaultModel(cm);

        TextField<Long> maxFeatures =
                new TextField<>("maxFeatures", new PropertyModel<>(cm, "maxFeatures"));
        maxFeatures.add(RangeValidator.minimum(0l));
        add(maxFeatures);

        TextField<Long> rasterSizeLimit =
                new TextField<>("rasterSizeLimits", new PropertyModel<>(cm, "rasterSizeLimits"));
        rasterSizeLimit.add(RangeValidator.minimum(0l));
        add(rasterSizeLimit);

        TextField<Long> writeLimits =
                new TextField<>("writeLimits", new PropertyModel<>(cm, "writeLimits"));
        writeLimits.add(RangeValidator.minimum(0l));
        add(writeLimits);

        TextField<Long> hardOutputLimit =
                new TextField<>("hardOutputLimit", new PropertyModel<>(cm, "hardOutputLimit"));
        hardOutputLimit.add(RangeValidator.minimum(0l));
        add(hardOutputLimit);

        TextField<Long> maxAnimationFrames =
                new TextField<>(
                        "maxAnimationFrames", new PropertyModel<>(cm, "maxAnimationFrames"));
        maxAnimationFrames.add(RangeValidator.minimum(0));
        add(maxAnimationFrames);

        TextField<Long> compressionLevel =
                new TextField<>("compressionLevel", new PropertyModel<>(cm, "compressionLevel"));
        compressionLevel.add(RangeValidator.range(0, 8));
        add(compressionLevel);
    }

    private DownloadServiceConfigurationWatcher getConfigurationPersister() {
        return GeoServerApplication.get().getBeanOfType(DownloadServiceConfigurationWatcher.class);
    }

    @Override
    public void onMainFormSubmit() {
        DownloadServiceConfiguration config =
                (DownloadServiceConfiguration) getDefaultModelObject();
        try {
            getConfigurationPersister().setConfiguration(config);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to save WPS download configuration", e);
            error(new ParamResourceModel("saveFailed", this, e.getMessage()).getString());
        }
    }

    class ConfigurationModel extends LoadableDetachableModel<DownloadServiceConfiguration> {

        @Override
        protected DownloadServiceConfiguration load() {
            return getConfigurationPersister().getConfiguration();
        }
    }
}

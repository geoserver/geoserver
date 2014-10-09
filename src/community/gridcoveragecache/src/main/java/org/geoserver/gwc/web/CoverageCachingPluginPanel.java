/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.geoserver.coverage.configuration.CoverageCacheConfig;
import org.geoserver.coverage.configuration.CoverageCacheConfigPersister;
import org.geoserver.coverage.layer.CoverageTileLayerInfo.InterpolationType;
import org.geoserver.coverage.layer.CoverageTileLayerInfo.SeedingPolicy;
import org.geoserver.coverage.layer.CoverageTileLayerInfo.TiffCompression;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.gwc.web.DefaultGridsetsEditor;
import org.geoserver.gwc.web.plugin.GWCSettingsPluginPanel;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.LocalizedChoiceRenderer;
import org.geotools.coverage.grid.io.OverviewPolicy;
import org.geowebcache.locks.LockProvider;
import org.springframework.context.ApplicationContext;

public class CoverageCachingPluginPanel extends GWCSettingsPluginPanel {

    private Model<CoverageCacheConfig> configModel;

    public CoverageCachingPluginPanel(String id, IModel<GWCConfig> model) {
        super(id, model);
        
        // Create a new model for the CoverageCacheConfigPersister
        CoverageCacheConfigPersister persister = GeoServerExtensions.bean(CoverageCacheConfigPersister.class);
        
        // Load the Configuration
        CoverageCacheConfig config = persister.getConfiguration();
        
        // Create the model
        configModel = new Model<CoverageCacheConfig>(config);
        
        // Creation of the Panel
        final WebMarkupContainer configs = new WebMarkupContainer("configsCov");
        configs.setOutputMarkupId(true);
        add(configs);
        
        IModel<String> lockProviderModel = new PropertyModel<String>(configModel, "lockProviderName");
        ApplicationContext applicationContext = GeoServerApplication.get().getApplicationContext();
        String[] lockProviders = applicationContext.getBeanNamesForType(LockProvider.class);
        List<String> lockProviderChoices = new ArrayList<String>(Arrays.asList(lockProviders));
        Collections.sort(lockProviderChoices); // make sure we get a stable listing order
        DropDownChoice<String> lockProviderChoice = new DropDownChoice<String>("lockProviderCov", lockProviderModel,
                lockProviderChoices, new LocalizedChoiceRenderer(this));
        configs.add(lockProviderChoice);
        
        List<Integer> metaTilingChoices = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13,
                14, 16, 16, 17, 18, 19, 20);
        IModel<Integer> metaTilingXModel = new PropertyModel<Integer>(configModel, "metaTilingX");
        DropDownChoice<Integer> metaTilingX = new DropDownChoice<Integer>("metaTilingXCov",
                metaTilingXModel, metaTilingChoices);
        metaTilingX.setRequired(true);
        configs.add(metaTilingX);

        IModel<Integer> metaTilingYModel = new PropertyModel<Integer>(configModel, "metaTilingY");
        DropDownChoice<Integer> metaTilingY = new DropDownChoice<Integer>("metaTilingYCov",
                metaTilingYModel, metaTilingChoices);
        metaTilingY.setRequired(true);
        configs.add(metaTilingY);

        IModel<Integer> gutterModel = new PropertyModel<Integer>(configModel, "gutter");
        List<Integer> gutterChoices = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 15, 20, 50,
                100);
        DropDownChoice<Integer> gutterChoice = new DropDownChoice<Integer>("gutterCov", gutterModel,
                gutterChoices);
        configs.add(gutterChoice);
        
        
        IModel<SeedingPolicy> policyModel = new PropertyModel<SeedingPolicy>(configModel, "seedingPolicy");
        DropDownChoice<SeedingPolicy> seedingPolicy = new DropDownChoice<SeedingPolicy>("seedingPolicy", policyModel, Arrays.asList(SeedingPolicy.values()));
        configs.add(seedingPolicy);

        IModel<InterpolationType> interpolationModel = new PropertyModel<InterpolationType>(configModel, "interpolationType");

        DropDownChoice<InterpolationType> interpolationPolicy = new DropDownChoice<InterpolationType>("interpolationType",
                interpolationModel, Arrays.asList(InterpolationType.values()));
        configs.add(interpolationPolicy);

        IModel<OverviewPolicy> overviewModel = new PropertyModel<OverviewPolicy>(configModel, "overviewPolicy");

        DropDownChoice<OverviewPolicy> overviewPolicy = new DropDownChoice<OverviewPolicy>("overviewPolicy",
                overviewModel, Arrays.asList(OverviewPolicy.values()));
        configs.add(overviewPolicy);

        IModel<TiffCompression> compressionModel = new PropertyModel<TiffCompression>(configModel, "tiffCompression");

        DropDownChoice<TiffCompression> tiffCompression = new DropDownChoice<TiffCompression>("tiffCompression", compressionModel,
                Arrays.asList(TiffCompression.values()));
        configs.add(tiffCompression);
        
        IModel<Set<String>> cachedGridsetsModel = new PropertyModel<Set<String>>(configModel,
                "defaultCachingGridSetIds");
        DefaultGridsetsEditor cachedGridsets = new DefaultGridsetsEditor("cachedGridsetsCov",
                cachedGridsetsModel);
        configs.add(cachedGridsets);

        cachedGridsets.add(new IValidator<Set<String>>() {

            private static final long serialVersionUID = 1L;

            @Override
            public void validate(IValidatable<Set<String>> validatable) {
                if (validatable.getValue().isEmpty()) {
                    ValidationError error = new ValidationError();
                    error.setMessage(new ResourceModel(
                            "CachingOptionsPanel.validation.emptyGridsets").getObject());
                    validatable.error(error);
                }
            }
        });
    }

    @Override
    public void doSave() {
        // Create a new model for the CoverageCacheConfigPersister
        CoverageCacheConfigPersister persister = GeoServerExtensions.bean(CoverageCacheConfigPersister.class);
        
        try {
            persister.save(configModel.getObject());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

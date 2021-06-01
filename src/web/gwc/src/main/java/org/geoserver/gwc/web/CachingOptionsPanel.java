/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Check;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.CheckGroup;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.geoserver.catalog.PublishedType;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.gwc.web.layer.WarningSkipsPanel;
import org.geoserver.util.DimensionWarning.WarningType;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.LocalizedChoiceRenderer;
import org.geowebcache.locks.LockProvider;
import org.springframework.context.ApplicationContext;

public class CachingOptionsPanel extends Panel {
    private static final long serialVersionUID = 1L;

    private CheckGroup<String> otherFormatsGroup;

    private CheckGroup<String> rasterFormatsGroup;

    private CheckGroup<String> vectorFormatsGroup;

    public CachingOptionsPanel(final String id, final IModel<GWCConfig> gwcConfigModel) {

        super(id, gwcConfigModel);

        final IModel<Boolean> autoCacheLayersModel =
                new PropertyModel<>(gwcConfigModel, "cacheLayersByDefault");
        final CheckBox autoCacheLayers = new CheckBox("cacheLayersByDefault", autoCacheLayersModel);
        add(autoCacheLayers);

        final WebMarkupContainer container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        add(container);

        final WebMarkupContainer configs = new WebMarkupContainer("configs");
        configs.setOutputMarkupId(true);
        configs.setVisible(autoCacheLayersModel.getObject().booleanValue());
        container.add(configs);

        autoCacheLayers.add(
                new OnChangeAjaxBehavior() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        final boolean visibleConfigs =
                                autoCacheLayers.getModelObject().booleanValue();
                        configs.setVisible(visibleConfigs);
                        target.add(container);
                    }
                });

        IModel<String> lockProviderModel = new PropertyModel<>(gwcConfigModel, "lockProviderName");
        ApplicationContext applicationContext = GeoServerApplication.get().getApplicationContext();
        String[] lockProviders = applicationContext.getBeanNamesForType(LockProvider.class);
        List<String> lockProviderChoices = new ArrayList<>(Arrays.asList(lockProviders));
        Collections.sort(lockProviderChoices); // make sure we get a stable listing order
        DropDownChoice<String> lockProviderChoice =
                new DropDownChoice<>(
                        "lockProvider",
                        lockProviderModel,
                        lockProviderChoices,
                        new LocalizedChoiceRenderer(this));
        configs.add(lockProviderChoice);

        IModel<Boolean> nonDefaultStylesModel =
                new PropertyModel<>(gwcConfigModel, "cacheNonDefaultStyles");
        CheckBox cacheNonDefaultStyles =
                new CheckBox("cacheNonDefaultStyles", nonDefaultStylesModel);
        configs.add(cacheNonDefaultStyles);

        List<Integer> metaTilingChoices =
                Arrays.asList(
                        1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20);
        IModel<Integer> metaTilingXModel = new PropertyModel<>(gwcConfigModel, "metaTilingX");
        DropDownChoice<Integer> metaTilingX =
                new DropDownChoice<>("metaTilingX", metaTilingXModel, metaTilingChoices);
        metaTilingX.setRequired(true);
        configs.add(metaTilingX);

        IModel<Integer> metaTilingYModel = new PropertyModel<>(gwcConfigModel, "metaTilingY");
        DropDownChoice<Integer> metaTilingY =
                new DropDownChoice<>("metaTilingY", metaTilingYModel, metaTilingChoices);
        metaTilingY.setRequired(true);
        configs.add(metaTilingY);

        IModel<Integer> gutterModel = new PropertyModel<>(gwcConfigModel, "gutter");
        List<Integer> gutterChoices =
                Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 15, 20, 50, 100);
        DropDownChoice<Integer> gutterChoice =
                new DropDownChoice<>("gutter", gutterModel, gutterChoices);
        configs.add(gutterChoice);

        {
            List<String> formats =
                    new ArrayList<>(GWC.getAdvertisedCachedFormats(PublishedType.VECTOR));
            IModel<List<String>> vectorFormatsModel =
                    new PropertyModel<>(gwcConfigModel, "defaultVectorCacheFormats");
            mergeExisting(formats, vectorFormatsModel.getObject());
            vectorFormatsGroup = new CheckGroup<>("vectorFormatsGroup", vectorFormatsModel);
            configs.add(vectorFormatsGroup);
            ListView<String> formatsList =
                    new ListView<String>("vectorFromats", formats) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected void populateItem(ListItem<String> item) {
                            item.add(new Check<>("vectorFormatsOption", item.getModel()));
                            item.add(new Label("name", item.getModel()));
                        }
                    };
            formatsList.setReuseItems(true); // otherwise it looses state on invalid form submits
            vectorFormatsGroup.add(formatsList);
        }

        {
            List<String> formats =
                    new ArrayList<>(GWC.getAdvertisedCachedFormats(PublishedType.RASTER));
            IModel<List<String>> rasterFormatsModel =
                    new PropertyModel<>(gwcConfigModel, "defaultCoverageCacheFormats");
            mergeExisting(formats, rasterFormatsModel.getObject());
            rasterFormatsGroup = new CheckGroup<>("rasterFormatsGroup", rasterFormatsModel);
            configs.add(rasterFormatsGroup);
            ListView<String> formatsList =
                    new ListView<String>("rasterFromats", formats) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected void populateItem(ListItem<String> item) {
                            item.add(new Check<>("rasterFormatsOption", item.getModel()));
                            item.add(new Label("name", item.getModel()));
                        }
                    };
            formatsList.setReuseItems(true); // otherwise it looses state on invalid form submits
            rasterFormatsGroup.add(formatsList);
        }
        {
            List<String> formats =
                    new ArrayList<>(GWC.getAdvertisedCachedFormats(PublishedType.GROUP));
            IModel<List<String>> otherFormatsModel =
                    new PropertyModel<>(gwcConfigModel, "defaultOtherCacheFormats");
            mergeExisting(formats, otherFormatsModel.getObject());
            otherFormatsGroup = new CheckGroup<>("otherFormatsGroup", otherFormatsModel);
            configs.add(otherFormatsGroup);
            ListView<String> formatsList =
                    new ListView<String>("otherFromats", formats) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected void populateItem(ListItem<String> item) {
                            item.add(new Check<>("otherFormatsOption", item.getModel()));
                            item.add(new Label("name", item.getModel()));
                        }
                    };
            formatsList.setReuseItems(true); // otherwise it looses state on invalid form submits
            otherFormatsGroup.add(formatsList);
        }

        // Add a new Panel for configuring In Memory caching
        InMemoryBlobStorePanel storePanel =
                new InMemoryBlobStorePanel("blobstores", gwcConfigModel);
        configs.add(storePanel.setOutputMarkupId(true));

        IModel<Set<String>> cachedGridsetsModel =
                new PropertyModel<>(gwcConfigModel, "defaultCachingGridSetIds");
        DefaultGridsetsEditor cachedGridsets =
                new DefaultGridsetsEditor("cachedGridsets", cachedGridsetsModel);
        configs.add(cachedGridsets);

        // cache skips
        IModel<Set<WarningType>> warningSkipsModel =
                new PropertyModel<>(gwcConfigModel, "cacheWarningSkips");
        configs.add(new WarningSkipsPanel("warningSkips", warningSkipsModel));

        cachedGridsets.add(
                new IValidator<Set<String>>() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public void validate(IValidatable<Set<String>> validatable) {
                        boolean validate = autoCacheLayersModel.getObject().booleanValue();
                        if (validate && validatable.getValue().isEmpty()) {
                            ValidationError error = new ValidationError();
                            error.setMessage(
                                    new ResourceModel(
                                                    "CachingOptionsPanel.validation.emptyGridsets")
                                            .getObject());
                            validatable.error(error);
                        }
                    }
                });

        class FormatsValidator implements IValidator<Collection<String>> {
            private static final long serialVersionUID = 1L;

            @Override
            public void validate(IValidatable<Collection<String>> validatable) {
                boolean validate = autoCacheLayersModel.getObject().booleanValue();
                Collection<String> value = validatable.getValue();
                if (validate && value.isEmpty()) {
                    ValidationError error = new ValidationError();
                    error.setMessage(
                            new ResourceModel("CachingOptionsPanel.validation.emptyCacheFormatList")
                                    .getObject());
                    validatable.error(error);
                }
            }
        };
        FormatsValidator validator = new FormatsValidator();
        vectorFormatsGroup.add(validator);
        rasterFormatsGroup.add(validator);
        otherFormatsGroup.add(validator);
    }

    /** Merges the elements of existingFormats missing from formats into formats */
    private void mergeExisting(List<String> formats, Collection<String> existingFormats) {
        for (String x : existingFormats) {
            if (!formats.contains(x)) formats.add(x);
        }
    }
}

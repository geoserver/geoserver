/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.geoserver.gwc.ConfigurableBlobStore;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.web.util.MapModel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geowebcache.storage.blobstore.memory.CacheConfiguration;
import org.geowebcache.storage.blobstore.memory.CacheConfiguration.EvictionPolicy;
import org.geowebcache.storage.blobstore.memory.CacheProvider;
import org.geowebcache.storage.blobstore.memory.CacheStatistics;

/**
 * This class is a new Panel for configuring In Memory Caching for GWC. The user can enable/disable
 * In Memory caching, enable/disable file persistence. Also from this panel the user can have
 * information about cache statistics and also change the cache configuration.
 *
 * @author Nicola Lagomarsini Geosolutions
 */
public class InMemoryBlobStorePanel extends Panel {

    /** Key for the miss rate */
    public static final String KEY_MISS_RATE = "missRate";

    /** Key for the hit rate */
    public static final String KEY_HIT_RATE = "hitRate";

    /** Key for the evicted elements number */
    public static final String KEY_EVICTED = "evicted";

    /** Key for the miss count */
    public static final String KEY_MISS_COUNT = "missCount";

    /** Key for the hit count */
    public static final String KEY_HIT_COUNT = "hitCount";

    /** Key for the total elements count */
    public static final String KEY_TOTAL_COUNT = "totalCount";

    /** Key for the cache current memory occupation */
    public static final String KEY_CURRENT_MEM = "currentMemory";

    /** Key for the cache current/total size */
    public static final String KEY_SIZE = "cacheSize";

    /** HashMap containing the values for all the statistics values */
    private HashMap<String, String> values;

    public InMemoryBlobStorePanel(String id, final IModel<GWCConfig> gwcConfigModel) {

        super(id, gwcConfigModel);
        // Initialize the map
        values = new HashMap<String, String>();

        // Creation of the Checbox for enabling disabling inmemory caching
        IModel<Boolean> innerCachingEnabled =
                new PropertyModel<Boolean>(gwcConfigModel, "innerCachingEnabled");
        final CheckBox innerCachingEnabledChoice =
                new CheckBox("innerCachingEnabled", innerCachingEnabled);

        // Container containing all the other parameters
        final WebMarkupContainer container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true).setEnabled(true);

        // Container containing all the parameters related to cache configuration which can be seen
        // only if In Memory caching is enabled
        final CacheConfigContainerWrapper cacheConfigContainer =
                new CacheConfigContainerWrapper(
                        "cacheConfContainer",
                        gwcConfigModel.getObject().getCacheProviderClass(),
                        gwcConfigModel);
        cacheConfigContainer.setOutputMarkupId(true);

        // Avoid Persistence checkbox
        IModel<Boolean> persistenceEnabled =
                new PropertyModel<Boolean>(gwcConfigModel, "persistenceEnabled");
        final CheckBox persistenceEnabledChoice =
                new CheckBox("persistenceEnabled", persistenceEnabled);
        boolean visible =
                innerCachingEnabledChoice.getModelObject() == null
                        ? false
                        : innerCachingEnabledChoice.getModelObject();
        container.setVisible(visible);

        // Choice between the various Cache objects
        final DropDownChoice<String> choice;
        final ConfigurableBlobStore store = GeoServerExtensions.bean(ConfigurableBlobStore.class);
        if (store != null) {
            final Map<String, String> cacheProviders = store.getCacheProvidersNames();
            final IModel<String> providerClass =
                    new PropertyModel<String>(gwcConfigModel, "cacheProviderClass");
            ChoiceRenderer<String> renderer = new CacheProviderRenderer(cacheProviders);
            choice =
                    new DropDownChoice<String>(
                            "caches",
                            providerClass,
                            new ArrayList<String>(cacheProviders.keySet()),
                            renderer);
            choice.add(
                    new AjaxFormComponentUpdatingBehavior("change") {

                        @Override
                        protected void onUpdate(AjaxRequestTarget target) {
                            ConfigurableBlobStore store =
                                    GeoServerExtensions.bean(ConfigurableBlobStore.class);
                            String cacheClass = providerClass.getObject();
                            boolean immutable = false;
                            if (store != null) {
                                immutable = store.getCacheProviders().get(cacheClass).isImmutable();
                            }
                            cacheConfigContainer.setEnabled(!immutable);
                            // If changing the cacheProvider, you must change also the configuration
                            if (!immutable) {
                                if (!gwcConfigModel
                                        .getObject()
                                        .getCacheConfigurations()
                                        .containsKey(cacheClass)) {
                                    gwcConfigModel
                                            .getObject()
                                            .getCacheConfigurations()
                                            .put(cacheClass, new CacheConfiguration());
                                }
                                cacheConfigContainer.setMapKey(cacheClass, gwcConfigModel);
                            }

                            target.add(cacheConfigContainer);
                        }
                    });
            cacheConfigContainer.setEnabled(
                    !store.getCacheProviders().get(providerClass.getObject()).isImmutable());
        } else {
            choice = new DropDownChoice<String>("caches", new ArrayList<String>());
        }
        // Adding cache choice to the container
        container.add(choice);

        persistenceEnabledChoice.setOutputMarkupId(true).setEnabled(true);
        // Adding cache configuration container to the global container
        container.add(cacheConfigContainer);
        // Definition of the behavior related to caching
        innerCachingEnabledChoice.add(
                new AjaxFormComponentUpdatingBehavior("change") {

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        // If In Memory caching is disabled, all the other parameters cannot be seen
                        boolean isVisible =
                                innerCachingEnabledChoice.getModelObject() == null
                                        ? false
                                        : innerCachingEnabledChoice.getModelObject();
                        container.setVisible(isVisible);
                        target.add(container.getParent());
                    }
                });

        add(innerCachingEnabledChoice);
        container.add(persistenceEnabledChoice);
        add(container);

        // Cache Clearing Option
        Button clearCache =
                new Button("cacheClear") {
                    @Override
                    public void onSubmit() {
                        final ConfigurableBlobStore store =
                                GeoServerExtensions.bean(ConfigurableBlobStore.class);
                        // Clear cache
                        if (store != null) {
                            store.clearCache();
                        }
                    }
                };
        container.add(clearCache);

        // Cache Statistics
        final WebMarkupContainer statsContainer = new WebMarkupContainer("statsContainer");
        statsContainer.setOutputMarkupId(true);

        // Container for the statistics
        final Label totalCountLabel =
                new Label("totalCount", new MapModel(values, KEY_TOTAL_COUNT));
        final Label hitCountLabel = new Label("hitCount", new MapModel(values, KEY_HIT_COUNT));
        final Label missCountLabel = new Label("missCount", new MapModel(values, KEY_MISS_COUNT));
        final Label missRateLabel = new Label("missRate", new MapModel(values, KEY_MISS_RATE));
        final Label hitRateLabel = new Label("hitRate", new MapModel(values, KEY_HIT_RATE));
        final Label evictedLabel = new Label("evicted", new MapModel(values, KEY_EVICTED));
        final Label currentMemoryLabel =
                new Label("currentMemory", new MapModel(values, KEY_CURRENT_MEM));
        final Label cacheSizeLabel = new Label("cacheSize", new MapModel(values, KEY_SIZE));

        statsContainer.add(totalCountLabel);
        statsContainer.add(hitCountLabel);
        statsContainer.add(missCountLabel);
        statsContainer.add(missRateLabel);
        statsContainer.add(hitRateLabel);
        statsContainer.add(evictedLabel);
        statsContainer.add(currentMemoryLabel);
        statsContainer.add(cacheSizeLabel);

        AjaxButton statistics =
                new AjaxButton("statistics") {

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        try {
                            final ConfigurableBlobStore store =
                                    GeoServerExtensions.bean(ConfigurableBlobStore.class);
                            // If checked, all the statistics are reported
                            if (store != null) {
                                CacheStatistics stats = store.getCacheStatistics();

                                long hitCount = stats.getHitCount();
                                long missCount = stats.getMissCount();
                                long total = stats.getRequestCount();
                                double hitRate = stats.getHitRate();
                                double missRate = stats.getMissRate();
                                long evicted = stats.getEvictionCount();
                                double currentMem = stats.getCurrentMemoryOccupation();
                                long byteToMb = 1024 * 1024;
                                double actualSize =
                                        ((long) (100 * (stats.getActualSize() * 1.0d) / byteToMb))
                                                / 100d;
                                double totalSize =
                                        ((long) (100 * (stats.getTotalSize() * 1.0d) / byteToMb))
                                                / 100d;
                                // If a parameter is not correct, Unavailable is used
                                values.put(
                                        KEY_MISS_RATE,
                                        missRate >= 0 ? missRate + " %" : "Unavailable");
                                values.put(
                                        KEY_HIT_RATE,
                                        hitRate >= 0 ? hitRate + " %" : "Unavailable");
                                values.put(
                                        KEY_EVICTED, evicted >= 0 ? evicted + "" : "Unavailable");
                                values.put(
                                        KEY_TOTAL_COUNT, total >= 0 ? total + "" : "Unavailable");
                                values.put(
                                        KEY_MISS_COUNT,
                                        missCount >= 0 ? missCount + "" : "Unavailable");
                                values.put(
                                        KEY_HIT_COUNT,
                                        hitCount >= 0 ? hitCount + "" : "Unavailable");
                                values.put(
                                        KEY_CURRENT_MEM,
                                        currentMem >= 0 ? currentMem + " %" : "Unavailable");
                                values.put(
                                        KEY_SIZE,
                                        currentMem >= 0 && actualSize >= 0
                                                ? actualSize + " / " + totalSize + " Mb"
                                                : "Unavailable");
                            }
                        } catch (Throwable t) {
                            error(t);
                        }
                        target.add(statsContainer);
                    }
                };

        container.add(statsContainer);
        container.add(statistics);
    }

    /**
     * {@link IValidator} implementation for checking if the value is null, or less or equal to 0
     *
     * @author Nicola Lagomarsini Geosolutions
     */
    static class MinimumLongValidator implements IValidator<Long> {

        private String errorKey;

        public MinimumLongValidator(String error) {
            this.errorKey = error;
        }

        @Override
        public void validate(IValidatable<Long> iv) {
            if (iv.getValue() <= 0) {
                ValidationError error = new ValidationError();
                error.setMessage(new ParamResourceModel(errorKey, null, "").getObject());
                iv.error(error);
            }
        }
    }

    /**
     * {@link IValidator} implementation for checking if the concurrency Level is null, or less than
     * or equal to 0
     *
     * @author Nicola Lagomarsini Geosolutions
     */
    static class MinimumConcurrencyValidator implements IValidator<Integer> {

        @Override
        public void validate(IValidatable<Integer> iv) {
            if (iv.getValue() <= 0) {
                ValidationError error = new ValidationError();
                error.setMessage(
                        new ParamResourceModel("BlobStorePanel.invalidConcurrency", null, "")
                                .getObject());
                iv.error(error);
            }
        }
    }

    /**
     * {@link ChoiceRenderer} implementation mapping available {@link CacheProvider} names with the
     * {@link CacheProvider} class names.
     *
     * @author Nicola Lagomarsini Geosolutions
     */
    static class CacheProviderRenderer extends ChoiceRenderer<String> {

        private Map<String, String> map;

        public CacheProviderRenderer(Map<String, String> map) {
            this.map = map;
        }

        @Override
        public Object getDisplayValue(String object) {
            return map.get(object);
        }

        @Override
        public String getIdValue(String object, int index) {
            return object;
        }
    }

    /**
     * {@link WebMarkupContainer} extension used for rendering a set of components based on the
     * mapping of a key
     *
     * @author Nicola Lagomarsini Geosolutions
     */
    static class CacheConfigContainerWrapper extends WebMarkupContainer {

        public CacheConfigContainerWrapper(
                String id, String key, IModel<GWCConfig> gwcConfigModel) {
            super(id);
            setMapKey(key, gwcConfigModel);
        }

        /**
         * This method removes all the previous mappings from the container and then adds the
         * components again by setting as default value the one taken from the key mapped.
         */
        public void setMapKey(final String key, IModel<GWCConfig> gwcConfigModel) {
            removeAll();
            // get the CacheConfigurations Model
            IModel<Map<String, CacheConfiguration>> cacheConfigurations =
                    new PropertyModel<Map<String, CacheConfiguration>>(
                            gwcConfigModel, "cacheConfigurations");

            // Get CacheConfiguration model
            MapModel cacheConfiguration = new MapModel(cacheConfigurations, key);

            // Cache configuration parameters
            IModel<Long> hardMemoryLimit =
                    new PropertyModel<Long>(cacheConfiguration, "hardMemoryLimit");

            IModel<Long> evictionTimeValue =
                    new PropertyModel<Long>(cacheConfiguration, "evictionTime");

            IModel<EvictionPolicy> policy =
                    new PropertyModel<EvictionPolicy>(cacheConfiguration, "policy");

            IModel<Integer> concurrencyLevel =
                    new PropertyModel<Integer>(cacheConfiguration, "concurrencyLevel");

            final TextField<Long> hardMemory =
                    new TextField<Long>("hardMemoryLimit", hardMemoryLimit);
            hardMemory.setType(Long.class).setOutputMarkupId(true).setEnabled(true);
            hardMemory.add(new MinimumLongValidator("BlobStorePanel.invalidHardMemory"));

            final TextField<Long> evictionTime =
                    new TextField<Long>("evictionTime", evictionTimeValue);
            evictionTime.setType(Long.class).setOutputMarkupId(true).setEnabled(true);

            // by the default all available eviction policies are available
            List<EvictionPolicy> evictionPolicies = Arrays.asList(EvictionPolicy.values());
            ConfigurableBlobStore store = GeoServerExtensions.bean(ConfigurableBlobStore.class);
            if (store != null) {
                // we use the current cache accepted eviction policies
                CacheProvider cache = store.getCacheProviders().get(key);
                evictionPolicies = cache.getSupportedPolicies();
            }

            final DropDownChoice<EvictionPolicy> policyDropDown =
                    new DropDownChoice<EvictionPolicy>("policy", policy, evictionPolicies);
            policyDropDown.setOutputMarkupId(true).setEnabled(true);

            final TextField<Integer> textConcurrency =
                    new TextField<Integer>("concurrencyLevel", concurrencyLevel);
            textConcurrency.setType(Integer.class).setOutputMarkupId(true).setEnabled(true);
            textConcurrency.add(new MinimumConcurrencyValidator());

            // Add all the parameters to the containes
            add(hardMemory);
            add(policyDropDown);
            add(textConcurrency);
            add(evictionTime);
        }
    }
}

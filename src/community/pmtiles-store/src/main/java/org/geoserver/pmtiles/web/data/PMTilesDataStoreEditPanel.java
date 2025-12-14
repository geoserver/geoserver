/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.pmtiles.web.data;

import static org.geotools.tileverse.rangereader.RangeReaderParams.MEMORY_CACHE_BLOCK_SIZE;
import static org.geotools.tileverse.rangereader.RangeReaderParams.RANGEREADER_PROVIDER_ID;
import static org.geotools.tileverse.rangereader.RangeReaderParams.S3_AWS_REGION;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.danekja.java.util.function.serializable.SerializableFunction;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.web.data.store.DefaultDataStoreEditPanel;
import org.geoserver.web.data.store.ParamInfo;
import org.geoserver.web.util.MapModel;
import org.geotools.api.data.DataAccessFactory.Param;
import org.geotools.pmtiles.store.PMTilesDataStoreFactory;
import org.springframework.util.unit.DataSize;

/**
 * Specific edit panel for Protomaps PMTiles data stores.
 *
 * @see PMTilesDataStoreFactory
 */
@SuppressWarnings("serial")
public class PMTilesDataStoreEditPanel extends DefaultDataStoreEditPanel {

    private Map<String, Panel> visiblePanelsPerProviderId = new HashMap<>();

    /**
     * Creates a new PMTiles-specific parameters panel with a list of input fields matching the {@link Param}s for the
     * factory.
     *
     * @param componentId the id for this component instance
     * @param storeEditForm the form being built by the calling class, whose model is the {@link DataStoreInfo} being
     *     edited
     */
    public PMTilesDataStoreEditPanel(final String componentId, final Form<DataStoreInfo> storeEditForm) {
        super(componentId, storeEditForm);
        // This method is meant to be used by components to control visibility of other components
        this.setVisibilityAllowed(true);
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        DataStoreInfo storeInfo = (DataStoreInfo) super.storeEditForm.getModelObject();
        String providerId = (String) storeInfo.getConnectionParameters().get(RANGEREADER_PROVIDER_ID.key);
        sendEvent(new RangeReaderChangedEvent(providerId, null));
    }

    /**
     * Override to fix an issue with ParamInfo constructor that assigns the first item in the list of values as the
     * default value when the param does not define a default value, and we don't want that. This is only called for new
     * stores.
     */
    @Override
    protected void applyParamDefault(ParamInfo paramInfo, StoreInfo info) {
        super.applyParamDefault(paramInfo, info);
        List<Serializable> options = paramInfo.getOptions();
        if (options != null && !options.isEmpty()) {
            info.getConnectionParameters().remove(paramInfo.getName());
        }
    }

    /** Creates a form input component for the given datastore param based on its type and metadata properties. */
    @Override
    protected Panel getInputComponent(
            final String componentId,
            final IModel<Map<String, Serializable>> paramsModel,
            final ParamInfo paramMetadata) {

        final String paramName = paramMetadata.getName();

        Panel panel;
        if (paramName.equals(RANGEREADER_PROVIDER_ID.key)) {
            panel = rangeReaderProvider(componentId, paramsModel, paramMetadata);
        } else if (paramName.equals(S3_AWS_REGION.key)) {
            panel = awsRegion(componentId, paramsModel, paramMetadata);
        } else if (paramName.equals(MEMORY_CACHE_BLOCK_SIZE.key)) {
            panel = memoryCacheBlockSize(componentId, paramsModel, paramMetadata);
        } else {
            panel = super.getInputComponent(componentId, paramsModel, paramMetadata);
        }
        this.visiblePanelsPerProviderId.put(paramName, panel);
        panel.setOutputMarkupId(true);
        panel.setOutputMarkupPlaceholderTag(true); // required to toggle visibility
        return panel;
    }

    @Override
    public void onEvent(IEvent<?> event) {
        Object payload = event.getPayload();
        if (payload instanceof RangeReaderChangedEvent providerChanged) {
            applyVisibility(providerChanged);
        }
    }

    private void applyVisibility(RangeReaderChangedEvent providerChanged) {
        visiblePanelsPerProviderId.entrySet().forEach(e -> applyVisibility(e.getKey(), e.getValue(), providerChanged));
    }

    private void applyVisibility(String paramName, Panel paramPanel, RangeReaderChangedEvent event) {
        final String providerId = event.providerId() == null ? "" : event.providerId();
        final Set<String> alwaysVisible = Set.of("namespace", "pmtiles", "io.tileverse.rangereader.provider");
        final Set<String> cacheable = Set.of("http", "s3", "gcs", "azure");

        if (alwaysVisible.contains(paramName)) {
            return;
        }
        boolean visible = false;
        if (paramName.startsWith("io.tileverse.rangereader.caching")) {
            visible = cacheable.contains(providerId);
        } else if ("s3".equals(providerId)) {
            visible = paramName.startsWith("io.tileverse.rangereader.s3.");
        } else if ("azure".equals(providerId)) {
            visible = paramName.startsWith("io.tileverse.rangereader.azure.");
        } else if ("gcs".equals(providerId)) {
            visible = paramName.startsWith("io.tileverse.rangereader.gcs.");
        } else if ("http".equals(providerId)) {
            visible = paramName.startsWith("io.tileverse.rangereader.http.");
        } else if ("file".equals(providerId)) {
            visible = paramName.startsWith("io.tileverse.rangereader.file.");
        }

        paramPanel.setVisible(visible);
        if (event.target() != null) {
            event.target().add(paramPanel);
        }
    }

    private Select2ChoiceParamPanel<String> awsRegion(
            String componentId, IModel<Map<String, Serializable>> paramsModel, ParamInfo paramMetadata) {

        String paramName = paramMetadata.getName();
        IModel<String> labelModel = new ResourceModel(paramName, paramName);
        IModel<String> model = new MapModel<>(paramsModel, paramName);
        List<String> options = paramMetadata.getOptions().stream()
                .sorted()
                .map(String::valueOf)
                .toList();
        return Select2ChoiceParamPanel.ofStrings(componentId, labelModel, model, options)
                .allowCustomValues(true)
                .setPlaceHolder("us-east-1");
    }

    private Select2ChoiceParamPanel<Integer> memoryCacheBlockSize(
            String componentId, IModel<Map<String, Serializable>> paramsModel, ParamInfo paramMetadata) {
        String paramName = paramMetadata.getName();
        IModel<String> labelModel = new ResourceModel(paramName, paramName);
        IModel<Integer> model = new IntegerModel(new MapModel<>(paramsModel, paramName));
        List<Integer> options = paramMetadata.getOptions().stream()
                .map(opt -> Integer.valueOf(String.valueOf(opt)))
                .toList();
        SerializableFunction<Integer, String> displayFunction =
                i -> DataSize.ofBytes(i).toKilobytes() + "KB";
        return Select2ChoiceParamPanel.of(componentId, labelModel, model, options, displayFunction)
                .allowCustomValues(false)
                .setPlaceHolder(16384);
    }

    static class IntegerModel implements IModel<Integer> {

        private IModel<Serializable> baseModel;

        IntegerModel(IModel<Serializable> baseModel) {
            this.baseModel = baseModel;
        }

        @Override
        public Integer getObject() {
            Serializable object = baseModel.getObject();
            if (object == null) {
                return null;
            }
            if (object instanceof Integer i) {
                return i;
            }
            return Integer.valueOf(String.valueOf(object));
        }

        @Override
        public void setObject(final Integer object) {
            if (object == null) {
                baseModel.setObject(null);
            } else {
                baseModel.setObject(String.valueOf(object));
            }
        }

        @Override
        public void detach() {
            baseModel.detach();
        }
    }

    private RadioGroupParamPanel<String> rangeReaderProvider(
            String componentId, IModel<Map<String, Serializable>> paramsModel, ParamInfo paramInfo) {

        final String paramLabel = paramInfo.getName();

        IModel<String> label = new ResourceModel(paramLabel, paramLabel);
        IModel<String> model = new MapModel<>(paramsModel, RANGEREADER_PROVIDER_ID.key);
        List<String> options =
                paramInfo.getOptions().stream().map(String::valueOf).toList();

        RadioGroupParamPanel<String> paramPanel =
                new RadioGroupParamPanel<>(componentId, label, model, options, this::providerIdLabelModel);

        RadioGroup<String> radioGroup = paramPanel.getFormComponent();
        radioGroup.add(new AjaxFormChoiceComponentUpdatingBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                String providerId = radioGroup.getModel().getObject();
                sendEvent(new RangeReaderChangedEvent(providerId, target));
            }
        });
        return paramPanel;
    }

    private IModel<String> providerIdLabelModel(String providerId) {
        String resourceKey = "%s.%s".formatted(RANGEREADER_PROVIDER_ID.key, providerId);
        String defaultValue = providerId;
        return new ResourceModel(resourceKey, defaultValue);
    }

    private <T> void sendEvent(T payload) {
        send(getPage(), Broadcast.BREADTH, payload);
    }

    static final record RangeReaderChangedEvent(String providerId, AjaxRequestTarget target) {}
}

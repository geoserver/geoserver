/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.pmtiles.web.data;

import static org.geoserver.web.util.WebUtils.IsWicketCssFileEmpty;
import static org.geotools.tileverse.rangereader.RangeReaderParams.RANGEREADER_PROVIDER_ID;
import static org.geotools.tileverse.rangereader.RangeReaderParams.S3_AWS_REGION;

import io.tileverse.storage.StorageConfig;
import java.io.Serializable;
import java.net.URI;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.data.store.DataAccessEditPage;
import org.geoserver.web.data.store.DataAccessNewPage;
import org.geoserver.web.data.store.DefaultDataStoreEditPanel;
import org.geoserver.web.data.store.ParamInfo;
import org.geoserver.web.data.store.panel.ParamPanel;
import org.geoserver.web.util.MapModel;
import org.geotools.api.data.DataAccessFactory.Param;
import org.geotools.pmtiles.store.PMTilesDataStoreFactory;

/**
 * Specific edit panel for Protomaps PMTiles data stores.
 *
 * @see PMTilesDataStoreFactory
 */
@SuppressWarnings("serial")
public class PMTilesDataStoreEditPanel extends DefaultDataStoreEditPanel {

    private static final boolean isCssEmpty = IsWicketCssFileEmpty(PMTilesDataStoreEditPanel.class);

    private static final Set<String> ALWAYS_VISIBLE_PARAMS = Set.of("namespace", "pmtiles", "storage.provider");

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
        DataStoreInfo storeInfo = (DataStoreInfo) super.storeEditForm.getModelObject();
        Map<String, Serializable> params = storeInfo.getConnectionParameters();
        rewriteLegacyKeys(params);
        alignNamespaceWithWorkspace(storeInfo);

        super.onBeforeRender();
        String providerId = (String) params.get(RANGEREADER_PROVIDER_ID.key);
        sendEvent(new RangeReaderChangedEvent(providerId, null));
    }

    /**
     * Forces the namespace connection parameter to the store's workspace namespace. GeoServer keeps the namespace in
     * step with the workspace only when the workspace dropdown is changed; on the workspace-scoped "Add new store" flow
     * the workspace is pre-selected and never fires that change, leaving the namespace seeded from the default
     * workspace. A store saved that way reads over WMS but fails WFS GetFeature, whose catalog lookup is keyed by the
     * namespace. Re-deriving the namespace from the workspace on every render keeps the store correct regardless, and
     * matches GeoServer's own intent that the namespace follows the workspace.
     */
    private void alignNamespaceWithWorkspace(DataStoreInfo storeInfo) {
        WorkspaceInfo workspace = storeInfo.getWorkspace();
        if (workspace == null) {
            return;
        }
        NamespaceInfo namespace = GeoServerApplication.get().getCatalog().getNamespaceByPrefix(workspace.getName());
        if (namespace == null) {
            return;
        }
        storeInfo.getConnectionParameters().put("namespace", namespace.getURI());
    }

    /**
     * Called by {@link DataAccessEditPage} and {@link DataAccessNewPage} to determine whether to apply and save
     * changes. Overriding to discourage
     */
    @Override
    public boolean onSave() {
        return true;
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        // if the panel-specific CSS file contains actual css then have the browser load the css
        if (!isCssEmpty) {
            Class<? extends PMTilesDataStoreEditPanel> scope = getClass();
            String name = scope.getSimpleName() + ".css";
            PackageResourceReference reference = new PackageResourceReference(scope, name);
            response.render(CssHeaderItem.forReference(reference));
        }
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
        } else {
            panel = super.getInputComponent(componentId, paramsModel, paramMetadata);
            keepUriParamsAsPlainStrings(panel, paramMetadata);
        }
        panel.setOutputMarkupId(true);
        // Only the provider-dependent parameters take part in the show/hide toggle. The always-visible core
        // parameters are left to the base panel; in particular this keeps the namespace field under GeoServer's
        // own namespace-follows-workspace synchronization.
        if (!ALWAYS_VISIBLE_PARAMS.contains(paramName)) {
            panel.setOutputMarkupPlaceholderTag(true); // required to toggle visibility
            this.visiblePanelsPerProviderId.put(paramName, panel);
        }
        return panel;
    }

    /**
     * GeoServer's application-wide Wicket converter for {@link URI} ({@code DataDirectoryConverterLocator}) resolves
     * submitted values as files in the data directory and converts anything else to null, silently dropping http
     * endpoints like {@code storage.s3.endpoint} on save. Keep URI-typed params as plain strings; the factory converts
     * them when connecting.
     */
    private void keepUriParamsAsPlainStrings(Panel panel, ParamInfo paramMetadata) {
        if (URI.class.equals(paramMetadata.getBinding()) && panel instanceof ParamPanel<?> paramPanel) {
            paramPanel.getFormComponent().setType(String.class);
        }
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
        final Set<String> cacheable = Set.of("http", "s3", "gcs", "azure");

        boolean visible = false;
        if (paramName.startsWith("storage.caching.")) {
            visible = cacheable.contains(providerId);
        } else if ("s3".equals(providerId)) {
            visible = paramName.startsWith("storage.s3.");
        } else if ("azure".equals(providerId)) {
            visible = paramName.startsWith("storage.azure.");
        } else if ("gcs".equals(providerId)) {
            visible = paramName.startsWith("storage.gcs.");
        } else if ("http".equals(providerId)) {
            visible = paramName.startsWith("storage.http.");
        } else if ("file".equals(providerId)) {
            visible = paramName.startsWith("storage.file.");
        }

        paramPanel.setVisible(visible);
        if (event.target() != null) {
            event.target().add(paramPanel);
        }
    }

    /**
     * Rewrite any legacy {@code io.tileverse.rangereader.*} keys into the canonical {storage.*} form, in place; Wicket
     * MapModel widgets are keyed off the factory's short {@code Param.key} and would otherwise miss values persisted in
     * pre-migration GeoServer catalogs.
     */
    static void rewriteLegacyKeys(Map<String, Serializable> params) {
        Map<String, Serializable> rewritten = new LinkedHashMap<>(params.size());
        params.forEach((k, v) -> rewritten.put(StorageConfig.normalizeKey(k), v));
        params.clear();
        params.putAll(rewritten);
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

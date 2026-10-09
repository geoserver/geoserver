/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.pmtiles.web.data;

import io.tileverse.geoserver.web.storage.StorageAwareDataStoreEditPanel;
import io.tileverse.geoserver.web.storage.StorageParamsPanel.BackendSelection;
import io.tileverse.storage.StorageConfig;
import java.io.Serializable;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.web.data.store.ParamInfo;
import org.geoserver.web.data.store.panel.ParamPanel;
import org.geotools.pmtiles.store.PMTilesDataStoreFactory;

/**
 * Specific edit panel for Protomaps PMTiles data stores. The shared storage section from
 * {@code tileverse-geoserver-storage-web} renders the provider selector and the selected backend's connection fields.
 *
 * @see PMTilesDataStoreFactory
 */
// S110: the GeoServer/Wicket panel hierarchy (DefaultDataStoreEditPanel) exceeds Sonar's parent-count limit.
@SuppressWarnings({"serial", "java:S110"})
public class PMTilesDataStoreEditPanel extends StorageAwareDataStoreEditPanel {

    /**
     * Creates a new PMTiles-specific parameters panel with a list of input fields matching the
     * {@link org.geotools.api.data.DataAccessFactory.Param}s for the factory.
     *
     * @param componentId the id for this component instance
     * @param storeEditForm the form being built by the calling class, whose model is the {@link DataStoreInfo} being
     *     edited
     */
    public PMTilesDataStoreEditPanel(final String componentId, final Form<DataStoreInfo> storeEditForm) {
        super(componentId, storeEditForm, BackendSelection.SINGLE_PROVIDER, true);
    }

    @Override
    protected void onBeforeRender() {
        DataStoreInfo storeInfo = (DataStoreInfo) storeEditForm.getModelObject();
        rewriteLegacyKeys(storeInfo.getConnectionParameters());
        super.onBeforeRender();
    }

    /**
     * Called by {@link org.geoserver.web.data.store.DataAccessEditPage} and
     * {@link org.geoserver.web.data.store.DataAccessNewPage} to determine whether to apply and save changes.
     */
    @Override
    public boolean onSave() {
        return true;
    }

    @Override
    protected Panel getInputComponent(
            final String componentId,
            final IModel<Map<String, Serializable>> paramsModel,
            final ParamInfo paramMetadata) {

        Panel panel = super.getInputComponent(componentId, paramsModel, paramMetadata);
        keepUriParamsAsPlainStrings(panel, paramMetadata);
        return panel;
    }

    /**
     * GeoServer's application-wide Wicket converter for {@link URI} ({@code DataDirectoryConverterLocator}) resolves
     * submitted values as files in the data directory and converts anything else to null, silently dropping http
     * endpoints like the PMTiles URI on save. Keep URI-typed params as plain strings; the factory converts them when
     * connecting.
     */
    private void keepUriParamsAsPlainStrings(Panel panel, ParamInfo paramMetadata) {
        if (URI.class.equals(paramMetadata.getBinding()) && panel instanceof ParamPanel<?> paramPanel) {
            paramPanel.getFormComponent().setType(String.class);
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
}

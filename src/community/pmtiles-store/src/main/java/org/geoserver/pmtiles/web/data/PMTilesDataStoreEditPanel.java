/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.pmtiles.web.data;

import java.io.Serializable;
import java.util.List;
import org.apache.wicket.markup.html.form.Form;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.web.data.store.DefaultDataStoreEditPanel;
import org.geoserver.web.data.store.ParamInfo;
import org.geotools.api.data.DataAccessFactory.Param;
import org.geotools.pmtiles.store.PMTilesDataStoreFactory;

/**
 * Specific edit panel for Protomaps PMTiles data stores.
 *
 * @see PMTilesDataStoreFactory
 */
@SuppressWarnings("serial")
public class PMTilesDataStoreEditPanel extends DefaultDataStoreEditPanel {

    /**
     * Creates a new PMTiles-specific parameters panel with a list of input fields matching the {@link Param}s for the
     * factory.
     *
     * @param componentId the id for this component instance
     * @param storeEditForm the form being built by the calling class, whose model is the {@link DataStoreInfo} being
     *     edited
     */
    public PMTilesDataStoreEditPanel(final String componentId, @SuppressWarnings("rawtypes") final Form storeEditForm) {
        super(componentId, storeEditForm);
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
}

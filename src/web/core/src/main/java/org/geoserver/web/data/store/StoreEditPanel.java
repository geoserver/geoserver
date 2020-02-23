/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.Map;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.web.GeoServerApplication;
import org.geotools.data.DataAccessFactory;
import org.geotools.data.DataAccessFactory.Param;

/**
 * Base class for panels containing the form edit components for {@link StoreInfo} objects
 *
 * @author Gabriel Roldan
 * @see DefaultCoverageStoreEditPanel
 */
public abstract class StoreEditPanel extends Panel {

    private static final long serialVersionUID = 1L;

    protected final Form storeEditForm;

    protected StoreEditPanel(final String componentId, final Form storeEditForm) {
        super(componentId);
        this.storeEditForm = storeEditForm;

        StoreInfo info = (StoreInfo) storeEditForm.getModel().getObject();
        final boolean isNew = null == info.getId();
        if (isNew && info instanceof DataStoreInfo) {
            applyDataStoreParamsDefaults(info);
        }
    }

    /** Initializes all store parameters to their default value */
    protected void applyDataStoreParamsDefaults(StoreInfo info) {
        // grab the factory
        final DataStoreInfo dsInfo = (DataStoreInfo) info;
        DataAccessFactory dsFactory;
        try {
            dsFactory = getCatalog().getResourcePool().getDataStoreFactory(dsInfo);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final Param[] dsParams = dsFactory.getParametersInfo();
        Map connParams = info.getConnectionParameters();
        for (Param p : dsParams) {
            ParamInfo paramInfo = new ParamInfo(p);

            // set default value if not already set to some default
            if (!connParams.containsKey(p.key) || connParams.get(p.key) == null) {
                applyParamDefault(paramInfo, info);
            }
        }
    }

    protected void applyParamDefault(ParamInfo paramInfo, StoreInfo info) {
        Serializable defValue = paramInfo.getValue();
        if ("namespace".equals(paramInfo.getName())) {
            defValue = getCatalog().getDefaultNamespace().getURI();
        } else if (URL.class == paramInfo.getBinding() && null == defValue) {
            defValue = "file:data/example.extension";
        } else {
            defValue = paramInfo.getValue();
        }
        info.getConnectionParameters().put(paramInfo.getName(), defValue);
    }

    protected Catalog getCatalog() {
        GeoServerApplication application = (GeoServerApplication) getApplication();
        return application.getCatalog();
    }

    /** Gives an option to store panels to raise an opinion before saving */
    public boolean onSave() {
        return true;
    }
}

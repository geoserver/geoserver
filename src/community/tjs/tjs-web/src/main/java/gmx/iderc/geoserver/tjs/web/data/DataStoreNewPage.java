/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package gmx.iderc.geoserver.tjs.web.data;

import gmx.iderc.geoserver.tjs.catalog.DataStoreInfo;
import gmx.iderc.geoserver.tjs.catalog.TJSCatalog;
import gmx.iderc.geoserver.tjs.data.TJSDataStore;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.geotools.util.NullProgressListener;

import java.util.logging.Level;

/**
 * Provides a form to configure a new geotools {@link DataAccess}
 *
 * @author Gabriel Roldan
 */
public class DataStoreNewPage extends AbstractDataStorePage {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a new datastore configuration page to create a new datastore of the given type
     *
     * @param dataStoreFactDisplayName the type of datastore to create, given by its factory display name
     */
    public DataStoreNewPage(final String dataStoreFactDisplayName) {
        super();

        // Param[] parametersInfo = dsFact.getParametersInfo();
        // for (int i = 0; i < parametersInfo.length; i++) {
        // Serializable value;
        // final Param param = parametersInfo[i];
        // if (param.sample == null || param.sample instanceof Serializable) {
        // value = (Serializable) param.sample;
        // } else {
        // value = String.valueOf(param.sample);
        // }
        // }

        DataStoreInfo info = getTJSCatalog().getFactory().newDataStoreInfo();
//        info.setWorkspace(defaultWs);
        info.setEnabled(true);
        info.setType(dataStoreFactDisplayName);

        initUI(info);
    }

    @Override
    protected final void onSaveDataStore(final DataStoreInfo info, AjaxRequestTarget target)
            throws IllegalArgumentException {
        final TJSCatalog catalog = getTJSCatalog();

        TJSDataStore dataStore;
        try {
            // REVISIT: this may need to be done after saveing the DataStoreInfo
            dataStore = info.getTJSDataStore(new NullProgressListener());
//            dataStore.dispose();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error obtaining new data store", e);
            String message = e.getMessage();
            if (message == null && e.getCause() != null) {
                message = e.getCause().getMessage();
            }
            throw new IllegalArgumentException(
                                                      "Error creating data store, check the parameters. Error message: " + message);
        }

        // save a copy, so if NewLayerPage fails we can keep on editing this one without being
        // proxied
        DataStoreInfo savedStore = catalog.getFactory().newDataStoreInfo();
        clone(info, savedStore);
        try {
            catalog.add(savedStore);
            catalog.save();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error adding data store to catalog", e);
            String message = e.getMessage();
            if (message == null && e.getCause() != null) {
                message = e.getCause().getMessage();
            }

            throw new IllegalArgumentException(
                                                      "Error creating data store with the provided parameters: " + message);
        }

        setResponsePage(new DataStorePage());
    }

}

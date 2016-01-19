/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.logging.Level;

import javax.management.RuntimeErrorException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidationError;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.web.data.layer.NewLayerPage;
import org.geotools.data.ows.HTTPClient;
import org.geotools.data.ows.SimpleHttpClient;
import org.geotools.data.wms.WebMapServer;
import org.geotools.ows.ServiceException;

public class WMSStoreNewPage extends AbstractWMSStorePage {

    public WMSStoreNewPage() {
        try {
            CatalogBuilder builder = new CatalogBuilder(getCatalog());
            WMSStoreInfo store = builder.buildWMSStore(null);

            initUI(store);
            capabilitiesURL.getFormComponent().add(new WMSCapabilitiesURLValidator());
        } catch (IOException e) {
            throw new RuntimeException("Could not setup the WMS store: " + e.getMessage(), e);
        }
    }

    @Override
    protected void onSave(WMSStoreInfo info, AjaxRequestTarget target)
            throws IllegalArgumentException {
        /*
         * Try saving a copy of it so if the process fails somehow the original "info" does not end
         * up with an id set
         */
        WMSStoreInfo savedStore = getCatalog().getFactory().createWebMapServer();
        clone(info, savedStore);

        // GR: this shouldn't fail, the Catalog.save(StoreInfo) API does not declare any action in
        // case of a failure!... strange, why a save can't fail?
        // Still, be cautious and wrap it in a try/catch block so the page does not blow up
        try {
            getCatalog().validate(savedStore, false).throwIfInvalid();
            getCatalog().save(savedStore);
        } catch (RuntimeException e) {
            LOGGER.log(Level.INFO, "Adding the store for " + info.getCapabilitiesURL(), e);
            throw new IllegalArgumentException(
                    "The WMS store could not be saved. Failure message: " + e.getMessage());
        }

        // the StoreInfo save succeeded... try to present the list of coverages (well, _the_
        // coverage while the getotools coverage api does not allow for more than one
        NewLayerPage layerChooserPage;
        try {
            layerChooserPage = new NewLayerPage(savedStore.getId());
        } catch (RuntimeException e) {
            LOGGER.log(Level.INFO, "Getting list of layers for the WMS store " + info.getCapabilitiesURL(), e);
            // doh, can't present the list of coverages, means saving the StoreInfo is meaningless.
            try {// be extra cautious
                getCatalog().remove(savedStore);
            } catch (RuntimeErrorException shouldNotHappen) {
                LOGGER.log(Level.WARNING, "Can't remove CoverageStoreInfo after adding it!", e);
            }
            // tell the caller why we failed...
            throw new IllegalArgumentException(e.getMessage(), e);
        }

        setResponsePage(layerChooserPage);
    }
    
    final class WMSCapabilitiesURLValidator implements IValidator {

        @Override
        public void validate(IValidatable validatable) {
            String url = (String) validatable.getValue();
            try {
                HTTPClient client = new SimpleHttpClient();
                usernamePanel.getFormComponent().processInput();
                String user = usernamePanel.getFormComponent().getInput();
                password.getFormComponent().processInput();
                String pwd = password.getFormComponent().getInput();
                if (user != null && user.length() > 0 && pwd != null && pwd.length() > 0) {
                    client.setUser(user);
                    client.setPassword(pwd);
                }
                WebMapServer server = new WebMapServer(new URL(url), client);
                server.getCapabilities();
            } catch(IOException | ServiceException e) {
                IValidationError err = new ValidationError("WMSCapabilitiesValidator.connectionFailure")
                        .addKey("WMSCapabilitiesValidator.connectionFailure")
                        .setVariable("error", e.getMessage());
                validatable.error(err);
            }
        }
        
    }

}

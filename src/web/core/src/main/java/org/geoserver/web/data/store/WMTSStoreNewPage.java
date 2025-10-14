/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import javax.management.RuntimeErrorException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidationError;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.ValidationResult;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.platform.GeoServerEnvironment;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.util.EntityResolverProvider;
import org.geoserver.web.data.layer.NewLayerPage;
import org.geotools.http.HTTPClient;
import org.geotools.http.HTTPClientFinder;
import org.geotools.ows.ServiceException;
import org.geotools.ows.wmts.WebMapTileServer;
import org.geotools.xml.DocumentFactory;
import org.geotools.xml.XMLHandlerHints;
import org.xml.sax.EntityResolver;

@SuppressWarnings("serial")
public class WMTSStoreNewPage extends AbstractWMTSStorePage {

    public WMTSStoreNewPage() {
        try {
            CatalogBuilder builder = new CatalogBuilder(getCatalog());

            WMTSStoreInfo store = builder.buildWMTSStore(null);

            initUI(store);

            final GeoServerEnvironment gsEnvironment = GeoServerExtensions.bean(GeoServerEnvironment.class);

            // AF: Disable Binding if GeoServer Env Parametrization is enabled!
            if (gsEnvironment == null || !GeoServerEnvironment.allowEnvParametrization()) {
                capabilitiesURL.getFormComponent().add(new WMTSCapabilitiesURLValidator());
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not setup the WMTS store: " + e.getMessage(), e);
        }
    }

    @Override
    protected void onSave(WMTSStoreInfo info, AjaxRequestTarget target) throws IllegalArgumentException {
        /*
         * Try saving a copy of it so if the process fails somehow the original "info" does not end
         * up with an id set
         */
        WMTSStoreInfo expandedStore = getCatalog().getResourcePool().clone(info, true);
        WMTSStoreInfo savedStore = getCatalog().getFactory().createWebMapTileServer();

        // GR: this shouldn't fail, the Catalog.save(StoreInfo) API does not declare any action in
        // case of a failure!... strange, why a save can't fail?
        // Still, be cautious and wrap it in a try/catch block so the page does not blow up
        try {
            // GeoServer Env substitution; validate first
            ValidationResult validate = getCatalog().validate(expandedStore, false);
            validate.throwIfInvalid();

            // GeoServer Env substitution; force to *AVOID* resolving env placeholders...
            savedStore = getCatalog().getResourcePool().clone(info, false);
            // ... and save
            getCatalog().save(savedStore);
        } catch (RuntimeException e) {
            LOGGER.log(Level.INFO, "Adding the store for " + info.getCapabilitiesURL(), e);
            throw new IllegalArgumentException("The WMTS store could not be saved. Failure message: " + e.getMessage());
        }

        // the StoreInfo save succeeded... try to present the list of coverages (well, _the_
        // coverage while the getotools coverage api does not allow for more than one
        NewLayerPage layerChooserPage;
        try {
            // The ID is assigned by the catalog and therefore cannot be cloned
            layerChooserPage = new NewLayerPage(savedStore.getId());
        } catch (RuntimeException e) {
            LOGGER.log(Level.INFO, "Getting list of layers for the WMTS store " + info.getCapabilitiesURL(), e);
            // doh, can't present the list of coverages, means saving the StoreInfo is meaningless.
            try { // be extra cautious
                getCatalog().remove(expandedStore);
                getCatalog().remove(savedStore);
            } catch (RuntimeErrorException shouldNotHappen) {
                LOGGER.log(Level.WARNING, "Can't remove CoverageStoreInfo after adding it!", e);
            }
            // tell the caller why we failed...
            throw new IllegalArgumentException(e.getMessage(), e);
        }

        setResponsePage(layerChooserPage);
    }

    final class WMTSCapabilitiesURLValidator implements IValidator<String> {

        @Override
        public void validate(IValidatable<String> validatable) {
            String url = validatable.getValue();
            try {
                HTTPClient client = HTTPClientFinder.createClient();
                usernamePanel.getFormComponent().processInput();
                String user = usernamePanel.getFormComponent().getInput();
                password.getFormComponent().processInput();
                String pwd = password.getFormComponent().getInput();
                if (user != null && !user.isEmpty() && pwd != null && !pwd.isEmpty()) {
                    client.setUser(user);
                    client.setPassword(pwd);
                }
                Map<String, Object> hints = new HashMap<>();

                hints.put(DocumentFactory.VALIDATION_HINT, Boolean.FALSE);
                EntityResolverProvider provider = getCatalog().getResourcePool().getEntityResolverProvider();
                if (provider != null) {
                    EntityResolver entityResolver = provider.getEntityResolver();
                    if (entityResolver != null) {
                        hints.put(XMLHandlerHints.ENTITY_RESOLVER, entityResolver);
                    }
                }

                WebMapTileServer server = new WebMapTileServer(new URL(url), client);
                server.getCapabilities();
            } catch (IOException | ServiceException e) {
                IValidationError err = new ValidationError("WMTSCapabilitiesValidator.connectionFailure")
                        .addKey("WMTSCapabilitiesValidator.connectionFailure")
                        .setVariable("error", e.getMessage());
                validatable.error(err);
            }
        }
    }
}

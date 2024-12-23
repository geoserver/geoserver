/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import java.io.File;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.HTTPStoreInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.event.AbstractCatalogListener;
import org.geoserver.catalog.event.CatalogBeforeAddEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.security.FileAccessManager;
import org.geotools.api.data.DataAccessFactory;
import org.geotools.api.data.DataAccessFactory.Param;
import org.geotools.util.URLs;
import org.geotools.util.factory.Hints;
import org.geotools.util.logging.Logging;

/**
 * A {@link CatalogListener} that enforce the file sandbox rules. Checks if the user is allowed to access the file
 * system in response to store modification events.
 */
public class FileSandboxEnforcer extends AbstractCatalogListener {

    private static final Logger LOGGER = Logging.getLogger(FileSandboxEnforcer.class);

    private final ResourcePool resourcePool;
    private final FileAccessManager fileAccessManager;

    public FileSandboxEnforcer(Catalog catalog) {
        catalog.addListener(this);
        this.resourcePool = catalog.getResourcePool();
        this.fileAccessManager = FileAccessManager.lookupFileAccessManager();
    }

    @Override
    public void handlePreAddEvent(CatalogBeforeAddEvent event) throws CatalogException {
        CatalogInfo source = event.getSource();
        if (!(source instanceof StoreInfo)) return;

        if (source instanceof DataStoreInfo) {
            DataStoreInfo store = (DataStoreInfo) source;
            checkDataStoreParameters(store, store.getConnectionParameters());
        } else if (source instanceof CoverageStoreInfo) {
            CoverageStoreInfo store = (CoverageStoreInfo) source;
            checkAccess(store.getURL(), store);
        } else if (source instanceof HTTPStoreInfo) {
            HTTPStoreInfo store = (HTTPStoreInfo) source;
            checkAccess(store.getCapabilitiesURL(), store);
        } else {
            // the above should cover all the store types, but let's make sure we're not missing
            // possible future extensions, as this is security
            throw new CatalogException("Unsupported store type: " + source.getClass());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void handleModifyEvent(CatalogModifyEvent event) throws CatalogException {
        CatalogInfo source = event.getSource();
        if (!(source instanceof StoreInfo)) return;

        if (source instanceof DataStoreInfo) {
            Object params = getNewPropertyValue(event, "connectionParameters");
            if (params instanceof Map) {
                checkDataStoreParameters((DataStoreInfo) source, (Map<String, Serializable>) params);
            }
        } else if (source instanceof CoverageStoreInfo) {
            CoverageStoreInfo store = (CoverageStoreInfo) source;
            Object url = getNewPropertyValue(event, "uRL");
            if (url instanceof String) {
                checkAccess((String) url, store);
            }
            if (url instanceof URL) {
                checkAccess(url.toString(), store);
            }
        } else if (source instanceof HTTPStoreInfo) {
            HTTPStoreInfo store = (HTTPStoreInfo) source;
            String capabilitiesURL = (String) getNewPropertyValue(event, "capabilitiesURL");
            if (capabilitiesURL != null) {
                checkAccess(capabilitiesURL, store);
            }
        } else {
            throw new CatalogException("Unsupported store type: " + source.getClass());
        }
    }

    private void checkDataStoreParameters(DataStoreInfo store, Map<String, Serializable> connectionParameters) {
        try {
            GeoServerResourceLoader loader = resourcePool.getCatalog().getResourceLoader();
            // expand environment variables and data dir local references
            Map<String, Serializable> params = ResourcePool.getParams(connectionParameters, loader);
            DataAccessFactory factory = resourcePool.getDataStoreFactory(store);

            if (factory != null) {
                for (Param param : factory.getParametersInfo()) {
                    if (File.class.isAssignableFrom(param.getType())) {
                        File value = (File) param.lookUp(params);
                        if (value == null) continue;
                        checkAccess(value);
                    } else if (URL.class.isAssignableFrom(param.getType())) {
                        URL value = (URL) param.lookUp(params);
                        if (value == null) continue;
                        if ("file".equals(value.getProtocol()) || value.getProtocol() == null)
                            checkAccess(URLs.urlToFile(value));
                    }
                }
            }
        } catch (SandboxException e) {
            throw e;
        } catch (Exception e) {
            throw new CatalogException("Error checking data store parameters", e);
        }
    }

    private Object getNewPropertyValue(CatalogModifyEvent event, String propertyName) {
        List<String> propertyNames = event.getPropertyNames();
        for (int i = 0; i < propertyNames.size(); i++) {
            if (propertyName.equals(propertyNames.get(i))) {
                return event.getNewValues().get(i);
            }
        }
        return null;
    }

    private void checkAccess(File value) {
        if (!fileAccessManager.checkAccess(value)) {
            throw new SandboxException("Access to " + value + " denied by file sandboxing", value);
        }
    }

    private void checkAccess(String url, CoverageStoreInfo storeInfo) {
        Object converted = ResourcePool.getCoverageStoreSource(url, null, storeInfo, new Hints());
        if (converted instanceof File) {
            checkAccess((File) converted);
        } else if (converted instanceof URL) {
            URL u = (URL) converted;
            if ("file".equals(u.getProtocol()) || u.getProtocol() == null) {
                checkAccess(URLs.urlToFile(u));
            }
        } else {
            // may be a COG, e.g., s3://... or http://...
            try {
                // see if there is a scheme, if not assume it's a file reference and test
                URI uri = new URI(url);
                if (StringUtils.isEmpty(uri.getScheme()) || "file".equals(uri.getScheme())) {
                    checkAccess(new File(url));
                } else {
                    LOGGER.log(
                            Level.FINE,
                            "Not a file URI in coverage store, not validating it against the sandbox: {0}",
                            uri);
                }
            } catch (URISyntaxException e) {
                // not a valid URI, but it may still be a Windows path
                try {
                    Path path = Paths.get(url);
                    checkAccess(path.toFile());
                } catch (InvalidPathException ex) {
                    LOGGER.log(Level.FINEST, "Not a valid URI/Path in coverage store, not validating it", ex);
                }
            }
        }
    }

    private void checkAccess(String urlSpec, HTTPStoreInfo storeInfo) {
        try {
            URL url = new URL(urlSpec);
            if ("file".equals(url.getProtocol())) {
                checkAccess(URLs.urlToFile(url));
            }
        } catch (MalformedURLException e) {
            LOGGER.log(Level.FINE, "Not a valid URL in HTTP store, not validating it", e);
        }
    }

    /** Sandbox exception, thrown when a user tries to access a file outside the sandbox */
    public static class SandboxException extends CatalogException {
        private final File file;

        public SandboxException(String message, File file) {
            super(message);
            this.file = file;
        }

        public File getFile() {
            return file;
        }
    }
}

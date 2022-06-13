/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.web.resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.resource.loader.IStringResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geotools.util.logging.Logging;

public class WicketResourceResourceLoader implements IStringResourceLoader {

    private static final Logger LOGGER = Logging.getLogger(WicketResourceResourceLoader.class);

    private Resource folder;

    private String resourceBundleName;

    private static String EXTENSION = ".properties";

    private boolean shouldThrowException = true;

    public WicketResourceResourceLoader(Resource folder, String resourceBundleName) {
        this.folder = folder;
        this.resourceBundleName = resourceBundleName;
        if (resourceBundleName.endsWith(EXTENSION)) {
            this.resourceBundleName = this.resourceBundleName.replace(EXTENSION, "");
        }
    }

    public String loadStringResource(Component component, String key) {
        return findResource(component.getLocale(), key);
    }

    public String loadStringResource(Class<?> clazz, String key, Locale locale, String style) {
        return findResource(locale, key);
    }

    private String findResource(Locale locale, String key) {
        String string = null;

        ResourceBundle resourceBundle = null;
        if (locale != null && key != null) {
            try {
                Resource res =
                        folder.get(resourceBundleName + "_" + locale.getLanguage() + EXTENSION);
                // Try the specific resource
                if (Resources.exists(res)) {
                    try (InputStream fis = res.in()) {
                        resourceBundle = new PropertyResourceBundle(fis);
                        try {
                            string = findString(key, string, resourceBundle);
                        } catch (Exception ignored) {
                            // ignore, try the generic resource
                        }
                    }
                }
                // Fallback to the main resource
                if (string == null) {
                    res = folder.get(resourceBundleName + EXTENSION);
                    try (InputStream fis = res.in()) {
                        resourceBundle = new PropertyResourceBundle(fis);
                        string = findString(key, string, resourceBundle);
                    }
                }
            } catch (IOException e) {
                if (shouldThrowExceptionForMissingResource()) {
                    throw new WicketRuntimeException(
                            String.format(
                                    "Unable able to locate resource bundle for the specifed base name: %s",
                                    resourceBundleName));
                }
                LOGGER.fine(
                        "Unable able to locate resource bundle for the specifed base name:"
                                + resourceBundleName);
            }
        }
        return string;
    }

    private boolean shouldThrowExceptionForMissingResource() {
        return Application.get().getResourceSettings().getThrowExceptionOnMissingResource()
                && shouldThrowException;
    }

    @Override
    public String loadStringResource(
            Class<?> clazz, String key, Locale locale, String style, String variation) {
        return findResource(locale, key);
    }

    @Override
    public String loadStringResource(
            Component component, String key, Locale locale, String style, String variation) {
        if (component != null) {
            return findResource(component.getLocale(), key);
        }
        return null;
    }

    public void setShouldThrowException(boolean shouldThrowException) {
        this.shouldThrowException = shouldThrowException;
    }

    private String findString(String key, String string, ResourceBundle resourceBundle) {
        boolean caught = false;
        try {
            string = resourceBundle.getString(key);
        } catch (MissingResourceException e) {
            caught = true;
        }

        if (caught || string == null) {
            if (shouldThrowExceptionForMissingResource()) {
                throw new WicketRuntimeException(
                        String.format(
                                "Unable able to locate resource bundle for the specifed base name: %s",
                                resourceBundleName));
            }

            LOGGER.fine("No value found key " + key + " in resource bundle " + resourceBundleName);
        }
        return string;
    }

    public String getResourceBundleName() {
        return resourceBundleName;
    }
}

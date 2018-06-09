/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform.exception;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GeoServerExceptions {

    private static final Logger LOGGER = Logger.getLogger("org.geoserver.platform.exception");

    static Control control = new Control();

    /**
     * Returns a localized message for the specific exception for the default system locale.
     *
     * @see #localize(IGeoServerException, Locale)
     */
    public static String localize(IGeoServerException e) {
        return localize(e, Locale.getDefault());
    }

    /**
     * Returns a localized message for the specific exception, given the specified locale.
     *
     * <p>This method processes the {@link ResourceBundle} extension point to find the appropriate
     * {@link ResourceBundle} containing the localized message. The base name used to look up the
     * message is the name of the exception class. First the fully qualified exception name is used,
     * and if no bundle found, the non qualified name is used.
     *
     * @param e The exception whose message to localize.
     * @param locale The locale to use.
     * @return The localized message, or <code>null</code> if none could be found.
     */
    public static String localize(IGeoServerException e, Locale locale) {
        Class<? extends IGeoServerException> clazz = e.getClass();
        while (clazz != null) {
            String localized = doLocalize(e.getId(), e.getArgs(), clazz, locale);
            if (localized != null) {
                return localized;
            }

            // could not find string, if the exception parent class is also a geoserver exception
            // move up the hierarchy and try that

            if (IGeoServerException.class.isAssignableFrom(clazz.getSuperclass())) {
                clazz = (Class<? extends IGeoServerException>) clazz.getSuperclass();
            } else {
                clazz = null;
            }
        }
        return null;
    }

    static String doLocalize(
            String id, Object[] args, Class<? extends IGeoServerException> clazz, Locale locale) {

        ResourceBundle bundle = null;
        try {
            bundle = ResourceBundle.getBundle("GeoServerException", locale, control);
            // bundle = ResourceBundle.getBundle(clazz.getCanonicalName(), locale, control);
        } catch (MissingResourceException ex) {
        }

        if (bundle == null) {
            // could not locate a bundle
            return null;
        }

        // get the message
        String localized = null;
        try {
            // first try with the qualified class name
            localized = bundle.getString(clazz.getCanonicalName() + "." + id);
        } catch (MissingResourceException ex) {
            // next try with the non qualifiied
            try {
                localized = bundle.getString(clazz.getSimpleName() + "." + id);
            } catch (MissingResourceException ex2) {
            }
        }
        if (localized == null) {
            if (LOGGER.isLoggable(Level.FINER)) {
                LOGGER.finer("Resource lookup failed for key" + id + ", class = " + clazz);
            }
            return null;
        }

        // if there are arguments, format the message accordingly
        if (args != null && args.length > 0) {
            localized = MessageFormat.format(localized, args);
        }

        return localized;
    }

    static class Control extends ResourceBundle.Control {
        static final List<String> FORMATS = Arrays.asList("java.properties");

        @Override
        public List<String> getFormats(String baseName) {
            if (baseName == null) {
                throw new NullPointerException();
            }
            return FORMATS;
        }

        @Override
        public ResourceBundle newBundle(
                String baseName, Locale locale, String format, ClassLoader loader, boolean reload)
                throws IllegalAccessException, InstantiationException, IOException {

            // look for properties file
            String lang = locale.getLanguage();
            String filename = baseName;
            if (lang != null && !"".equals(lang)) {
                filename += "_" + lang;
            }
            filename += ".properties";

            Enumeration<URL> e = loader.getResources(filename);
            Properties props = null;
            while (e.hasMoreElements()) {
                if (props == null) {
                    props = new Properties();
                }

                URL url = e.nextElement();
                InputStream in = url.openStream();
                try {
                    props.load(in);
                } catch (Exception ex) {
                    LOGGER.log(Level.WARNING, "Error loading properties from: ", url);
                } finally {
                    if (in != null) {
                        try {
                            in.close();
                        } catch (Exception ex2) {
                            LOGGER.log(Level.FINEST, ex2.getMessage(), ex2);
                        }
                    }
                }
            }
            return props != null ? new PropResourceBundle(props) : null;
        }
    }

    static class PropResourceBundle extends ResourceBundle {

        Properties props;

        PropResourceBundle(Properties props) {
            this.props = props;
        }

        @Override
        protected Object handleGetObject(String key) {
            return props.get(key);
        }

        @Override
        public Enumeration<String> getKeys() {
            return (Enumeration) props.keys();
        }
    }
}

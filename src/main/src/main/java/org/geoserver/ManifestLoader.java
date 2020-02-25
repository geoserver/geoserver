/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.Properties;
import java.util.TreeSet;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.geoserver.ManifestLoader.AboutModel.ManifestModel;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geotools.util.SuppressFBWarnings;
import org.geotools.util.factory.GeoTools;
import org.geotools.util.logging.Logging;

/** @author cancellieri carlo - GeoSolutions SAS */
public class ManifestLoader {

    private static final Logger LOGGER = Logging.getLogger(ManifestLoader.class.toString());

    // SETTIMGS
    public static final String RESOURCE_NAME_REGEX = "resourceNameRegex";

    public static final String RESOURCE_ATTRIBUTE_EXCLUSIONS = "resourceAttributeExclusions";

    public static final String VERSION_ATTRIBUTE_INCLUSIONS = "versionAttributeInclusions";

    public static final String PROPERTIES_FILE = "manifest.properties";

    // loaded settings form PROPERTIES_FILE
    private static Properties props;

    private static Pattern resourceNameRegex;

    private static String resourceAttributeExclusions[];

    private static String versionAttributeInclusions[];

    private static ClassLoader classLoader;

    @SuppressFBWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
    public ManifestLoader(GeoServerResourceLoader loader) throws Exception {

        classLoader = loader.getClassLoader();
        if (classLoader == null) {
            throw new IllegalStateException(
                    "Could not get the class loader from GeoServerResourceLoader");
        }

        props = new Properties();

        // load from jar or classpath
        try (InputStream is = classLoader.getResourceAsStream("org/geoserver/" + PROPERTIES_FILE)) {
            if (is != null) {
                props.load(is);
            }
        } catch (IOException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }
        // override settings from datadir
        // datadir search
        Resource resource = loader.get(PROPERTIES_FILE);
        if (resource.getType() == Type.RESOURCE) {
            try (InputStream is = resource.in()) {
                props.load(is);
            } catch (IOException e2) {
                LOGGER.log(Level.FINER, e2.getMessage(), e2);
            }
        }

        try {
            resourceNameRegex =
                    Pattern.compile(
                            props.getProperty(RESOURCE_NAME_REGEX) + "!/META-INF/MANIFEST.MF");
        } catch (PatternSyntaxException e) {
            LOGGER.log(java.util.logging.Level.SEVERE, e.getLocalizedMessage(), e);
            throw e;
        }

        String ae = props.getProperty(RESOURCE_ATTRIBUTE_EXCLUSIONS);
        if (ae != null) {
            resourceAttributeExclusions = ae.split(",");
        } else {
            resourceAttributeExclusions = new String[0];
        }

        String ai = props.getProperty(VERSION_ATTRIBUTE_INCLUSIONS);
        if (ai != null) {
            versionAttributeInclusions = ai.split(",");
        } else {
            // defaults
            throw new Exception("Include attribute array cannot be null");
        }
    }

    /**
     * load an about model
     *
     * @throws IllegalArgumentException if arguments are null
     */
    private static AboutModel getAboutModel(final ClassLoader loader)
            throws IllegalArgumentException {

        if (loader == null) {
            throw new IllegalArgumentException("Unable to run with null arguments");
        }

        final AboutModel model = new AboutModel();
        Map<String, Manifest> manifests = loadManifest(loader);
        Iterator<java.util.Map.Entry<String, Manifest>> it = manifests.entrySet().iterator();
        while (it.hasNext()) {
            java.util.Map.Entry<String, Manifest> entry = it.next();
            model.add(
                    ManifestModel.parseManifest(
                            trimName(entry.getKey()),
                            entry.getValue(),
                            new ManifestModel.ExcludeAttributeFilter(resourceAttributeExclusions)));
        }
        return model;
    }

    private static Map<String, Manifest> loadManifest(final ClassLoader loader)
            throws IllegalArgumentException {

        if (loader == null) {
            throw new IllegalArgumentException("Unable to run with null arguments");
        }

        Map<String, Manifest> manifests = new HashMap<String, Manifest>();
        try {
            Enumeration<URL> resources = loader.getResources("META-INF/MANIFEST.MF");
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();

                if (LOGGER.isLoggable(Level.FINE))
                    LOGGER.fine("Loading resources: " + resource.getFile());
                try (InputStream is = resource.openStream()) {
                    manifests.put(resource.getPath(), new Manifest(is));
                } catch (IOException e) {
                    // handle
                    LOGGER.log(
                            java.util.logging.Level.SEVERE,
                            "Error loading resources file: " + e.getLocalizedMessage(),
                            e);
                }
            }
        } catch (IOException e) {
            LOGGER.log(
                    java.util.logging.Level.SEVERE,
                    "Error loading resources file: " + e.getLocalizedMessage(),
                    e);
        }

        return manifests;
    }

    private static String trimName(String path) {
        Matcher m = resourceNameRegex.matcher(path);
        if (m.matches()) return m.group(1);
        else {
            String name = path.substring(0, path.length() - 22);
            return name.substring(name.lastIndexOf('/') + 1);
        }
    }

    /** @return load the AboutModel of all the loaded resources */
    public static AboutModel getResources() {
        return getAboutModel(classLoader);
    }

    public static Manifest getManifest(Class<?> clz) {
        String resource = "/" + clz.getName().replace(".", "/") + ".class";
        String fullPath = clz.getResource(resource).toString();
        String archivePath = fullPath.substring(0, fullPath.length() - resource.length());
        if (archivePath.endsWith("\\WEB-INF\\classes")
                || archivePath.endsWith("/WEB-INF/classes")) {
            archivePath =
                    archivePath.substring(
                            0,
                            archivePath.length()
                                    - "/WEB-INF/classes".length()); // Required for wars
        }

        try (InputStream input = new URL(archivePath + "/META-INF/MANIFEST.MF").openStream()) {
            return new Manifest(input);
        } catch (Exception e) {
            throw new RuntimeException("Loading MANIFEST for class " + clz + " failed!", e);
        }
    }

    /** @return dynamically built AboutModel of the geoserver's versions */
    public static AboutModel getVersions() {

        if (classLoader == null) {
            throw new IllegalArgumentException("Unable to run with null classLoader");
        }

        // start building the model
        AboutModel model = new AboutModel();
        try {
            // prepare the GeoServer metadata key
            String geoserverPath =
                    GeoServer.class
                            .getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI()
                            .toString();
            geoserverPath = geoserverPath + "!/META-INF/MANIFEST.MF";

            Class geoserver_class = GeoServer.class;
            Manifest manifest = ManifestLoader.getManifest(geoserver_class);
            if (manifest != null) {
                model.add(
                        ManifestModel.parseManifest(
                                "GeoServer",
                                manifest,
                                new ManifestModel.IncludeAttributeFilter(
                                        versionAttributeInclusions)));
            }

        } catch (Exception e) {
            // be safe
            LOGGER.log(Level.FINE, "Error looking up geoserver package", e);
        }

        try {
            // prepare the GeoTools metadata key
            String path =
                    GeoTools.class
                            .getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI()
                            .toString();
            path = path + "!/META-INF/MANIFEST.MF";

            Class geoserver_class = GeoTools.class;
            Manifest manifest = ManifestLoader.getManifest(geoserver_class);

            if (manifest != null) {
                model.add(
                        ManifestModel.parseManifest(
                                "GeoTools",
                                manifest,
                                new ManifestModel.IncludeAttributeFilter(
                                        versionAttributeInclusions)));
            }
            // ManifestModel manifest = new ManifestModel("GeoTools");
            // manifest.putEntry("Version", GeoTools.getVersion().toString());
            // manifest.putEntry("Git-Revision", GeoTools.getBuildRevision().toString());
            // manifest.putEntry("Build-Timestamp", GeoTools.getBuildTimestamp());
            // model.add(manifest);
        } catch (Exception e) {
            // be safe
            LOGGER.log(Level.FINE, "Error looking up geoserver package", e);
        }

        try {
            // prepare the GeoWebCache metadata key
            String path =
                    Class.forName("org.geowebcache.GeoWebCache")
                            .getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI()
                            .toString();
            path = path + "!/META-INF/MANIFEST.MF";

            Class geoserver_class = Class.forName("org.geowebcache.GeoWebCache");
            Manifest manifest = ManifestLoader.getManifest(geoserver_class);
            if (manifest != null) {
                model.add(
                        ManifestModel.parseManifest(
                                "GeoWebCache",
                                manifest,
                                new ManifestModel.IncludeAttributeFilter(
                                        versionAttributeInclusions)));
            }

            // Package p = GeoWebCache.class.getPackage();
            // if (p != null) {
            // ManifestModel manifest = new ManifestModel("GeoWebCache");
            // manifest.putEntry("Version",
            // p.getSpecificationVersion() != null ? p.getSpecificationVersion() : "");
            // manifest.putEntry("Git-Revision",
            // p.getImplementationVersion() != null ? p.getImplementationVersion() : "");
            // model.add(manifest);
            // }
        } catch (Exception e) {
            // be safe
            LOGGER.log(Level.FINE, "Error looking up org.geowebcache package", e);
        }
        return model;
    }

    /**
     * This is the model used to store resources from the class loader.
     *
     * @author Cancellieri Carlo - GeoSolutions SAS
     */
    public static class AboutModel {

        private TreeSet<ManifestModel> manifests;

        /**
         * Type for the Model:<br>
         * {@link AboutModelType#VERSIONS} - means this model contains versions<br>
         * {@link AboutModelType#RESOURCES} - means this model contains resources
         */
        public enum AboutModelType {
            VERSIONS,
            RESOURCES;
        }

        public AboutModel() {
            manifests = new TreeSet<ManifestModel>(new ManifestModel.ManifestComparator());
        }

        /** */
        public AboutModel(AboutModel am) throws IllegalArgumentException {
            if (am == null) {
                throw new IllegalArgumentException("Unable to initialize model with a null model");
            }
            manifests = new TreeSet<ManifestModel>(am.getManifests());
        }

        private AboutModel(NavigableSet<ManifestModel> manifests) throws IllegalArgumentException {
            if (manifests == null) {
                throw new IllegalArgumentException(
                        "Unable to initialize model with a null manifests tree");
            }
            this.manifests = new TreeSet<ManifestModel>(manifests);
        }

        /**
         * Filter resources from the used model generating a new one containing only resources
         * having the name between from and to string.<br>
         * Note that objects are shared between models so changes to objects in the filtered model
         * will also affect the current model.
         *
         * @return the filtered model
         * @throws IllegalArgumentException if from or to are null
         */
        public AboutModel filterNameByRange(String from, String to)
                throws IllegalArgumentException {
            if (from == null || to == null) {
                throw new IllegalArgumentException("Unable to parse from or to are null");
            }
            return new AboutModel(
                    getManifests()
                            .subSet(new ManifestModel(from), true, new ManifestModel(to), true));
        }

        /**
         * Filter resources from the used model generating a new one containing only resources
         * having the name matching the passed regular expression.<br>
         * Note that objects are shared between models so changes to objects in the filtered model
         * will also affect the current model.
         *
         * @param regex regular expression
         * @return a filtered model
         * @throws IllegalArgumentException if the regex is null
         */
        public AboutModel filterNameByRegex(String regex) throws IllegalArgumentException {
            if (regex == null) {
                throw new IllegalArgumentException("Unable to parse regex is null");
            }
            AboutModel am =
                    new AboutModel(
                            new TreeSet<ManifestModel>(new ManifestModel.ManifestComparator()));
            Iterator<ManifestModel> it = manifests.iterator();
            while (it.hasNext()) {
                ManifestModel tModel = it.next();
                // filter over properties
                if (LOGGER.isLoggable(Level.FINE)) LOGGER.fine(tModel.getName());
                if (tModel.getName().matches(regex)) {
                    am.getManifests().add(tModel);
                }
            }
            return am;
        }

        /**
         * Filter resources from the used model generating a new one containing only resources
         * having a key matching the passed string.<br>
         * Note that objects are shared between models so changes to objects in the filtered model
         * will also affect the current model.
         *
         * @param key the key to match
         * @return a filtered model
         * @throws IllegalArgumentException if the key is null
         */
        public AboutModel filterPropertyByKey(String key) throws IllegalArgumentException {
            if (key == null) {
                throw new IllegalArgumentException("Unable to parse key is null");
            }
            AboutModel am = new AboutModel();
            Iterator<ManifestModel> it = manifests.iterator();
            while (it.hasNext()) {
                ManifestModel tModel = it.next();
                if (filterPropertyByKey(tModel, key)) {
                    am.getManifests().add(tModel);
                }
            }
            return am;
        }

        /**
         * Filter resources from the used model generating a new one containing only resources
         * having a property matching the passed string.<br>
         * Note that objects are shared between models so changes to objects in the filtered model
         * will also affect the current model.
         *
         * @param value the value of the property
         * @return the filtered model
         * @throws IllegalArgumentException if the value is null
         */
        public AboutModel filterPropertyByValue(final String value)
                throws IllegalArgumentException {
            if (value == null) {
                throw new IllegalArgumentException("Unable to parse: value is null");
            }
            AboutModel am = new AboutModel();
            Iterator<ManifestModel> it = manifests.iterator();
            while (it.hasNext()) {
                ManifestModel tModel = it.next();
                if (filterByPropertyValue(tModel, value)) {
                    am.getManifests().add(tModel);
                }
            }
            return am;
        }

        /**
         * Filter resources from the used model generating a new one containing only resources
         * having a property key matching the passed key string with a value matching the passed
         * value string.<br>
         * Note that objects are shared between models so changes to objects in the filtered model
         * will also affect the current model.
         *
         * @param value the value of the property
         * @param key the name of the property
         * @return the filtered model
         * @throws IllegalArgumentException if the key or the value are null
         */
        public AboutModel filterPropertyByKeyValue(final String value, final String key)
                throws IllegalArgumentException {
            if (value == null && key == null) {
                throw new IllegalArgumentException("Unable to parse: property or key are null");
            }
            AboutModel am = new AboutModel();
            Iterator<ManifestModel> it = manifests.iterator();
            while (it.hasNext()) {
                ManifestModel tModel = it.next();
                if (filterPropertyByKeyValue(tModel, key, value)) {
                    am.getManifests().add(tModel);
                }
            }
            return am;
        }

        private boolean filterPropertyByKeyValue(
                final ManifestModel tModel, final String key, final String value) {
            // filter over properties
            for (Entry<String, String> e : tModel.getEntries().entrySet()) {
                if (e.getKey().matches(key) && e.getValue().matches(value)) {
                    // property mane matches
                    return true;
                }
            }
            return false;
        }

        private boolean filterPropertyByKey(final ManifestModel tModel, final String key) {
            // filter over properties
            for (Entry<String, String> e : tModel.getEntries().entrySet()) {
                if (e.getKey().matches(key)) {
                    // property mane matches
                    return true;
                }
            }
            return false;
        }

        private boolean filterByPropertyValue(final ManifestModel tModel, final String value) {
            // filter over values matches
            for (Entry<String, String> e : tModel.getEntries().entrySet()) {
                if (e.getValue().matches(value)) {
                    // property mane matches
                    return true;
                }
            }
            return false;
        }

        /**
         * Add a manifest file as resource with the given name
         *
         * @return true if this set did not already contain the specified name
         */
        public boolean add(final String name, final Manifest manifest) {
            return manifests.add(
                    ManifestModel.parseManifest(
                            name,
                            manifest,
                            new ManifestModel.ExcludeAttributeFilter(resourceAttributeExclusions)));
        }

        /**
         * Add a manifest file as resource
         *
         * @return true if this set did not already contain the specified name
         */
        public boolean add(final ManifestModel manifest) {
            return manifests.add(manifest);
        }

        /**
         * remove the resource named 'name'
         *
         * @param name (if null false is returned)
         * @return true if this set contained the specified element
         */
        public boolean remove(final String name) {
            if (name != null) {
                return manifests.remove(new ManifestModel(name));
            }
            return false;
        }

        public TreeSet<ManifestModel> getManifests() {
            return manifests;
        }

        /**
         * This is the model used to store one resource from the class loader.
         *
         * @author Cancellieri Carlo - GeoSolutions SAS
         */
        public static class ManifestModel {

            private final String name;

            private final Map<String, String> entries;

            /** A comparator useful to compare {@link ManifestModel}s by name */
            public static class ManifestComparator implements Comparator<ManifestModel> {

                @Override
                public int compare(ManifestModel o1, ManifestModel o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            }

            /** @return the name of the model */
            public String getName() {
                return name;
            }

            public Map<String, String> getEntries() {
                return entries;
            }

            public ManifestModel(final String name) {
                this.name = name;
                this.entries = new HashMap<String, String>();
            }

            public void putAllEntries(Map<String, String> entries) {
                this.entries.putAll(entries);
            }

            public void putEntry(final String name, final String value) {
                entries.put(name, value);
            }

            /**
             * A parser for {@link Manifest} bean which generates {@link ManifestModel}s
             *
             * @param name the name to assign to the generated model
             * @param manifest the manifest bean to load
             * @return the generated model
             */
            private static ManifestModel parseManifest(
                    final String name,
                    final Manifest manifest,
                    final AttributesFilter<Map<String, String>> filter) {

                final ManifestModel m = new ManifestModel(name);

                // Main attributes
                try {
                    m.putAllEntries(filter.filter(manifest.getMainAttributes()));
                } catch (Exception e1) {
                    LOGGER.log(Level.FINER, e1.getMessage(), e1);
                }

                //
                Map<String, Attributes> attrs = manifest.getEntries();
                for (java.util.Map.Entry<String, Attributes> entry : attrs.entrySet()) {
                    try {
                        m.putAllEntries(filter.filter(entry.getValue()));
                    } catch (Exception e) {
                        LOGGER.log(Level.FINER, e.getMessage(), e);
                    }
                }

                return m;
            }

            /**
             * Interface used to define Attributes filter in {@link
             * ManifestModel#parseManifest(String, Manifest, AttributesFilter)}
             *
             * @author cancellieri
             * @param <T> the type return for the filter function
             */
            public interface AttributesFilter<T> {
                T filter(final Attributes at) throws Exception;
            }

            /**
             * INTERSECTION: create a map of properties from an attributes including only those
             * matching the include array elements<br>
             * This implementation also supports attribute renaming using into the include array the
             * pattern:<br>
             * include= { "attrName1:replaceName1", "attrName2:replaceName2", ...}<br>
             */
            public static class IncludeAttributeFilter
                    implements AttributesFilter<Map<String, String>> {
                private final String[] include;

                public IncludeAttributeFilter(final String[] include) {
                    super();
                    this.include = include;
                }

                @Override
                public Map<String, String> filter(final Attributes at) throws Exception {
                    return filterIncludingAttributes(at, include);
                }

                /** @return a map of properties */
                private static Map<String, String> filterIncludingAttributes(
                        final Attributes at, String[] include) {
                    if (at == null) throw new IllegalArgumentException("Null argument");

                    Map<String, String> ret = new HashMap<String, String>();

                    if (include == null) {
                        if (LOGGER.isLoggable(Level.FINE))
                            LOGGER.log(Level.FINE, "No includes: including all");
                        final Iterator<java.util.Map.Entry<Object, Object>> it =
                                at.entrySet().iterator();
                        while (it.hasNext()) {
                            java.util.Map.Entry<Object, Object> entry = it.next();
                            String attrName = ((Attributes.Name) entry.getKey()).toString();
                            ret.put(attrName, entry.getValue().toString());
                        }
                    } else {
                        // for each attribute
                        final Iterator<java.util.Map.Entry<Object, Object>> it =
                                at.entrySet().iterator();
                        while (it.hasNext()) {
                            java.util.Map.Entry<Object, Object> entry = it.next();
                            String attrName = ((Attributes.Name) entry.getKey()).toString();

                            // search into including array to filter over attributes
                            int i = 0;
                            while (i < include.length) {
                                // split key in original_key:replace_key
                                String key[] = include[i++].split(":");
                                if (attrName.matches(key[0]) == true) {
                                    ret.put(
                                            key.length > 1 ? key[1] : key[0],
                                            entry.getValue().toString());
                                    break;
                                }
                            }
                        }
                    }
                    return ret;
                }
            }

            /**
             * COMPLEMENT: create a map of properties from an attributes excluding those matching
             * the exclude array elements
             */
            public static class ExcludeAttributeFilter
                    implements AttributesFilter<Map<String, String>> {

                private final String[] exclude;

                public ExcludeAttributeFilter(final String[] exclude) {
                    super();
                    this.exclude = exclude;
                }

                @Override
                public Map<String, String> filter(final Attributes at) throws Exception {
                    return filterExcludingAttributes(at, exclude);
                }

                /**
                 * @param at the attribute to parse
                 * @param exclude the list of properties to exlude
                 * @return a map
                 */
                private static Map<String, String> filterExcludingAttributes(
                        final Attributes at, String[] exclude) {
                    if (at == null) throw new IllegalArgumentException("Null arguments");
                    if (exclude == null) {
                        if (LOGGER.isLoggable(Level.FINE)) LOGGER.log(Level.FINE, "No exceptions");
                        exclude = new String[0];
                    }
                    Map<String, String> ret = new HashMap<String, String>();
                    // for each attribute
                    final Iterator<Object> it = at.keySet().iterator();
                    while (it.hasNext()) {
                        String attrName = ((Attributes.Name) it.next()).toString();
                        boolean skip = false;
                        // search into including array to filter over attributes
                        int i = 0;
                        while (i < exclude.length) {
                            if (attrName.matches(exclude[i++]) == true) {
                                skip = true;
                                break;
                            }
                        }
                        if (!skip) ret.put(attrName, at.getValue(attrName));
                    }
                    return ret;
                }
            }
        }
    }
}

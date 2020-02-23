/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform.resource;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Utility class for handling Resource paths in a consistent fashion.
 *
 * <p>This utility class is primarily aimed at implementations of ResourceStore and may be helpful
 * when writing test cases. These methods are suitable for static import.
 *
 * <p>Resource paths are consistent with file URLs. The base location is represented with "",
 * relative paths are not supported.
 *
 * @author Jody Garnett
 */
public class Paths {
    /** Path to base resource. */
    public static final String BASE = "";

    static String parent(String path) {
        if (path == null) {
            return null;
        }
        int last = path.lastIndexOf('/');
        if (last == -1) {
            if (BASE.equals(path)) {
                return null;
            } else {
                return BASE;
            }
        } else {
            return path.substring(0, last);
        }
    }

    static String name(String path) {
        if (path == null) {
            return null;
        }
        int last = path.lastIndexOf('/');
        if (last == -1) {
            return path; // top level resource
        } else {
            String item = path.substring(last + 1);
            return item;
        }
    }

    /** Used to quickly check path extension */
    static String extension(String path) {
        String name = name(path);
        if (name == null) {
            return null;
        }
        int last = name.lastIndexOf('.');
        if (last == -1) {
            return null; // no extension
        } else {
            return name.substring(last + 1);
        }
    }

    static String sidecar(String path, String extension) {
        if (extension == null) {
            return null;
        }
        int last = path.lastIndexOf('.');
        if (last == -1) {
            return path + "." + extension;
        } else {
            return path.substring(0, last) + "." + extension;
        }
    }

    /**
     * Path construction.
     *
     * @param path Items defining a Path
     * @return path Path used to identify a Resource
     */
    public static String path(String... path) {
        return path(STRICT_PATH, path);
    }

    /**
     * Path construction.
     *
     * @param strictPath whether problematic characters are an error
     * @param path Items defining a Path
     * @return path Path used to identify a Resource
     */
    static String path(boolean strictPath, String... path) {
        if (path == null || (path.length == 1 && path[0] == null)) {
            return null;
        }
        ArrayList<String> names = new ArrayList<String>();
        for (String item : path) {
            names.addAll(names(item));
        }
        return toPath(strictPath, names);
    }

    // runtime flag which, if true, throws an error for the WARN characters
    static final boolean STRICT_PATH = Boolean.valueOf(System.getProperty("STRICT_PATH", "false"));

    /**
     * Pattern used to check for invalid file characters.
     *
     * <ul>
     *   <li>backslash
     * </ul>
     */
    static final Pattern VALID = Pattern.compile("^[^\\\\]*$");
    /**
     * Pattern used to check for ill-advised file characters:
     *
     * <ul>
     *   <li>star
     *   <li>colon - potential conflict with xml prefix separator and workspace style separator
     *   <li>comma
     *   <li>single quote
     *   <li>ampersand
     *   <li>question mark
     *   <li>double quote
     *   <li>less than
     *   <li>greater than
     *   <li>bar
     * </ul>
     *
     * These characters can cause problems for different protocols.
     */
    static final Pattern WARN = Pattern.compile("^[^:*,\'&?\"<>|]*$");
    /** Set of invalid resource names (currently used to quickly identify relative paths). */
    static final Set<String> INVALID = new HashSet<String>(Arrays.asList(new String[] {"..", "."}));

    /**
     * Internal method used to convert a list of names to a normal Resource path.
     *
     * @param strictPath whether problematic characters are an error
     * @param names List of resource names forming a path
     * @return resource path composed of provided names
     * @throws IllegalArgumentException If names includes any {@link #INVALID} chracters
     */
    private static String toPath(boolean strictPath, List<String> names) {
        StringBuilder buf = new StringBuilder();
        final int LIMIT = names.size();
        for (int i = 0; i < LIMIT; i++) {
            String item = names.get(i);
            if (item == null) {
                continue; // skip null names
            }
            if (INVALID.contains(item)) {
                throw new IllegalArgumentException("Contains invalid " + item + " path: " + buf);
            }
            if (!VALID.matcher(item).matches()) {
                throw new IllegalArgumentException("Contains invalid " + item + " path: " + buf);
            }
            if (!WARN.matcher(item).matches()) {
                if (strictPath) {
                    throw new IllegalArgumentException(
                            "Contains invalid " + item + " path: " + buf);
                }
            }
            buf.append(item);
            if (i < LIMIT - 1) {
                buf.append("/");
            }
        }
        return buf.toString();
    }

    /**
     * Quick check of path for invalid characters
     *
     * @return path
     * @throws IllegalArgumentException If path fails {@link #VALID} check
     */
    public static String valid(String path) {
        return path(STRICT_PATH, path);
    }

    /**
     * Quick check of path for invalid characters
     *
     * @param strictPath whether problematic characters are an error
     * @return path
     * @throws IllegalArgumentException If path fails {@link #VALID} check
     */
    static String valid(boolean strictPath, String path) {
        if (path == null) {
            throw new NullPointerException("Resource path required");
        }
        if (path.contains("..") || ".".equals(path)) {
            throw new IllegalArgumentException("Relative paths not supported " + path);
        }
        if (!VALID.matcher(path).matches()) {
            throw new IllegalArgumentException("Contains invalid chracters " + path);
        }
        if (!WARN.matcher(path).matches()) {
            if (strictPath) {
                throw new IllegalArgumentException("Contains invalid chracters " + path);
            }
        }
        return path;
    }

    public static List<String> names(String path) {
        if (path == null || path.length() == 0) {
            return Collections.emptyList();
        }
        int index = 0;
        int split = path.indexOf('/');
        if (split == -1) {
            return Collections.singletonList(path);
        }
        ArrayList<String> names = new ArrayList<String>(3);
        String item;
        do {
            item = path.substring(index, split);
            if (item.length() != 0 && item != "/") {
                names.add(item);
            }
            index = split + 1;
            split = path.indexOf('/', index);
        } while (split != -1);
        item = path.substring(index);
        if (item != null && item.length() != 0 && !item.equals("/")) {
            names.add(item);
        }

        return names;
    }

    /**
     * Convert to file to resource path.
     *
     * @param base directory location
     * @param file relative file reference
     * @return relative path used for Resource lookup
     */
    public static String convert(File base, File file) {
        if (base == null) {
            if (file.isAbsolute()) {
                throw new IllegalArgumentException(
                        "Unable to determine relative path as file was absolute");
            } else {
                return convert(file.getPath());
            }
        }
        if (file == null) {
            return Paths.BASE;
        }
        URI baseURI = base.toURI();
        URI fileURI = file.toURI();

        if (fileURI.toString().startsWith(baseURI.toString())) {
            URI relativize = baseURI.relativize(fileURI);

            return relativize.getPath();
        } else {
            return convert(file.getPath());
        }
    }

    /**
     * Convert to file to resource path, allows for relative references (but is limited to content
     * within the provided base directory).
     *
     * @param base directory location
     * @param folder context for relative path (may be "." or null for base directory)
     * @param fileLocation File path (using {@link File#separator}) allowing for relative references
     * @return relative path used for Resource lookup
     */
    public static String convert(File base, File folder, String fileLocation) {
        if (base == null) {
            throw new NullPointerException("Base directory required for relative path");
        }
        List<String> folderPath = names(convert(base, folder));
        List<String> filePath = names(convert(fileLocation));

        List<String> resolvedPath = new ArrayList<String>(folderPath.size() + filePath.size());
        resolvedPath.addAll(folderPath);

        for (String item : filePath) {
            if (item == null) continue;
            if (item.equals(".")) continue;
            if (item.equals("..")) {
                if (!resolvedPath.isEmpty()) {
                    resolvedPath.remove(resolvedPath.size() - 1);
                    continue;
                } else {
                    throw new IllegalStateException(
                            "File location " + fileLocation + " outside of " + base.getPath());
                }
            }
            resolvedPath.add(item);
        }
        return toPath(STRICT_PATH, resolvedPath);
    }

    /**
     * Convert to file to resource path, allows for relative references (but is limited to content
     * within the provided base directory).
     *
     * @param base directory location
     * @param folder context for relative path (may be "." or null for base directory)
     * @param location File path (using {@link File#separator}) allowing for relative references
     * @return relative path used for Resource lookup
     */
    public static String convert(File base, File folder, String... location) {
        if (base == null) {
            throw new NullPointerException("Base directory required for relative path");
        }
        List<String> folderPath = names(convert(base, folder));
        List<String> filePath = Arrays.asList(location);

        List<String> resolvedPath = new ArrayList<String>(folderPath.size() + filePath.size());
        resolvedPath.addAll(folderPath);

        for (String item : filePath) {
            if (item == null) continue;
            if (item.equals(".")) continue;
            if (item.equals("..")) {
                if (!resolvedPath.isEmpty()) {
                    resolvedPath.remove(resolvedPath.size() - 1);
                    continue;
                } else {
                    throw new IllegalStateException(
                            "File location " + filePath + " outside of " + base.getPath());
                }
            }
            resolvedPath.add(item);
        }
        return toPath(STRICT_PATH, resolvedPath);
    }

    /**
     * Convert a filePath to resource path (supports absolute paths).
     *
     * <p>This method converts file paths (using {@link File#separator}) to the URL style paths used
     * for {@link ResourceStore#get(String)}.
     *
     * @param filePath File path using {@link File#separator}
     * @return Resource path suitable for use with {@link ResourceStore#get(String)} or null for
     *     absolute path
     */
    public static String convert(String filePath) {
        if (filePath == null) {
            return null;
        }
        if (filePath.length() == 0) {
            return filePath;
        }
        if (File.separatorChar == '/') {
            return filePath;
        } else {
            return filePath.replace(File.separatorChar, '/');
        }
    }

    /**
     * Convert a filePath to resource path (starting from the provided path). Absolute file paths
     * are not supported, and the final resource must still be within the data directory.
     *
     * <p>This method converts file paths (using {@link File#separator}) to the URL style paths used
     * for {@link ResourceStore#get(String)}.
     *
     * @param path Initial path used resolve relative reference lookup
     * @param filename File path (using {@link File#separator})
     * @return Resource path suitable for use with {@link ResourceStore#get(String)} or null for
     *     absolute path
     */
    public static String convert(String path, String filename) {
        if (path == null) {
            throw new NullPointerException("Initial path required to handle relative filenames");
        }
        List<String> folderPath = names(path);
        List<String> filePath = names(convert(filename));

        List<String> resolvedPath = new ArrayList<String>(folderPath.size() + filePath.size());
        resolvedPath.addAll(folderPath);

        for (String item : filePath) {
            if (item == null) continue;
            if (item.equals(".")) continue;
            if (item.equals("..")) {
                if (!resolvedPath.isEmpty()) {
                    resolvedPath.remove(resolvedPath.size() - 1);
                    continue;
                } else {
                    throw new IllegalStateException(
                            "File location " + filename + " outside of " + path);
                }
            }
            resolvedPath.add(item);
        }
        return toPath(STRICT_PATH, resolvedPath);
    }

    /**
     * Convert a Resource path to file reference for provided base directory.
     *
     * <p>This method requires the base directory of the ResourceStore. Note ResourceStore
     * implementations may not create the file until needed. In the case of an absolute path, base
     * should be null.
     *
     * @param base Base directory, often GeoServer Data Directory
     * @param path Resource path reference
     * @return File reference
     */
    public static File toFile(File base, String path) {
        for (String item : Paths.names(path)) {
            base = new File(base, item);
        }
        return base;
    }
}

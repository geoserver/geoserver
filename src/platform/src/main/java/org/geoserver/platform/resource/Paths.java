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
import java.util.stream.Collectors;
import org.apache.commons.lang3.SystemUtils;

/**
 * Utility class for handling Resource paths in a consistent fashion.
 *
 * <p>This utility class is primarily aimed at implementations of ResourceStore and may be helpful
 * when writing test cases. These methods are suitable for static import.
 *
 * <p>Resource paths are consistent with file URLs. The base location is represented with "",
 * relative paths are not supported.
 *
 * <p>Absolute paths are supported, with Linux systems using a leading {@code /}, and windows using
 * {@code L:}.
 *
 * @author Jody Garnett
 */
public class Paths {
    /** Path to base resource. */
    public static final String BASE = "";

    /**
     * Parent dir of this resource, or {@link #BASE} dir for top-level resources.
     *
     * <p>A special case is {@code null} as parent of {@link #BASE} dir to indicate no parent is
     * available.
     *
     * @param path resource path
     * @return path of parent dir, this {@link #BASE} for top-level resources, or {@code null} for
     *     {@link #BASE} dir.
     */
    public static String parent(String path) {
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

    /**
     * Name of resource.
     *
     * @param path resource path
     * @return name of resource
     */
    public static String name(String path) {
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

    /**
     * Path extension (in lower-case) or {@code null} if there is no extension.
     *
     * @param path resource path
     * @return resource extension (lowercase)
     */
    public static String extension(String path) {
        String name = name(path);
        if (name == null) {
            return null;
        }
        int last = name.lastIndexOf('.');
        if (last == -1) {
            return null; // no extension
        } else {
            return name.substring(last + 1).toLowerCase();
        }
    }

    /**
     * Path to sidecar, replacing resource extension with the provided sidecar extension.
     *
     * @param path resource path
     * @param sidecarExtension Sidecar extension (or {@code null} for no extension)
     * @return complete path to sidecar (result of changing extension)
     */
    public static String sidecar(String path, String sidecarExtension) {
        int last = path.lastIndexOf('.');

        if (sidecarExtension == null) {
            if (last == -1) {
                return path;
            } else {
                return path.substring(0, last);
            }
        }
        if (last == -1) {
            return path + "." + sidecarExtension;
        } else {
            return path.substring(0, last) + "." + sidecarExtension;
        }
    }

    /**
     * Path construction.
     *
     * @param path Items defining a Path
     * @return path Path used to identify a Resource
     */
    public static String path(String... path) {
        if (path == null || (path.length == 1 && path[0] == null)) {
            return null;
        }
        ArrayList<String> names = new ArrayList<>();
        for (String item : path) {
            names.addAll(names(item));
        }
        return toPath(names);
    }

    /**
     * Path construction.
     *
     * @param strictPath whether problematic characters are an error
     * @param path Items defining a Path
     * @return path Path used to identify a Resource
     * @deprecated Please use {@link #path(String...)} as strictPath no longer supported
     */
    public static String path(boolean strictPath, String... path) {
        return path(path);
    }

    /**
     * Pattern used to check for invalid file characters.
     *
     * <ul>
     *   <li>backslash
     * </ul>
     *
     * Paths agree with file URL representation of a relative file path, which uses forward slashes
     * as a path seperator.
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
    static final Set<String> INVALID = new HashSet<>(Arrays.asList(new String[] {"..", "."}));

    /**
     * Internal method used to convert a list of names to a normal Resource path.
     *
     * @param names List of resource names forming a path
     * @return resource path composed of provided names
     * @throws IllegalArgumentException If names includes any {@link #INVALID} chracters
     */
    static String toPath(List<String> names) {
        StringBuilder buf = new StringBuilder();
        final int LIMIT = names.size();
        for (int i = 0; i < LIMIT; i++) {
            String item = names.get(i);
            if (item == null) {
                continue; // skip null names
            }
            if (INVALID.contains(item)) {
                return reportInvalidPath(names, item);
            }
            if (!VALID.matcher(item).matches()) {
                return reportInvalidPath(names, item);
            }
            buf.append(item);
            if (i < LIMIT - 1 && !isAbsolute(item)) {
                buf.append("/");
            }
        }
        return buf.toString();
    }

    /**
     * Internal method to report an invalid path.
     *
     * @param names Names forming path
     * @param item Invalid item identified
     * @return path is not returned as exception thrown.
     * @throws IllegalArgumentException Indicating invalid item identified
     */
    private static String reportInvalidPath(List<String> names, String item) {
        throw new IllegalArgumentException(
                "Contains invalid '"
                        + item
                        + "' path: "
                        + names.stream().collect(Collectors.joining("/")));
    }

    /**
     * True if path is valid.
     *
     * <p>For details see {@link #valid(boolean, String)} which will provide an IllegalArgument
     * describing validation problem detected.
     *
     * @param path Resource path
     * @return True if path is valid
     */
    public static boolean isValid(String path) {
        if (path == null) {
            return false;
        } else if (path.isEmpty()) {
            return true; // Paths.BASE
        } else {
            for (String component : Paths.names(path)) {
                if (INVALID.contains(component)) {
                    return false;
                } else if (!VALID.matcher(component).matches()) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Quick in-line check of path for invalid characters (will throw exception if needed).
     *
     * @return path Resource path
     * @throws IllegalArgumentException If path fails {@link #VALID} check
     */
    public static String valid(String path) {
        if (path == null) {
            throw new NullPointerException("Resource path required");
        } else if (path.isEmpty()) {
            return Paths.BASE;
        } else {
            for (String component : Paths.names(path)) {
                if (INVALID.contains(component)) {
                    throw new IllegalArgumentException("Relative paths not supported " + path);
                } else if (!VALID.matcher(component).matches()) {
                    throw new IllegalArgumentException("Path contains invalid characters " + path);
                }
            }
            return path;
        }
    }

    /**
     * Path components listed into absolute prefix, directory names, and final file name or
     * directory name.
     *
     * <p><b>Relative</b>: Relative paths are represented in a straight forward fashion with: {@code
     * Paths.names("data/tasmania/roads.shp"} --> {"data","tasmania","roads.shp"}}.
     *
     * <p><b>Absolute path</b>: When working with an absolute path the list starts with a special
     * marker.
     *
     * <p>Linux absolute paths are start with leading slash character ({@code / } ). <br>
     * {@code convert("/srv/gis/cadaster/district.geopg") --> "/srv/gis/cadaster/district.geopg" <br>
     * {@code names("/srv/gis/cadaster/district.geopkg) --> {"/", "srv","gis", "cadaster",
     * "district.geopkg"}}. <br>
     * This agrees with URL representation of
     * {@code file:///srv/gis/cadaster/district.geopkg}.
     *
     * <p>Windows absolute drive letter and slash ( {@code C:\ } ). <br>
     * {@code names("D:\\gis\cadaster\district.geopkg") --> {"D:", "gis", "cadaster",
     * "district.geopkg"}}. This agrees with URL representation of
     * {@code file:///D:/gis/cadaster/district.geopkg}.
     *
     * @param path Path used for reference lookup
     * @return List of path components divided into absolute prefix, directory names, and final file
     *     name or directory name.
     */
    public static List<String> names(String path) {
        if (path == null || path.length() == 0) {
            return Collections.emptyList();
        }
        int index = 0;
        int split = path.indexOf('/');
        if (split == -1) {
            return Collections.singletonList(path);
        }
        ArrayList<String> names = new ArrayList<>(3);
        String item;

        if (isAbsolute(path)) {
            item = path.substring(0, split + 1);
        }
        do {
            if (index == 0 && isAbsolute(path)) {
                item = path.substring(0, split + 1);
            } else {
                item = path.substring(index, split);
            }
            // ignoring zero length items resulting from double slash
            // path breaks (occasionally produced when concatenating paths without due care).
            if (item.length() != 0) {
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

        List<String> resolvedPath = new ArrayList<>(folderPath.size() + filePath.size());
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
        return toPath(resolvedPath);
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

        List<String> resolvedPath = new ArrayList<>(folderPath.size() + filePath.size());
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
        return toPath(resolvedPath);
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
        String filePath = convert(filename);
        if (isAbsolute(filePath)) {
            throw new IllegalArgumentException(
                    "File location " + filename + " absolute, must be relative to " + path);
        }
        List<String> folderPathNames = names(path);
        List<String> filePathNames = names(filePath);

        List<String> resolvedPath = new ArrayList<>(folderPathNames.size() + filePathNames.size());
        resolvedPath.addAll(folderPathNames);

        for (String item : filePathNames) {
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
        return toPath(resolvedPath);
    }

    /**
     * Pattern used to recognize absolute path in Windows, as indicated by driver letter reference
     * (included {@code :}), followed by as slash character.
     *
     * <p>Aside: A drive letter reference on its own results in a relative path (relative to the
     * current directory for that drive).
     */
    static final Pattern WINDOWS_DRIVE_LETTER = Pattern.compile("^\\w\\:/.*$");

    /**
     * While paths are primarily intended as paths relative to the GeoServer data directory, there
     * is some support for absolute paths.
     *
     * <p><b>Linux</b>: Linux absolute paths start with a leading {@code /} character. As this slash
     * character is also used as the path separator special handling is required. Notably {@link
     * #names(String)} will represent an absolute path as: {@code { "/", "srv" "gis", "cadaster",
     * "district.geopkg"}}
     *
     * <p><b>Windows</b>: Windows absolute paths start with a drive letter, colon, and slash
     * characters.{@link #names(String)} will represent an absolute path on windows as: {@code {
     * "D:/, "gis", "cadaster", "district.geopkg"}}
     *
     * <p>Aside: A drive letter reference on its own results in a relative path (relative to the
     * current directory for that drive).
     *
     * <p><b>Guidance</b>: On both platforms an absolute path should agree with the file URL
     * representation while dropping the {@code file:/} prefix
     *
     * @param path Resource path reference
     * @return {@code true} if path forms an absolute reference to a location outside the data
     *     directory.
     */
    public static boolean isAbsolute(String path) {
        return isAbsolute(path, SystemUtils.IS_OS_WINDOWS);
    }

    // package visibility for test case coverage on all platforms
    static boolean isAbsolute(String path, boolean isWindows) {
        if (isWindows) return WINDOWS_DRIVE_LETTER.matcher(path).matches();
        else return path != null && path.startsWith("/");
    }

    /**
     * Convert a Resource path to file reference for provided base directory.
     *
     * <p>This method requires the base directory of the ResourceStore. Note ResourceStore
     * implementations may not create the file until needed.
     *
     * <p>In the case of an absolute path, base should be null. Both linux {@code /} and windows
     * {@code Z:/} absolute resource paths are supported.
     *
     * <p>Relative paths when base is {@code null}, are not supported.
     *
     * @param base Base directory, often GeoServer Data Directory
     * @param path Resource path reference
     * @return File reference (will be an absolute file reference)
     */
    public static File toFile(File base, String path) {
        if (isAbsolute(path)) {
            if (base != null) {
                // To be forgiving we will ignore duplicate slash between base and relative path
                if (path.startsWith("/")) {
                    path = path.substring(1);
                } else {
                    base = null;
                }
            }
        }
        for (String item : Paths.names(path)) {
            if (base == null && Paths.isAbsolute(item)) {
                base = root(item.replace('/', File.separatorChar));
            } else {
                base = new File(base, item);
            }
        }
        return base;
    }

    /**
     * Carefully look up a filesystem root directory (matching {@code /} or {@code C:\} as
     * appropriate).
     *
     * @param name
     * @return filesystem root directory matching name, or {@code null} if not found.
     */
    private static File root(String name) {
        for (File root : File.listRoots()) {
            if (root.getPath().equalsIgnoreCase(name)) {
                return root;
            }
        }
        return null;
    }
}

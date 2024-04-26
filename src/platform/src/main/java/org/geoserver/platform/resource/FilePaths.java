/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform.resource;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.commons.lang3.SystemUtils;

/**
 * Utility class for handling File paths in a consistent fashion.
 *
 * <p>File paths differ from resource paths because they refer to a file in the file system rather
 * than a resource in the resource store, and therefore they are not OS independent. Absolute paths
 * are supported, with Linux systems using a leading {@code /}, and windows using {@code L:}.
 *
 * @author Jody Garnett
 */
public class FilePaths {

    /**
     * Pattern used to recognize absolute path in Windows, as indicated by driver letter reference
     * (included {@code :}), followed by as slash character.
     *
     * <p>Aside: A drive letter reference on its own results in a relative path (relative to the
     * current directory for that drive).
     */
    static final Pattern WINDOWS_DRIVE_LETTER = Pattern.compile("^\\w\\:/.*$");

    /**
     * File Path components listed into absolute prefix, directory names, and final file name or
     * directory name.
     *
     * <p><b>Relative</b>: Relative paths are represented in a straight forward fashion with: {@code
     * Paths.names("data/tasmania/roads.shp"} --> {"data","tasmania","roads.shp"}}.
     *
     * <p><b>Absolute path</b>: When working with an absolute path the list starts with a special
     * marker.
     *
     * <p>Linux absolute paths are start with leading slash character ({@code / } ). <br>
     * {@code convert("/srv/gis/cadaster/district.geopkg") --> "/srv/gis/cadaster/district.geopkg" <br>
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
        if (path == null || path.isEmpty()) {
            return Collections.emptyList();
        }
        int index = 0;
        int split = path.indexOf('/');
        if (split == -1) {
            return Collections.singletonList(path);
        }
        ArrayList<String> names = new ArrayList<>(3);
        String item;

        do {
            if (index == 0 && isAbsolute(path)) {
                item = path.substring(0, split + 1);
            } else {
                item = path.substring(index, split);
            }
            // ignoring zero length items resulting from double slash
            // path breaks (occasionally produced when concatenating paths without due care).
            if (!item.isEmpty()) {
                names.add(item);
            }
            index = split + 1;
            split = path.indexOf('/', index);
        } while (split != -1);
        item = path.substring(index);
        if (item != null && !item.isEmpty() && !item.equals("/")) {
            names.add(item);
        }

        return names;
    }

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

    /**
     * Convert a file path to file reference for provided base directory.
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
        for (String item : names(path)) {
            if (base == null && isAbsolute(item)) {
                base = root(item.replace('/', File.separatorChar));
            } else {
                base = new File(base, item);
            }
        }
        return base;
    }
}

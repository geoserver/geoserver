/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket.browser;

import java.io.File;
import java.io.FileFilter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.security.FileAccessManager;

/**
 * Support class to locate the file system roots the file chooser uses to locate files, along with utility to match file
 * paths in said roots
 */
public class FileRootsFinder implements Serializable {

    /**
     * Utility so split and rebuild paths accounting for ResourceStore little own illusion of working on a *nix file
     * system regardless of the actual file system
     */
    class PathSplitter {

        String separator;
        boolean dataDirectoryPath;
        String base;
        String name;

        public PathSplitter(String input, boolean dataDirectoryPath) {
            // decide which separator to use based on data dir vs actual file system
            this.separator = dataDirectoryPath ? "/" : File.separator;
            this.dataDirectoryPath = dataDirectoryPath;

            // remove protocol part if needed (we have messy inputs stored that do not always start
            // with file:// but sometimes with file:/ and sometimes with file: (no / at all)
            if (input.startsWith("file:")) {
                if (input.startsWith("file:/")) {
                    if (input.startsWith("file://")) {
                        input = input.substring(7);
                    } else {
                        input = input.substring(6);
                    }
                } else {
                    input = input.substring(5);
                }
            }

            // split input into base and last segment
            int idx = input.lastIndexOf(separator);
            if (idx >= 0) {
                base = input.substring(0, idx);
                name = input.substring(idx + 1, input.length()).toLowerCase();
            } else {
                base = separator;
                name = input.toLowerCase();
            }

            // fix base in case of data dir
            if (dataDirectoryPath) {
                base = Paths.convert(base);
            }
        }

        private String buildPath(String name) {
            // Data dir relative path weirdness, the protocol has to be
            // file:/ instead of file:// or it won't work.
            String prefix = prefixPaths ? (dataDirectoryPath ? "file:" : "file://") : "";
            // make data dir relative paths actually relative despite user's input
            String localBase = base;
            if (dataDirectoryPath && localBase.startsWith(separator)) {
                localBase = base.substring(1);
            }
            if (localBase.endsWith(separator)) {
                return prefix + localBase + name;
            } else {
                return prefix + localBase + separator + name;
            }
        }
    }

    private ArrayList<File> roots;
    private File dataDirectory;
    private boolean prefixPaths = true;

    public FileRootsFinder(boolean includeDataDir) {
        this(GeoServerFileChooser.HIDE_FS, includeDataDir);
    }

    public FileRootsFinder(boolean hideFileSystem, boolean includeDataDir) {
        // set up for file access restrictions
        FileAccessManager fam = FileAccessManager.lookupFileAccessManager();

        //  get the roots from the restrictions manager
        List<File> famRoots = fam.getAvailableRoots();
        if (famRoots != null) {
            this.roots = new ArrayList<>(famRoots);
            // if restrictions are in place, we are done, sort and return
            Collections.sort(roots);
            return;
        }

        // if no restrictions are in place, build the roots from the file system
        roots = new ArrayList<>();
        if (!hideFileSystem) {
            roots.addAll(Arrays.asList(File.listRoots()));
        }

        Collections.sort(roots);

        // the data directory is always the first root, if it's not hidden
        GeoServerResourceLoader loader = getLoader();
        dataDirectory = loader.getBaseDirectory();

        if (includeDataDir) {
            roots.add(0, dataDirectory);
        }

        // add the home directory as well if it was possible to determine it at all
        if (!hideFileSystem && GeoServerFileChooser.USER_HOME != null) {
            roots.add(1, GeoServerFileChooser.USER_HOME);
        }
    }

    public boolean isPrefixPaths() {
        return prefixPaths;
    }

    public void setPrefixPaths(boolean prefixPaths) {
        this.prefixPaths = prefixPaths;
    }

    public ArrayList<File> getRoots() {
        return roots;
    }

    public GeoServerResourceLoader getLoader() {
        return GeoServerExtensions.bean(GeoServerResourceLoader.class);
    }

    public File getDataDirectory() {
        return dataDirectory;
    }

    /**
     * Support method for autocomplete text boxes, given a input and an optional file filter returns an a {@link Stream}
     * containing the actual paths matching the provided input (any file/directory starting with the same path as the
     * input and containing the file name in a case insensitive way)
     *
     * @param input A partial path, can be a single name or a full path (can be relative, will be24:14 matched against
     *     the data directory)
     * @param fileFilter An optional file filter to filter the returned files. The file filter should accept
     *     directories.
     */
    public Stream<String> getMatches(String input, FileFilter fileFilter) {
        // null safe, simplify code
        FileFilter ff = fileFilter == null ? f -> true : fileFilter;

        FileAccessManager fam = FileAccessManager.lookupFileAccessManager();
        List<File> famRoots = fam.getAvailableRoots();

        // if there are no sandbox restrictions, start by checking the data directory
        Stream<String> result;
        if (famRoots == null) {
            // check the data directory (which lives in its own *nix dream, so paths need
            // conversion)
            PathSplitter ddSplitter = new PathSplitter(input, true);
            GeoServerResourceLoader loader = getLoader();
            Resource resource = loader.get(ddSplitter.base);
            File dataDirectoryRoot = loader.get("/").dir();
            result = resource.list().stream()
                    .filter(r -> r.name().toLowerCase().contains(ddSplitter.name))
                    .filter(r -> ff.accept(new File(dataDirectoryRoot, r.path())))
                    .map(r -> ddSplitter.buildPath(r.name()));
        } else {
            result = Stream.empty();
        }

        // check all the roots as well
        PathSplitter fsSplitter = new PathSplitter(input, false);
        for (File root : getRoots()) {
            String pathInRoot = fsSplitter.base;
            if (pathInRoot.startsWith(root.getPath())) {
                pathInRoot = pathInRoot.substring(root.getPath().length());
                if (pathInRoot.startsWith(File.separator)) {
                    pathInRoot = pathInRoot.substring(1);
                }
            } else {
                continue;
            }

            File searchBase = new File(root, pathInRoot);
            String[] names =
                    searchBase.list((dir, fileName) -> fileName.toLowerCase().contains(fsSplitter.name));
            if (names != null) {
                Stream<String> rootPaths = Arrays.stream(names)
                        .filter(name -> ff.accept(new File(fsSplitter.base, name)))
                        .map(fileName -> fsSplitter.buildPath(fileName));
                result = Stream.concat(result, rootPaths);
            }
        }

        // the above won't work for roots that are full paths (e.g., sandboxing)
        // so we need to check the input against the roots themselves
        String prefix = prefixPaths ? "file://" : "";
        Stream<String> rootMatches = getRoots().stream()
                .filter(root -> root.getPath().contains(input))
                .map(r -> prefix + r.getPath());
        result = Stream.concat(result, rootMatches);

        return result.distinct().sorted();
    }
}

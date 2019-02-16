/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket.browser;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public class FileProvider extends SortableDataProvider<File, String> {

    private static final long serialVersionUID = 2387540012977156321L;

    public static final String NAME = "name";

    public static final String LAST_MODIFIED = "lastModified";

    public static final String SIZE = "size";

    /** Compares the file names, makes sure directories are listed first */
    private static final Comparator<File> FILE_NAME_COMPARATOR =
            new AbstractFileComparator() {
                @Override
                public int compareProperty(File o1, File o2) {
                    // otherwise compare the name
                    return o1.getName().compareToIgnoreCase(o2.getName());
                }
            };

    /** Compares last modified time */
    private static final Comparator<File> FILE_LM_COMPARATOR =
            new AbstractFileComparator() {
                @Override
                public int compareProperty(File o1, File o2) {
                    long lm1 = o1.lastModified();
                    long lm2 = o2.lastModified();
                    if (lm1 == lm2) {
                        return 0;
                    } else {
                        return lm1 < lm2 ? -1 : 1;
                    }
                }
            };

    /** Compares file size */
    private static final Comparator<File> FILE_SIZE_COMPARATOR =
            new AbstractFileComparator() {
                @Override
                public int compareProperty(File o1, File o2) {
                    long l1 = o1.length();
                    long l2 = o2.length();
                    if (l1 == l2) {
                        return 0;
                    } else {
                        return l1 < l2 ? -1 : 1;
                    }
                }
            };

    /** The current directory */
    IModel<File> directory;

    /** An eventual file filter */
    IModel<? extends FileFilter> fileFilter;

    public FileProvider(File directory) {
        this.directory = new Model<File>(directory);
    }

    public FileProvider(IModel<File> directory) {
        this.directory = directory;
    }

    @Override
    public Iterator<File> iterator(long first, long count) {
        List<File> files = getFilteredFiles();

        // sorting
        Comparator<File> comparator = getComparator(getSort());
        if (comparator != null) Collections.sort(files, comparator);

        // paging
        long last = first + count;
        if (last > files.size()) {
            last = files.size();
        }
        return files.subList((int) first, (int) last).iterator();
    }

    List<File> getFilteredFiles() {
        // grab the current directory
        File d = (File) directory.getObject();
        if (d.isFile()) d = d.getParentFile();

        // return a filtered view of the contents
        File[] files;
        if (fileFilter != null)
            files = d.listFiles(new HiddenFileFilter((FileFilter) fileFilter.getObject()));
        else files = d.listFiles(new HiddenFileFilter());

        if (files != null) return Arrays.asList(files);
        else return Collections.emptyList();
    }

    @Override
    public IModel<File> model(File object) {
        return new Model<File>(object);
    }

    @Override
    public long size() {
        return getFilteredFiles().size();
    }

    private Comparator<File> getComparator(SortParam<String> sort) {
        if (sort == null) return FILE_NAME_COMPARATOR;

        // build base comparator
        Comparator<File> comparator = null;
        if (NAME.equals(sort.getProperty())) {
            comparator = FILE_NAME_COMPARATOR;
        } else if (LAST_MODIFIED.equals(sort.getProperty())) {
            comparator = FILE_LM_COMPARATOR;
        } else if (SIZE.equals(sort.getProperty())) {
            comparator = FILE_SIZE_COMPARATOR;
        } else {
            throw new IllegalArgumentException("Uknown sorting property " + sort.getProperty());
        }

        // reverse comparison direction if needed
        if (sort.isAscending()) return comparator;
        else return new ReverseComparator(comparator);
    }

    public IModel<File> getDirectory() {
        return directory;
    }

    public void setDirectory(IModel<File> directory) {
        this.directory = directory;
    }

    public IModel<? extends FileFilter> getFileFilter() {
        return fileFilter;
    }

    public void setFileFilter(IModel<? extends FileFilter> fileFilter) {
        this.fileFilter = fileFilter;
    }

    /**
     * A base file comparator: makes sure directories go first, files later. The subclass is used to
     * perform comparison when both are files, or both directories
     */
    private abstract static class AbstractFileComparator implements Comparator<File> {

        @Override
        public final int compare(File o1, File o2) {
            // directories first
            if (o1.isDirectory()) if (!o2.isDirectory()) return -1;
            if (o2.isDirectory()) if (!o1.isDirectory()) return 1;

            return compareProperty(o1, o2);
        }

        protected abstract int compareProperty(File f1, File f2);
    }

    /** A simple comparator inverter */
    private static class ReverseComparator implements Comparator<File> {
        Comparator<File> comparator;

        public ReverseComparator(Comparator<File> comparator) {
            this.comparator = comparator;
        }

        @Override
        public int compare(File o1, File o2) {
            return comparator.compare(o2, o1);
        }
    }

    private static class HiddenFileFilter implements FileFilter {
        FileFilter delegate;

        public HiddenFileFilter() {
            // no delegate, just skip the hidden ones
        }

        public HiddenFileFilter(FileFilter delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean accept(File pathname) {
            if (pathname.isHidden()) {
                return false;
            }

            if (delegate != null) {
                return delegate.accept(pathname);
            } else {
                return true;
            }
        }
    }
}

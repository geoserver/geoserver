/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipOutputStream;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.vfs2.AllFileSelector;
import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.VFS;
import org.geoserver.importer.job.ProgressMonitor;
import org.geoserver.util.IOUtils;
import org.geotools.util.logging.Logging;

public class Directory extends FileData {

    private static final Logger LOGGER = Logging.getLogger(Directory.class);

    private static final long serialVersionUID = 1L;

    /** list of files contained in directory */
    protected List<FileData> files = new ArrayList<FileData>();

    /** flag controlling whether file look up should recurse into sub directories. */
    boolean recursive;

    String name;

    public Directory(File file) {
        this(file, true);
    }

    public Directory(File file, boolean recursive) {
        super(file);
        this.recursive = recursive;
    }

    public static Directory createNew(File parent) throws IOException {
        File directory = File.createTempFile("tmp", "", parent);
        if (!directory.delete() || !directory.mkdir())
            throw new IOException(
                    "Error creating temp directory at " + directory.getAbsolutePath());
        return new Directory(directory);
    }

    public static Directory createFromArchive(File archive) throws IOException {
        VFSWorker vfs = new VFSWorker();
        if (!vfs.canHandle(archive)) {
            throw new IOException(archive.getPath() + " is not a recognizable  format");
        }

        String basename = FilenameUtils.getBaseName(archive.getName());
        File dir = new File(archive.getParentFile(), basename);
        int i = 0;
        while (dir.exists()) {
            dir = new File(archive.getParentFile(), basename + i++);
        }
        vfs.extractTo(archive, dir);
        return new Directory(dir);
    }

    public File getFile() {
        return file;
    }

    public List<FileData> getFiles() {
        return files;
    }

    public void unpack(File file) throws IOException {
        // if the file is an archive, unpack it
        VFSWorker vfs = new VFSWorker();
        if (vfs.canHandle(file)) {
            LOGGER.fine(
                    "unpacking " + file.getAbsolutePath() + " to " + this.file.getAbsolutePath());
            vfs.extractTo(file, this.file);

            LOGGER.fine("deleting " + file.getAbsolutePath());
            if (!file.delete()) {
                throw new IOException("unable to delete file");
            }
        }
    }

    public File child(String name) {
        if (name == null) {
            // create random
            try {
                return File.createTempFile("child", "tmp", file);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return new File(this.file, name);
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name != null ? this.name : file.getName();
    }

    @Override
    public void prepare(ProgressMonitor m) throws IOException {
        files = new ArrayList<FileData>();

        // recursively search for spatial files, maintain a queue of directories to recurse into
        LinkedList<File> q = new LinkedList<File>();
        q.add(file);

        while (!q.isEmpty()) {
            File dir = q.poll();

            if (m.isCanceled()) {
                return;
            }
            m.setTask("Scanning " + dir.getPath());

            // get all the regular (non directory) files
            File[] fileList =
                    dir.listFiles(
                            new FilenameFilter() {
                                public boolean accept(File dir, String name) {
                                    return !new File(dir, name).isDirectory();
                                }
                            });
            if (fileList == null) {
                // it can be null in case of I/O error, even if the
                // dir is indeed a directory
                continue;
            }
            Set<File> all = new LinkedHashSet<File>(Arrays.asList(fileList));

            // scan all the files looking for spatial ones
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isHidden()) {
                        all.remove(f);
                        continue;
                    }
                    if (f.isDirectory()) {
                        if (!recursive && !f.equals(file)) {
                            // skip it
                            continue;
                        }
                        // @hacky - ignore __MACOSX
                        // this could probably be dealt with in a better way elsewhere
                        // like by having Directory ignore the contents since they
                        // are all hidden files anyway
                        if (!"__MACOSX".equals(f.getName())) {
                            Directory d = new Directory(f);
                            d.prepare(m);

                            this.files.add(d);
                        }
                        // q.push(f);
                        continue;
                    }

                    // special case for .aux files, they are metadata but get picked up as readable
                    // by the erdas imagine reader...just ignore them for now
                    if ("aux".equalsIgnoreCase(FilenameUtils.getExtension(f.getName()))) {
                        continue;
                    }

                    // determine if this is a spatial format or not
                    DataFormat format = DataFormat.lookup(f);

                    if (format != null) {
                        SpatialFile sf = newSpatialFile(f, format);

                        // gather up the related files
                        sf.prepare(m);

                        this.files.add(sf);

                        all.removeAll(sf.allFiles());
                    }
                }
            }

            // take any left overs and add them as unspatial/unrecognized
            for (File f : all) {
                this.files.add(new ASpatialFile(f));
            }
        }

        format = format();
        //        //process ignored for files that should be grouped with the spatial files
        //        for (DataFile df : files) {
        //            SpatialFile sf = (SpatialFile) df;
        //            String base = FilenameUtils.getBaseName(sf.getFile().getName());
        //            for (Iterator<File> i = ignored.iterator(); i.hasNext(); ) {
        //                File f = i.next();
        //                if (base.equals(FilenameUtils.getBaseName(f.getName()))) {
        //                    //.prj file?
        //                    if ("prj".equalsIgnoreCase(FilenameUtils.getExtension(f.getName()))) {
        //                        sf.setPrjFile(f);
        //                    }
        //                    else {
        //                        sf.getSuppFiles().add(f);
        //                    }
        //                    i.remove();
        //                }
        //            }
        //        }
        //
        //        //take any left overs and add them as unspatial/unrecognized
        //        for (File f : ignored) {
        //            files.add(new ASpatialFile(f));
        //        }
        //
        //        return files;
        //
        //        for (DataFile f : files()) {
        //            f.prepare();
        //        }
    }

    /**
     * Creates a new spatial file.
     *
     * @param f The raw file.
     * @param format The spatial format of the file.
     */
    protected SpatialFile newSpatialFile(File f, DataFormat format) {
        SpatialFile sf = new SpatialFile(f);
        sf.setFormat(format);
        return sf;
    }

    public List<Directory> flatten() {
        List<Directory> flat = new ArrayList<Directory>();

        LinkedList<Directory> q = new LinkedList<Directory>();
        q.addLast(this);
        while (!q.isEmpty()) {
            Directory dir = q.removeFirst();
            flat.add(dir);

            for (Iterator<FileData> it = dir.getFiles().iterator(); it.hasNext(); ) {
                FileData f = it.next();
                if (f instanceof Directory) {
                    Directory d = (Directory) f;
                    it.remove();
                    q.addLast(d);
                }
            }
        }

        return flat;
    }

    //    public List<DataFile> files() throws IOException {
    //        LinkedList<DataFile> files = new LinkedList<DataFile>();
    //
    //        LinkedList<File> ignored = new LinkedList<File>();
    //
    //        LinkedList<File> q = new LinkedList<File>();
    //        q.add(file);
    //
    //        while(!q.isEmpty()) {
    //            File f = q.poll();
    //
    //            if (f.isDirectory()) {
    //                q.addAll(Arrays.asList(f.listFiles()));
    //                continue;
    //            }
    //
    //            //determine if this is a spatial format or not
    //            DataFormat format = DataFormat.lookup(f);
    //
    //            if (format != null) {
    //                SpatialFile file = new SpatialFile(f);
    //                file.setFormat(format);
    //                files.add(file);
    //            }
    //            else {
    //                ignored.add(f);
    //            }
    //        }
    //
    //        //process ignored for files that should be grouped with the spatial files
    //        for (DataFile df : files) {
    //            SpatialFile sf = (SpatialFile) df;
    //            String base = FilenameUtils.getBaseName(sf.getFile().getName());
    //            for (Iterator<File> i = ignored.iterator(); i.hasNext(); ) {
    //                File f = i.next();
    //                if (base.equals(FilenameUtils.getBaseName(f.getName()))) {
    //                    //.prj file?
    //                    if ("prj".equalsIgnoreCase(FilenameUtils.getExtension(f.getName()))) {
    //                        sf.setPrjFile(f);
    //                    }
    //                    else {
    //                        sf.getSuppFiles().add(f);
    //                    }
    //                    i.remove();
    //                }
    //            }
    //        }
    //
    //        //take any left overs and add them as unspatial/unrecognized
    //        for (File f : ignored) {
    //            files.add(new ASpatialFile(f));
    //        }
    //
    //        return files;
    //    }

    /**
     * Returns the data format of the files in the directory iff all the files are of the same
     * format, if they are not this returns null.
     */
    public DataFormat format() throws IOException {
        if (files.isEmpty()) {
            LOGGER.warning("no files recognized");
            return null;
        }

        FileData file = files.get(0);
        DataFormat format = file.getFormat();
        for (int i = 1; i < files.size(); i++) {
            FileData other = files.get(i);
            if (format != null && !format.equals(other.getFormat())) {
                logFormatMismatch();
                return null;
            }
            if (format == null && other.getFormat() != null) {
                logFormatMismatch();
                return null;
            }
        }

        return format;
    }

    private void logFormatMismatch() {
        StringBuilder buf = new StringBuilder("all files are not the same format:\n");
        for (int i = 0; i < files.size(); i++) {
            FileData f = files.get(i);
            String format = "not recognized";
            if (f.getFormat() != null) {
                format = f.getName();
            }
            buf.append(f.getFile().getName()).append(" : ").append(format).append('\n');
        }
        LOGGER.warning(buf.toString());
    }

    /** @throws IOException */
    protected void lockDirectory() throws IOException {
        File locking = new File(this.getFile(), ".locking");
        if (!locking.exists()) {
            locking.createNewFile();
        }
    }

    public Directory filter(List<FileData> files) {
        Filtered f = new Filtered(file, files);
        f.setFormat(getFormat());
        return f;
    }

    @Override
    public String toString() {
        return file.getPath();
    }

    public void accept(FileObject fo) throws IOException {
        FileName name = fo.getName();
        String localName = name.getBaseName();
        File dest = child(localName);
        FileObject dfo = null;
        try {
            dfo = VFS.getManager().resolveFile(dest.getAbsolutePath());
            dfo.copyFrom(fo, new AllFileSelector());

            unpack(dest);
        } finally {
            if (dfo != null) {
                dfo.close();
            }
        }
    }

    public void accept(String childName, InputStream in) throws IOException {
        // lock folder before it gets removed from JobQueue cleaner
        lockDirectory();

        File dest = child(childName);

        IOUtils.copy(in, dest);

        try {
            unpack(dest);
        } catch (IOException ioe) {
            // problably should delete on error
            LOGGER.warning("Possible invalid file uploaded to " + dest.getAbsolutePath());
            throw ioe;
        }
    }

    public void accept(FileItem item) throws Exception {
        // lock folder before it gets removed from JobQueue cleaner
        lockDirectory();

        File dest = child(item.getName());
        item.write(dest);

        try {
            unpack(dest);
        } catch (IOException e) {
            // problably should delete on error
            LOGGER.warning("Possible invalid file uploaded to " + dest.getAbsolutePath());
            throw e;
        }
    }

    public void archive(File output) throws IOException {
        File archiveDir = output.getAbsoluteFile().getParentFile();
        String outputName = output.getName().replace(".zip", "");
        int id = 0;
        while (output.exists()) {
            output = new File(archiveDir, outputName + id + ".zip");
            id++;
        }
        ZipOutputStream zout =
                new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(output)));

        // don't call zout.close in finally block, if an error occurs and the zip
        // file is empty by chance, the second error will mask the first
        try {
            IOUtils.zipDirectory(file, zout, null);
        } catch (Exception ex) {
            try {
                zout.close();
            } catch (Exception ex2) {
                // nothing, we're totally aborting
            }
            output.delete();
            if (ex instanceof IOException) throw (IOException) ex;
            throw (IOException) new IOException("Error archiving").initCause(ex);
        }

        // if we get here, the zip is properly written
        try {
            zout.close();
        } finally {
            cleanup();
        }
    }

    @Override
    public void cleanup() throws IOException {
        File[] files = file.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    new Directory(f).cleanup();
                } else {
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.fine("Deleting file " + f.getAbsolutePath());
                    }
                    if (!f.delete()) {
                        throw new IOException("unable to delete " + f);
                    }
                }
            }
        }
        super.cleanup();
    }

    @Override
    public FileData part(final String name) {
        List<FileData> files = this.files;
        if (this instanceof Filtered) {
            files = ((Filtered) this).filter;
        }

        try {
            return Iterables.find(
                    files,
                    new Predicate<FileData>() {
                        @Override
                        public boolean apply(FileData input) {
                            return name.equals(input.getName());
                        }
                    });
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    static class Filtered extends Directory {

        List<FileData> filter;

        public Filtered(File file, List<FileData> filter) {
            super(file);
            this.filter = filter;
        }

        @Override
        public void prepare(ProgressMonitor m) throws IOException {
            super.prepare(m);

            files.retainAll(filter);
            format = format();
        }
    }
}

/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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
import org.geoserver.data.util.IOUtils;
import org.geoserver.importer.job.ProgressMonitor;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.Resources;
import org.geoserver.util.Filter;
import org.geotools.util.logging.Logging;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

public class Directory extends FileData {

    private static final Logger LOGGER = Logging.getLogger(Directory.class);
    
    private static final long serialVersionUID = 1L;

    /**
     * list of files contained in directory
     */
    protected List<FileData> files = new ArrayList<FileData>();

    /**
     * flag controlling whether file look up should recurse into sub directories.
     */
    boolean recursive;
    String name;

    public Directory(Resource file) {
        this(file, true);
    }

    public Directory(Resource file, boolean recursive) {
        super(file);
        this.recursive = recursive;
    }

    @Deprecated
    public Directory(File dir) {
        this(Files.asResource(dir));
    }

    public static Directory createNew(Resource parent) throws IOException {
        Resource directory = Resources.createRandom("tmp", "", parent);
        return new Directory(directory);
    }

    public static Directory createFromArchive(Resource archive) throws IOException {
        VFSWorker vfs = new VFSWorker();
        if (!vfs.canHandle(archive)) {
            throw new IOException(archive.path() + " is not a recognizable  format");
        }

        String basename = FilenameUtils.getBaseName(archive.name());
        Resource dir = archive.parent().get(basename);
        int i = 0;
        while (Resources.exists(dir)) {
            dir = archive.parent().get(basename + i++);
        }
        vfs.extractTo(archive, dir);
        return new Directory(dir);
    }

    public Resource getFile() {
        return file;
    }

    public List<FileData> getFiles() {
        return files;
    }

    public void unpack(Resource file) throws IOException {
        //if the file is an archive, unpack it
        VFSWorker vfs = new VFSWorker();
        if (vfs.canHandle(file)) {
            LOGGER.fine("unpacking " + file.path() + " to " + this.file.path());
            vfs.extractTo(file, this.file);

            LOGGER.fine("deleting " + file.path());
            if (!file.delete()) {
                throw new IOException("unable to delete file");
            }
        }
    }
    
    public Resource child(String name) {
        if (name == null) {
            //create random
            try {
                return Resources.createRandom("child", "tmp", file);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return file.get(name);
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name != null ? this.name : file.name();
    }

    @Override
    public void prepare(ProgressMonitor m) throws IOException {
        files = new ArrayList<FileData>();

        //recursively search for spatial files, maintain a queue of directories to recurse into
        LinkedList<Resource> q = new LinkedList<Resource>();
        q.add(file);

        while(!q.isEmpty()) {
            Resource dir = q.poll();

            if (m.isCanceled()) {
                return;
            }
            m.setTask("Scanning " + dir.path());

            //get all the regular (non directory) files
            List<Resource> fileList = Resources.list(dir, new Filter<Resource>() {
                @Override
                public boolean accept(Resource obj) {
                    return obj.getType() != Type.DIRECTORY;
                }
            });
            Set<Resource> all = new LinkedHashSet<Resource>(fileList);

            //scan all the files looking for spatial ones
            for (Resource f : dir.list()) {
                if (Resources.isHidden(f)) {
                    all.remove(f);
                    continue;
                }
                if (f.getType() == Type.DIRECTORY) {
                    if (!recursive && !f.equals(file)) {
                        //skip it
                        continue;
                    }
                    // @hacky - ignore __MACOSX
                    // this could probably be dealt with in a better way elsewhere
                    // like by having Directory ignore the contents since they
                    // are all hidden files anyway
                    if (!"__MACOSX".equals(f.name())) {
                        Directory d = new Directory(f);
                        d.prepare(m);

                        files.add(d);
                    }
                    //q.push(f);
                    continue;
                }

                //special case for .aux files, they are metadata but get picked up as readable 
                // by the erdas imagine reader...just ignore them for now 
                if ("aux".equalsIgnoreCase(FilenameUtils.getExtension(f.name()))) {
                    continue;
                }

                //determine if this is a spatial format or not
                DataFormat format = DataFormat.lookup(f);

                if (format != null) {
                    SpatialFile sf = newSpatialFile(f, format);
                    
                    //gather up the related files
                    sf.prepare(m);

                    files.add(sf);

                    all.removeAll(sf.allFiles());
                }
            }

            //take any left overs and add them as unspatial/unrecognized
            for (Resource f : all) {
                files.add(new ASpatialFile(f));
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
    protected SpatialFile newSpatialFile(Resource f, DataFormat format) {
        SpatialFile sf = new SpatialFile(f);
        sf.setFormat(format);
        return sf;
    }

    public List<Directory> flatten() {
        List<Directory> flat = new ArrayList<Directory>();

        LinkedList<Directory> q = new LinkedList<Directory>();
        q.addLast(this);
        while(!q.isEmpty()) {
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
            buf.append(f.getFile().name()).append(" : ").append(format).append('\n');
        }
        LOGGER.warning(buf.toString());
    }

    public Directory filter(List<FileData> files) {
        Filtered f = new Filtered(file, files);
        f.setFormat(getFormat());
        return f;
    }

    @Override
    public String toString() {
        return file.path();
    }

    public void accept(FileObject fo) throws IOException {
        FileName name = fo.getName();
        String localName = name.getBaseName();
        Resource dest = child(localName);
        FileObject dfo = null;
        try {
            dfo = VFS.getManager().resolveFile(dest.file().getAbsolutePath());
            dfo.copyFrom(fo, new AllFileSelector());

            unpack(dest);
        } finally {
            if (dfo != null) {
                dfo.close();
            }
        }
    }

    public void accept(String childName, InputStream in) throws IOException {
        Resource dest = child(childName);
        
        IOUtils.copy(in, dest.out());

        try {
            unpack(dest);
        } catch (IOException ioe) {
            // problably should delete on error
            LOGGER.warning("Possible invalid file uploaded to " + dest.path());
            throw ioe;
        }
    }

    public void accept(FileItem item) throws Exception {
        Resource dest = child(item.getName());
        item.write(dest.file());

        try {
            unpack(dest);
        } 
        catch (IOException e) {
            // problably should delete on error
            LOGGER.warning("Possible invalid file uploaded to " + dest.path());
            throw e;
        }
    }
    
    public void archive(Resource output) throws IOException {
        Resource archiveDir = output.parent();
        String outputName = output.name().replace(".zip","");
        int id = 0;
        while (Resources.exists(output)) {
            output = archiveDir.get(outputName + id + ".zip");
            id++;
        }
        ZipOutputStream zout = new ZipOutputStream(new BufferedOutputStream(output.out()));
        Exception error = null;

        // don't call zout.close in finally block, if an error occurs and the zip
        // file is empty by chance, the second error will mask the first
        try {
            IOUtils.zipDirectory(file.dir(), zout, null);
        } catch (Exception ex) {
            error = ex;
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
        for (Resource f : file.list()) {
            if (f.getType() == Type.DIRECTORY) {
                new Directory(f).cleanup();
            } else {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine("Deleting file " + f.path());
                }
                if (!f.delete()) {
                    throw new IOException("unable to delete " + f);
                }
            }
        }
        super.cleanup();
    }

    @Override
    public FileData part(final String name) {
        List<FileData> files = this.files;
        if (this instanceof Filtered) {
            files = ((Filtered)this).filter;
        }

        try {
            return Iterables.find(files, new Predicate<FileData>() {
                @Override
                public boolean apply(FileData input) {
                    return name.equals(input.getName());
                }
            });
        }
        catch(NoSuchElementException e) {
            return null;
        }
    }

    static class Filtered extends Directory {

        List<FileData> filter;

        public Filtered(Resource file, List<FileData> filter) {
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

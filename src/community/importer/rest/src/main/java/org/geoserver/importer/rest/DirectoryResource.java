/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.rest;

import static org.restlet.data.MediaType.APPLICATION_GNU_TAR;
import static org.restlet.data.MediaType.APPLICATION_GNU_ZIP;
import static org.restlet.data.MediaType.APPLICATION_TAR;
import static org.restlet.data.MediaType.APPLICATION_ZIP;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import net.sf.json.JSONObject;

import org.geoserver.rest.RestletException;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.rest.format.StreamDataFormat;
import org.geoserver.importer.Directory;
import org.geoserver.importer.FileData;
import org.geoserver.importer.ImportContext;
import org.geoserver.importer.Importer;
import org.geoserver.importer.SpatialFile;
import org.geoserver.importer.mosaic.Granule;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;

public class DirectoryResource extends BaseResource {

    static Map<MediaType,String> ARCHIVE_MIME_TYPES = new HashMap<MediaType,String>();
    {
        ARCHIVE_MIME_TYPES.put(APPLICATION_ZIP, "zip");
        ARCHIVE_MIME_TYPES.put(APPLICATION_GNU_ZIP, "gz");
        ARCHIVE_MIME_TYPES.put(APPLICATION_TAR, ".tar");
    
        //GNU_TAR doesn't mean gzipped tarball but we'll hack for now
        ARCHIVE_MIME_TYPES.put(APPLICATION_GNU_TAR, ".tar.gz");
    }

    public DirectoryResource(Importer importer) {
        super(importer);
    }

    @Override
    protected List<DataFormat> createSupportedFormats(Request request, Response response) {
        return (List) Arrays.asList(new DirectoryJSONFormat(MediaType.APPLICATION_JSON),
                new DirectoryJSONFormat(MediaType.TEXT_HTML));
    }

    @Override
    public void handleGet() {
        Directory dir = lookupDirectory();
        Object response = dir;

        if (getAttribute("file") != null) {
            response = lookupFile(dir);
        }

        DataFormat formatGet = getFormatGet();
        if (formatGet == null) {
            formatGet = new DirectoryJSONFormat(MediaType.APPLICATION_JSON);
        }

        getResponse().setEntity(formatGet.toRepresentation(response));
    }

    @Override
    public boolean allowPost() {
        return getAttribute("file") == null;
    }

    @Override
    public void handlePost() {
        MediaType mimeType = getRequest().getEntity().getMediaType();
        if (ARCHIVE_MIME_TYPES.containsKey(mimeType)) {
            try {
                handleFileUpload(mimeType);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        else {
            throw new RestletException("Unsupported POST", Status.CLIENT_ERROR_FORBIDDEN);
        }
    }

    void handleFileUpload(MediaType mimeType) throws IOException {
        File tmp = File.createTempFile("upload", "tmp");
        tmp.delete();

        String ext = ARCHIVE_MIME_TYPES.get(mimeType);
        
        Directory dir = lookupDirectory();
        dir.accept(tmp.getName() + "." + ext, getRequest().getEntity().getStream());
        dir.prepare();
    }

    @Override
    public boolean allowPut() {
        return getAttribute("file") != null;
    }

    @Override
    public void handlePut() {
        //TODO: this only handles granule timestamps at the moment, expand to handle more
        JSONObject obj = (JSONObject) getFormatPostOrPut().toObject(getRequest().getEntity());
        FileData file = lookupFile();
        if (file instanceof SpatialFile) {
            SpatialFile sf = (SpatialFile) file;
            if (sf instanceof Granule) {
                Granule g = (Granule) sf;
                if (obj.has("timestamp")) {
                    String ts = obj.getString("timestamp");
                    try {
                        g.setTimestamp(ImportJSONWriter.DATE_FORMAT.parse(ts));
                    } catch (ParseException e) {
                        throw new RestletException("Could not parse timestamp: " + ts + ", must be " 
                            + "format: " + ImportJSONWriter.DATE_FORMAT.toPattern(), 
                            Status.CLIENT_ERROR_BAD_REQUEST);
                    }

                    getResponse().setEntity(getFormatGet().toRepresentation(g));
                }
            }
        }

        getResponse().setStatus(Status.SUCCESS_OK);
    }

    public boolean allowDelete() {
        return allowPut();
    }

    public void handleDelete() {
        Directory dir = lookupDirectory();
        FileData file = lookupFile();

        if (dir.getFiles().remove(file)) {
            getResponse().setStatus(Status.SUCCESS_NO_CONTENT);
        }
        else {
            throw new RestletException("Unable to remove file: " + file.getName(), 
                Status.CLIENT_ERROR_BAD_REQUEST);
        }

    };

    Directory lookupDirectory() {
        ImportContext context = context();
        if (!(context.getData() instanceof Directory)) {
            throw new RestletException("Data is not a directory", Status.CLIENT_ERROR_BAD_REQUEST);    
        }

        return (Directory) context.getData();
    }

    FileData lookupFile() {
        return lookupFile(lookupDirectory());
    }

    FileData lookupFile(Directory dir ) {
        final String file = getAttribute("file");
        try {
            if (file != null) {
                return Iterators.find(dir.getFiles().iterator(), new Predicate<FileData>() {
                    @Override
                    public boolean apply(FileData input) {
                        return input.getFile().getName().equals(file);
                    }
                });
            }
        }
        catch(NoSuchElementException e) {
            
        }
        throw new RestletException("No such file: " + file, Status.CLIENT_ERROR_NOT_FOUND);
    }

    class DirectoryJSONFormat extends StreamDataFormat {

        protected DirectoryJSONFormat(MediaType type) {
            super(type);
        }

        @Override
        protected Object read(InputStream in) throws IOException {
            return newReader(in).object();
        }

        @Override
        protected void write(Object object, OutputStream out) throws IOException {
            ImportJSONWriter io = newWriter(out);

            Object parent = task(true);
            if (parent == null) {
                parent = context();
            }

            if (object instanceof Directory) {
                Directory d = (Directory) object;
                String path = getRequest().getResourceRef().getPath();
                if (path.matches(".*/files/?")) {
                    io.files(d, parent, true, expand(1));
                }
                else {
                    io.directory(d, parent, expand(1));
                }
            }
            else {
                io.file((FileData)object, parent, expand(1), true);
            }
        }
    }
}

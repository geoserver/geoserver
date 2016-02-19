/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.rest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import net.sf.json.JSONException;
import net.sf.json.JSONObject;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.geoserver.platform.resource.Resources;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.rest.format.StreamDataFormat;
import org.geoserver.importer.Directory;
import org.geoserver.importer.FileData;
import org.geoserver.importer.ImportContext;
import org.geoserver.importer.ImportData;
import org.geoserver.importer.ImportTask;
import org.geoserver.importer.Importer;
import org.geoserver.importer.ValidationException;
import org.geoserver.importer.transform.TransformChain;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.ext.fileupload.RestletFileUpload;
import org.restlet.resource.Representation;

/**
 * REST resource for /imports/<import>/tasks[/<id>]
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class TaskResource extends BaseResource {

    public TaskResource(Importer importer) {
        super(importer);
    }

    @Override
    protected List<DataFormat> createSupportedFormats(Request request, Response response) {
        return (List) Arrays.asList(new ImportTaskJSONFormat(MediaType.APPLICATION_JSON),
                new ImportTaskJSONFormat(MediaType.TEXT_HTML));
    }

    @Override
    public void handleGet() {
        if (getRequest().getResourceRef().getLastSegment().equals("progress")) {
            getResponse().setEntity(createProgressRepresentation());
        }
        else {
            Object obj = lookupTask(true);
            if (obj instanceof ImportTask) {
                getResponse().setEntity(getFormatGet().toRepresentation((ImportTask)obj));
            }
            else {
                getResponse().setEntity(getFormatGet().toRepresentation((List<ImportTask>)obj));
            }
        }
    }

    public boolean allowPost() {
        return getAttribute("task") == null;
    }

    public void handlePost() {
        ImportData data = null;

        getLogger().info("Handling POST of " + getRequest().getEntity().getMediaType());
        //file posted from form
        MediaType mimeType = getRequest().getEntity().getMediaType(); 
        if (MediaType.MULTIPART_FORM_DATA.equals(mimeType, true)) {
            data = handleMultiPartFormUpload(context());
        }
        else if (MediaType.APPLICATION_WWW_FORM.equals(mimeType, true)) {
            data = handleFormPost();
        }

        if (data == null) {
            throw new RestletException("Unsupported POST", Status.CLIENT_ERROR_FORBIDDEN);
        }

        acceptData(data);
    }

    private void acceptData(ImportData data) {
        ImportContext context = context();
        List<ImportTask> newTasks = null;
        try {
            newTasks = importer.update(context, data);
            //importer.prep(context);
            //context.updated();
        } catch (ValidationException ve) {
            throw ImportJSONWriter.badRequest(ve.getMessage());
        } catch (IOException e) {
            throw new RestletException("Error updating context", Status.SERVER_ERROR_INTERNAL, e);
        }

        if (!newTasks.isEmpty()) {
            Object result = newTasks;
            if (newTasks.size() == 1) {
                result = newTasks.get(0);
                long taskId = newTasks.get(0).getId();
                getResponse().redirectSeeOther(getPageInfo().rootURI(
                    String.format("/imports/%d/tasks/%d", context.getId(), taskId)));
            }

            getResponse().setEntity(getFormatGet().toRepresentation(result));
            getResponse().setStatus(Status.SUCCESS_CREATED);
        }

    }

    private Directory findOrCreateDirectory(ImportContext context) {
        if (context.getData() instanceof Directory) {
            return (Directory) context.getData();
        }
    
        try {
            return Directory.createNew(importer.getUploadRoot());
        } catch (IOException ioe) {
            throw new RestletException("File upload failed", Status.SERVER_ERROR_INTERNAL, ioe);
        }
    }
    
    private ImportData handleFileUpload(ImportContext context) {
        Directory directory = findOrCreateDirectory(context);

        try {
            directory.accept(getAttribute("task"),getRequest().getEntity().getStream());
        } catch (IOException e) {
            throw new RestletException("Error unpacking file", 
                Status.SERVER_ERROR_INTERNAL, e);
        }
        
        return directory;
    }
    
    private ImportData handleMultiPartFormUpload(ImportContext context) {
        DiskFileItemFactory factory = new DiskFileItemFactory();
        // @revisit - this appears to be causing OOME
        //factory.setSizeThreshold(102400000);

        RestletFileUpload upload = new RestletFileUpload(factory);
        List<FileItem> items = null;
        try {
            items = upload.parseRequest(getRequest());
        } catch (FileUploadException e) {
            throw new RestletException("File upload failed", Status.SERVER_ERROR_INTERNAL, e);
        }

        //look for a directory to hold the files
        Directory directory = findOrCreateDirectory(context);

        //unpack all the files
        for (FileItem item : items) {
            if (item.getName() == null) {
                continue;
            }
            try {
                directory.accept(item);
            } catch (Exception ex) {
                throw new RestletException("Error writing file " + item.getName(), Status.SERVER_ERROR_INTERNAL, ex);
            }
        }
        return directory;
    }

    public boolean allowPut() {
        return getAttribute("task") != null;
    }

    public void handlePut() {
        if (getRequest().getEntity().getMediaType().equals(MediaType.APPLICATION_JSON)) {
            handleTaskPut();
        } else {
            acceptData(handleFileUpload(context()));
        }
    }

    public boolean allowDelete() {
        return getAttribute("task") != null;
    }

    @Override
    public void handleDelete() {
        ImportTask task = (ImportTask) lookupTask(false);
        task.getContext().removeTask(task);
        importer.changed(task.getContext());
        getResponse().setStatus(Status.SUCCESS_NO_CONTENT);
    }

    
    Object lookupTask(boolean allowAll) {
        ImportTask task = task(allowAll);

        if (task == null) {
            if (allowAll) {
                return context().getTasks();
            }
            throw new RestletException("No task specified", Status.CLIENT_ERROR_BAD_REQUEST);        
        }
        return task;
    }

    void handleTaskPut() {
        ImportTask orig = (ImportTask) lookupTask(false);
        ImportTask task;
        try {
            task = (ImportTask) getFormatPostOrPut().toObject(getRequest().getEntity());
        } catch (ValidationException ve) {
            getLogger().log(Level.WARNING, null, ve);
            throw ImportJSONWriter.badRequest(ve.getMessage());
        }

        boolean change = false;
        if (task.getStore() != null) {
            //JD: moved to TaskTargetResource, but handle here for backward compatability
            TaskTargetResource.updateStoreInfo(orig, task.getStore(), importer);
            change = true;
        }
        if (task.getData() != null) {
            //TODO: move this to data endpoint
            orig.getData().setCharsetEncoding(task.getData().getCharsetEncoding());
            change = true;
        }
        if (task.getUpdateMode() != null) {
            orig.setUpdateMode(task.getUpdateMode());
            change = orig.getUpdateMode() != task.getUpdateMode();
        }

        if (task.getLayer() != null) {
            change = true;
            //now handled by LayerResource, but handle here for backwards compatability
            LayerResource.updateLayer(orig, task.getLayer(), importer);
        }

        TransformChain chain = task.getTransform();
        if (chain != null) {
            orig.setTransform(chain);
            change = true;
        }

        if (!change) {
            throw new RestletException("Unknown representation", Status.CLIENT_ERROR_BAD_REQUEST);
        } else {
            importer.changed(orig);
            getResponse().setStatus(Status.SUCCESS_NO_CONTENT);
        }
    }
    
    private ImportData handleFormPost() {
        Form form = getRequest().getEntityAsForm();
        String url = form.getFirstValue("url", null);
        if (url == null) {
            throw new RestletException("Invalid request", Status.CLIENT_ERROR_BAD_REQUEST);
        }
        URL location = null;
        try {
            location = new URL(url);
        } catch (MalformedURLException ex) {
            getLogger().warning("invalid URL specified in upload : " + url);
        }
        // @todo handling remote URL implies asynchronous processing at this stage
        if (location == null || !location.getProtocol().equalsIgnoreCase("file")) {
            throw new RestletException("Invalid url in request", Status.CLIENT_ERROR_BAD_REQUEST);
        }
        FileData file;
        try {
            file = FileData.createFromFile(Resources.fromPath(location.toURI().getPath()));
        } catch (Exception ex) {
            throw new RuntimeException("Unexpected exception", ex);
        }

        if (file instanceof Directory) {
            try {
                file.prepare();
            } catch (IOException ioe) {
                String msg = "Error processing file: " + file.getFile().path();
                getLogger().log(Level.WARNING, msg, ioe);
                throw new RestletException(msg, Status.SERVER_ERROR_INTERNAL);
            }
        }

        return file;
    }

    private Representation createProgressRepresentation() {
        JSONObject progress = new JSONObject();
        long imprt = Long.parseLong(getAttribute("import"));
        ImportTask inProgress = importer.getCurrentlyProcessingTask(imprt);
        try {
            if (inProgress != null) {
                progress.put("progress", inProgress.getNumberProcessed());
                progress.put("total", inProgress.getTotalToProcess());
                progress.put("state", inProgress.getState().toString());
            } else {
                ImportTask task = (ImportTask) lookupTask(false);
                progress.put("state", task.getState().toString());
                if (task.getState() == ImportTask.State.ERROR) {
                    if (task.getError() != null) {
                        progress.put("message", task.getError().getMessage());
                    }
                }
            }
        } catch (JSONException jex) {
            throw new RestletException("Internal Error", Status.SERVER_ERROR_INTERNAL, jex);
        }
        return new JSONRepresentation(progress);
    }

    class ImportTaskJSONFormat extends StreamDataFormat {

        ImportTaskJSONFormat(MediaType type) {
            super(type);
        }

        @Override
        protected Object read(InputStream in) throws IOException {
            return newReader(in).task();
        }

        @Override
        protected void write(Object object, OutputStream out) throws IOException {
            ImportJSONWriter json = newWriter(out);

            if (object instanceof ImportTask) {
                ImportTask task = (ImportTask) object;
                json.task(task, true, expand(1));
            }
            else {
                json.tasks((List<ImportTask>)object, true, expand(0));
            }
        }

    }
}

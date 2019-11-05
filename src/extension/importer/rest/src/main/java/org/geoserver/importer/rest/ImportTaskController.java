/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.rest;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.catalog.impl.StoreInfoImpl;
import org.geoserver.importer.Directory;
import org.geoserver.importer.FileData;
import org.geoserver.importer.ImportContext;
import org.geoserver.importer.ImportData;
import org.geoserver.importer.ImportTask;
import org.geoserver.importer.Importer;
import org.geoserver.importer.ValidationException;
import org.geoserver.importer.rest.converters.ImportJSONReader;
import org.geoserver.importer.rest.converters.ImportJSONWriter;
import org.geoserver.importer.transform.TransformChain;
import org.geoserver.rest.PutIgnoringExtensionContentNegotiationStrategy;
import org.geoserver.rest.RequestInfo;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.geoserver.rest.util.MediaTypeExtensions;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;

@RestController
@RequestMapping(
    path = RestBaseController.ROOT_PATH + "/imports/{id}/tasks",
    produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_HTML_VALUE}
)
public class ImportTaskController extends ImportBaseController {

    static final Logger LOGGER = Logging.getLogger(ImportTaskController.class);

    @Autowired
    protected ImportTaskController(Importer importer) {
        super(importer);
    }

    @GetMapping
    public ImportWrapper tasksGet(
            @PathVariable Long id, @RequestParam(required = false) String expand) {
        return (writer, builder, converter) ->
                converter.tasks(builder, context(id).getTasks(), true, converter.expand(expand, 0));
    }

    @GetMapping(path = "/{taskId}")
    public ImportTask taskGet(@PathVariable Long id, @PathVariable Integer taskId) {
        return task(id, taskId, false);
    }

    @GetMapping(path = "/{taskId}/progress")
    public ImportWrapper progressGet(@PathVariable Long id, @PathVariable Integer taskId) {

        JSONObject progress = new JSONObject();
        ImportTask inProgress = importer.getCurrentlyProcessingTask(id);
        try {
            if (inProgress != null) {
                progress.put("progress", inProgress.getNumberProcessed());
                progress.put("total", inProgress.getTotalToProcess());
                progress.put("state", inProgress.getState().toString());
            } else {
                ImportTask task = task(id, taskId);
                progress.put("state", task.getState().toString());
                if (task.getState() == ImportTask.State.ERROR) {
                    if (task.getError() != null) {
                        progress.put("message", task.getError().getMessage());
                    }
                }
            }
        } catch (JSONException jex) {
            throw new RestException("Internal Error", HttpStatus.INTERNAL_SERVER_ERROR, jex);
        }
        return (writer, builder, converter) -> writer.write(progress.toString());
    }

    @GetMapping(path = "/{taskId}/target")
    public ImportWrapper targetGet(
            @PathVariable Long id,
            @PathVariable Integer taskId,
            @RequestParam(required = false) String expand) {
        final ImportTask task = task(id, taskId);
        if (task.getStore() == null) {
            throw new RestException("Task has no target store", HttpStatus.NOT_FOUND);
        }
        return (writer, builder, converter) ->
                converter.store(builder, task.getStore(), task, true, converter.expand(expand, 1));
    }

    @GetMapping(path = "/{taskId}/layer")
    public ImportWrapper layersGet(
            @PathVariable Long id,
            @PathVariable Integer taskId,
            @RequestParam(required = false) String expand) {
        ImportTask task = task(id, taskId);
        return (writer, builder, converter) ->
                converter.layer(builder, task, true, converter.expand(expand, 1));
    }

    @PostMapping(
        consumes = {
            MediaType.MULTIPART_FORM_DATA_VALUE,
            MediaType.APPLICATION_FORM_URLENCODED_VALUE
        }
    )
    public Object taskPost(
            @PathVariable Long id,
            @RequestParam(required = false) String expand,
            HttpServletRequest request,
            HttpServletResponse response) {
        ImportData data = null;

        LOGGER.info("Handling POST of " + request.getContentType());
        // file posted from form
        if (request.getContentType().startsWith(MediaType.MULTIPART_FORM_DATA_VALUE)) {
            data = handleMultiPartFormUpload(request, context(id));
        } else if (request.getContentType()
                .startsWith(MediaType.APPLICATION_FORM_URLENCODED_VALUE)) {
            try {
                data = handleFormPost(request);
            } catch (IOException | ServletException e) {
                throw new RestException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, e);
            }
        }

        if (data == null) {
            throw new RestException("Unsupported POST", HttpStatus.FORBIDDEN);
        }

        // Construct response
        return acceptData(data, context(id), response, expand);
    }

    @PutMapping(
        path = "/{taskId}",
        consumes = {MediaType.APPLICATION_JSON_VALUE, MediaTypeExtensions.TEXT_JSON_VALUE}
    )
    public ImportWrapper taskPut(
            @PathVariable Long id,
            @PathVariable Integer taskId,
            HttpServletRequest request,
            HttpServletResponse response) {

        return (writer, builder, converter) ->
                handleTaskPut(id, taskId, request, response, converter);
    }

    /** Workaround to support regular response content type when file extension is in path */
    @Configuration
    static class ImportTaskControllerConfiguration {
        @Bean
        PutIgnoringExtensionContentNegotiationStrategy importTaskPutContentNegotiationStrategy() {
            return new PutIgnoringExtensionContentNegotiationStrategy(
                    new PatternsRequestCondition("/imports/{id}/tasks/{taskId:.+}"),
                    Arrays.asList(MediaType.APPLICATION_JSON, MediaType.TEXT_HTML));
        }
    }

    /**
     * Uploads a file as a new import task.
     *
     * @param id The import id
     * @param filename The destination name of the file
     */
    @PutMapping(path = "/{taskId:.+}")
    public Object taskPutFile(
            @PathVariable Long id,
            @PathVariable(name = "taskId") Object filename,
            @RequestParam(required = false) String expand,
            HttpServletRequest request,
            HttpServletResponse response) {

        ImportContext context = context(id);
        return acceptData(handleFileUpload(context, filename, request), context, response, expand);
    }

    @PutMapping(path = "/{taskId}/target")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void targetPut(
            @PathVariable Long id, @PathVariable Integer taskId, @RequestBody StoreInfo store) {

        if (store == null) {
            throw new RestException("Task has no target store", HttpStatus.NOT_FOUND);
        } else {
            ImportTask task = task(id, taskId);
            updateStoreInfo(task, store, importer);
            try {
                importer.changed(task);
            } catch (IOException e) {
                throw new RestException(
                        "Error while initializing Importer Context",
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        e);
            }
        }
    }

    @PutMapping(path = "/{taskId}/layer")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ImportWrapper layerPut(
            @PathVariable Long id,
            @PathVariable Integer taskId,
            @RequestParam(required = false) String expand,
            @RequestBody ImportLayer layer) {
        ImportTask task = task(id, taskId);

        return (writer, builder, converter) -> {
            updateLayer(task, layer.getLayer(), importer, converter);
            importer.changed(task);
            converter.layer(builder, task, true, converter.expand(expand, 1));
        };
    }

    @DeleteMapping(path = "/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void taskDelete(@PathVariable Long id, @PathVariable Integer taskId) {
        ImportTask task = task(id, taskId);
        task.getContext().removeTask(task);
        try {
            importer.changed(task.getContext());
        } catch (IOException e) {
            throw new RestException(
                    "Error deleting Importer Context", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    public Object acceptData(
            ImportData data, ImportContext context, HttpServletResponse response, String expand) {
        List<ImportTask> newTasks = null;
        try {
            newTasks = importer.update(context, data);
        } catch (ValidationException ve) {
            throw ImportJSONWriter.badRequest(ve.getMessage());
        } catch (IOException e) {
            throw new RestException("Error updating context", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
        if (!newTasks.isEmpty()) {
            final List<ImportTask> result = newTasks;
            if (newTasks.size() == 1) {
                long taskId = newTasks.get(0).getId();
                response.setHeader(
                        "Location",
                        RequestInfo.get()
                                .servletURI(
                                        String.format(
                                                "/imports/%d/tasks/%d", context.getId(), taskId)));
            }
            response.setStatus(HttpStatus.CREATED.value());

            return (ImportWrapper)
                    (writer, builder, converter) -> {
                        if (result.size() == 1) {
                            converter.task(
                                    builder, result.get(0), true, converter.expand(expand, 1));
                        } else {
                            converter.tasks(builder, result, true, converter.expand(expand, 0));
                        }
                    };
        }
        return "";
    }

    public ImportData handleFormPost(HttpServletRequest request)
            throws IOException, ServletException {

        String url = IOUtils.toString(request.getPart("url").getInputStream(), encoding);
        if (url == null) {
            throw new RestException("Invalid request", HttpStatus.BAD_REQUEST);
        }
        URL location = null;
        try {
            location = new URL(url);
        } catch (MalformedURLException ex) {
            LOGGER.warning("invalid URL specified in upload : " + url);
        }
        // @todo handling remote URL implies asynchronous processing at this stage
        if (location == null || !location.getProtocol().equalsIgnoreCase("file")) {
            throw new RestException("Invalid url in request", HttpStatus.BAD_REQUEST);
        }
        FileData file;
        try {
            file = FileData.createFromFile(new File(location.toURI().getPath()));
        } catch (Exception ex) {
            throw new RuntimeException("Unexpected exception", ex);
        }

        if (file instanceof Directory) {
            try {
                file.prepare();
            } catch (IOException ioe) {
                String msg = "Error processing file: " + file.getFile().getAbsolutePath();
                LOGGER.log(Level.WARNING, msg, ioe);
                throw new RestException(msg, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return file;
    }

    public ImportData handleMultiPartFormUpload(HttpServletRequest request, ImportContext context) {
        DiskFileItemFactory factory = new DiskFileItemFactory();
        // @revisit - this appears to be causing OOME
        // factory.setSizeThreshold(102400000);

        ServletFileUpload upload = new ServletFileUpload(factory);
        List<FileItem> items = null;
        try {
            items = upload.parseRequest(request);
        } catch (FileUploadException e) {
            throw new RestException("File upload failed", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }

        // look for a directory to hold the files
        Directory directory = findOrCreateDirectory(context);

        // unpack all the files
        for (FileItem item : items) {
            if (item.getName() == null) {
                continue;
            }
            try {
                directory.accept(item);
            } catch (Exception ex) {
                throw new RestException(
                        "Error writing file " + item.getName(),
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        ex);
            }
        }
        return directory;
    }

    void handleTaskPut(
            Long id,
            Integer taskId,
            HttpServletRequest request,
            HttpServletResponse response,
            ImportJSONWriter converter) {
        ImportTask orig = task(id, taskId);
        ImportTask task;
        try {
            ImportJSONReader reader = new ImportJSONReader(importer);
            task = reader.task(request.getInputStream());
            //            task = new
            // ImportContextJSONConverterReader(importer,request.getInputStream()).task();
        } catch (ValidationException | IOException ve) {
            LOGGER.log(Level.WARNING, null, ve);
            throw converter.badRequest(ve.getMessage());
        }

        boolean change = false;
        if (task.getStore() != null) {
            // JD: moved to TaskTargetResource, but handle here for backward compatability
            updateStoreInfo(orig, task.getStore(), importer);
            change = true;
        }
        if (task.getData() != null) {
            // TODO: move this to data endpoint
            orig.getData().setCharsetEncoding(task.getData().getCharsetEncoding());
            change = true;
        }
        if (task.getUpdateMode() != null) {
            orig.setUpdateMode(task.getUpdateMode());
            change = orig.getUpdateMode() != task.getUpdateMode();
        }

        if (task.getLayer() != null) {
            change = true;
            // now handled by LayerResource, but handle here for backwards compatability
            updateLayer(orig, task.getLayer(), importer, converter);
        }

        TransformChain chain = task.getTransform();
        if (chain != null) {
            orig.setTransform(chain);
            change = true;
        }

        if (!change) {
            throw new RestException("Unknown representation", HttpStatus.BAD_REQUEST);
        } else {
            try {
                importer.changed(orig);
            } catch (IOException e) {
                throw new RestException(
                        "Error updating Importer Context", HttpStatus.INTERNAL_SERVER_ERROR, e);
            }
            response.setStatus(HttpStatus.NO_CONTENT.value());
        }
    }

    private Directory findOrCreateDirectory(ImportContext context) {
        if (context.getData() instanceof Directory) {
            return (Directory) context.getData();
        }

        try {
            return Directory.createNew(importer.getUploadRoot());
        } catch (IOException ioe) {
            throw new RestException("File upload failed", HttpStatus.INTERNAL_SERVER_ERROR, ioe);
        }
    }

    private ImportData handleFileUpload(
            ImportContext context, Object taskId, HttpServletRequest request) {
        Directory directory = findOrCreateDirectory(context);
        try {
            directory.accept(taskId.toString(), request.getInputStream());
        } catch (IOException e) {
            throw new RestException("Error unpacking file", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }

        return directory;
    }

    static void updateLayer(
            ImportTask orig, LayerInfo l, Importer importer, ImportJSONWriter converter) {
        // update the original layer and resource from the new

        ResourceInfo r = l.getResource();

        // TODO: this is not thread safe, clone the object before overwriting it
        // save the existing resource, which will be overwritten below,
        ResourceInfo resource = orig.getLayer().getResource();

        if (r != null) {
            // we support the following resource info properties:
            // (don't just use blindly copy everything)
            if (r.getTitle() != null) {
                resource.setTitle(r.getTitle());
            }
            if (r.getAbstract() != null) {
                resource.setAbstract(r.getAbstract());
            }
            if (r.getDescription() != null) {
                resource.setDescription(r.getDescription());
            }
            if (r.getName() != null) {
                resource.setName(r.getName());
            }
            if (r.getNativeName() != null) {
                // seems weird I know, but check the code of getOriginalLayerName...
                if (!r.getNativeName().equalsIgnoreCase(orig.getOriginalLayerName())) {
                    orig.setOriginalLayerName(orig.getOriginalLayerName());
                }
                resource.setNativeName(r.getNativeName());
            }
        }

        CatalogBuilder cb = new CatalogBuilder(importer.getCatalog());
        l.setResource(resource);
        // @hack workaround OWSUtils bug - trying to copy null collections
        // why these are null in the first place is a different question
        LayerInfoImpl impl = (LayerInfoImpl) orig.getLayer();
        if (impl.getAuthorityURLs() == null) {
            impl.setAuthorityURLs(new ArrayList(1));
        }
        if (impl.getIdentifiers() == null) {
            impl.setIdentifiers(new ArrayList(1));
        }
        // @endhack
        cb.updateLayer(orig.getLayer(), l);

        // validate SRS - an invalid one will destroy capabilities doc and make
        // the layer totally broken in UI
        CoordinateReferenceSystem newRefSystem = null;

        String srs = r != null ? r.getSRS() : null;
        if (srs != null) {
            try {
                newRefSystem = CRS.decode(srs);
            } catch (NoSuchAuthorityCodeException ex) {
                String msg = "Invalid SRS " + srs;
                LOGGER.warning(msg + " in PUT request");
                throw converter.badRequest(msg);
            } catch (FactoryException ex) {
                throw new RestException(
                        "Error with referencing", HttpStatus.INTERNAL_SERVER_ERROR, ex);
            }
            // make this the specified native if none exists
            // useful for csv or other files
            if (resource.getNativeCRS() == null) {
                resource.setNativeCRS(newRefSystem);
            }
            resource.setSRS(srs);
        }
    }

    static void updateStoreInfo(ImportTask task, StoreInfo update, Importer importer) {
        // handle three cases here:
        // 1. current task store is null -> set the update as the new store
        // 2. update is reference to an existing store -> set the update as the new store
        // 3. update is a partial change to the current store -> update the current

        // allow an existing store to be referenced as the target

        StoreInfo orig = task.getStore();

        // check if the update is referencing an existing store
        StoreInfo existing = null;
        if (update.getName() != null) {
            Catalog cat = importer.getCatalog();
            if (update.getWorkspace() != null) {
                existing =
                        cat.getStoreByName(
                                update.getWorkspace(), update.getName(), StoreInfo.class);
            } else {
                existing = importer.getCatalog().getStoreByName(update.getName(), StoreInfo.class);
            }
            if (existing == null) {
                throw new RestException("Unable to find referenced store", HttpStatus.BAD_REQUEST);
            }
            if (!existing.isEnabled()) {
                throw new RestException(
                        "Proposed target store is not enabled", HttpStatus.BAD_REQUEST);
            }
        }

        if (existing != null) {
            // JD: not sure why we do this, rather than just task.setStore(existing);
            CatalogBuilder cb = new CatalogBuilder(importer.getCatalog());

            StoreInfo clone;
            if (existing instanceof DataStoreInfo) {
                clone = cb.buildDataStore(existing.getName());
                cb.updateDataStore((DataStoreInfo) clone, (DataStoreInfo) existing);
            } else if (existing instanceof CoverageStoreInfo) {
                clone = cb.buildCoverageStore(existing.getName());
                cb.updateCoverageStore((CoverageStoreInfo) clone, (CoverageStoreInfo) existing);
            } else {
                throw new RestException(
                        "Unable to handle existing store: " + update,
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }

            ((StoreInfoImpl) clone).setId(existing.getId());
            task.setStore(clone);
            task.setDirect(false);
        } else if (orig == null) {
            task.setStore(update);
        } else {
            // update the original
            CatalogBuilder cb = new CatalogBuilder(importer.getCatalog());
            if (orig instanceof DataStoreInfo) {
                cb.updateDataStore((DataStoreInfo) orig, (DataStoreInfo) update);
            } else if (orig instanceof CoverageStoreInfo) {
                cb.updateCoverageStore((CoverageStoreInfo) orig, (CoverageStoreInfo) update);
            } else {
                throw new RestException(
                        "Unable to update store with " + update, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
    }
}

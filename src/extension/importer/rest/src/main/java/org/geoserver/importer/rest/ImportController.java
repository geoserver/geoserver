/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.rest;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.importer.ImportContext;
import org.geoserver.importer.ImportFilter;
import org.geoserver.importer.Importer;
import org.geoserver.importer.ValidationException;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping(
        path = RestBaseController.ROOT_PATH + "/imports",
        produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_HTML_VALUE})
public class ImportController extends ImportBaseController {

    private static final Logger LOGGER = Logging.getLogger(ImportController.class);

    @Autowired
    public ImportController(Importer importer) {
        super(importer);
    }

    @PostMapping(value = {"/{id}", ""})
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Object> postImports(
            @PathVariable(required = false) Long id,
            @RequestParam(name = "async", required = false, defaultValue = "false") boolean async,
            @RequestParam(name = "exec", required = false, defaultValue = "false") boolean exec,
            @RequestBody(required = false) ImportContext obj,
            UriComponentsBuilder builder)
            throws IOException {

        ImportContext context = (ImportContext) context(id, true, false);
        if (context != null) {
            try {
                runImport(context, async);
            } catch (Throwable t) {
                if (t instanceof ValidationException) {
                    throw new RestException(t.getMessage(), HttpStatus.BAD_REQUEST, t);
                } else {
                    throw new RestException(
                            "Error occurred executing import", HttpStatus.INTERNAL_SERVER_ERROR, t);
                }
            }
            return new ResponseEntity<>("", new HttpHeaders(), HttpStatus.NO_CONTENT);
        }
        context = createImport(id, obj, async, exec);
        if (context != null) {
            importer.changed(context);
        } else {
            throw new RestException(
                    "Error occurred executing import", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        UriComponents uriComponents = getUriComponents(context.getId().toString(), builder);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setLocation(uriComponents.toUri());
        return new ResponseEntity<>(context, headers, HttpStatus.CREATED);
    }

    @GetMapping
    public ImportWrapper getImports(@RequestParam(required = false) String expand) {
        Object lookupContext = context(null, true, true);
        if (lookupContext == null) {
            // this means a specific lookup failed
            throw new RestException("Failed to find import context", HttpStatus.NOT_FOUND);
        } else {
            // For ImportContext, the expand parameter is handled at the converter level. Here, we
            // are listing contexts, and use a different (more succinct) default
            return (writer, builder, converter) -> {
                @SuppressWarnings("unchecked")
                Iterator<ImportContext> lc = (Iterator) lookupContext;
                converter.contexts(builder, lc, converter.expand(expand, 0));
            };
        }
    }

    @GetMapping(value = "/{id}")
    public ImportContext getImports(@PathVariable Long id) {
        return context(id);
    }

    @PutMapping(value = "/{id}")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Object> putImport(
            @PathVariable Long id,
            @RequestParam(name = "async", required = false, defaultValue = "false") boolean async,
            @RequestParam(name = "exec", required = false, defaultValue = "false") boolean exec,
            UriComponentsBuilder builder) {

        if (id != null) {
            ImportContext context = createImport(id, null, async, exec);
            assert context.getId() >= id;
            UriComponents uriComponents = getUriComponents(context.getId().toString(), builder);
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(uriComponents.toUri());
            headers.setContentType(MediaType.APPLICATION_JSON);
            return new ResponseEntity<>(context, headers, HttpStatus.CREATED);
        } else {
            throw new RestException("ID must be provided for PUT", HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping(value = {"", "/{id}"})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteImports(@PathVariable(required = false) Long id) {
        Iterator<ImportContext> contexts = null;
        if (id == null) {
            contexts = importer.getAllContexts();
        } else {
            contexts = Collections.singletonList(context(id)).iterator();
        }
        while (contexts.hasNext()) {
            ImportContext ctx = contexts.next();
            try {
                importer.delete(ctx);
            } catch (IOException ioe) {
                LOGGER.log(
                        Level.SEVERE,
                        "Error deleting context "
                                + ctx.getId()
                                + ", message is: "
                                + ioe.getMessage(),
                        ioe);
                throw new RestException(
                        "Error deleting context " + ctx.getId(),
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        ioe);
            }
        }
    }

    private UriComponents getUriComponents(String name, UriComponentsBuilder builder) {
        return builder.path("/imports/{id}").buildAndExpand(name);
    }

    private void runImport(ImportContext context, boolean async) throws IOException {
        if (context.getState() == ImportContext.State.INIT) {
            throw new RestException(
                    "Import context is still in INIT state, cannot run it yet",
                    HttpStatus.PRECONDITION_FAILED);
        }
        // if the import is empty, prep it but leave data as is
        if (context.getTasks().isEmpty()) {
            importer.init(context, false);
        }

        if (async) {
            importer.runAsync(context, ImportFilter.ALL, false);
        } else {
            importer.run(context);
        }
    }

    private ImportContext createImport(
            Long id, ImportContext newContext, boolean async, boolean execute) {
        // create a new import
        ImportContext context;
        try {
            if (async) {
                context = importer.registerContext(id);
            } else {
                context = importer.createContext(id);
            }
            if (newContext != null) {
                WorkspaceInfo targetWorkspace = newContext.getTargetWorkspace();
                StoreInfo targetStore = newContext.getTargetStore();
                Catalog cat = importer.getCatalog();
                if (targetWorkspace != null) {
                    // resolve to the 'real' workspace
                    WorkspaceInfo ws = cat.getWorkspaceByName(targetWorkspace.getName());
                    if (ws == null) {
                        throw new RestException(
                                "Target workspace does not exist : " + targetWorkspace.getName(),
                                HttpStatus.BAD_REQUEST);
                    }
                    context.setTargetWorkspace(ws);
                }
                if (targetStore != null) {
                    WorkspaceInfo ws = context.getTargetWorkspace();
                    String storeName = targetStore.getName();
                    StoreInfo ts;
                    if (ws != null) ts = cat.getStoreByName(ws, storeName, StoreInfo.class);
                    else ts = cat.getStoreByName(storeName, StoreInfo.class);
                    if (ts == null) {
                        throw new RestException(
                                "Target store does not exist : " + targetStore.getName(),
                                HttpStatus.BAD_REQUEST);
                    }
                    context.setTargetStore(ts);
                }
                if (targetStore != null && targetWorkspace == null) {
                    // take it from the store
                    context.setTargetWorkspace(targetStore.getWorkspace());
                }
                context.setData(newContext.getData());
                context.getDefaultTransforms().addAll(newContext.getDefaultTransforms());
            } else if (context == null) {
                context = context(id, true);
            }
            if (!async && context.getData() != null) {
                importer.init(context, true);
            }
            context.reattach(importer.getCatalog(), true);
            importer.changed(context);

            if (async && context.getData() != null) {
                if (execute) {
                    importer.runAsync(context, ImportFilter.ALL, true);
                } else {
                    importer.initAsync(context, true);
                }
            } else if (execute && context.getData() != null) {
                importer.run(context);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Unable to create import, message is: " + e.getMessage(), e);
            throw new RestException("Unable to create import", HttpStatus.INTERNAL_SERVER_ERROR, e);
        } catch (IllegalArgumentException iae) {
            throw new RestException(iae.getMessage(), HttpStatus.BAD_REQUEST, iae);
        }
        return context;
    }
}

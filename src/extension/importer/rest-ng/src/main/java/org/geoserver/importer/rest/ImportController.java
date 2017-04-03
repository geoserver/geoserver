/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.rest;

import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.rest.CatalogController;
import org.geoserver.importer.ImportContext;
import org.geoserver.importer.ImportData;
import org.geoserver.importer.ImportFilter;
import org.geoserver.importer.Importer;
import org.geoserver.rest.RequestInfo;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

@RestController
@RequestMapping(path = RestBaseController.ROOT_PATH+"/imports", produces = {
        MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_HTML_VALUE })
public class ImportController extends ImportBaseController {

    Object importContext; // ImportContext or Iterator<ImportContext>

    private int expand;

    @Autowired
    public ImportController(Importer importer) {
        super(importer);

    }

    @PostMapping(value = {"/{id}",""}, produces = { MediaType.APPLICATION_JSON_VALUE })
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ImportContext> postImports(@PathVariable(name="id",required=false) Long id, @RequestBody(required=false) ImportContext obj,
            UriComponentsBuilder builder) {
        // Object obj = context(null, true, true);
        ImportContext context = null;
        /*
         * if (obj instanceof ImportContext) { // run an existing import try { runImport((ImportContext) obj); context = obj; } catch (Throwable t) {
         * if (t instanceof ValidationException) { throw new RestException(t.getMessage(), HttpStatus.BAD_REQUEST, t); } else { throw new
         * RestException("Error occured executing import", HttpStatus.INTERNAL_SERVER_ERROR, t); } } } else {
         */
        context = createImport(id, obj);
        // }
        if (context != null) {
            importer.changed(context);
        } else {
            throw new RestException("Error occured executing import",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        UriComponents uriComponents = getUriComponents(context.getId().toString(), builder);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(uriComponents.toUri());
        return new ResponseEntity<ImportContext>(context, headers, HttpStatus.CREATED);
    }

    @GetMapping(produces = { MediaType.APPLICATION_JSON_VALUE,
            CatalogController.TEXT_JSON })
    public ImportWrapper getImports() {

        Object lookupContext = context(null, true, true);
        if (lookupContext == null) {
            // this means a specific lookup failed
            throw new RestException("Failed to find import context", HttpStatus.NOT_FOUND);
        } else {
            return (writer, converter) -> converter.contexts((Iterator<ImportContext>)lookupContext, converter.expand(0));
        }
    }

    @GetMapping(value = "/{id}", produces = { MediaType.APPLICATION_JSON_VALUE,
            CatalogController.TEXT_JSON , MediaType.TEXT_HTML_VALUE})
    public ImportContext getImports(@PathVariable Long id,
            @RequestParam(name = "expand", required = false, defaultValue = "0") int expand) {
        this.expand = expand;
        return context(id);
    }

    @PutMapping(value = "/{id}", consumes = { MediaType.APPLICATION_JSON_VALUE,
            CatalogController.TEXT_JSON })
    public ResponseEntity<String> putImport(@PathVariable Long id, UriComponentsBuilder builder) {
        if (id != null) {
            ImportContext context = createImport(id, null);
            assert context.getId() >= id;
            UriComponents uriComponents = getUriComponents(context.getId().toString(), builder);
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(uriComponents.toUri());
            return new ResponseEntity<String>(context.getId().toString(), headers, HttpStatus.CREATED);
        } else {
            throw new RestException("ID must be provided for PUT", HttpStatus.BAD_REQUEST);
        }
        
        
    }

    private UriComponents getUriComponents(String name, UriComponentsBuilder builder) {
        UriComponents uriComponents;

        uriComponents = builder.path("/imports/{id}").buildAndExpand(name);

        return uriComponents;
    }

    private void runImport(ImportContext context) throws IOException {
        if (context.getState() == ImportContext.State.INIT) {
            throw new RestException("Import context is still in INIT state, cannot run it yet",
                    HttpStatus.PRECONDITION_FAILED);
        }

        // if the import is empty, prep it but leave data as is
        if (context.getTasks().isEmpty()) {
            importer.init(context, false);
        }

        Map<String, String[]> query = RequestInfo.get().getQueryMap();

        if (query.containsKey("async")) {
            importer.runAsync(context, ImportFilter.ALL, false);
        } else {
            importer.run(context);
            // @todo revisit - if errors occur, they are logged. A second request
            // is required to verify success
        }
        // getResponse().setStatus(HttpStatus.SUCCESS_NO_CONTENT);
    }

    private ImportContext createImport(Long id, ImportContext newContext) {
        // create a new import
        ImportContext context;
        try {
            Map<String, String[]> query = RequestInfo.get().getQueryMap();
            boolean async = query.containsKey("async");
            boolean execute = query.containsKey("exec");

            if (async) {
                context = importer.registerContext(id);
            } else {
                context = importer.createContext(id);
            }

            ImportData data = null;
            if (newContext != null) {
                /*
                 * // read representation specified by user, use it to read ImportContext newContext = (ImportContext) getFormatPostOrPut()
                 * .toObject(getRequest().getEntity());
                 */

                WorkspaceInfo targetWorkspace = newContext.getTargetWorkspace();
                StoreInfo targetStore = newContext.getTargetStore();

                if (targetWorkspace != null) {
                    // resolve to the 'real' workspace
                    WorkspaceInfo ws = importer.getCatalog()
                            .getWorkspaceByName(newContext.getTargetWorkspace().getName());
                    if (ws == null) {
                        throw new RestException(
                                "Target workspace does not exist : "
                                        + newContext.getTargetStore().getName(),
                                HttpStatus.BAD_REQUEST);

                    }
                    context.setTargetWorkspace(ws);
                }
                if (targetStore != null) {
                    StoreInfo ts = importer.getCatalog()
                            .getStoreByName(newContext.getTargetStore().getName(), StoreInfo.class);
                    if (ts == null) {
                        throw new RestException(
                                "Target store does not exist : "
                                        + newContext.getTargetStore().getName(),
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
            } else if (context==null){
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
            throw new RestException("Unable to create import", HttpStatus.INTERNAL_SERVER_ERROR, e);
        } catch (IllegalArgumentException iae) {
            throw new RestException(iae.getMessage(), HttpStatus.BAD_REQUEST, iae);
        }
        return context;
    }

}

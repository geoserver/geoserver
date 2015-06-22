/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.importer.ImportContext;
import org.geoserver.importer.ImportData;
import org.geoserver.importer.ImportFilter;
import org.geoserver.importer.Importer;
import org.geoserver.importer.ValidationException;
import org.geoserver.rest.PageInfo;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.rest.format.StreamDataFormat;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

/**
 * REST resource for /contexts[/<id>]
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class ImportResource extends BaseResource {

    Object importContext; // ImportContext or Iterator<ImportContext>

    public ImportResource(Importer importer) {
        super(importer);
    }

    @Override
    protected List<DataFormat> createSupportedFormats(Request request, Response response) {
        return (List) Arrays.asList(new ImportContextJSONFormat(MediaType.APPLICATION_JSON),
                new ImportContextJSONFormat(MediaType.TEXT_HTML));
    }

    @Override
    public void handleGet() {
        DataFormat formatGet = getFormatGet();
        if (formatGet == null) {
            formatGet = new ImportContextJSONFormat(MediaType.APPLICATION_JSON);
        }
        Object lookupContext = lookupContext(true, false);
        if (lookupContext == null) {
            // this means a specific lookup failed
            getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
        } else {
            getResponse().setEntity(formatGet.toRepresentation(lookupContext));
        }
    }

    @Override
    public boolean allowPost() {
        return true;
    }
    
    @Override
    public boolean allowPut() {
        return true;
    }

    @Override
    public boolean allowDelete() {
        boolean allowDelete = false;
        importContext = lookupContext(true, false);
        if (importContext instanceof ImportContext) {
            ImportContext ctx = (ImportContext) importContext;
            allowDelete = ctx.getState() != ImportContext.State.COMPLETE;
        } else {
            allowDelete = true;
        }
        return allowDelete;
    }

    @Override
    public void handleDelete() {
        Iterator<ImportContext> contexts = null;
        if (importContext instanceof ImportContext) {
            contexts = Collections.singletonList((ImportContext) importContext).iterator();
        } else {
            contexts = (Iterator<ImportContext>) importContext;
        }
        while (contexts.hasNext()) {
            ImportContext ctx = contexts.next();
            if (ctx.getState() != ImportContext.State.COMPLETE) {
                try {
                    importer.delete(ctx);
                } catch (IOException ioe) {
                    throw new RestletException("Error deleting context " + ctx.getId(), Status.SERVER_ERROR_INTERNAL, ioe);
                }
            }
        }
        getResponse().setStatus(Status.SUCCESS_NO_CONTENT);
    }

    private void runImport(ImportContext context) throws IOException {
        if (context.getState() == ImportContext.State.INIT) {
            getResponse().setStatus(Status.CLIENT_ERROR_PRECONDITION_FAILED,
                    "Import context is still in INIT state, cannot run it yet");
        }

        // if the import is empty, prep it but leave data as is
        if (context.getTasks().isEmpty()) {
            importer.init(context, false);
        }

        Form query = getRequest().getResourceRef().getQueryAsForm();

        if (query.getNames().contains("async")) {
            importer.runAsync(context, ImportFilter.ALL, false);
        } else {
            importer.run(context);
            // @todo revisit - if errors occur, they are logged. A second request
            // is required to verify success
        }
        getResponse().setStatus(Status.SUCCESS_NO_CONTENT);
    }
    
    private ImportContext createImport(Long id) {
        //create a new import
        ImportContext context;
        try {
            Form query = getRequest().getResourceRef().getQueryAsForm();
            boolean async = query.getNames().contains("async");
            boolean execute = query.getNames().contains("execute");

            if (async) {
                context = importer.registerContext(id);
            } else {
                context = importer.createContext(id);
            }

            ImportData data = null;
            if (MediaType.APPLICATION_JSON.equals(getRequest().getEntity().getMediaType())) {
                //read representation specified by user, use it to read 
                ImportContext newContext = 
                    (ImportContext) getFormatPostOrPut().toObject(getRequest().getEntity());

                WorkspaceInfo targetWorkspace = newContext.getTargetWorkspace();
                StoreInfo targetStore = newContext.getTargetStore();

                if (targetWorkspace != null) {
                    // resolve to the 'real' workspace
                    WorkspaceInfo ws = importer.getCatalog().getWorkspaceByName(
                            newContext.getTargetWorkspace().getName());
                    if (ws == null) {
                        throw new RestletException("Target workspace does not exist : "
                                + newContext.getTargetStore().getName(), Status.CLIENT_ERROR_BAD_REQUEST);

                    }
                    context.setTargetWorkspace(ws);
                }
                if (targetStore != null) {
                    StoreInfo ts = importer.getCatalog().getStoreByName(newContext.getTargetStore().getName(), StoreInfo.class);
                    if (ts == null) {
                        throw new RestletException("Target store does not exist : "
                                + newContext.getTargetStore().getName(), Status.CLIENT_ERROR_BAD_REQUEST);
                    }
                    context.setTargetStore(ts);
                }
                if (targetStore != null && targetWorkspace == null) {
                    //take it from the store 
                    context.setTargetWorkspace(targetStore.getWorkspace());
                }

                context.setData(newContext.getData());
                context.getDefaultTransforms().addAll(newContext.getDefaultTransforms());
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

            getResponse().redirectSeeOther(getPageInfo().rootURI("/imports/"+context.getId()));
            getResponse().setEntity(getFormatGet().toRepresentation(context));
            getResponse().setStatus(Status.SUCCESS_CREATED);

        } 
        catch (IOException e) {
            throw new RestletException("Unable to create import", Status.SERVER_ERROR_INTERNAL, e);
        }
        catch (IllegalArgumentException iae) {
            throw new RestletException(iae.getMessage(), Status.CLIENT_ERROR_BAD_REQUEST, iae);
        }
        return context;
    }
    
    @Override
    public void handlePost() {
        Object obj = lookupContext(true, true);
        ImportContext context = null;
        if (obj instanceof ImportContext) {
            //run an existing import
            try {
                runImport((ImportContext) obj);
            } catch (Throwable t) {
                if (t instanceof ValidationException) {
                    throw new RestletException(t.getMessage(), Status.CLIENT_ERROR_BAD_REQUEST, t);
                } else {
                    throw new RestletException("Error occured executing import", Status.SERVER_ERROR_INTERNAL, t);
                }
            }
        }
        else {
            context = createImport(null);
        }
        if (context != null) {
            importer.changed(context);
        }
    }

    @Override
    public void handlePut() {
        // allow the client to specify an id - this will move the sequence forward
        String i = getAttribute("import");
        if (i != null) {
            Long id = null;
            String error = null;
            try {
                id = new Long(i);
            } catch (NumberFormatException nfe) {
                throw new RestletException("Invalid ID : " + i, Status.CLIENT_ERROR_BAD_REQUEST);
            }
            ImportContext context = createImport(id);
            assert context.getId() >= id;
        } else {
            throw new RestletException("ID must be provided for PUT", Status.CLIENT_ERROR_BAD_REQUEST);
        }
    }

    Object lookupContext(boolean allowAll, boolean mustExist) {
        String i = getAttribute("import");
        if (i != null) {
            ImportContext context = null;
            try {
                context = importer.getContext(Long.parseLong(i));
            } catch (NumberFormatException e) {
            }
            if (context == null && mustExist) {
                throw new RestletException("No such import: " + i, Status.CLIENT_ERROR_NOT_FOUND);
            }
            return context;
        }
        else {
            if (allowAll) {
                return importer.getAllContexts();
            }
            throw new RestletException("No import specified", Status.CLIENT_ERROR_BAD_REQUEST);
        }
    }

    class ImportContextJSONFormat extends StreamDataFormat {

        public ImportContextJSONFormat(MediaType type) {
            super(type);
        }

        @Override
        protected Object read(InputStream in) throws IOException {
            return newReader(in).context();
        }

        @Override
        protected void write(Object object, OutputStream out) throws IOException {
            
            PageInfo pageInfo = getPageInfo();
            // @hack lop off query if there is one or resulting URIs look silly
            int queryIdx = pageInfo.getPagePath().indexOf('?');
            if (queryIdx > 0) {
                pageInfo.setPagePath(pageInfo.getPagePath().substring(0, queryIdx));
            }

            ImportJSONWriter json = newWriter(out);
            if (object instanceof ImportContext) {
                json.context((ImportContext) object, true, expand(1));
            }
            else {
                json.contexts((Iterator<ImportContext>)object, expand(0));
            }
        }
    }
}

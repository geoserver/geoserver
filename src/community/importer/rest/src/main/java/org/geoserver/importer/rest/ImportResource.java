/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
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
import org.geoserver.rest.PageInfo;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.rest.format.StreamDataFormat;
import org.geoserver.importer.ImportContext;
import org.geoserver.importer.ImportFilter;
import org.geoserver.importer.Importer;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

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
    
    private ImportContext createImport(Long id) {
        //create a new import
        ImportContext context;
        try {
            context = importer.createContext(id);
            context.setUser(getCurrentUser());

            if (MediaType.APPLICATION_JSON.equals(getRequest().getEntity().getMediaType())) {
                //read representation specified by user, use it to read 
                ImportContext newContext = 
                    (ImportContext) getFormatPostOrPut().toObject(getRequest().getEntity());

                WorkspaceInfo targetWorkspace = newContext.getTargetWorkspace();
                StoreInfo targetStore = newContext.getTargetStore();

                if (targetWorkspace != null) {
                    context.setTargetWorkspace(targetWorkspace);
                }
                if (targetStore != null) {
                    context.setTargetStore(targetStore);
                }
                if (targetStore != null && targetWorkspace == null) {
                    //take it from the store 
                    context.setTargetWorkspace(targetStore.getWorkspace());
                }

                context.setData(newContext.getData());
                if (newContext.getData() != null) {
                    importer.init(context, true);
                }
            }

            context.reattach(importer.getCatalog(), true);
            getResponse().redirectSeeOther(getPageInfo().rootURI("/imports/"+context.getId()));
            getResponse().setEntity(getFormatGet().toRepresentation(context));
            getResponse().setStatus(Status.SUCCESS_CREATED);
            importer.changed(context);
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
                context = (ImportContext) obj;
                
                //if the import is empty, prep it but leave data as is
                if (context.getTasks().isEmpty()) {
                    importer.init(context, false);
                }

                Form query = getRequest().getResourceRef().getQueryAsForm();
                
                if (query.getNames().contains("async")) {
                    importer.runAsync(context, ImportFilter.ALL);
                } else {
                    importer.run(context);
                    // @todo revisit - if errors occur, they are logged. A second request
                    // is required to verify success
                }
                getResponse().setStatus(Status.SUCCESS_NO_CONTENT);
            } catch (Exception e) {
                throw new RestletException("Error occured executing import", Status.SERVER_ERROR_INTERNAL, e);
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

    private String getCurrentUser() {
        String user = null;
        Authentication authentication = null;
        try {
            authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null) {
                user = (String) authentication.getCredentials();
            }
        } catch (NoClassDefFoundError cnfe) {
            try {
                // @todo fix once upgraded to spring3
                Class clazz = Class.forName("org.springframework.security.core.context.SecurityContextHolder");
                Object context = clazz.getMethod("getContext").invoke(null);
                Object auth = context.getClass().getMethod("getAuthentication").invoke(context);
                user = (String) auth.getClass().getMethod("getCredentials").invoke(auth);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        if (user == null) {
            user = "anonymous";
        }
        return user;
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
                Form form = getRequest().getResourceRef().getQueryAsForm();
                if (form.getNames().contains("all")) {
                    return importer.getAllContexts();
                } else {
                    return importer.getContextsByUser(getCurrentUser());
                }
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

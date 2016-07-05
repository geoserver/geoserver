/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.geoserver.backuprestore.Backup;
import org.geoserver.backuprestore.RestoreExecutionAdapter;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.rest.format.ReflectiveJSONFormat;
import org.geoserver.rest.format.ReflectiveXMLFormat;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;

/**
 * REST resource for 
 * 
 * <pre>/br/restore[/&lt;restoreId&gt;]?expand=1</pre>
 * 
 * @author Alessio Fabiani, GeoSolutions
 *
 */
public class RestoreResource  extends BaseResource {

    public RestoreResource(Backup backupFacade) {
        super(backupFacade);
    }

    @Override
    protected List<DataFormat> createSupportedFormats(Request arg0, Response arg1) {
       
        List<DataFormat> formats = new ArrayList<DataFormat>();
        
        //JSON
        ReflectiveJSONFormat jsonFormat = new ReflectiveJSONFormat();
        intializeXStreamContext(jsonFormat.getXStream());
        formats.add(jsonFormat);

        //XML        
        ReflectiveXMLFormat xmlFormat = new ReflectiveXMLFormat();
        intializeXStreamContext(xmlFormat.getXStream());
        formats.add(xmlFormat);
        
        return formats;
    }

    @Override
    public boolean allowPost() {
        return true;
    }
    
    @Override
    public boolean allowPut() {
        return false;
    }
    
    
    @Override
    public boolean allowDelete() {
        return true;
    }

    @Override
    public void handleGet() {
        DataFormat formatGet = getFormatGet(false);
        
        Object lookupContext = lookupContext(true, false);
        if (lookupContext == null) {
            // this means a specific lookup failed
            getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
        } else {
            getResponse().setEntity(formatGet.toRepresentation(lookupContext));
        }
    }
    
    @Override
    public void handlePost() {
        Object obj = lookupContext(true, true);
        RestoreExecutionAdapter execution = null;
        if (obj instanceof RestoreExecutionAdapter) {
            Long executionId = ((RestoreExecutionAdapter) obj).getId();
            try {
                getBackupFacade().restartExecution(executionId);
            } catch (JobInstanceAlreadyCompleteException | NoSuchJobExecutionException
                    | NoSuchJobException | JobRestartException | JobParametersInvalidException e) {
                if (e instanceof JobInstanceAlreadyCompleteException) {
                    throw new RestletException(e.getMessage(), Status.CLIENT_ERROR_BAD_REQUEST, e);
                } else {
                    throw new RestletException("Error occurred executing restore: ", Status.SERVER_ERROR_INTERNAL, e);
                }
            }
        }
        else {
            execution = runRestore();
        }
        if (execution == null) {
            throw new RestletException("Error occurred executing restore.", Status.CLIENT_ERROR_BAD_REQUEST);
        }
    }
    
    @Override
    public void handleDelete() {
        DataFormat formatGet = getFormatGet(false);
        
        Object lookupContext = lookupContext(true, false);
        if (lookupContext == null) {
            // this means a specific lookup failed
            getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
        } else if (lookupContext instanceof RestoreExecutionAdapter) {
            Long executionId = ((RestoreExecutionAdapter) lookupContext).getId();
            try {
                getBackupFacade().abandonExecution(executionId);
            } catch (NoSuchJobExecutionException | JobExecutionAlreadyRunningException e) {
                if (e instanceof NoSuchJobExecutionException) {
                    throw new RestletException(e.getMessage(), Status.CLIENT_ERROR_BAD_REQUEST, e);
                } else {
                    throw new RestletException("Error occurred canceling backup: ", Status.SERVER_ERROR_INTERNAL, e);
                }
            }
            getResponse().redirectSeeOther(getPageInfo().rootURI("/br/restore/"+executionId));
            getResponse().setEntity(formatGet.toRepresentation(lookupContext));
            getResponse().setStatus(Status.SUCCESS_ACCEPTED);
        }
    }
    
    /**
     * 
     * @return
     */
    @SuppressWarnings("unused")
    protected RestoreExecutionAdapter runRestore() {
        RestoreExecutionAdapter execution = null;
        
        try {
            Form query = getRequest().getResourceRef().getQueryAsForm();
    
            if (MediaType.APPLICATION_JSON.equals(getRequest().getEntity().getMediaType()) || 
                    MediaType.APPLICATION_XML.equals(getRequest().getEntity().getMediaType())) {
                RestoreExecutionAdapter newExecution = (RestoreExecutionAdapter) getFormatPostOrPut().toObject(getRequest().getEntity());
    
                execution = getBackupFacade().runRestoreAsync(
                        newExecution.getArchiveFile(),
                        newExecution.getFilter(),
                        asParams(newExecution.getOptions()));
                
                LOGGER.log(Level.INFO, "Restore file started: " + newExecution.getArchiveFile());
    
                getResponse().redirectSeeOther(getPageInfo().rootURI("/br/restore/"+execution.getId()));
                getResponse().setEntity(getFormatGet().toRepresentation(execution));
                getResponse().setStatus(Status.SUCCESS_CREATED);
            } else {
                throw new IllegalArgumentException("Unknown Content-Type: " + getRequest().getEntity().getMediaType());
            }
        } catch (IllegalArgumentException iae) {
            throw new RestletException("Unable to perform restore: ", Status.CLIENT_ERROR_BAD_REQUEST, iae);
        } catch (Exception e) {
            throw new RestletException("Unable to perform restore: ", Status.SERVER_ERROR_INTERNAL, e);
        }
        
        return execution;
    }
    
    /**
     * 
     * @param allowAll
     * @param mustExist
     * @return
     */
    Object lookupContext(boolean allowAll, boolean mustExist) {
        String i = getAttribute("restoreId");
        if (i != null) {
            RestoreExecutionAdapter restoreExecution = null;
            try {
                restoreExecution = getBackupFacade().getRestoreExecutions().get(Long.parseLong(i));
            } catch (NumberFormatException e) {
            }
            if (restoreExecution == null && mustExist) {
                throw new RestletException("No such restore execution: " + i, Status.CLIENT_ERROR_NOT_FOUND);
            }
            return restoreExecution;
        }
        else {
            if (allowAll) {
                return getBackupFacade().getRestoreExecutions().entrySet().iterator();
            }
            throw new RestletException("No restore execution specified", Status.CLIENT_ERROR_BAD_REQUEST);
        }
    }

}

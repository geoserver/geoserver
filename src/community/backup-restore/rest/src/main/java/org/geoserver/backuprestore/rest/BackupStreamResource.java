/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.rest;

import java.util.Arrays;
import java.util.List;

import org.geoserver.backuprestore.Backup;
import org.geoserver.backuprestore.BackupExecutionAdapter;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.format.DataFormat;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.FileRepresentation;
import org.springframework.batch.core.BatchStatus;

/**
 * REST resource for 
 * 
 * <pre>/br/backup[/&lt;backupId&gt;].zip</pre>
 * 
 * @author Alessio Fabiani, GeoSolutions
 *
 */
public class BackupStreamResource  extends BaseResource {

    public BackupStreamResource(Backup backupFacade) {
        super(backupFacade);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    protected List<DataFormat> createSupportedFormats(Request request, Response response) {
        return (List) Arrays.asList();
    }
    
    @Override
    public boolean allowGet() {
        return true;
    }
    
    @Override
    public boolean allowPut() {
        return false;
    }
    
    @Override
    public boolean allowDelete() {
        return false;
    }
    
    @Override
    public boolean allowPost() {
        return false;
    }

    @Override
    public void handleGet() {
        Object lookupContext = lookupContext(false, true);
        if (lookupContext == null || !(lookupContext instanceof BackupExecutionAdapter)) {
            // this means a specific lookup failed
            getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
        } else {
            BackupExecutionAdapter execution = (BackupExecutionAdapter) lookupContext;
            if (execution.getStatus() == BatchStatus.COMPLETED) {
                getResponse().setEntity(new FileRepresentation(execution.getArchiveFile().file(), MediaType.APPLICATION_ZIP, 0));
            } else {
                throw new RestletException("Backup execution status not ready yet!", Status.CLIENT_ERROR_NOT_FOUND);
            }
        }
    }
    
    /**
     * 
     * @param allowAll
     * @param mustExist
     * @return
     */
    Object lookupContext(boolean allowAll, boolean mustExist) {
        String i = getAttribute("backupId");
        if (i != null) {
            BackupExecutionAdapter backupExecution = null;
            try {
                backupExecution = getBackupFacade().getBackupExecutions().get(Long.parseLong(i));
            } catch (NumberFormatException e) {
            }
            if (backupExecution == null && mustExist) {
                throw new RestletException("No such backup execution: " + i, Status.CLIENT_ERROR_NOT_FOUND);
            }
            return backupExecution;
        }
        else {
            if (allowAll) {
                return getBackupFacade().getBackupExecutions().entrySet().iterator();
            }
            throw new RestletException("No backup execution specified", Status.CLIENT_ERROR_BAD_REQUEST);
        }
    }
    
}

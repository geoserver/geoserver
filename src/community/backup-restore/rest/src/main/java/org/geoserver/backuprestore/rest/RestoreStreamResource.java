/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.rest;

import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.geoserver.backuprestore.Backup;
import org.geoserver.backuprestore.RestoreExecutionAdapter;
import org.geoserver.backuprestore.utils.BackupUtils;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.util.IOUtils;
import org.geotools.factory.Hints;
import org.geotools.filter.text.ecql.ECQL;
import org.opengis.filter.Filter;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Status;

/**
 * REST resource for 
 * 
 * <pre>/br/restore/{archivefile}.zip?options=BK_DRY_RUN</pre>
 * 
 * @author Alessio Fabiani, GeoSolutions
 *
 */
public class RestoreStreamResource  extends RestoreResource {

    public RestoreStreamResource(Backup backupFacade) {
        super(backupFacade);
    }
   
    @Override
    public boolean allowGet() {
        return false;
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
        return true;
    }

    @Override    
    public void handlePost() {
        doFileUpload();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void doFileUpload() {
        try {
            Form query = getRequest().getResourceRef().getQueryAsForm();
            
            Hints hints = null;
            final String options = query.getValues("options");
            if (options != null) {
                hints = new Hints(new HashMap(2));
                for (String option : options.split(",")) {
                    switch (option) {
                        case Backup.PARAM_BEST_EFFORT_MODE:
                            hints.add(new Hints(new Hints.OptionKey(Backup.PARAM_BEST_EFFORT_MODE), Backup.PARAM_BEST_EFFORT_MODE));
                            break;
                        case Backup.PARAM_DRY_RUN_MODE:
                            hints.add(new Hints(new Hints.OptionKey(Backup.PARAM_DRY_RUN_MODE), Backup.PARAM_DRY_RUN_MODE));
                            break;
                    }
                }
            }
            
            final String filter = query.getValues("filter");
            Filter wsFilter = null;
            if (filter != null) {
                try {
                    wsFilter = ECQL.toFilter(filter);
                } catch(Exception e) {
                    throw new RuntimeException(e);
                }
            }
            
            getResponse().setStatus(Status.SUCCESS_ACCEPTED);
            Resource directory = BackupUtils.tmpDir();

            if (LOGGER.isLoggable(Level.INFO)) {
                MediaType mediaType = getRequest().getEntity().getMediaType();
                LOGGER.info("PUT file: mimetype=" + mediaType + ", path=" + directory.path());
            }
            
            Resource archiveFile = IOUtils.handleBinUpload(getAttribute("restoreId") + ".zip", directory, false, getRequest());
            
            if (archiveFile != null && Resources.exists(archiveFile) && FileUtils.sizeOf(archiveFile.file())>0) {
                RestoreExecutionAdapter execution = getBackupFacade().runRestoreAsync(archiveFile, wsFilter, hints);
                
                LOGGER.log(Level.INFO, "Restore file started: " + execution.getArchiveFile());

                getResponse().redirectSeeOther(getPageInfo().rootURI("/br/restore/"+execution.getId()));
                getResponse().setEntity(getFormatGet().toRepresentation(execution));
                getResponse().setStatus(Status.SUCCESS_CREATED);                
            } else {
                throw new IOException("Unable to perform restore: could not read backup ZIP archive!");
            }
        } catch (IOException e) {
            throw new RestletException(e.getMessage(), Status.SERVER_ERROR_INTERNAL, e);
        }
    }
}

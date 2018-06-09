/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.web;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.backuprestore.AbstractExecutionAdapter;
import org.geoserver.backuprestore.BackupExecutionAdapter;
import org.geoserver.backuprestore.RestoreExecutionAdapter;
import org.geotools.util.logging.Logging;

/** @author Alessio Fabiani, GeoSolutions */
public class BackupRestoreExecutionModel<T extends AbstractExecutionAdapter>
        extends LoadableDetachableModel<AbstractExecutionAdapter> {

    static Logger LOGGER = Logging.getLogger(BackupRestoreExecutionModel.class);

    long id;

    private Class<T> clazz;

    public BackupRestoreExecutionModel(AbstractExecutionAdapter exec, Class<T> clazz) {
        this(exec.getId(), clazz);
    }

    public BackupRestoreExecutionModel(long id, Class<T> clazz) {
        this.id = id;
        this.clazz = clazz;
    }

    public Class<T> getType() {
        return this.clazz;
    }

    @Override
    protected AbstractExecutionAdapter load() {
        try {
            if (getType() == BackupExecutionAdapter.class) {
                return BackupRestoreWebUtils.backupFacade().getBackupExecutions().get(id);
            } else if (getType() == RestoreExecutionAdapter.class) {
                return BackupRestoreWebUtils.backupFacade().getRestoreExecutions().get(id);
            }
            return null;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Unable to load execution " + id, e);
            return null;
        }
    }
}

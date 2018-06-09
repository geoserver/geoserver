/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.wicket.model.IModel;
import org.geoserver.backuprestore.AbstractExecutionAdapter;
import org.geoserver.backuprestore.BackupExecutionAdapter;
import org.geoserver.backuprestore.RestoreExecutionAdapter;
import org.geoserver.web.wicket.GeoServerDataProvider;

/** @author Alessio Fabiani, GeoSolutions */
public class BackupRestoreExecutionsProvider<T>
        extends GeoServerDataProvider<AbstractExecutionAdapter> {
    public static Property<AbstractExecutionAdapter> ID = new BeanProperty("id", "id");
    public static Property<AbstractExecutionAdapter> STATE = new BeanProperty("state", "status");
    public static Property<AbstractExecutionAdapter> STARTED = new BeanProperty("started", "time");
    public static Property<AbstractExecutionAdapter> OPTIONS =
            new BeanProperty("options", "options");
    public static Property<AbstractExecutionAdapter> PROGRESS =
            new BeanProperty("progress", "progress");
    public static Property<AbstractExecutionAdapter> ARCHIVEFILE =
            new BeanProperty("archiveFile", "archiveFile");

    boolean sortByUpdated = false;
    private Class<T> clazz;

    public BackupRestoreExecutionsProvider(Class<T> clazz) {
        this(false, clazz);
    }

    public BackupRestoreExecutionsProvider(boolean sortByUpdated, Class<T> clazz) {
        this.sortByUpdated = sortByUpdated;
        this.clazz = clazz;
    }

    public Class<T> getType() {
        return this.clazz;
    }

    @Override
    protected List<Property<AbstractExecutionAdapter>> getProperties() {
        return Arrays.asList(ID, STATE, STARTED, PROGRESS, ARCHIVEFILE);
    }

    @Override
    protected List<AbstractExecutionAdapter> getItems() {
        if (getType() == BackupExecutionAdapter.class) {
            return new ArrayList<AbstractExecutionAdapter>(
                    BackupRestoreWebUtils.backupFacade().getBackupExecutions().values());
        } else if (getType() == RestoreExecutionAdapter.class) {
            return new ArrayList<AbstractExecutionAdapter>(
                    BackupRestoreWebUtils.backupFacade().getRestoreExecutions().values());
        }
        return null;
    }

    @Override
    protected IModel<AbstractExecutionAdapter> newModel(AbstractExecutionAdapter object) {
        return new BackupRestoreExecutionModel((AbstractExecutionAdapter) object, getType());
    }
}

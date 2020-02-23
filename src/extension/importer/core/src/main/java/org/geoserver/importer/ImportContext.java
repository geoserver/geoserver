/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import static org.geoserver.importer.ImporterUtils.resolve;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.importer.job.ProgressMonitor;
import org.geoserver.importer.transform.ImportTransform;
import org.geoserver.importer.transform.RasterTransform;
import org.geoserver.importer.transform.RasterTransformChain;
import org.geoserver.importer.transform.TransformChain;
import org.geoserver.importer.transform.VectorTransform;
import org.geoserver.importer.transform.VectorTransformChain;
import org.geotools.util.logging.Logging;

/**
 * Maintains state about an import.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class ImportContext implements Serializable {

    /** serialVersionUID */
    private static final long serialVersionUID = 8790675013874051197L;

    static final Logger LOGGER = Logging.getLogger(ImportContext.class);

    public static enum State {
        /**
         * Context is ready to be initialized, but still misses tasks. Used to create a context
         * while planning for an asynchronous initialization
         */
        INIT,
        /** Context init failed */
        INIT_ERROR,
        /** Context ready to be started */
        PENDING,
        /** Import is running */
        RUNNING,
        /** Import is complete */
        COMPLETE;
    }

    /** identifier */
    Long id;

    /** state */
    State state = State.PENDING;

    /** data source */
    ImportData data;

    /** target workspace for the import */
    WorkspaceInfo targetWorkspace;

    /** target store of the import */
    StoreInfo targetStore;

    /** import tasks */
    List<ImportTask> tasks = new ArrayList<ImportTask>();

    /** The default transformations that will be applied on task creation */
    List<ImportTransform> defaultTransforms = new ArrayList<>();

    /** id generator for task */
    int taskid = 0;

    /** date import was created */
    Date created;

    /** date import was finished */
    Date updated;

    /** credentials of creator */
    String user;

    /**
     * flag to control whether imported files (indirect) should be archived after import JD: this
     * used to be true by default, now false since by default importing a shapefile directly from
     * the local file system would result in the shapefile, and its parent directory being deleted
     */
    boolean archive = false;

    /** Used for error messages */
    String message;

    volatile ProgressMonitor progress;

    public ImportContext(long id) {
        this();
        this.id = id;
    }

    public ImportContext() {
        this.created = new Date();
        this.updated = new Date(created.getTime());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public Date getCreated() {
        return created;
    }

    public Date getUpdated() {
        return updated;
    }

    public ImportData getData() {
        return data;
    }

    public void setData(ImportData data) {
        this.data = data;
    }

    public WorkspaceInfo getTargetWorkspace() {
        return targetWorkspace;
    }

    public void setTargetWorkspace(WorkspaceInfo targetWorkspace) {
        this.targetWorkspace = targetWorkspace;
    }

    public StoreInfo getTargetStore() {
        return targetStore;
    }

    public void setTargetStore(StoreInfo targetStore) {
        this.targetStore = targetStore;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public boolean isArchive() {
        return archive;
    }

    public void setArchive(boolean archive) {
        this.archive = archive;
    }

    public List<ImportTask> getTasks() {
        return Collections.unmodifiableList(tasks);
    }

    public void addTask(ImportTask task) {
        task.setId(taskid++);
        task.setContext(this);
        this.tasks.add(task);

        // apply the default transformations
        TransformChain chain = task.getTransform();
        for (ImportTransform tx : defaultTransforms) {
            if (chain instanceof RasterTransformChain && tx instanceof RasterTransform) {
                chain.add(tx);
            } else if (chain instanceof VectorTransformChain && tx instanceof VectorTransform) {
                chain.add(tx);
            }
        }
    }

    public void removeTask(ImportTask task) {
        this.tasks.remove(task);
    }

    public ImportTask task(long id) {
        for (ImportTask t : tasks) {
            if (t.getId() == id) {
                return t;
            }
        }
        return null;
    }

    /**
     * Returns a live list with the default transform, can be modified directly to add/remove the
     * default transforms
     */
    public List<ImportTransform> getDefaultTransforms() {
        return defaultTransforms;
    }

    private void updateState() throws IOException {
        State newState;
        if (tasks.isEmpty()) {
            if (state == State.INIT) {
                newState = State.INIT;
            } else if (state == State.INIT_ERROR) {
                newState = State.INIT_ERROR;
            } else {
                newState = State.PENDING;
            }
        } else {
            newState = State.COMPLETE;
        }
        O:
        for (ImportTask task : tasks) {
            switch (task.getState()) {
                case COMPLETE:
                    continue;
                case RUNNING:
                    newState = State.RUNNING;
                    break O;
                default:
                    newState = State.PENDING;
                    break O;
            }
        }
        state = newState;

        final Directory directory = getUploadDirectory();

        if (state == ImportContext.State.COMPLETE) {
            if (!isDirect()) {
                // It seems like we can safely mark Import data as temporary data
                // Let's remove ".locking" file
                unlockUploadFolder(directory);
            }
        } else {
            lockUploadFolder(directory);
        }
    }

    /**
     * We are going to scan all the Import Context Tasks and return "true" if any "isDirect".
     *
     * <p>"isDirect" means that the Importer will rely on uploaded data position to create the
     * Store. The uploaded data should be preserved, otherwise the Layer will be broken.
     *
     * @return boolean
     */
    public boolean isDirect() {
        boolean isDirect =
                Iterables.any(
                        getTasks(),
                        new Predicate<ImportTask>() {
                            @Override
                            public boolean apply(ImportTask input) {
                                return input.isDirect();
                            }
                        });
        return isDirect;
    }

    /**
     * We are going to scan all the Import Context Tasks and return "true" if all of them do not
     * have a configured Layer on the Catalog.
     *
     * <p>That means that the user has removed the Layers from the Catalog and therefore we are safe
     * to wipe out the uploaded data.
     *
     * @return boolean
     */
    public boolean isEmpty() {
        boolean noLayersAvailable =
                Iterables.all(
                        getTasks(),
                        new Predicate<ImportTask>() {
                            @Override
                            public boolean apply(ImportTask input) {
                                final StoreInfo store = input != null ? input.getStore() : null;
                                final Catalog catalog = store != null ? store.getCatalog() : null;
                                final LayerInfo layer =
                                        catalog != null
                                                ? catalog.getLayer(input.getLayer().getId())
                                                : null;
                                return (layer == null);
                            }
                        });
        return noLayersAvailable;
    }

    /**
     * This method will write an empty ".locking" file into the uploaded data unique directory.
     *
     * <p>Whenever a ".locking" file is present, the scheduler won't wipe out the directory.
     * Otherwise the folder will be completely removed.
     */
    public void lockUploadFolder(Directory directory) throws IOException {
        if (directory != null) {
            File locking = new File(directory.getFile(), ".locking");
            if (!locking.exists()) {
                locking.createNewFile();
            }
        }
    }

    /**
     * This method will delete any ".locking" file present into the uploaded data unique directory.
     *
     * <p>Whenever a ".locking" file is present, the scheduler won't wipe out the directory.
     * Otherwise the folder will be completely removed.
     */
    public void unlockUploadFolder(Directory directory) {
        if (directory != null) {
            File locking = new File(directory.getFile(), ".locking");
            if (locking.exists()) {
                locking.delete();
                File cleanerMark = new File(directory.getFile(), ".clean-me");
                if (!cleanerMark.exists()) {
                    try {
                        cleanerMark.createNewFile();
                    } catch (IOException e) {
                        LOGGER.log(
                                Level.WARNING,
                                "It was not possible to set the directory '"
                                        + directory
                                        + "' eligible for cleaning!",
                                e);
                    }
                }
            }
        }
    }

    /**
     * This method will return the base upload folder configured for this Import Context.
     *
     * <p>The upload folder base can be configured in several ways:
     *
     * <p>1. A property into the geoserver-importer.properties file 2. A System Env variable
     *
     * <p>Default: a folder named "uploads" into the GEOSERVER_DATA_DIR/
     *
     * @return {@linkplain Directory}
     */
    public Directory getUploadDirectory() {
        Directory directory = null;
        if (getData() instanceof Directory) {
            directory = (Directory) getData();
        } else if (getData() instanceof SpatialFile) {
            directory = new Directory(((SpatialFile) getData()).getFile().getParentFile());
        }

        if (directory == null) {
            for (ImportTask task : tasks) {
                if (task.getData() instanceof Directory) {
                    directory = (Directory) task.getData();
                } else if (task.getData() instanceof SpatialFile) {
                    directory =
                            new Directory(((SpatialFile) task.getData()).getFile().getParentFile());
                }
            }
        }
        return directory;
    }

    public void updated() throws IOException {
        updated = new Date();
        updateState();
    }

    public void delete() throws IOException {
        for (ImportTask task : this.getTasks()) {
            if (task.getData() != null) {
                task.getData().cleanup();
            }
        }
    }

    public void reattach(Catalog catalog) {
        reattach(catalog, false);
    }

    public void reattach(Catalog catalog, boolean lookupByName) {
        if (data != null) {
            data.reattach();
        }

        if (targetWorkspace != null) {
            targetWorkspace = resolve(targetWorkspace, catalog, lookupByName);

            if (targetStore != null) {
                targetStore.setWorkspace(targetWorkspace);
            }
        }
        targetStore = resolve(targetStore, catalog, lookupByName);

        for (ImportTask task : tasks) {
            task.setContext(this);
            task.reattach(catalog, lookupByName);
        }
    }

    public ProgressMonitor progress() {
        if (progress == null) {
            progress = new ProgressMonitor();
        }
        return progress;
    }

    public void setProgress(ProgressMonitor progress) {
        this.progress = progress;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        ImportContext other = (ImportContext) obj;
        if (id == null) {
            if (other.id != null) return false;
        } else if (!id.equals(other.id)) return false;
        return true;
    }

    private Object readResolve() {
        if (tasks == null) {
            tasks = new ArrayList<>();
        }
        if (defaultTransforms == null) {
            defaultTransforms = new ArrayList<>();
        }
        return this;
    }

    /**
     * Returns the current context message, if any
     *
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the context message
     *
     * @param message the message to set
     */
    public void setMessage(String message) {
        this.message = message;
    }
}

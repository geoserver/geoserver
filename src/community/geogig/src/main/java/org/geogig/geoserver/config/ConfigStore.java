/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.config;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.thoughtworks.xstream.XStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.eclipse.jdt.annotation.Nullable;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceListener;
import org.geoserver.platform.resource.ResourceNotification;
import org.geoserver.platform.resource.ResourceNotification.Event;
import org.geoserver.platform.resource.ResourceNotificationDispatcher;
import org.geoserver.platform.resource.ResourceStore;
import org.geotools.util.logging.Logging;

/**
 * Handles storage for {@link RepositoryInfo}s inside the GeoServer data directory's {@code
 * geogig/config/repos/} subdirectory.
 *
 * <p>{@link RepositoryInfo} instances are created through its default constructor, which assigns a
 * {@code null} id, meaning its a new instance and has not yet being saved.
 *
 * <p>Persistence is handled with {@link XStream} on a one file per {@code RepositoryInfo} bases
 * under {@code <data-dir>/geogig/config/repos/}, named {@code RepositoryInfo.getId()+".xml"}.
 *
 * <p>{@link #save(RepositoryInfo)} sets an id on new instances, which is the String representation
 * of a random {@link UUID}.
 *
 * <p>{@code RepositoryInfo} instances deserialized from XML have its id set by {@link XStream}, and
 * {@link #save(RepositoryInfo)} knows its an existing instance and replaces its file.
 */
public class ConfigStore {

    private static final Logger LOGGER = Logging.getLogger(ConfigStore.class);

    private static final Predicate<Resource> FILENAMEFILTER = (res) -> res.name().endsWith(".xml");

    public static interface RepositoryInfoChangedCallback {
        public void repositoryInfoChanged(String repoId);
    }

    /** Regex pattern to assert the format of ids on {@link #save(RepositoryInfo)} */
    public static final Pattern UUID_PATTERN =
            Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

    private static final String CONFIG_DIR_NAME = "geogig/config/repos";

    private ResourceStore resourceStore;

    private final ReadWriteLock lock;

    private Queue<RepositoryInfoChangedCallback> callbacks;

    private ConcurrentMap<String, RepositoryInfo> infosById = new ConcurrentHashMap<>();

    public ConfigStore(ResourceStore resourceLoader) {
        checkNotNull(resourceLoader, "resourceLoader");
        this.resourceStore = resourceLoader;
        if (resourceLoader.get(CONFIG_DIR_NAME) == null) {
            throw new IllegalStateException("Unable to create config directory " + CONFIG_DIR_NAME);
        }
        this.lock = new ReentrantReadWriteLock();
        this.callbacks = new ConcurrentLinkedQueue<RepositoryInfoChangedCallback>();
        preload();

        ResourceNotificationDispatcher dispatcher;
        dispatcher = resourceLoader.getResourceNotificationDispatcher();

        dispatcher.addListener(
                CONFIG_DIR_NAME,
                new ResourceListener() {
                    @Override
                    public void changed(ResourceNotification notify) {
                        for (Event event : notify.events()) {
                            final String repoId;
                            {
                                final String path =
                                        event.getPath().startsWith(CONFIG_DIR_NAME)
                                                ? event.getPath()
                                                : CONFIG_DIR_NAME + "/" + event.getPath();
                                repoId = idFromPath(path);
                            }
                            final String thread = Thread.currentThread().getName();
                            if (LOGGER.isLoggable(Level.FINER)) {
                                LOGGER.finer(
                                        "Processing event "
                                                + event.getKind()
                                                + " for repo "
                                                + repoId
                                                + " on thread "
                                                + thread);
                            }
                            switch (event.getKind()) {
                                case ENTRY_CREATE:
                                    {
                                        // ResourceStore may issue entry_create before or after the
                                        // contents where
                                        // written, but it'll always issue an entry_modify event
                                        // just after the
                                        // contents are written, so we ignore the create events and
                                        // wait for the
                                        // modify events to avoid double processing
                                        if (LOGGER.isLoggable(Level.FINE)) {
                                            LOGGER.fine(
                                                    "Received an ENTRY_CREATE event for repo id "
                                                            + repoId
                                                            + " Event ignored, waiting for the ENTRY_MODIFY event to process it");
                                        }
                                        break;
                                    }
                                case ENTRY_MODIFY:
                                    {
                                        lock.writeLock().lock();
                                        try {
                                            get(repoId);
                                        } catch (NoSuchElementException e) {
                                            LOGGER.info(
                                                    "Error loading repo "
                                                            + repoId
                                                            + " upon event "
                                                            + event
                                                            + ". Thread: "
                                                            + thread);
                                            Preconditions.checkState(
                                                    !infosById.containsKey(repoId));
                                            // ignore, exception already logged by get(), but call
                                            // repositoryInfoChanged for
                                            // RepositoryManager to close the live Repository
                                        } finally {
                                            lock.writeLock().unlock();
                                        }
                                        repositoryInfoChanged(repoId);
                                        break;
                                    }
                                case ENTRY_DELETE:
                                    infosById.remove(repoId);
                                    repositoryInfoChanged(repoId);
                                    break;
                            }
                            if (LOGGER.isLoggable(Level.FINER)) {
                                LOGGER.finer(
                                        "Finished processing event "
                                                + event.getKind()
                                                + " for repo "
                                                + repoId
                                                + " on thread "
                                                + thread);
                            }
                        }
                    }
                });
    }

    /**
     * Add a callback that will be called whenever a RepositoryInfo is changed.
     *
     * @param callback the callback
     */
    public void addRepositoryInfoChangedCallback(RepositoryInfoChangedCallback callback) {
        this.callbacks.add(callback);
    }

    /**
     * Remove a callback that was previously added to the config store.
     *
     * @param callback the callback
     */
    public void removeRepositoryInfoChangedCallback(RepositoryInfoChangedCallback callback) {
        this.callbacks.remove(callback);
    }

    /**
     * Saves a {@link RepositoryInfo} to its {@code <data-dir>/geogig/config/repos/<id>.xml} file.
     *
     * <p>If {@code info} has no id set, one is assigned, meaning it didn't yet exist. Otherwise its
     * xml file is replaced meaning it has been modified.
     *
     * @return {@code info}, possibly with its id set if it was {@code null}
     * @implNote this method saves the {@link RepositoryInfo} to the internal cache, setting its
     *     {@link RepositoryInfo#getLastModified() timestamp} to {@code -1}, meaning it was just
     *     saved on this node. The resourcestore event listener will not reload it but just update
     *     its timestamp, while on other nodes it'll load/reload it as needed. This ensures on this
     *     node the {@link RepositoryInfo} will be available right after this method returns,
     *     regardless of how long it takes the resource store event dispatcher to notify this or
     *     other nodes.
     */
    public RepositoryInfo save(RepositoryInfo info) {
        checkNotNull(info, "null RepositoryInfo");
        ensureIdPresent(info);
        checkNotNull(info.getLocation(), "null location URI: %s", info);

        Resource resource = resource(info.getId());
        lock.writeLock().lock();
        try (OutputStream out = resource.out()) {
            info.setLastModified(-1L); // meaning just saved on this node
            infosById.put(info.getId(), info);
            getConfigredXstream().toXML(info, new OutputStreamWriter(out, Charsets.UTF_8));
        } catch (IOException e) {
            throw Throwables.propagate(e);
        } finally {
            lock.writeLock().unlock();
        }
        return info;
    }

    public boolean delete(final String id) {
        checkNotNull(id, "provided a null id");
        checkIdFormat(id);
        lock.writeLock().lock();
        try {
            infosById.remove(id);
            return resource(id).delete();
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void checkIdFormat(final String id) {
        checkArgument(UUID_PATTERN.matcher(id).matches(), "Id doesn't match UUID format: '%s'", id);
    }

    private void ensureIdPresent(RepositoryInfo info) {
        String id = info.getId();
        if (id == null) {
            id = UUID.randomUUID().toString();
            info.setId(id);
        } else {
            checkIdFormat(id);
        }
    }

    private Resource resource(String id) {
        Resource resource = resourceStore.get(path(id));
        return resource;
    }

    public Resource getConfigRoot() {
        return resourceStore.get(CONFIG_DIR_NAME);
    }

    static String path(String infoId) {
        return Paths.path(CONFIG_DIR_NAME, infoId + ".xml");
    }

    static String idFromPath(String path) {
        List<String> names = Paths.names(path);
        String resourceName = names.get(names.size() - 1);
        return resourceName.substring(0, resourceName.length() - ".xml".length());
    }

    private RepositoryInfo loadInfo(Resource resource) throws IllegalStateException {
        RepositoryInfo info;
        try (Reader reader = new InputStreamReader(resource.in(), Charsets.UTF_8)) {
            info = (RepositoryInfo) getConfigredXstream().fromXML(reader);
            if (info.getLocation() == null) {
                throw new IllegalStateException(
                        "Repository info has incomplete information: " + info);
            }
            info.setLastModified(resource.lastmodified());
        } catch (Exception e) {
            // the contract for Resource.in() is not clear on what to expect but both the
            // FileSystem
            // and Redis implementations throw IllegalStateException for well known causes like
            // resource not existing or being a directory.
            Throwables.propagateIfInstanceOf(e, IllegalStateException.class);
            String msg = "Unable to load repo config " + resource.name();
            LOGGER.log(Level.WARNING, msg, e);
            throw new IllegalStateException(msg, e);
        }
        return info;
    }

    private void repositoryInfoChanged(String repoId) {
        for (RepositoryInfoChangedCallback callback : callbacks) {
            callback.repositoryInfoChanged(repoId);
        }
    }

    /**
     * Loads and returns all <b>valid</b> {@link RepositoryInfo}'s from {@code
     * <data-dir>/geogig/config/repos/}; any xml file that can't be parsed is ignored.
     */
    public List<RepositoryInfo> getRepositories() {
        lock.readLock().lock();
        try {
            return ImmutableList.copyOf(infosById.values());
        } finally {
            lock.readLock().unlock();
        }
    }

    private void preload() {
        List<RepositoryInfo> repos = loadRepositories();
        repos.forEach((info) -> infosById.put(info.getId(), info));
    }

    /** Loads and returns all {@link RepositoryInfo}s directly from the {@link ResourceStore} */
    private List<RepositoryInfo> loadRepositories() {
        Resource configRoot = getConfigRoot();
        List<Resource> resources = configRoot.list();
        if (null == resources || resources.isEmpty()) {
            return Collections.emptyList();
        }

        List<RepositoryInfo> infos =
                resources //
                        .parallelStream() //
                        .filter(FILENAMEFILTER) //
                        .map(
                                (res) -> {
                                    try {
                                        return loadInfo(res);
                                    } catch (RuntimeException e) {
                                        LOGGER.log(Level.INFO, "Ignoring malformed resource", e);
                                    }
                                    return null;
                                }) //
                        .filter(Objects::nonNull) //
                        .collect(Collectors.toList());

        return infos;
    }

    /** Loads the security whitelist. */
    public List<WhitelistRule> getWhitelist() throws IOException {
        lock.readLock().lock();
        try {
            Resource resource = whitelistResource();
            return loadWhitelist(resource);
        } finally {
            lock.readLock().unlock();
        }
    }

    private Resource whitelistResource() {
        return resourceStore.get("geogig/config/whitelist.xml");
    }

    private static List<WhitelistRule> loadWhitelist(Resource input) throws IOException {
        Resource parent = input.parent();
        if (!(parent.getType().equals(Resource.Type.DIRECTORY)
                && input.getType().equals(Resource.Type.RESOURCE))) {
            return newArrayList();
        }
        try (Reader reader = new InputStreamReader(input.in(), Charsets.UTF_8)) {
            return (List<WhitelistRule>) getConfigredXstream().fromXML(reader);
        } catch (Exception e) {
            String msg = "Unable to load whitelist " + input.name();
            LOGGER.log(Level.WARNING, msg, e);
            throw new IOException(msg, e);
        }
    }

    /** Saves the security whitelist. */
    public List<WhitelistRule> saveWhitelist(List<WhitelistRule> whitelist) {
        checkNotNull(whitelist);
        lock.writeLock().lock();
        try (OutputStream out = whitelistResource().out()) {
            getConfigredXstream().toXML(whitelist, new OutputStreamWriter(out, Charsets.UTF_8));
        } catch (IOException e) {
            throw Throwables.propagate(e);
        } finally {
            lock.writeLock().unlock();
        }
        return whitelist;
    }

    /**
     * Loads a {@link RepositoryInfo} by {@link RepositoryInfo#getId() id} from its xml file under
     * {@code <data-dir>/geogig/config/repos/}
     *
     * @implNote Ensures to return the most current version of the {@link RepositoryInfo} by
     *     comparing its {@link RepositoryInfo#getLastModified()} with the {@link
     *     Resource#lastmodified()}, and reloading in case it was cached but the timestamps don't
     *     match
     */
    public RepositoryInfo get(final String id) throws NoSuchElementException {
        checkNotNull(id, "provided a null id");
        checkIdFormat(id);
        lock.readLock().lock();
        try {
            Resource resource = resource(id);
            final @Nullable RepositoryInfo cached = infosById.get(id);
            final long currentTimeStamp = resource.lastmodified();
            final long cachedTimestamp = cached == null ? Long.MIN_VALUE : cached.getLastModified();
            RepositoryInfo info;
            if (currentTimeStamp != cachedTimestamp) {
                if (0L == currentTimeStamp) {
                    throw new NoSuchElementException("Repository not found: " + id);
                } else {
                    if (-1L == cachedTimestamp) {
                        cached.setLastModified(currentTimeStamp);
                        info = cached;
                    } else {
                        try {
                            info = loadInfo(resource);
                            infosById.put(info.getId(), info);
                        } catch (IllegalStateException failed) {
                            infosById.remove(id);
                            throw failed;
                        }
                    }
                }
            } else {
                info = cached;
            }
            return info;
        } catch (RuntimeException e) {
            Throwables.propagateIfInstanceOf(e, NoSuchElementException.class);
            NoSuchElementException nse = new NoSuchElementException(e.getMessage());
            nse.initCause(e);
            throw nse;
        } finally {
            lock.readLock().unlock();
        }
    }

    public @Nullable RepositoryInfo getByName(final String name) {
        checkNotNull(name);
        Optional<RepositoryInfo> match =
                infosById
                        .values()
                        .parallelStream()
                        .filter((info) -> name.equals(info.getRepoName()))
                        .findFirst();

        return match.orElse(null);
    }

    public boolean repoExistsByName(String name) {
        checkNotNull(name);
        Optional<RepositoryInfo> match =
                infosById
                        .values()
                        .parallelStream()
                        .filter((info) -> name.equals(info.getRepoName()))
                        .findFirst();

        return match.isPresent();
    }

    public @Nullable RepositoryInfo getByLocation(final URI location) {
        checkNotNull(location);
        Optional<RepositoryInfo> match =
                infosById
                        .values()
                        .parallelStream()
                        .filter((info) -> location.equals(info.getLocation()))
                        .findFirst();

        return match.orElse(null);
    }

    public boolean repoExistsByLocation(URI location) {
        checkNotNull(location);
        Optional<RepositoryInfo> match =
                infosById
                        .values()
                        .parallelStream()
                        .filter((info) -> location.equals(info.getLocation()))
                        .findFirst();

        return match.isPresent();
    }

    /**
     * According to http://x-stream.github.io/faq.html#Scalability_Thread_safety, XStream instances
     * are thread safe and it's recommended to share them as they're pretty expensive to create
     */
    private static final XStream xStream = new XStream();

    static {
        xStream.alias("RepositoryInfo", RepositoryInfo.class);
    }

    private static XStream getConfigredXstream() {
        return xStream;
    }
}

/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.config;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterators.filter;
import static com.google.common.collect.Lists.newArrayList;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceListener;
import org.geoserver.platform.resource.ResourceNotification;
import org.geoserver.platform.resource.ResourceNotification.Event;
import org.geoserver.platform.resource.ResourceNotificationDispatcher;
import org.geoserver.platform.resource.ResourceStore;
import org.geotools.util.logging.Logging;
import org.locationtech.geogig.repository.RepositoryResolver;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.thoughtworks.xstream.XStream;

/**
 * Handles storage for {@link RepositoryInfo}s inside the GeoServer data directory's
 * {@code geogig/config/repos/} subdirectory.
 * <p>
 * {@link RepositoryInfo} instances are created through its default constructor, which assigns a
 * {@code null} id, meaning its a new instance and has not yet being saved.
 * <p>
 * Persistence is handled with {@link XStream} on a one file per {@code RepositoryInfo} bases under
 * {@code <data-dir>/geogig/config/repos/}, named {@code RepositoryInfo.getId()+".xml"}.
 * <p>
 * {@link #save(RepositoryInfo)} sets an id on new instances, which is the String representation of
 * a random {@link UUID}.
 * <p>
 * {@code RepositoryInfo} instances deserialized from XML have its id set by {@link XStream}, and
 * {@link #save(RepositoryInfo)} knows its an existing instance and replaces its file.
 * 
 *
 */
public class ConfigStore {

    private static final Logger LOGGER = Logging.getLogger(ConfigStore.class);

    /**
     * Regex pattern to assert the format of ids on {@link #save(RepositoryInfo)}
     */
    public static final Pattern UUID_PATTERN = Pattern
            .compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

    private static final String CONFIG_DIR_NAME = "geogig/config/repos";

    private ResourceStore resourceLoader;

    private final ReadWriteLock lock;

    private Queue<RepositoryInfoChangedCallback> callbacks;

    /**
     * Map of cached {@link RepositoryInfo} instances key'ed by id
     */
    private ConcurrentMap<String, RepositoryInfo> cache = new ConcurrentHashMap<>();

    public ConfigStore(ResourceStore resourceLoader) {
        checkNotNull(resourceLoader, "resourceLoader");
        this.resourceLoader = resourceLoader;
        if (resourceLoader.get(CONFIG_DIR_NAME) == null) {
            throw new IllegalStateException("Unable to create config directory " + CONFIG_DIR_NAME);
        }
        this.lock = new ReentrantReadWriteLock();
        this.callbacks = new ConcurrentLinkedQueue<RepositoryInfoChangedCallback>();
        populateCache();

        ResourceNotificationDispatcher dispatcher = resourceLoader
                .getResourceNotificationDispatcher();
        dispatcher.addListener(CONFIG_DIR_NAME, new ResourceListener() {
            @Override
            public void changed(ResourceNotification notify) {
                for (Event event : notify.events()) {
                    String path = event.getPath().startsWith(CONFIG_DIR_NAME) ? event.getPath()
                            : CONFIG_DIR_NAME + "/" + event.getPath();
                    String repoId = idFromPath(path);
                    switch (event.getKind()) {
                    case ENTRY_CREATE:
                    case ENTRY_MODIFY:
                        cache.remove(repoId);
                        loadResource(resourceLoader.get(path));
                        break;
                    case ENTRY_DELETE:
                        cache.remove(repoId);
                        break;
                    }
                    repositoryInfoChanged(repoId);
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
     * <p>
     * If {@code info} has no id set, one is assigned, meaning it didn't yet exist. Otherwise its
     * xml file is replaced meaning it has been modified.
     * 
     * @return {@code info}, possibly with its id set if it was {@code null}
     * 
     */
    public RepositoryInfo save(RepositoryInfo info) {
        checkNotNull(info, "null RepositoryInfo");
        ensureIdPresent(info);
        checkNotNull(info.getLocation(), "null location URI: %s", info);

        lock.writeLock().lock();
        Resource resource = resource(info.getId());
        try (OutputStream out = resource.out()) {
            getConfigredXstream().toXML(info, new OutputStreamWriter(out, Charsets.UTF_8));
            cache.put(info.getId(), info);
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
            cache.remove(id);
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
        Resource resource = resourceLoader.get(path(id));
        return resource;
    }

    public Resource getConfigRoot() {
        return resourceLoader.get(CONFIG_DIR_NAME);
    }

    static String path(String infoId) {
        return Paths.path(CONFIG_DIR_NAME, infoId + ".xml");
    }

    static String idFromPath(String path) {
        List<String> names = Paths.names(path);
        String resourceName = names.get(names.size() - 1);
        return resourceName.substring(0, resourceName.length() - 4);
    }

    @VisibleForTesting
    void populateCache() {
        cache.clear();
        Resource configRoot = getConfigRoot();
        List<Resource> list = configRoot.list();
        if (null == list) {
            return;
        }
        Iterator<Resource> xmlfiles = filter(list.iterator(), FILENAMEFILTER);
        while (xmlfiles.hasNext()) {
            loadResource(xmlfiles.next());
        }
    }

    private void loadResource(Resource resource) {
        try {
            RepositoryInfo info = load(resource);
            if (info != null) {
                cache.put(info.getId(), info);
            }
        } catch (IOException e) {
            // log the bad info
            LOGGER.log(Level.WARNING, "Error loading RepositoryInfo", e);
        }
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
        return newArrayList(cache.values());
    }

    /**
     * Loads the security whitelist.
     */
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
        return resourceLoader.get("geogig/config/whitelist.xml");
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

    /**
     * Saves the security whitelist.
     */
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
     */
    public RepositoryInfo get(final String id) throws IOException {
        checkNotNull(id, "provided a null id");
        checkIdFormat(id);
        lock.readLock().lock();
        try {
            RepositoryInfo info = cache.get(id);
            if (info == null) {
                throw new FileNotFoundException("Repository not found: " + id);
            }
            return info;
        } finally {
            lock.readLock().unlock();
        }
    }

    public RepositoryInfo getByName(final String name) {
        for (RepositoryInfo cached : cache.values()) {
            final String cachedRepoName = cached.getRepoName();
            if (cachedRepoName != null && cachedRepoName.equals(name)) {
                return cached;
            }
        }
        return null;
    }

    public RepositoryInfo getByLocation(final URI location) {
        for (RepositoryInfo cached : cache.values()) {
            if (cached.getLocation().equals(location)) {
                return cached;
            }
        }
        return null;
    }

    private static RepositoryInfo load(Resource input) throws IOException {
        Resource parent = input.parent();
        if (!(parent.getType().equals(Resource.Type.DIRECTORY)
                && input.getType().equals(Resource.Type.RESOURCE))) {
            throw new FileNotFoundException("File not found: " + input.path());
        }
        RepositoryInfo info;
        try (Reader reader = new InputStreamReader(input.in(), Charsets.UTF_8)) {
            info = (RepositoryInfo) getConfigredXstream().fromXML(reader);
        } catch (Exception e) {
            String msg = "Unable to load repo config " + input.name();
            LOGGER.log(Level.WARNING, msg, e);
            throw new IOException(msg, e);
        }
        if (info.getLocation() == null) {
            throw new IOException("Repository info has incomplete information: " + info);
        }
        // we have a location, make sure we have a resolver for the scheme
        if (!RepositoryResolver.resolverAvailableForURIScheme(info.getLocation().getScheme())) {
            LOGGER.log(Level.WARNING, "No RepositoryResolver available for URI: {0}",
                    info.getLocation().toString());
            return null;
        }
        return info;
    }

    private static XStream getConfigredXstream() {
        XStream xStream = new XStream();
        xStream.alias("RepositoryInfo", RepositoryInfo.class);
        return xStream;
    }

    private static final Predicate<Resource> FILENAMEFILTER = new Predicate<Resource>() {
        @Override
        public boolean apply(Resource input) {
            return input.name().endsWith(".xml");
        }
    };

    public static abstract class RepositoryInfoChangedCallback {
        public abstract void repositoryInfoChanged(String repoId);
    }
}

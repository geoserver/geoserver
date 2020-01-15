/* (c) 2014-2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import static java.util.Optional.ofNullable;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.opengis.wfs20.StoredQueryDescriptionType;
import org.apache.commons.lang3.StringUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.ows.LocalWorkspace;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.exception.GeoServerRuntimException;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geotools.util.logging.Logging;
import org.geotools.wfs.v2_0.WFS;
import org.geotools.wfs.v2_0.WFSConfiguration;
import org.geotools.xsd.Encoder;
import org.geotools.xsd.Parser;

/**
 * Extension point for WFS stored queries.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class StoredQueryProvider {

    /**
     * language(S) for this provider (the name changed across specs versions, apparently before the
     * 2.0 spec was published)
     */
    public static String LANGUAGE_20_PRE = "urn:ogc:def:queryLanguage:OGC-WFS::WFS_QueryExpression";

    public static String LANGUAGE_20 = "urn:ogc:def:queryLanguage:OGC-WFS::WFSQueryExpression";

    /** logger */
    static Logger LOGGER = Logging.getLogger(StoredQueryProvider.class);

    /** catalog */
    Catalog catalog;

    /** file system access */
    GeoServerResourceLoader loader;

    final WFSInfo wfsInfo;

    final boolean allowPerWorkspace;
    final String workspaceName;

    public StoredQueryProvider(Catalog catalog) {
        this(catalog, null, false, null);
    }

    public StoredQueryProvider(Catalog catalog, WFSInfo wfsInfo, boolean allowPerWorkspace) {
        this(catalog, wfsInfo, allowPerWorkspace, null);
    }

    public StoredQueryProvider(
            Catalog catalog, WFSInfo wfsInfo, boolean allowPerWorkspace, String workspaceName) {
        this.catalog = catalog;
        this.loader = catalog.getResourceLoader();
        this.wfsInfo = wfsInfo;
        this.allowPerWorkspace = allowPerWorkspace;
        this.workspaceName = workspaceName;
    }

    /** The language/type of stored query the provider handles. */
    public String getLanguage() {
        return LANGUAGE_20;
    }

    /** Lists all the stored queries provided. */
    public List<StoredQuery> listStoredQueries() {
        final List<StoredQuery> queries = new ArrayList<>();

        // add the default as mandated by spec
        queries.add(StoredQuery.DEFAULT);

        // add user created ones
        final Resource dir = storedQueryDir();
        final List<StoredQuery> globalQueries = getStoredQueryByResource(dir);
        final List<StoredQuery> localQueries = getLocalWorkspaceStoredQueries();
        // add all global queries don't collide with local ones names
        if (shouldProcessGlobalQueries()) {
            globalQueries
                    .stream()
                    .filter(q -> checkQueryNotExists(q, localQueries))
                    .forEach(q -> queries.add(q));
        }
        // add available local queries
        queries.addAll(localQueries);

        return queries;
    }

    private boolean checkQueryNotExists(StoredQuery query, List<StoredQuery> localQueries) {
        return localQueries
                .stream()
                .noneMatch(local -> Objects.equals(query.getName(), local.getName()));
    }

    private boolean shouldProcessGlobalQueries() {
        // if executed in global
        if (getLocalWorkspace() == null) {
            return true;
        } else {
            // is executed inside a local workspace
            return isGlobalQueriesAllowedOnLocalWorkspace();
        }
    }

    /**
     * Creates a new stored query.
     *
     * @param query The stored query definition.
     */
    public StoredQuery createStoredQuery(StoredQueryDescriptionType query) {
        return createStoredQuery(query, true);
    }

    /**
     * Creates a new stored query specifying whether to persist the query to disk or not.
     *
     * @param query The stored query definition.
     * @param store Whether to persist the query or not.
     */
    public StoredQuery createStoredQuery(StoredQueryDescriptionType query, boolean store) {
        StoredQuery sq = new StoredQuery(query, catalog);
        if (store) {
            putStoredQuery(sq);
        }
        return sq;
    }

    /**
     * Removes an existing stored query.
     *
     * @param query The stored query
     */
    public void removeStoredQuery(StoredQuery query) {
        final String filename = toFilename(query.getName());
        Resource resource = storedQueryDirByContext().get(filename);
        // resource exists? Delete it
        if (resource.getType() == Type.RESOURCE) {
            resource.delete();
        } else {
            // resource doesn't exists
            // are we inside a virtual service and global query exists ?
            if (localWorkspaceDir() != null
                    && storedQueryDir().get(filename).getType() == Type.RESOURCE) {
                throw new GeoServerRuntimException(
                        "Global query can not be deleted from a virtual service.");
            }
        }
    }

    /** Removes all stored queries. */
    public void removeAll() {
        for (Resource file : storedQueryDirByContext().list()) {
            file.delete();
        }
    }

    /**
     * Retrieves a stored query by name.
     *
     * @param name Identifying name of the stored query.
     */
    public StoredQuery getStoredQuery(String name) {
        // default?
        if (StoredQuery.DEFAULT.getName().equals(name)) {
            return StoredQuery.DEFAULT;
        }

        try {
            Resource res = getResourceByContext(toFilename(name));

            if (res == null || res.getType() != Type.RESOURCE) {
                return null;
            }

            return parseStoredQuery(res);
        } catch (Exception e) {
            throw new RuntimeException("Error accessign stoed query: " + name, e);
        }
    }

    private Resource getResourceByContext(String filename) {
        Resource res = storedQueryDirByContext().get(filename);
        if (res.getType() == Type.RESOURCE) {
            return res;
        }
        // if local workspace was evaluated, now check on global one if allowed
        if (getLocalWorkspace() != null && isGlobalQueriesAllowedOnLocalWorkspace()) {
            res = storedQueryDir().get(filename);
            if (res.getType() == Type.RESOURCE) {
                return res;
            }
        }
        return null;
    }

    /**
     * Persists a stored query, overwriting it if the query already exists.
     *
     * @param query The stored query.
     */
    public void putStoredQuery(StoredQuery query) {
        try {
            Resource dir = storedQueryDirByContext();
            Resource f = dir.get(toFilename(query.getName()));
            // if (f.getType() != Type.UNDEFINED) {
            // TODO: back up the old file in case there is an error during encoding
            // }

            BufferedOutputStream bout = new BufferedOutputStream(f.out());
            try {
                Encoder e = new Encoder(new WFSConfiguration());
                e.setRootElementType(WFS.StoredQueryDescriptionType);
                e.encode(
                        query.getQuery(),
                        WFS.StoredQueryDescription,
                        new BufferedOutputStream(bout));
                bout.flush();
            } finally {
                bout.close();
            }
        } catch (IOException e) {
            throw new RuntimeException("i/o error listing stored queries", e);
        }
    }

    String toFilename(String name) {
        // remove any special characters... like ':'
        return name.replaceAll("\\W", "") + ".xml";
    }

    Resource storedQueryDir() {
        return loader.get("wfs/query");
    }

    StoredQuery parseStoredQuery(Resource file) throws Exception {
        return parseStoredQuery(file, new Parser(new WFSConfiguration()));
    }

    StoredQuery parseStoredQuery(Resource file, Parser p) throws Exception {
        p.setRootElementType(WFS.StoredQueryDescriptionType);
        InputStream fin = file.in();
        try {
            StoredQueryDescriptionType q =
                    (StoredQueryDescriptionType) p.parse(new BufferedInputStream(fin));
            return createStoredQuery(q, false);
        } finally {
            fin.close();
        }
    }

    public boolean supportsLanguage(String language) {
        return LANGUAGE_20.equalsIgnoreCase(language) || LANGUAGE_20_PRE.equalsIgnoreCase(language);
    }

    /**
     * Provides the current local workspace name for the OWS request.
     *
     * @return The workspace name, or NULL if local workspace is not available.
     */
    private String getLocalWorkspace() {
        if (!allowPerWorkspace) return null;
        if (workspaceName != null) return workspaceName;
        return ofNullable(LocalWorkspace.get()).map(WorkspaceInfo::getName).orElse(null);
    }

    private List<StoredQuery> getLocalWorkspaceStoredQueries() {
        Resource dir = localWorkspaceStoredQueryDir();
        if (dir == null) {
            return Collections.emptyList();
        }
        return getStoredQueryByResource(dir);
    }

    private List<StoredQuery> getStoredQueryByResource(Resource dir) {
        Objects.requireNonNull(dir);
        final Parser p = new Parser(new WFSConfiguration());
        final List<StoredQuery> queries = new ArrayList<>();
        for (Resource f : dir.list()) {
            try {
                queries.add(parseStoredQuery(f, p));
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error occured parsing stored query: " + f, e);
            }
        }
        return queries;
    }

    private String localWorkspaceDir() {
        final String localWorkspace = getLocalWorkspace();
        if (StringUtils.isBlank(localWorkspace)) {
            return null;
        }
        return "workspaces/" + localWorkspace + "/wfs/query";
    }

    private Resource localWorkspaceStoredQueryDir() {
        String localWorkspaceDir = localWorkspaceDir();
        if (StringUtils.isBlank(localWorkspaceDir)) return null;
        return loader.get(localWorkspaceDir);
    }

    private Resource storedQueryDirByContext() {
        return ofNullable(localWorkspaceStoredQueryDir()).orElse(storedQueryDir());
    }

    private boolean isGlobalQueriesAllowedOnLocalWorkspace() {
        return wfsInfo == null ? true : wfsInfo.getAllowGlobalQueries();
    }
}

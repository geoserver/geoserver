/* (c) 2014-2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.opengis.wfs20.StoredQueryDescriptionType;
import org.geoserver.catalog.Catalog;
import org.geoserver.platform.GeoServerResourceLoader;
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

    public StoredQueryProvider(Catalog catalog) {
        this.catalog = catalog;
        this.loader = catalog.getResourceLoader();
    }

    /** The language/type of stored query the provider handles. */
    public String getLanguage() {
        return LANGUAGE_20;
    }

    /** Lists all the stored queries provided. */
    public List<StoredQuery> listStoredQueries() {
        Parser p = new Parser(new WFSConfiguration());

        List<StoredQuery> queries = new ArrayList();

        // add the default as mandated by spec
        queries.add(StoredQuery.DEFAULT);

        // add user created ones
        Resource dir = storedQueryDir();
        for (Resource f : dir.list()) {
            try {
                queries.add(parseStoredQuery(f, p));
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error occured parsing stored query: " + f, e);
            }
        }

        return queries;
    }

    /**
     * Creates a new stored query.
     *
     * @param def The stored query definition.
     */
    public StoredQuery createStoredQuery(StoredQueryDescriptionType query) {
        return createStoredQuery(query, true);
    }

    /**
     * Creates a new stored query specifying whether to persist the query to disk or not.
     *
     * @param def The stored query definition.
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
        storedQueryDir().get(toFilename(query.getName())).delete();
    }

    /** Removes all stored queries. */
    public void removeAll() {
        for (Resource file : storedQueryDir().list()) {
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
            Resource res = storedQueryDir().get(toFilename(name));

            if (res.getType() != Type.RESOURCE) {
                return null;
            }

            return parseStoredQuery(res);
        } catch (Exception e) {
            throw new RuntimeException("Error accessign stoed query: " + name, e);
        }
    }

    /**
     * Persists a stored query, overwriting it if the query already exists.
     *
     * @param query The stored query.
     */
    public void putStoredQuery(StoredQuery query) {
        try {
            Resource dir = storedQueryDir();
            Resource f = dir.get(toFilename(query.getName()));
            if (f.getType() != Type.UNDEFINED) {
                // TODO: back up the old file in case there is an error during encoding
            }

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
}

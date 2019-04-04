/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import java.util.Iterator;

/**
 * Data access interface for persisting imports.
 *
 * @todo refactor various queries into query object
 * @author Justin Deoliveira, OpenGeo
 */
public interface ImportStore {

    public interface ImportVisitor {
        void visit(ImportContext context);
    }

    String getName();

    void init();

    /**
     * Negotiate an ID and reserver the spot for an import. The returned ID will be equal to or
     * greater than the proposal.
     *
     * @param id the non-null proposed ID
     * @return the negotiated id
     */
    Long advanceId(Long id);

    ImportContext get(long id);

    void add(ImportContext context);

    void save(ImportContext context);

    void remove(ImportContext importContext);

    void removeAll();

    Iterator<ImportContext> iterator();

    Iterator<ImportContext> iterator(String sortBy);

    Iterator<ImportContext> allNonCompleteImports();

    Iterator<ImportContext> importsByUser(String user);

    void query(ImportVisitor visitor);

    void destroy();
}

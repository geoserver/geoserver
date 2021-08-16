/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;

public class MemoryImportStore implements ImportStore {

    static final Logger LOGGER = Logging.getLogger(MemoryImportStore.class);

    AtomicLong idseq = new AtomicLong();

    Queue<ImportContext> imports = new ConcurrentLinkedQueue<>();

    @Override
    public String getName() {
        return "memory";
    }

    @Override
    public void init() {}

    @Override
    public ImportContext get(long id) {
        for (ImportContext context : imports) {
            if (context.getId() == id) {
                return context;
            }
        }
        LOGGER.log(Level.FINE, "Could not find import context with id: {0}", id);
        return null;
    }

    @Override
    public Long advanceId(Long id) {
        if (id <= idseq.longValue()) {
            id = idseq.getAndIncrement();
        } else {
            idseq.set(id + 1);
        }
        return id;
    }

    @Override
    public void add(ImportContext context) {
        context.setId(idseq.getAndIncrement());
        imports.add(context);
        LOGGER.log(Level.FINE, "Added import context {0} ", context);
        if (imports.size() > 100) {
            clearCompletedImports();
        }
    }

    void clearCompletedImports() {
        LOGGER.log(Level.FINE, "Clearing all completed imports from storage");
        List<ImportContext> completed =
                collect(
                        new ImportCollector() {
                            @Override
                            protected boolean capture(ImportContext context) {
                                return context.getState() == ImportContext.State.COMPLETE;
                            }
                        });
        imports.removeAll(completed);
    }

    @Override
    public void save(ImportContext context) {
        imports.remove(context);
        imports.add(context);
        LOGGER.log(Level.FINE, "Saved import context {0}", context);
    }

    @Override
    public void remove(ImportContext importContext) {
        LOGGER.log(Level.FINE, "Removing import context {0}", importContext);
        imports.remove(importContext);
    }

    @Override
    public void removeAll() {
        LOGGER.log(Level.FINE, "Removing all import contexts");
        imports.clear();
    }

    @Override
    public Iterator<ImportContext> iterator() {
        return imports.iterator();
    }

    @Override
    public Iterator<ImportContext> iterator(String sortBy) {
        if (sortBy == null) {
            return iterator();
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<ImportContext> allNonCompleteImports() {
        return collect(
                        new ImportCollector() {
                            @Override
                            protected boolean capture(ImportContext context) {
                                return context.getState() != ImportContext.State.COMPLETE;
                            }
                        })
                .iterator();
    }

    @Override
    public Iterator<ImportContext> importsByUser(final String user) {
        return collect(
                        new ImportCollector() {
                            @Override
                            protected boolean capture(ImportContext context) {
                                return user.equals(context.getUser());
                            }
                        })
                .iterator();
    }

    @Override
    public void query(ImportVisitor visitor) {
        for (ImportContext context : imports) {
            visitor.visit(context);
        }
    }

    List<ImportContext> collect(ImportCollector collector) {
        query(collector);
        return collector.getCollected();
    }

    @Override
    public void destroy() {
        idseq.set(0);
        imports.clear();
    }

    abstract static class ImportCollector implements ImportVisitor {

        List<ImportContext> collected = new ArrayList<>();

        @Override
        public final void visit(ImportContext context) {
            if (capture(context)) {
                collected.add(context);
            }
        }

        public List<ImportContext> getCollected() {
            return collected;
        }

        protected abstract boolean capture(ImportContext context);
    }
}

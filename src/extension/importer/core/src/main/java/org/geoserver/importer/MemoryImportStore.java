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

public class MemoryImportStore implements ImportStore {

    AtomicLong idseq = new AtomicLong();

    Queue<ImportContext> imports = new ConcurrentLinkedQueue<ImportContext>();

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
        if (imports.size() > 100) {
            clearCompletedImports();
        }
    }

    void clearCompletedImports() {
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
    }

    @Override
    public void remove(ImportContext importContext) {
        imports.remove(importContext);
    }

    @Override
    public void removeAll() {
        imports.clear();
    }

    @Override
    public Iterator<ImportContext> iterator() {
        return imports.iterator();
    }

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

        List<ImportContext> collected = new ArrayList();

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

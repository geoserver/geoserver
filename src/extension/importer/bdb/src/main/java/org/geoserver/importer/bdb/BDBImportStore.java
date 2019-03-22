/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.bdb;

import com.sleepycat.bind.EntryBinding;
import com.sleepycat.bind.serial.ClassCatalog;
import com.sleepycat.bind.serial.SerialBinding;
import com.sleepycat.bind.serial.StoredClassCatalog;
import com.sleepycat.bind.tuple.LongBinding;
import com.sleepycat.collections.StoredMap;
import com.sleepycat.je.CacheMode;
import com.sleepycat.je.Cursor;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.Durability;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.Sequence;
import com.sleepycat.je.SequenceConfig;
import com.sleepycat.je.StatsConfig;
import com.sleepycat.je.Transaction;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.iterators.FilterIterator;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.importer.ImportContext;
import org.geoserver.importer.ImportStore;
import org.geoserver.importer.ImportTask;
import org.geoserver.importer.Importer;
import org.geotools.util.logging.Logging;

/**
 * Import store implementation based on Berkley DB Java Edition.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class BDBImportStore implements ImportStore {

    static Logger LOGGER = Logging.getLogger(Importer.class);

    public static enum BindingType {
        SERIAL {
            @Override
            ImportBinding createBinding() {
                return new SerialImportBinding();
            }
        },
        XSTREAM {
            @Override
            ImportBinding createBinding() {
                return new XStreamBinding();
            }
        };

        abstract ImportBinding createBinding();
    }

    Importer importer;

    Database db, seqDb;
    Sequence importIdSeq;

    BindingType bindingType = BindingType.SERIAL;
    ImportBinding dbBinding;
    EntryBinding<ImportContext> importBinding;

    public BDBImportStore(Importer importer) {
        this.importer = importer;
    }

    @Override
    public String getName() {
        return "bdb";
    }

    public void setBinding(BindingType bindingType) {
        this.bindingType = bindingType;
    }

    public BindingType getBinding() {
        return bindingType;
    }

    public void init() {
        if ("serial".equalsIgnoreCase(System.getProperty("org.geoserver.importer.bdb.binding"))) {
            bindingType = BindingType.SERIAL;
            LOGGER.info("Using serial binding");
        }
        dbBinding = bindingType.createBinding();

        // create the db environment
        EnvironmentConfig envCfg = new EnvironmentConfig();
        envCfg.setAllowCreate(true);
        envCfg.setCacheMode(CacheMode.DEFAULT);
        envCfg.setLockTimeout(1000, TimeUnit.MILLISECONDS);
        envCfg.setDurability(Durability.COMMIT_WRITE_NO_SYNC);
        envCfg.setSharedCache(true);
        envCfg.setTransactional(true);
        envCfg.setConfigParam("je.log.fileMax", String.valueOf(100 * 1024 * 1024));

        File dbRoot = new File(importer.getImportRoot(), "bdb");
        dbRoot.mkdir();

        Environment env = new Environment(dbRoot, envCfg);

        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setAllowCreate(true);
        dbConfig.setTransactional(true);

        initDb(dbConfig, env);
    }

    void initDb(DatabaseConfig dbConfig, Environment env) {
        // main database
        db = env.openDatabase(null, "imports", dbConfig);

        // sequence for identifiers
        SequenceConfig seqConfig = new SequenceConfig();
        seqConfig.setAllowCreate(true);
        seqDb = env.openDatabase(null, "seq", dbConfig);
        importIdSeq =
                seqDb.openSequence(null, new DatabaseEntry("import_id".getBytes()), seqConfig);

        dbBinding.initDb(dbConfig, env);
        importBinding = dbBinding.createImportBinding(importer);

        // importBinding = new SerialVersionSafeSerialBinding<ImportContext>();
        // importBinding = new XStreamInfoSerialBinding<ImportContext>(
        //    importer.createXStreamPersister(), ImportContext.class);

        checkAndFixDbIncompatability(dbConfig, env);
    }

    void checkAndFixDbIncompatability(DatabaseConfig dbConfig, Environment env) {
        // check for potential class incompatibilities and attempt recovery
        try {
            ImportContext context = iterator().next();
            LOGGER.fine(context.toString());
        } catch (RuntimeException re) {
            if (re.getCause() instanceof java.io.ObjectStreamException) {
                LOGGER.warning("Unable to read import database, attempting recovery");

                // wipe out the catalog
                dbBinding.closeDb(env);
                dbBinding.destroyDb(env);

                // and the import db
                db.close();
                env.removeDatabase(null, "imports");

                // reopen
                initDb(dbConfig, env);
            }
        }
    }

    @Override
    public Long advanceId(Long id) {
        assert id != null;
        // if not an advance, error
        long current = importIdSeq.getStats(StatsConfig.DEFAULT).getCurrent();
        if (id.longValue() < current) {
            id = Long.valueOf(current);
        }

        // reserve the spot now (the delta must have one added to it)
        int delta = (int) (id.longValue() - current + 1);
        current = importIdSeq.get(null, delta);
        // verify existing doesn't exists (shouldn't but just in case)
        if (get(current) != null) {
            throw new IllegalStateException("proposed in exists!");
        }
        ImportContext reserved = new ImportContext(current);
        put(reserved);
        return id;
    }

    public ImportContext get(long id) {
        DatabaseEntry val = new DatabaseEntry();
        OperationStatus op = db.get(null, key(id), val, LockMode.DEFAULT);
        if (op == OperationStatus.NOTFOUND) {
            return null;
        }

        return importBinding.entryToObject(val);
    }

    ImportContext dettach(ImportContext context) {
        Catalog catalog = importer.getCatalog();
        for (ImportTask task : context.getTasks()) {
            StoreInfo store = task.getStore();
            if (store != null && store.getId() != null) {
                task.setStore(catalog.detach(store));
            }
        }
        return context;
    }

    public synchronized void add(ImportContext context) {
        context.setId(importIdSeq.get(null, 1));

        put(context);
    }

    public void remove(ImportContext importContext) {
        db.delete(null, key(importContext));
    }

    public void removeAll() {

        Transaction tx = db.getEnvironment().beginTransaction(null, null);
        Cursor c = db.openCursor(tx, null);

        DatabaseEntry key = new DatabaseEntry();
        DatabaseEntry val = new DatabaseEntry();

        LongBinding keyBinding = new LongBinding();
        List<Long> ids = new ArrayList();

        while (c.getNext(key, val, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
            ids.add(LongBinding.entryToLong(key));
        }
        c.close();

        for (Long id : ids) {
            keyBinding.objectToEntry(id, key);
            db.delete(tx, key);
        }

        tx.commit();
    }

    public void save(ImportContext context) {
        dettach(context);
        if (context.getId() == null) {
            add(context);
        } else {
            put(context);
        }
    }

    public Iterator<ImportContext> iterator() {
        return new StoredMap<Long, ImportContext>(db, new LongBinding(), importBinding, false)
                .values()
                .iterator();
    }

    public Iterator<ImportContext> iterator(String sortBy) {
        if (sortBy == null) {
            return iterator();
        }

        throw new UnsupportedOperationException();
    }

    public Iterator<ImportContext> allNonCompleteImports() {
        // if this becomes too slow a secondary database could be used for indexing
        return new FilterIterator(
                iterator(),
                new Predicate() {

                    public boolean evaluate(Object o) {
                        return ((ImportContext) o).getState() != ImportContext.State.COMPLETE;
                    }
                });
    }

    public Iterator<ImportContext> importsByUser(final String user) {
        // if this becomes too slow a secondary database could be used for indexing
        return new FilterIterator(
                allNonCompleteImports(),
                new Predicate() {

                    public boolean evaluate(Object o) {
                        return user.equals(((ImportContext) o).getUser());
                    }
                });
    }

    public void query(ImportVisitor visitor) {
        Cursor c = db.openCursor(null, null);
        try {
            DatabaseEntry key = new DatabaseEntry();
            DatabaseEntry val = new DatabaseEntry();

            while (c.getNext(key, val, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
                visitor.visit(importBinding.entryToObject(val));
            }
        } finally {
            c.close();
        }
    }

    synchronized void put(ImportContext context) {
        assert context.getId() != null;

        DatabaseEntry val = new DatabaseEntry();
        importBinding.objectToEntry(context, val);

        db.put(null, key(context), val);
    }

    DatabaseEntry key(ImportContext context) {
        return key(context.getId());
    }

    DatabaseEntry key(long id) {
        DatabaseEntry key = new DatabaseEntry();
        new LongBinding().objectToEntry(id, key);
        return key;
    }

    byte[] toBytes(long l) {
        byte[] b = new byte[8];
        b[0] = (byte) (0xff & (l >> 56));
        b[1] = (byte) (0xff & (l >> 48));
        b[2] = (byte) (0xff & (l >> 40));
        b[3] = (byte) (0xff & (l >> 32));
        b[4] = (byte) (0xff & (l >> 24));
        b[5] = (byte) (0xff & (l >> 16));
        b[6] = (byte) (0xff & (l >> 8));
        b[7] = (byte) (0xff & l);
        return b;
    }

    public void destroy() {
        // destroy the db environment
        Environment env = db.getEnvironment();

        dbBinding.closeDb(env);
        seqDb.close();
        db.close();

        env.close();
    }

    abstract static class ImportBinding {
        void initDb(DatabaseConfig dbConfig, Environment env) {}

        void closeDb(Environment env) {}

        void destroyDb(Environment env) {}

        protected abstract EntryBinding<ImportContext> createImportBinding(Importer importer);
    }

    static class SerialImportBinding extends ImportBinding {
        Database classDb;
        ClassCatalog classCatalog;

        @Override
        void initDb(DatabaseConfig dbConfig, Environment env) {
            // class database
            classDb = env.openDatabase(null, "classes", dbConfig);
            classCatalog = new StoredClassCatalog(classDb);
        }

        @Override
        void closeDb(Environment env) {
            classCatalog.close();
            classDb.close();
        }

        @Override
        void destroyDb(Environment env) {
            env.removeDatabase(null, "classes");
        }

        @Override
        protected EntryBinding<ImportContext> createImportBinding(Importer importer) {
            return new SerialBinding<ImportContext>(classCatalog, ImportContext.class);
        }
    }

    static class XStreamBinding extends ImportBinding {
        @Override
        protected EntryBinding<ImportContext> createImportBinding(Importer importer) {
            return new XStreamInfoSerialBinding<ImportContext>(
                    importer.createXStreamPersisterXML(), ImportContext.class);
        }
    }
}

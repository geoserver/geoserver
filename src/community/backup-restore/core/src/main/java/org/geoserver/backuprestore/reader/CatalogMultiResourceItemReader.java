package org.geoserver.backuprestore.reader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.geoserver.backuprestore.Backup;
import org.geotools.util.logging.Logging;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.ResourceAware;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * Multi-resource reader that: - expands numbered shard files (e.g. store.dat, store.dat.1, store.dat.2, ...) - sorts
 * shards numerically (base file first, then 1,2,3,...,10)
 */
public class CatalogMultiResourceItemReader<T> extends CatalogReader<T> {

    private static final Logger logger = Logging.getLogger(CatalogMultiResourceItemReader.class);
    private static final String RESOURCE_KEY = "resourceIndex";

    private CatalogReader<? extends T> delegate;
    private Resource[] resources;
    private boolean saveState = true;
    private int currentResource = -1;
    private boolean noInput;
    private boolean strict = false;

    public CatalogMultiResourceItemReader(Class<T> clazz, Backup backupFacade) {
        super(clazz, backupFacade);
    }

    @Override
    protected void initialize(StepExecution stepExecution) {
        delegate.retrieveInterstepData(stepExecution);
    }

    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    /** Numeric-aware comparator so 1,2,...,10 are in logical order; base file (no suffix) comes first. */
    private Comparator<Resource> comparator = new Comparator<Resource>() {
        @Override
        public int compare(Resource r1, Resource r2) {
            String f1 = r1.getFilename(), f2 = r2.getFilename();
            // try numeric suffix (.1, .2, ...)
            int n1 = suffixIndex(f1), n2 = suffixIndex(f2);
            if (n1 != n2) return Integer.compare(n1, n2);
            return f1.compareTo(f2);
        }

        private int suffixIndex(String name) {
            try {
                int dot = name.lastIndexOf('.');
                return (dot >= 0) ? Integer.parseInt(name.substring(dot + 1)) : 0;
            } catch (Exception e) {
                return 0;
            }
        }
    };

    @Override
    public T read() throws Exception, UnexpectedInputException, ParseException {
        if (noInput) return null;

        if (currentResource == -1) {
            currentResource = 0;
            delegate.setResource(resources[currentResource]);
            delegate.open(new ExecutionContext());
        }
        return readNextItem();
    }

    private T readNextItem() throws Exception {
        T item = readFromDelegate();
        while (item == null) {
            currentResource++;
            if (currentResource >= resources.length) return null;

            delegate.close();
            delegate.setResource(resources[currentResource]);
            delegate.open(new ExecutionContext());

            item = readFromDelegate();
        }
        return item;
    }

    private T readFromDelegate() throws Exception {
        T item = delegate.read();
        if (item instanceof ResourceAware aware) {
            aware.setResource(getCurrentResource());
        }
        return item;
    }

    @Override
    public void close() throws ItemStreamException {
        super.close();
        if (!this.noInput) {
            delegate.close();
        }
        noInput = false;
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        super.open(executionContext);
        Assert.notNull(resources, "Resources must be set");

        noInput = false;
        if (resources.length == 0) {
            if (strict) {
                throw new IllegalStateException(
                        "No resources to read. Set strict=false if this is not an error condition.");
            } else {
                logger.warning("No resources to read. Set strict=true if this should be an error condition.");
                noInput = true;
                return;
            }
        }

        Arrays.sort(resources, comparator);

        if (executionContext.containsKey(getExecutionContextKey(RESOURCE_KEY))) {
            currentResource = executionContext.getInt(getExecutionContextKey(RESOURCE_KEY));
            if (currentResource == -1) currentResource = 0;

            delegate.setResource(resources[currentResource]);
            delegate.open(executionContext);
        } else {
            currentResource = -1;
        }
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        super.update(executionContext);
        if (saveState) {
            executionContext.putInt(getExecutionContextKey(RESOURCE_KEY), currentResource);
            delegate.update(executionContext);
        }
    }

    /** Reader for a single Resource. */
    public void setDelegate(CatalogReader<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public void setSaveState(boolean saveState) {
        this.saveState = saveState;
    }

    /** Override the default comparator if needed. */
    public void setComparator(Comparator<Resource> comparator) {
        this.comparator = comparator;
    }

    /**
     * Set input resources. This method also expands numbered shards that live next to each base file (e.g.,
     * "namespace.dat" -> also picks "namespace.dat.1", ".2", ...).
     */
    public void setResources(Resource[] resources) {
        Assert.notNull(resources, "The resources must not be null");
        List<Resource> expanded = new ArrayList<>();

        for (Resource r : resources) {
            expanded.add(r); // always include the base

            // Try to discover numbered siblings when possible (file-system based resources)
            try {
                File base = r.getFile();
                if (base != null && base.isFile()) {
                    File dir = base.getParentFile();
                    String baseName = base.getName();
                    // pattern: baseName + '.' + digits
                    Pattern p = Pattern.compile(Pattern.quote(baseName) + "\\.(\\d+)");
                    File[] shards = dir.listFiles(f -> p.matcher(f.getName()).matches());
                    if (shards != null) {
                        for (File s : shards) {
                            expanded.add(new FileSystemResource(s));
                        }
                    }
                }
            } catch (IOException | UnsupportedOperationException ignored) {
                // Non-file resources (e.g. classpath, VFS) -> we can't expand; keep the base only.
            }
        }

        this.resources = expanded.toArray(new Resource[0]);
    }

    public Resource getCurrentResource() {
        if (currentResource >= resources.length || currentResource < 0) return null;
        return resources[currentResource];
    }

    @Override
    public void setResource(Resource resource) {}

    @Override
    public void afterPropertiesSet() throws Exception {}

    @Override
    protected T doRead() throws Exception {
        return null;
    }

    @Override
    protected void doOpen() throws Exception {}

    @Override
    protected void doClose() throws Exception {}
}

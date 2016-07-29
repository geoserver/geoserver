/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.tasklet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.geoserver.backuprestore.Backup;
import org.geoserver.backuprestore.BackupRestoreItem;
import org.geoserver.backuprestore.utils.BackupUtils;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.platform.resource.Resources;
import org.geoserver.platform.resource.Resources.AnyFilter;
import org.geoserver.util.Filter;
import org.geotools.util.logging.Logging;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * TODO
 * 
 * @author Alessio Fabiani, GeoSolutions
 *
 */
@SuppressWarnings("rawtypes")
public abstract class AbstractCatalogBackupRestoreTasklet<T> extends BackupRestoreItem
        implements Tasklet, InitializingBean {

    protected static Logger LOGGER = Logging.getLogger(AbstractCatalogBackupRestoreTasklet.class);

    /*
     * 
     */
    protected static Map<String, Filter<Resource>> resources = new HashMap<String, Filter<Resource>>();

    /*
     * 
     */
    static {
        resources.put("demo", AnyFilter.INSTANCE);
        resources.put("images", AnyFilter.INSTANCE);
        resources.put("logs", new Filter<Resource>() {

            @Override
            public boolean accept(Resource res) {
                if (res.name().endsWith(".properties")) {
                    return true;
                }
                return false;
            }

        });
        resources.put("palettes", AnyFilter.INSTANCE);
        resources.put("plugIns", AnyFilter.INSTANCE);
        
        // NOTE: it would be better to use ad-hoc Visitors in order to scan the 
        //       Style Resources and download only the ones needed.
        //       This maybe an improvement for a future release/refactoring.
        resources.put("styles", new Filter<Resource>() {

            @Override
            public boolean accept(Resource res) {
                if (res.name().toLowerCase().endsWith("sld") || // exclude everything ends with SLD ext (SLD, YSLD, ...)
                    res.name().toLowerCase().endsWith(".xml") ||
                    res.name().toLowerCase().endsWith(".css")) // exclude CSS also
                {
                    return false;
                }
                return true;
            }

        });
        resources.put("user_projections", AnyFilter.INSTANCE);
        resources.put("validation", AnyFilter.INSTANCE);
        resources.put("www", AnyFilter.INSTANCE);
    }

    public AbstractCatalogBackupRestoreTasklet(Backup backupFacade,
            XStreamPersisterFactory xStreamPersisterFactory) {
        super(backupFacade, xStreamPersisterFactory);
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)
            throws Exception {
        super.retrieveInterstepData(chunkContext.getStepContext().getStepExecution());
        JobExecution jobExecution = chunkContext.getStepContext().getStepExecution()
                .getJobExecution();

        return doExecute(contribution, chunkContext, jobExecution);
    }

    /**
     * 
     * @param contribution
     * @param chunkContext
     * @param jobExecution
     * @return
     * @throws Exception
     */
    abstract RepeatStatus doExecute(StepContribution contribution, ChunkContext chunkContext,
            JobExecution jobExecution) throws Exception;

    /**
     * @param resourceStore
     * @param baseDir
     * @throws Exception
     * @throws IOException
     */
    public void backupRestoreAdditionalResources(ResourceStore resourceStore, Resource baseDir)
            throws Exception {
        try {
            for (Entry<String, Filter<Resource>> entry : resources.entrySet()) {
                Resource resource = resourceStore.get(entry.getKey());
                if (resource != null && Resources.exists(resource)) {

                    List<Resource> resources = Resources.list(resource, entry.getValue(), false);

                    Resource targetDir = BackupUtils.dir(baseDir, resource.name());
                    for (Resource res : resources) {
                        if (res.getType() != Type.DIRECTORY) {
                            Resources.copy(res.file(), targetDir);
                        } else {
                            Resources.copy(res, BackupUtils.dir(targetDir, res.name()));
                        }
                    }
                }
            }
        } catch (Exception e) {
            logValidationExceptions((T) null, e);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(backupFacade, "backupFacade must be set");
        Assert.notNull(getxStreamPersisterFactory(), "xstream must be set");
    }

    //
    @SuppressWarnings({ "unchecked", "static-access" })
    public void doWrite(Object item, Resource directory, String fileName) throws Exception {
        try {
            if (item instanceof ServiceInfo) {
                ServiceInfo service = (ServiceInfo) item;
                XStreamServiceLoader loader = findServiceLoader(service);

                try {
                    loader.save(service, backupFacade.getGeoServer(),
                            BackupUtils.dir(directory, fileName));
                } catch (Throwable t) {
                    throw new RuntimeException(t);
                    // LOGGER.log(Level.SEVERE, "Error occurred while saving configuration", t);
                }
            } else {
                // unwrap dynamic proxies
                OutputStream out = Resources.fromPath(fileName, directory).out();
                try {
                    if (getXp() == null) {
                        xstream = getxStreamPersisterFactory().createXMLPersister();
                        setXp(xstream.getXStream());
                    }
                    item = xstream.unwrapProxies(item);
                    getXp().toXML(item, out);
                } finally {
                    out.close();
                }
            }
        } catch (Exception e) {
            logValidationExceptions((T) null, e);
        }
    }

    //
    @SuppressWarnings({ "unchecked" })
    public Object doRead(Resource directory, String fileName) throws Exception {
        Object item = null;
        try {
            InputStream in = Resources.fromPath(fileName, directory).in();

            // Try first using the Services Loaders
            final List<XStreamServiceLoader> loaders = GeoServerExtensions
                    .extensions(XStreamServiceLoader.class);
            for (XStreamServiceLoader<ServiceInfo> l : loaders) {
                try {
                    if (l.getFilename().equals(fileName)) {
                        item = l.load(backupFacade.getGeoServer(),
                                Resources.fromPath(fileName, directory));

                        if (item != null && item instanceof ServiceInfo) {
                            return item;
                        }
                    }
                } catch (Exception e) {
                    // Just skip and try with another loader
                    item = null;
                }
            }

            try {
                if (item == null) {
                    try {
                        if (getXp() == null) {
                            xstream = getxStreamPersisterFactory().createXMLPersister();
                            setXp(xstream.getXStream());
                        }
                        item = getXp().fromXML(in);
                    } finally {
                        in.close();
                    }
                }
            } catch (Exception e) {
                // Collect warnings
                item = null;
                if (getCurrentJobExecution() != null) {
                    getCurrentJobExecution().addWarningExceptions(Arrays.asList(e));
                }
            }
        } catch (Exception e) {
            logValidationExceptions((T) null, e);
        }

        return item;
    }

    @SuppressWarnings({ "unchecked" })
    protected XStreamServiceLoader findServiceLoader(ServiceInfo service) {
        XStreamServiceLoader loader = null;

        final List<XStreamServiceLoader> loaders = GeoServerExtensions
                .extensions(XStreamServiceLoader.class);
        for (XStreamServiceLoader<ServiceInfo> l : loaders) {
            if (l.getServiceClass().isInstance(service)) {
                loader = l;
                break;
            }
        }

        if (loader == null) {
            throw new IllegalArgumentException("No loader for " + service.getName());
        }
        return loader;
    }
}

/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.collections.MapConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.ValidationResult;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.CoverageInfoImpl;
import org.geoserver.catalog.impl.CoverageStoreInfoImpl;
import org.geoserver.catalog.impl.DataStoreInfoImpl;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.catalog.impl.LayerGroupInfoImpl;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.catalog.impl.ProxyUtils;
import org.geoserver.catalog.impl.StoreInfoImpl;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.decorators.SecuredCoverageInfo;
import org.geoserver.security.decorators.SecuredCoverageStoreInfo;
import org.geoserver.security.decorators.SecuredDataStoreInfo;
import org.geoserver.security.decorators.SecuredFeatureTypeInfo;
import org.geoserver.security.decorators.SecuredWMSLayerInfo;
import org.geoserver.security.decorators.SecuredWMTSLayerInfo;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.util.Converters;
import org.geotools.util.logging.Logging;
import org.opengis.filter.Filter;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.util.Assert;

/** @author Alessio Fabiani, GeoSolutions S.A.S. */
public abstract class BackupRestoreItem<T> {

    /** logger */
    private static final Logger LOGGER = Logging.getLogger(BackupRestoreItem.class);

    protected Backup backupFacade;

    private Catalog catalog;

    protected XStreamPersister xstream;

    private XStream xp;

    private boolean isNew = true;

    private AbstractExecutionAdapter currentJobExecution;

    private boolean dryRun = true;

    private boolean bestEffort;

    private XStreamPersisterFactory xStreamPersisterFactory;

    private Filter filters[];

    public static final String ENCRYPTED_FIELDS_KEY = "backupRestoreParameterizedFields";

    public BackupRestoreItem(Backup backupFacade) {
        this.backupFacade = backupFacade;
        this.xStreamPersisterFactory = GeoServerExtensions.bean(XStreamPersisterFactory.class);
        ;
    }

    /** @return the xStreamPersisterFactory */
    public XStreamPersisterFactory getxStreamPersisterFactory() {
        return xStreamPersisterFactory;
    }

    /** @return the xp */
    public XStream getXp() {
        return xp;
    }

    /** @param xp the xp to set */
    public void setXp(XStream xp) {
        this.xp = xp;
    }

    /** @return the catalog */
    public Catalog getCatalog() {
        authenticate();
        return catalog;
    }

    /** */
    public void authenticate() {
        backupFacade.authenticate();
    }

    /** @return the isNew */
    public boolean isNew() {
        return isNew;
    }

    /** @return the currentJobExecution */
    public AbstractExecutionAdapter getCurrentJobExecution() {
        return currentJobExecution;
    }

    /** @return the dryRun */
    public boolean isDryRun() {
        return dryRun;
    }

    /** @return the bestEffort */
    public boolean isBestEffort() {
        return bestEffort;
    }

    /** @return the filter */
    public Filter[] getFilters() {
        return filters;
    }

    /** @param filter the filter to set */
    public void setFilters(Filter[] filters) {
        this.filters = filters;
    }

    /** @return a boolean indicating that at least one filter has been defined */
    public boolean filterIsValid() {
        return (this.filters != null
                && this.filters.length == 3
                && (this.filters[0] != null || this.filters[1] != null || this.filters[2] != null));
    }

    @BeforeStep
    public void retrieveInterstepData(StepExecution stepExecution) {
        // Accordingly to the running execution type (Backup or Restore) we
        // need to validate resources against the official GeoServer Catalog (Backup)
        // or the temporary one (Restore).
        //
        // For restore operations the order matters.
        JobExecution jobExecution = stepExecution.getJobExecution();

        this.xstream = xStreamPersisterFactory.createXMLPersister();

        if (backupFacade.getRestoreExecutions() != null
                && !backupFacade.getRestoreExecutions().isEmpty()
                && backupFacade.getRestoreExecutions().containsKey(jobExecution.getId())) {
            this.currentJobExecution =
                    backupFacade.getRestoreExecutions().get(jobExecution.getId());
            this.catalog = ((RestoreExecutionAdapter) currentJobExecution).getRestoreCatalog();
            this.isNew = true;
        } else {
            this.currentJobExecution = backupFacade.getBackupExecutions().get(jobExecution.getId());
            this.catalog = backupFacade.getCatalog();
            this.xstream.setExcludeIds();
            this.isNew = false;
        }

        Assert.notNull(this.catalog, "catalog must be set");

        // Set Catalog
        this.xstream.setCatalog(this.catalog);
        this.xstream.setReferenceByName(true);
        this.xp = this.xstream.getXStream();

        Assert.notNull(this.xp, "xStream persister should not be NULL");

        JobParameters jobParameters = this.currentJobExecution.getJobParameters();

        boolean parameterizePasswords =
                Boolean.parseBoolean(
                        jobParameters.getString(Backup.PARAM_PARAMETERIZE_PASSWDS, "false"));

        if (parameterizePasswords) {
            // here we set some customized XML handling code. For backups, we add a converter that
            // tokenizes
            // outgoing passwords. for restores, a handler for those tokenized backups.
            if (!isNew) {
                this.xp.registerLocalConverter(
                        StoreInfoImpl.class,
                        "connectionParameters",
                        this.createParameterizingMapConverter(xstream));
                this.xp.registerConverter(this.createStoreConverter(xstream));
            } else {
                String concatenatedPasswordTokens =
                        jobParameters.getString(Backup.PARAM_PASSWORD_TOKENS);
                Map<String, String> passwordTokens =
                        parseConcatenatedPasswordTokens(concatenatedPasswordTokens);
                this.xp.registerConverter(new TokenizedFieldConverter(passwordTokens));
                xstream.registerBreifMapComplexType("tokenizedPassword", BackupRestoreItem.class);
            }
        }

        this.dryRun =
                Boolean.parseBoolean(jobParameters.getString(Backup.PARAM_DRY_RUN_MODE, "false"));
        this.bestEffort =
                Boolean.parseBoolean(
                        jobParameters.getString(Backup.PARAM_BEST_EFFORT_MODE, "false"));

        // Initialize Filters
        this.filters = new Filter[3];
        String cql = jobParameters.getString("wsFilter", null);
        if (cql != null) {
            try {
                this.filters[0] = ECQL.toFilter(cql);
            } catch (CQLException e) {
                throw new IllegalArgumentException("Workspace Filter is not valid!", e);
            }
        } else {
            this.filters[0] = null;
        }

        cql = jobParameters.getString("siFilter", null);
        if (cql != null) {
            try {
                this.filters[1] = ECQL.toFilter(cql);
            } catch (CQLException e) {
                throw new IllegalArgumentException("Store Filter is not valid!", e);
            }
        } else {
            this.filters[1] = null;
        }

        cql = jobParameters.getString("liFilter", null);
        if (cql != null) {
            try {
                this.filters[2] = ECQL.toFilter(cql);
            } catch (CQLException e) {
                throw new IllegalArgumentException("Layer Filter is not valid!", e);
            }
        } else {
            this.filters[2] = null;
        }

        initialize(stepExecution);
    }

    private Map<String, String> parseConcatenatedPasswordTokens(String concatenatedPasswordTokens) {
        Map<String, String> tokenMap = new HashMap<>();
        if (concatenatedPasswordTokens != null) {
            Arrays.stream(concatenatedPasswordTokens.split(","))
                    .forEach(
                            tokenPair -> {
                                String[] tokenPairSplit = tokenPair.split("=");
                                if (tokenPairSplit.length == 2) {
                                    tokenMap.put(tokenPairSplit[0], tokenPairSplit[1]);
                                }
                            });
        }
        return tokenMap;
    }

    /** */
    protected abstract void initialize(StepExecution stepExecution);

    /** */
    public boolean logValidationExceptions(ValidationResult result, Exception e) throws Exception {
        CatalogException validationException = new CatalogException(e);
        if (!isBestEffort()) {
            if (result != null) {
                result.throwIfInvalid();
            } else {
                throw e;
            }
        }

        if (!isBestEffort()) {
            getCurrentJobExecution().addFailureExceptions(Arrays.asList(validationException));
        }
        return false;
    }

    /** @param resource */
    public boolean logValidationExceptions(T resource, Throwable e) {
        CatalogException validationException =
                e != null
                        ? new CatalogException(e)
                        : new CatalogException("Invalid resource: " + resource);
        if (!isBestEffort()) {
            getCurrentJobExecution().addFailureExceptions(Arrays.asList(validationException));
            throw validationException;
        } else {
            getCurrentJobExecution().addWarningExceptions(Arrays.asList(validationException));
        }
        return false;
    }

    /** */
    protected boolean filteredResource(T resource, WorkspaceInfo ws, boolean strict, Class clazz) {
        // Filtering Resources
        if (filterIsValid()) {
            if (resource == null || (clazz != null && clazz == WorkspaceInfo.class)) {
                if ((strict && ws == null)
                        || (ws != null
                                && getFilters()[0] != null
                                && !getFilters()[0].evaluate(ws))) {
                    LOGGER.info("Skipped filtered workspace: " + ws);
                    return true;
                }
            }

            if (resource != null && clazz != null && clazz == StoreInfo.class) {
                if (getFilters()[1] != null && !getFilters()[1].evaluate(resource)) {
                    LOGGER.info("Skipped filtered resource: " + resource);
                    return true;
                }
            }

            if (resource != null && clazz != null && clazz == LayerInfo.class) {
                if (getFilters()[2] != null && !getFilters()[2].evaluate(resource)) {
                    LOGGER.info("Skipped filtered resource: " + resource);
                    return true;
                }
            }

            if (resource != null && clazz != null && clazz == ResourceInfo.class) {
                if (((ResourceInfo) resource).getStore() == null
                        ||
                        // (getFilters()[1] != null && !getFilters()[1].evaluate(((ResourceInfo)
                        // resource).getStore())) ||
                        (getFilters()[2] != null && !getFilters()[2].evaluate(resource))) {
                    LOGGER.info("Skipped filtered resource: " + resource);
                    return true;
                }
            }
        }

        return false;
    }

    /** */
    protected boolean filteredResource(WorkspaceInfo ws, boolean strict) {
        return filteredResource(null, ws, strict, WorkspaceInfo.class);
    }

    private MapConverter createParameterizingMapConverter(XStreamPersister xstream) {
        return xstream.new BreifMapConverter() {
            @Override
            public void marshal(
                    Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
                ParameterizedFieldsHolder fieldsToParametrize =
                        (ParameterizedFieldsHolder) context.get(ENCRYPTED_FIELDS_KEY);

                Map map = (Map) source;
                for (Iterator iterator = map.entrySet().iterator(); iterator.hasNext(); ) {
                    Map.Entry entry = (Map.Entry) iterator.next();

                    if (entry.getValue() == null) {
                        continue;
                    }

                    writer.startNode("entry");
                    writer.addAttribute("key", entry.getKey().toString());
                    Object value = entry.getValue();
                    String complexTypeId = getComplexTypeId(value.getClass());
                    if (complexTypeId == null) {
                        String str = Converters.convert(value, String.class);
                        if (str == null) {
                            str = value.toString();
                        }
                        if (fieldsToParametrize != null
                                && fieldsToParametrize.getFields().contains(entry.getKey())) {

                            writer.startNode("tokenizedPassword");
                            str =
                                    "${"
                                            + fieldsToParametrize
                                                    .getStoreInfo()
                                                    .getWorkspace()
                                                    .getName()
                                            + ":"
                                            + fieldsToParametrize.getStoreInfo().getName()
                                            + "."
                                            + entry.getKey().toString()
                                            + ".encryptedValue}";
                            writer.setValue(str);
                            writer.endNode();
                        } else {
                            writer.setValue(str);
                        }
                    } else {
                        writer.startNode(complexTypeId);
                        context.convertAnother(value);
                        writer.endNode();
                    }

                    writer.endNode();
                }
            }
        };
    }

    private ReflectionConverter createStoreConverter(XStreamPersister xstream) {
        return xstream.new StoreInfoConverter() {
            @Override
            protected void doMarshal(
                    Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
                GeoServerSecurityManager secMgr =
                        xstream.isEncryptPasswordFields() ? xstream.getSecurityManager() : null;
                if (secMgr != null && secMgr.isInitialized()) {
                    // set the hint for the map converter as to which fields to encode in the
                    // connection
                    // parameter of this store
                    Set<String> encryptedFields =
                            secMgr.getConfigPasswordEncryptionHelper()
                                    .getEncryptedFields((StoreInfo) source);

                    if (!encryptedFields.isEmpty()) {
                        context.put(
                                ENCRYPTED_FIELDS_KEY,
                                new ParameterizedFieldsHolder((StoreInfo) source, encryptedFields));
                    }
                }

                super.doMarshal(source, writer, context);
            }
        };
    }

    public Converter getTokenizedPasswordConverter() {
        return new Converter() {
            @Override
            public void marshal(
                    Object source, HierarchicalStreamWriter writer, MarshallingContext context) {}

            @Override
            public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
                String tokenizedValue = reader.getValue();
                String replacedValue = this.replaceTokenizedValue(tokenizedValue);
                return replacedValue;
            }

            private String replaceTokenizedValue(String tokenizedValue) {
                return "foo";
            }

            @Override
            public boolean canConvert(Class type) {
                if (BackupRestoreItem.class.equals(type)) {
                    return true;
                } else {
                    return false;
                }
            }
        };
    }

    /** */
    private Object unwrap(Object item) {
        if (item instanceof Proxy) {
            item = ProxyUtils.unwrap(item, Proxy.getInvocationHandler(item).getClass());
        }
        return item;
    }

    /** */
    private ResourceInfo unwrapSecured(ResourceInfo info) {
        if (info instanceof SecuredFeatureTypeInfo)
            return ((SecuredFeatureTypeInfo) info).unwrap(ResourceInfo.class);
        if (info instanceof SecuredCoverageInfo)
            return ((SecuredCoverageInfo) info).unwrap(ResourceInfo.class);
        if (info instanceof SecuredWMSLayerInfo)
            return ((SecuredWMSLayerInfo) info).unwrap(ResourceInfo.class);
        if (info instanceof SecuredWMTSLayerInfo)
            return ((SecuredWMTSLayerInfo) info).unwrap(ResourceInfo.class);
        return info;
    }

    /** */
    private StoreInfo unwrapSecured(StoreInfo info) {
        if (info instanceof SecuredDataStoreInfo)
            return ((SecuredDataStoreInfo) info).unwrap(StoreInfo.class);
        if (info instanceof SecuredCoverageStoreInfo)
            return ((SecuredCoverageStoreInfo) info).unwrap(StoreInfo.class);
        return info;
    }

    /** @param catalog */
    protected void syncTo(Catalog srcCatalog) {
        // do a manual import

        // WorkSpaces && NameSpaces
        for (NamespaceInfo ns : srcCatalog.getFacade().getNamespaces()) {
            NamespaceInfo targetNamespace = catalog.getNamespaceByPrefix(ns.getPrefix());
            if (targetNamespace == null) {
                targetNamespace = clone((NamespaceInfo) unwrap(ns));
                catalog.add(targetNamespace);
                catalog.save(catalog.getNamespace(targetNamespace.getId()));
            }
        }

        for (WorkspaceInfo ws : srcCatalog.getFacade().getWorkspaces()) {
            WorkspaceInfo targetWorkspace = catalog.getWorkspaceByName(ws.getName());
            if (targetWorkspace == null) {
                targetWorkspace = clone((WorkspaceInfo) unwrap(ws));
                catalog.add(targetWorkspace);
                catalog.save(catalog.getWorkspace(targetWorkspace.getId()));
            }
        }

        // DataStores
        for (StoreInfo store : srcCatalog.getFacade().getStores(DataStoreInfo.class)) {
            DataStoreInfo targetDataStore = catalog.getDataStoreByName(store.getName());
            if (store != null && targetDataStore == null) {
                WorkspaceInfo targetWorkspace =
                        store.getWorkspace() != null
                                ? catalog.getWorkspaceByName(store.getWorkspace().getName())
                                : null;
                targetDataStore =
                        (DataStoreInfo)
                                clone(
                                        (DataStoreInfo) unwrap(unwrapSecured(store)),
                                        targetWorkspace,
                                        DataStoreInfo.class);
                if (targetDataStore != null) {
                    catalog.add(targetDataStore);
                    catalog.save(catalog.getDataStore(targetDataStore.getId()));
                }
            }
        }
        for (ResourceInfo resource : srcCatalog.getFacade().getResources(FeatureTypeInfo.class)) {
            FeatureTypeInfo targetResource =
                    catalog.getResourceByName(resource.getName(), FeatureTypeInfo.class);
            if (resource != null && targetResource == null) {
                DataStoreInfo targetDataStore =
                        catalog.getDataStoreByName(resource.getStore().getName());
                NamespaceInfo targetNamespace =
                        resource.getNamespace() != null
                                ? catalog.getNamespaceByPrefix(resource.getNamespace().getPrefix())
                                : null;
                if (targetDataStore != null) {
                    targetResource =
                            clone(
                                    (FeatureTypeInfo) unwrap(unwrapSecured(resource)),
                                    targetNamespace,
                                    targetDataStore);
                    catalog.add(targetResource);
                    catalog.save(
                            catalog.getResource(targetResource.getId(), FeatureTypeInfo.class));
                }
            }
        }

        // CoverageStores
        for (StoreInfo store : srcCatalog.getFacade().getStores(CoverageStoreInfo.class)) {
            CoverageStoreInfo targetCoverageStore = catalog.getCoverageStoreByName(store.getName());
            if (store != null && targetCoverageStore == null) {
                WorkspaceInfo targetWorkspace =
                        store.getWorkspace() != null
                                ? catalog.getWorkspaceByName(store.getWorkspace().getName())
                                : null;
                targetCoverageStore =
                        (CoverageStoreInfo)
                                clone(
                                        (CoverageStoreInfo) unwrap(unwrapSecured(store)),
                                        targetWorkspace,
                                        CoverageStoreInfo.class);
                if (targetCoverageStore != null) {
                    catalog.add(targetCoverageStore);
                    catalog.save(catalog.getCoverageStore(targetCoverageStore.getId()));
                }
            }
        }
        for (ResourceInfo resource : srcCatalog.getFacade().getResources(CoverageInfo.class)) {
            CoverageInfo targetResource =
                    catalog.getResourceByName(resource.getName(), CoverageInfo.class);
            if (resource != null && targetResource == null) {
                CoverageStoreInfo targetCoverageStore =
                        catalog.getCoverageStoreByName(resource.getStore().getName());
                NamespaceInfo targetNamespace =
                        resource.getNamespace() != null
                                ? catalog.getNamespaceByPrefix(resource.getNamespace().getPrefix())
                                : null;
                if (targetCoverageStore != null) {
                    targetResource =
                            clone(
                                    (CoverageInfo) unwrap(unwrapSecured(resource)),
                                    targetNamespace,
                                    targetCoverageStore);
                    catalog.add(targetResource);
                    catalog.save(catalog.getResource(targetResource.getId(), CoverageInfo.class));
                }
            }
        }

        // Styles
        for (StyleInfo s : srcCatalog.getFacade().getStyles()) {
            StyleInfo targetStyle = catalog.getStyleByName(s.getName());
            if (s != null && targetStyle == null) {
                WorkspaceInfo targetWorkspace =
                        s.getWorkspace() != null
                                ? catalog.getWorkspaceByName(s.getWorkspace().getName())
                                : null;
                targetStyle = clone((StyleInfo) unwrap(s), targetWorkspace);
                catalog.add(targetStyle);
                catalog.save(catalog.getStyle(targetStyle.getId()));
            }
        }

        // Layers && LayerGroups
        for (LayerInfo l : srcCatalog.getFacade().getLayers()) {
            LayerInfo targetLayerInfo = catalog.getLayerByName(l.getName());
            if (targetLayerInfo == null) {
                ResourceInfo sourceResourceInfo = l.getResource();
                StoreInfo sourceStoreInfo = sourceResourceInfo.getStore();
                StoreInfo targetStoreInfo = null;
                if (sourceStoreInfo instanceof DataStoreInfo) {
                    targetStoreInfo =
                            catalog.getStoreByName(sourceStoreInfo.getName(), DataStoreInfo.class);
                } else if (sourceStoreInfo instanceof CoverageStoreInfo) {
                    targetStoreInfo =
                            catalog.getStoreByName(
                                    sourceStoreInfo.getName(), CoverageStoreInfo.class);
                }
                if (targetStoreInfo != null) {
                    ResourceInfo targetResourceInfo = null;
                    if (sourceStoreInfo instanceof DataStoreInfo) {
                        targetResourceInfo =
                                catalog.getFeatureTypeByName(sourceResourceInfo.getName());
                    } else if (sourceStoreInfo instanceof CoverageStoreInfo) {
                        targetResourceInfo =
                                catalog.getCoverageByName(sourceResourceInfo.getName());
                    }
                    if (targetResourceInfo != null) {
                        targetLayerInfo = clone((LayerInfo) unwrap(l), targetResourceInfo);
                        catalog.add(targetLayerInfo);
                        catalog.save(catalog.getLayer(targetLayerInfo.getId()));
                    }
                }
            }
        }

        try {
            for (LayerGroupInfo lg : srcCatalog.getFacade().getLayerGroups()) {
                LayerGroupInfo targetLayerGroup = catalog.getLayerGroupByName(lg.getName());
                if (targetLayerGroup == null) {
                    WorkspaceInfo targetWorkspace =
                            lg.getWorkspace() != null
                                    ? catalog.getWorkspaceByName(lg.getWorkspace().getName())
                                    : null;
                    targetLayerGroup = clone((LayerGroupInfo) unwrap(lg), targetWorkspace);
                    catalog.add(targetLayerGroup);
                    catalog.save(catalog.getLayerGroup(targetLayerGroup.getId()));
                }
            }
        } catch (Exception e) {
            if (getCurrentJobExecution() != null) {
                getCurrentJobExecution().addWarningExceptions(Arrays.asList(e));
            }
        }

        // Set the original default WorkSpace and NameSpace
        if (srcCatalog.getFacade().getDefaultWorkspace() != null) {
            WorkspaceInfo targetDefaultWorkspace =
                    catalog.getWorkspaceByName(
                            srcCatalog.getFacade().getDefaultWorkspace().getName());
            catalog.setDefaultWorkspace(targetDefaultWorkspace);
        }

        if (srcCatalog.getFacade().getDefaultNamespace() != null) {
            NamespaceInfo targetDefaultNameSpace =
                    catalog.getNamespaceByPrefix(
                            srcCatalog.getFacade().getDefaultNamespace().getPrefix());
            catalog.setDefaultNamespace(targetDefaultNameSpace);
        }
    }

    protected NamespaceInfo clone(NamespaceInfo source) {
        NamespaceInfo target = catalog.getFactory().createNamespace();
        target.setPrefix(source.getPrefix());
        target.setURI(source.getURI());
        target.setIsolated(source.isIsolated());
        return target;
    }

    protected WorkspaceInfo clone(WorkspaceInfo source) {
        WorkspaceInfo target = catalog.getFactory().createWorkspace();
        target.setName(source.getName());
        target.setIsolated(source.isIsolated());
        return target;
    }

    protected StoreInfo clone(StoreInfo source, WorkspaceInfo workspace, Class type) {
        StoreInfo target = null;
        if (type == DataStoreInfo.class) {
            target = catalog.getFactory().createDataStore();
        } else if (type == CoverageStoreInfo.class) {
            target = catalog.getFactory().createCoverageStore();
        }

        if (target != null) {
            target.setWorkspace(workspace);
            target.setEnabled(source.isEnabled());
            target.setName(source.getName());
            target.setDescription(source.getDescription());
            target.setType(source.getType() != null ? source.getType() : "Shapefile");

            if (source instanceof DataStoreInfoImpl) {
                ((DataStoreInfoImpl) target).setDefault(((StoreInfoImpl) source).isDefault());
                ((DataStoreInfoImpl) target)
                        .setConnectionParameters(
                                ((DataStoreInfoImpl) source).getConnectionParameters());
                ((DataStoreInfoImpl) target).setMetadata(((StoreInfoImpl) source).getMetadata());
            }

            if (source instanceof CoverageStoreInfoImpl) {
                ((CoverageStoreInfoImpl) target).setURL(((CoverageStoreInfoImpl) source).getURL());
            }
        }

        if (type == DataStoreInfo.class && target.isEnabled()) {
            try {
                // test connection to data store
                ((DataStoreInfo) target).getDataStore(null);

                // connection ok
                LOGGER.info(
                        "Processed data store '"
                                + target.getName()
                                + "', "
                                + (target.isEnabled() ? "enabled" : "disabled"));
            } catch (Exception e) {
                LOGGER.warning("Error connecting to '" + target.getName() + "'");
                LOGGER.log(Level.INFO, "", e);

                target.setError(e);
                target.setEnabled(false);
            }
        }

        return target;
    }

    protected FeatureTypeInfo clone(
            FeatureTypeInfo source, NamespaceInfo namespace, StoreInfo store) {
        FeatureTypeInfo target = catalog.getFactory().createFeatureType();
        target.setStore(store);
        target.setNamespace(namespace);
        target.setAbstract(source.getAbstract());
        target.setAdvertised(source.isAdvertised());
        target.setCircularArcPresent(source.isCircularArcPresent());
        target.setCqlFilter(source.getCqlFilter());
        target.setDescription(source.getDescription());
        target.setEnabled(source.isEnabled());
        target.setLatLonBoundingBox(source.getLatLonBoundingBox());
        target.setLinearizationTolerance(source.getLinearizationTolerance());
        target.setMaxFeatures(source.getMaxFeatures());
        target.setName(source.getName());
        target.setNativeBoundingBox(source.getNativeBoundingBox());
        target.setNativeCRS(source.getNativeCRS());
        target.setNativeName(source.getNativeName());
        target.setNumDecimals(source.getNumDecimals());
        target.setOverridingServiceSRS(source.isOverridingServiceSRS());
        target.setProjectionPolicy(source.getProjectionPolicy());
        target.setSkipNumberMatched(source.getSkipNumberMatched());
        target.setSRS(source.getSRS());
        target.setTitle(source.getTitle());

        if (source instanceof FeatureTypeInfoImpl) {
            ((FeatureTypeInfoImpl) target)
                    .setMetadata(((FeatureTypeInfoImpl) source).getMetadata());
            ((FeatureTypeInfoImpl) target)
                    .setMetadataLinks(((FeatureTypeInfoImpl) source).getMetadataLinks());
            ((FeatureTypeInfoImpl) target).setAlias(((FeatureTypeInfoImpl) source).getAlias());
            ((FeatureTypeInfoImpl) target)
                    .setAttributes(((FeatureTypeInfoImpl) source).getAttributes());
            ((FeatureTypeInfoImpl) target)
                    .setDataLinks(((FeatureTypeInfoImpl) source).getDataLinks());
            ((FeatureTypeInfoImpl) target)
                    .setKeywords(((FeatureTypeInfoImpl) source).getKeywords());
            ((FeatureTypeInfoImpl) target)
                    .setResponseSRS(((FeatureTypeInfoImpl) source).getResponseSRS());
        }

        return target;
    }

    protected CoverageInfo clone(CoverageInfo source, NamespaceInfo namespace, StoreInfo store) {
        CoverageInfo target = catalog.getFactory().createCoverage();
        target.setStore(store);
        target.setNamespace(namespace);
        target.setAbstract(source.getAbstract());
        target.setAdvertised(source.isAdvertised());
        target.setDefaultInterpolationMethod(source.getDefaultInterpolationMethod());
        target.setDescription(source.getDescription());
        target.setEnabled(source.isEnabled());
        target.setGrid(source.getGrid());
        target.setLatLonBoundingBox(source.getLatLonBoundingBox());
        target.setName(source.getName());
        target.setNativeBoundingBox(source.getNativeBoundingBox());
        target.setNativeCRS(source.getNativeCRS());
        target.setNativeCoverageName(source.getNativeCoverageName());
        target.setNativeFormat(source.getNativeFormat());
        target.setNativeName(source.getNativeName());
        target.setProjectionPolicy(source.getProjectionPolicy());
        target.setSRS(source.getSRS());
        target.setTitle(source.getTitle());

        if (source instanceof CoverageInfoImpl) {
            ((CoverageInfoImpl) target).setDataLinks(((CoverageInfoImpl) source).getDataLinks());
            ((CoverageInfoImpl) target).setDimensions(((CoverageInfoImpl) source).getDimensions());
            ((CoverageInfoImpl) target)
                    .setInterpolationMethods(((CoverageInfoImpl) source).getInterpolationMethods());
            ((CoverageInfoImpl) target).setKeywords(((CoverageInfoImpl) source).getKeywords());
            ((CoverageInfoImpl) target).setMetadata(((CoverageInfoImpl) source).getMetadata());
            ((CoverageInfoImpl) target)
                    .setMetadataLinks(((CoverageInfoImpl) source).getMetadataLinks());
            ((CoverageInfoImpl) target).setParameters(((CoverageInfoImpl) source).getParameters());
            ((CoverageInfoImpl) target).setRequestSRS(((CoverageInfoImpl) source).getRequestSRS());
            ((CoverageInfoImpl) target)
                    .setResponseSRS(((CoverageInfoImpl) source).getResponseSRS());
            ((CoverageInfoImpl) target)
                    .setSupportedFormats(((CoverageInfoImpl) source).getSupportedFormats());
        }

        return target;
    }

    protected StyleInfo clone(StyleInfo source, WorkspaceInfo workspace) {
        StyleInfo target = catalog.getFactory().createStyle();
        target.setWorkspace(workspace);
        target.setFilename(source.getFilename());
        target.setFormat(source.getFormat());
        target.setFormatVersion(source.getFormatVersion());
        target.setLegend(source.getLegend());
        target.setName(source.getName());

        return target;
    }

    protected LayerInfo clone(LayerInfo source, ResourceInfo resourceInfo) {
        LayerInfo target = catalog.getFactory().createLayer();
        target.setResource(resourceInfo);
        target.setAbstract(source.getAbstract());
        target.setAdvertised(source.isAdvertised());
        target.setAttribution(source.getAttribution());
        target.setDefaultStyle(catalog.getStyleByName(source.getDefaultStyle().getName()));
        target.setDefaultWMSInterpolationMethod(source.getDefaultWMSInterpolationMethod());
        target.setEnabled(source.isEnabled());
        target.setLegend(source.getLegend());
        target.setName(source.getName());
        target.setOpaque(source.isOpaque());
        target.setPath(source.getPath());
        target.setQueryable(source.isQueryable());
        target.setTitle(source.getTitle());
        target.setType(source.getType());

        if (source instanceof LayerInfoImpl) {
            ((LayerInfoImpl) target).setAuthorityURLs(((LayerInfoImpl) source).getAuthorityURLs());
            ((LayerInfoImpl) target).setIdentifiers(((LayerInfoImpl) source).getIdentifiers());
            ((LayerInfoImpl) target).setMetadata(((LayerInfoImpl) source).getMetadata());
            ((LayerInfoImpl) target).setStyles(((LayerInfoImpl) source).getStyles());
        }

        return target;
    }

    protected LayerGroupInfo clone(LayerGroupInfo source, WorkspaceInfo workspace) {
        LayerGroupInfo target = catalog.getFactory().createLayerGroup();
        target.setWorkspace(workspace);
        target.setAbstract(source.getAbstract());
        target.setAttribution(source.getAttribution());
        ((LayerGroupInfoImpl) target).setAuthorityURLs(source.getAuthorityURLs());
        target.setBounds(source.getBounds());
        ((LayerGroupInfoImpl) target).setIdentifiers(source.getIdentifiers());
        ((LayerGroupInfoImpl) target).setKeywords(source.getKeywords());
        ((LayerGroupInfoImpl) target).setMetadata(source.getMetadata());
        ((LayerGroupInfoImpl) target).setMetadataLinks(source.getMetadataLinks());
        target.setMode(source.getMode());
        target.setName(source.getName());
        target.setTitle(source.getTitle());

        List<PublishedInfo> publishables = new ArrayList<PublishedInfo>();
        List<StyleInfo> styles = new ArrayList<StyleInfo>();
        for (int i = 0; i < source.getLayers().size(); i++) {
            PublishedInfo p = source.getLayers().get(i);
            boolean added = false;
            if (p instanceof LayerInfo) {
                LayerInfo tl = catalog.getLayerByName(p.getName());
                if (tl != null) {
                    publishables.add(tl);
                    added = true;
                }
            } else if (p instanceof LayerGroupInfo) {
                LayerGroupInfo tlg = catalog.getLayerGroupByName(p.getName());
                if (tlg != null) {
                    publishables.add(tlg);
                    added = true;
                }
            }

            if (added) {
                StyleInfo s = source.getStyles().get(i);
                if (s != null) {
                    StyleInfo ts = catalog.getStyleByName(s.getName());
                    styles.add(ts);
                } else {
                    styles.add(null);
                }
            }
        }
        ((LayerGroupInfoImpl) target).setLayers(publishables);
        ((LayerGroupInfoImpl) target).setStyles(styles);

        if (source.getRootLayer() != null) {
            LayerInfo l = catalog.getLayerByName(source.getRootLayer().getName());
            if (l != null) {
                ((LayerGroupInfoImpl) target).setRootLayer(l);
                if (source.getRootLayerStyle() != null) {
                    StyleInfo s = catalog.getStyleByName(source.getRootLayerStyle().getName());
                    if (s != null) {
                        ((LayerGroupInfoImpl) target).setRootLayerStyle(s);
                    }
                }
            }
        }

        return target;
    }

    private static class TokenizedFieldConverter implements Converter {

        Map<String, String> properties = new HashMap<>();

        public TokenizedFieldConverter(Map<String, String> passwordTokens) {
            this.properties = passwordTokens;
        }

        @Override
        public void marshal(
                Object source, HierarchicalStreamWriter writer, MarshallingContext context) {}

        @Override
        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
            String tokenizedValue = reader.getValue();
            String replacedValue = this.replaceTokenizedValue(tokenizedValue);
            // encrypt the value now?
            return replacedValue;
        }

        private String replaceTokenizedValue(String tokenizedValue) {
            return properties.getOrDefault(tokenizedValue, tokenizedValue);
        }

        @Override
        public boolean canConvert(Class type) {
            if (BackupRestoreItem.class.equals(type)) {
                return true;
            } else {
                return false;
            }
        }
    }

    public static class ParameterizedFieldsHolder {
        private StoreInfo storeInfo;
        private Set<String> fields;

        public ParameterizedFieldsHolder(StoreInfo storeInfo, Set<String> fields) {
            this.storeInfo = storeInfo;
            this.fields = fields;
        }

        public StoreInfo getStoreInfo() {
            return storeInfo;
        }

        public void setStoreInfo(StoreInfo storeInfo) {
            this.storeInfo = storeInfo;
        }

        public Set<String> getFields() {
            return fields;
        }

        public void setFields(Set<String> fields) {
            this.fields = fields;
        }
    }
}

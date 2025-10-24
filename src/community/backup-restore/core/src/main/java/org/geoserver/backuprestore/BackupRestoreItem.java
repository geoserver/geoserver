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
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.*;
import org.geoserver.catalog.impl.*;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.decorators.*;
import org.geotools.api.filter.Filter;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.util.Converters;
import org.geotools.util.logging.Logging;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.util.Assert;

/** Base item helper for backup/restore tasklets. */
public abstract class BackupRestoreItem<T> {

    private static final Logger LOGGER = Logging.getLogger(BackupRestoreItem.class);

    protected final Backup backupFacade;

    private Catalog catalog;

    protected XStreamPersister xstream;
    private XStream xp;

    private boolean isNew = true;
    private AbstractExecutionAdapter currentJobExecution;

    private boolean dryRun = true;
    private boolean bestEffort;

    private final XStreamPersisterFactory xStreamPersisterFactory;

    private Filter[] filters;

    public static final String ENCRYPTED_FIELDS_KEY = "backupRestoreParameterizedFields";

    public BackupRestoreItem(Backup backupFacade) {
        this.backupFacade = backupFacade;
        this.xStreamPersisterFactory = GeoServerExtensions.bean(XStreamPersisterFactory.class);
    }

    public XStreamPersisterFactory getxStreamPersisterFactory() {
        return xStreamPersisterFactory;
    }

    public XStream getXp() {
        return xp;
    }

    public void setXp(XStream xp) {
        this.xp = xp;
    }

    public Catalog getCatalog() {
        authenticate();
        return catalog;
    }

    public void authenticate() {
        backupFacade.authenticate();
    }

    public boolean isNew() {
        return isNew;
    }

    public AbstractExecutionAdapter getCurrentJobExecution() {
        return currentJobExecution;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public boolean isBestEffort() {
        return bestEffort;
    }

    public Filter[] getFilters() {
        return filters;
    }

    public void setFilters(Filter[] filters) {
        this.filters = filters;
    }

    /** @return true if at least one of the three filters is configured. */
    public boolean filterIsValid() {
        return this.filters != null
                && this.filters.length == 3
                && (this.filters[0] != null || this.filters[1] != null || this.filters[2] != null);
    }

    @BeforeStep
    public void retrieveInterstepData(StepExecution stepExecution) {
        // Establish which catalog we operate against and set up XStream
        JobExecution jobExecution = stepExecution.getJobExecution();
        this.xstream = xStreamPersisterFactory.createXMLPersister();

        if (backupFacade.getRestoreExecutions() != null
                && !backupFacade.getRestoreExecutions().isEmpty()
                && backupFacade.getRestoreExecutions().containsKey(jobExecution.getId())) {

            this.currentJobExecution = backupFacade.getRestoreExecutions().get(jobExecution.getId());
            this.catalog = ((RestoreExecutionAdapter) currentJobExecution).getRestoreCatalog();
            this.isNew = true; // restore uses a temp catalog
        } else {
            this.currentJobExecution = backupFacade.getBackupExecutions().get(jobExecution.getId());
            this.catalog = backupFacade.getCatalog();
            this.xstream.setExcludeIds(); // backup: exclude ids
            this.isNew = false;
        }

        Assert.notNull(this.catalog, "catalog must be set");

        this.xstream.setCatalog(this.catalog);
        this.xstream.setReferenceByName(true);
        this.xp = this.xstream.getXStream();
        Assert.notNull(this.xp, "xStream persister should not be NULL");

        // SB 5.x: no getString(key, default); use helpers
        JobParameters jobParameters = this.currentJobExecution.getDelegate().getJobParameters();

        boolean parameterizePasswords = getBoolean(jobParameters, Backup.PARAM_PARAMETERIZE_PASSWDS);

        if (parameterizePasswords) {
            // For backups, parameterize outgoing passwords;
            // for restores, de-tokenize incoming values.
            if (!isNew) {
                this.xp.registerLocalConverter(
                        StoreInfoImpl.class, "connectionParameters", this.createParameterizingMapConverter(xstream));
                this.xp.registerConverter(this.createStoreConverter(xstream));
            } else {
                String concatenatedPasswordTokens = getString(jobParameters, Backup.PARAM_PASSWORD_TOKENS);
                Map<String, String> passwordTokens = parseConcatenatedPasswordTokens(concatenatedPasswordTokens);
                this.xp.registerConverter(new TokenizedFieldConverter(passwordTokens));
                xstream.registerBriefMapComplexType("tokenizedPassword", BackupRestoreItem.class);
            }
        }

        this.dryRun = getBoolean(jobParameters, Backup.PARAM_DRY_RUN_MODE);
        this.bestEffort = getBoolean(jobParameters, Backup.PARAM_BEST_EFFORT_MODE);

        // Initialize Filters
        this.filters = new Filter[3];
        String cql = getString(jobParameters, "wsFilter");
        if (cql != null) {
            try {
                this.filters[0] = ECQL.toFilter(cql);
            } catch (CQLException e) {
                throw new IllegalArgumentException("Workspace Filter is not valid!", e);
            }
        }

        cql = getString(jobParameters, "siFilter");
        if (cql != null) {
            try {
                this.filters[1] = ECQL.toFilter(cql);
            } catch (CQLException e) {
                throw new IllegalArgumentException("Store Filter is not valid!", e);
            }
        }

        cql = getString(jobParameters, "liFilter");
        if (cql != null) {
            try {
                this.filters[2] = ECQL.toFilter(cql);
            } catch (CQLException e) {
                throw new IllegalArgumentException("Layer Filter is not valid!", e);
            }
        }

        initialize(stepExecution);
    }

    private static boolean getBoolean(JobParameters p, String key) {
        return Boolean.parseBoolean(getString(p, key, "false"));
    }

    private static String getString(JobParameters p, String key) {
        return getString(p, key, null);
    }

    private static String getString(JobParameters p, String key, String def) {
        if (p == null) return def;
        String v = p.getString(key);
        return v != null ? v : def;
        // (use getLong/getDate similarly when needed)
    }

    private Map<String, String> parseConcatenatedPasswordTokens(String concatenatedPasswordTokens) {
        Map<String, String> tokenMap = new HashMap<>();
        if (concatenatedPasswordTokens != null) {
            Arrays.stream(concatenatedPasswordTokens.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(tokenPair -> {
                        String[] tokenPairSplit = tokenPair.split("=", 2);
                        if (tokenPairSplit.length == 2) {
                            tokenMap.put(tokenPairSplit[0], tokenPairSplit[1]);
                        }
                    });
        }
        return tokenMap;
    }

    /** Step-specific initialization hook for subclasses. */
    protected abstract void initialize(StepExecution stepExecution);

    /** Log validation errors and rethrow/collect depending on best-effort mode. */
    public boolean logValidationExceptions(ValidationResult result, Exception e) throws Exception {
        CatalogException validationException = new CatalogException(e);
        if (!isBestEffort()) {
            if (result != null) result.throwIfInvalid();
            else throw e;
            // if we reach here, it means result was valid—no error to record
            return true;
        }
        // best-effort: collect as warning
        getCurrentJobExecution().addWarningExceptions(List.of(validationException));
        return false;
    }

    public boolean logValidationExceptions(T resource, Throwable e) {
        CatalogException ve =
                (e != null) ? new CatalogException(e) : new CatalogException("Invalid resource: " + resource);
        if (!isBestEffort()) {
            getCurrentJobExecution().addFailureExceptions(List.of(ve));
            throw ve;
        } else {
            getCurrentJobExecution().addWarningExceptions(List.of(ve));
            return false;
        }
    }

    /** Apply workspace/store/resource filters. */
    protected boolean filteredResource(T resource, WorkspaceInfo ws, boolean strict, Class<?> clazz) {
        if (!filterIsValid()) return false;

        if (resource == null || clazz == WorkspaceInfo.class) {
            if ((strict && ws == null) || (ws != null && getFilters()[0] != null && !getFilters()[0].evaluate(ws))) {
                LOGGER.info("Skipped filtered workspace: " + ws);
                return true;
            }
        }

        boolean skip = false;
        if (resource != null) {
            if (clazz == StoreInfo.class) {
                skip = getFilters()[1] != null && !getFilters()[1].evaluate(resource);
            } else if (clazz == LayerInfo.class) {
                skip = getFilters()[2] != null && !getFilters()[2].evaluate(resource);
            } else if (clazz == ResourceInfo.class) {
                skip = ((ResourceInfo) resource).getStore() == null
                        || (getFilters()[2] != null && !getFilters()[2].evaluate(resource));
            }
        }
        if (skip) LOGGER.info("Skipped filtered resource: " + resource);
        return skip;
    }

    protected boolean filteredResource(WorkspaceInfo ws, boolean strict) {
        return filteredResource(null, ws, strict, WorkspaceInfo.class);
    }

    private MapConverter createParameterizingMapConverter(XStreamPersister xstream) {
        return xstream.new BriefMapConverter() {
            @Override
            public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
                ParameterizedFieldsHolder fieldsToParametrize =
                        (ParameterizedFieldsHolder) context.get(ENCRYPTED_FIELDS_KEY);

                Map<?, ?> map = (Map<?, ?>) source;
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (entry.getValue() == null) continue;

                    writer.startNode("entry");
                    writer.addAttribute("key", String.valueOf(entry.getKey()));
                    Object value = entry.getValue();
                    String complexTypeId = getComplexTypeId(value.getClass());

                    if (complexTypeId == null) {
                        String str = Converters.convert(value, String.class);
                        if (str == null) str = value.toString();

                        if (fieldsToParametrize != null
                                && fieldsToParametrize.getFields().contains(entry.getKey())) {
                            writer.startNode("tokenizedPassword");
                            str = "${"
                                    + fieldsToParametrize
                                            .getStoreInfo()
                                            .getWorkspace()
                                            .getName()
                                    + ":"
                                    + fieldsToParametrize.getStoreInfo().getName()
                                    + "."
                                    + entry.getKey()
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
            protected void doMarshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
                GeoServerSecurityManager secMgr =
                        xstream.isEncryptPasswordFields() ? xstream.getSecurityManager() : null;
                if (secMgr != null && secMgr.isInitialized()) {
                    Set<String> encryptedFields =
                            secMgr.getConfigPasswordEncryptionHelper().getEncryptedFields((StoreInfo) source);
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
            public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {}

            @Override
            public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
                String tokenizedValue = reader.getValue();
                return replaceTokenizedValue(tokenizedValue);
            }

            private String replaceTokenizedValue(String tokenizedValue) {
                return "foo";
            }

            @Override
            public boolean canConvert(@SuppressWarnings("rawtypes") Class type) {
                return BackupRestoreItem.class.equals(type);
            }
        };
    }

    private Object unwrap(Object item) {
        if (item instanceof Proxy) {
            item = ProxyUtils.unwrap(item, Proxy.getInvocationHandler(item).getClass());
        }
        return item;
    }

    private ResourceInfo unwrapSecured(ResourceInfo info) {
        if (info instanceof SecuredFeatureTypeInfo typeInfo) return typeInfo.unwrap(ResourceInfo.class);
        if (info instanceof SecuredCoverageInfo coverageInfo) return coverageInfo.unwrap(ResourceInfo.class);
        if (info instanceof SecuredWMSLayerInfo layerInfo) return layerInfo.unwrap(ResourceInfo.class);
        if (info instanceof SecuredWMTSLayerInfo layerInfo) return layerInfo.unwrap(ResourceInfo.class);
        return info;
    }

    private StoreInfo unwrapSecured(StoreInfo info) {
        if (info instanceof SecuredDataStoreInfo s) return s.unwrap(StoreInfo.class);
        if (info instanceof SecuredCoverageStoreInfo s) return s.unwrap(StoreInfo.class);
        if (info instanceof SecuredWMSStoreInfo s) return s.unwrap(StoreInfo.class);
        if (info instanceof SecuredWMTSStoreInfo s) return s.unwrap(StoreInfo.class);
        return info;
    }

    /** Sync all objects from srcCatalog into this catalog. */
    protected void syncTo(Catalog srcCatalog) {
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
                WorkspaceInfo targetWorkspace = store.getWorkspace() != null
                        ? catalog.getWorkspaceByName(store.getWorkspace().getName())
                        : null;
                targetDataStore = (DataStoreInfo)
                        clone((DataStoreInfo) unwrap(unwrapSecured(store)), targetWorkspace, DataStoreInfo.class);
                if (targetDataStore != null) {
                    catalog.add(targetDataStore);
                    catalog.save(catalog.getDataStore(targetDataStore.getId()));
                }
            }
        }
        for (ResourceInfo resource : srcCatalog.getFacade().getResources(FeatureTypeInfo.class)) {
            FeatureTypeInfo targetResource = catalog.getResourceByName(resource.getName(), FeatureTypeInfo.class);
            if (targetResource == null) {
                DataStoreInfo targetDataStore =
                        catalog.getDataStoreByName(resource.getStore().getName());
                NamespaceInfo targetNamespace = resource.getNamespace() != null
                        ? catalog.getNamespaceByPrefix(resource.getNamespace().getPrefix())
                        : null;
                if (targetDataStore != null) {
                    targetResource =
                            clone((FeatureTypeInfo) unwrap(unwrapSecured(resource)), targetNamespace, targetDataStore);
                    catalog.add(targetResource);
                    catalog.save(catalog.getResource(targetResource.getId(), FeatureTypeInfo.class));
                }
            }
        }

        // WMSStores
        for (StoreInfo store : srcCatalog.getFacade().getStores(WMSStoreInfo.class)) {
            WMSStoreInfo targetWMSStore = catalog.getWMSStoreByName(store.getName());
            if (targetWMSStore == null) {
                WorkspaceInfo targetWorkspace = store.getWorkspace() != null
                        ? catalog.getWorkspaceByName(store.getWorkspace().getName())
                        : null;
                targetWMSStore = (WMSStoreInfo)
                        clone((WMSStoreInfo) unwrap(unwrapSecured(store)), targetWorkspace, WMSStoreInfo.class);
                if (targetWMSStore != null) {
                    catalog.add(targetWMSStore);
                    catalog.save(catalog.getStore(targetWMSStore.getId(), WMSStoreInfo.class));
                }
            }
        }

        // WMTSStores
        for (StoreInfo store : srcCatalog.getFacade().getStores(WMTSStoreInfo.class)) {
            WMTSStoreInfo targetWMTSStore = catalog.getWMTSStoreByName(store.getName());
            if (targetWMTSStore == null) {
                WorkspaceInfo targetWorkspace = store.getWorkspace() != null
                        ? catalog.getWorkspaceByName(store.getWorkspace().getName())
                        : null;
                targetWMTSStore = (WMTSStoreInfo)
                        clone((WMTSStoreInfo) unwrap(unwrapSecured(store)), targetWorkspace, WMTSStoreInfo.class);
                if (targetWMTSStore != null) {
                    catalog.add(targetWMTSStore);
                    catalog.save(catalog.getStore(targetWMTSStore.getId(), WMTSStoreInfo.class));
                }
            }
        }

        // CoverageStores
        for (StoreInfo store : srcCatalog.getFacade().getStores(CoverageStoreInfo.class)) {
            CoverageStoreInfo targetCoverageStore = catalog.getCoverageStoreByName(store.getName());
            if (targetCoverageStore == null) {
                WorkspaceInfo targetWorkspace = store.getWorkspace() != null
                        ? catalog.getWorkspaceByName(store.getWorkspace().getName())
                        : null;
                targetCoverageStore = (CoverageStoreInfo) clone(
                        (CoverageStoreInfo) unwrap(unwrapSecured(store)), targetWorkspace, CoverageStoreInfo.class);
                if (targetCoverageStore != null) {
                    catalog.add(targetCoverageStore);
                    catalog.save(catalog.getCoverageStore(targetCoverageStore.getId()));
                }
            }
        }
        for (ResourceInfo resource : srcCatalog.getFacade().getResources(CoverageInfo.class)) {
            CoverageInfo targetResource = catalog.getResourceByName(resource.getName(), CoverageInfo.class);
            if (targetResource == null) {
                CoverageStoreInfo targetCoverageStore =
                        catalog.getCoverageStoreByName(resource.getStore().getName());
                NamespaceInfo targetNamespace = resource.getNamespace() != null
                        ? catalog.getNamespaceByPrefix(resource.getNamespace().getPrefix())
                        : null;
                if (targetCoverageStore != null) {
                    targetResource =
                            clone((CoverageInfo) unwrap(unwrapSecured(resource)), targetNamespace, targetCoverageStore);
                    catalog.add(targetResource);
                    catalog.save(catalog.getResource(targetResource.getId(), CoverageInfo.class));
                }
            }
        }

        // Styles
        for (StyleInfo s : srcCatalog.getFacade().getStyles()) {
            StyleInfo targetStyle = catalog.getStyleByName(s.getName());
            if (targetStyle == null) {
                WorkspaceInfo targetWorkspace = s.getWorkspace() != null
                        ? catalog.getWorkspaceByName(s.getWorkspace().getName())
                        : null;
                targetStyle = clone((StyleInfo) unwrap(s), targetWorkspace);
                catalog.add(targetStyle);
                catalog.save(catalog.getStyle(targetStyle.getId()));
            }
        }

        // Layers
        for (LayerInfo l : srcCatalog.getFacade().getLayers()) {
            LayerInfo targetLayerInfo = catalog.getLayerByName(l.getName());
            if (targetLayerInfo == null) {
                ResourceInfo sourceResourceInfo = l.getResource();
                StoreInfo sourceStoreInfo = sourceResourceInfo.getStore();
                StoreInfo targetStoreInfo = null;
                if (sourceStoreInfo instanceof DataStoreInfo) {
                    targetStoreInfo = catalog.getStoreByName(sourceStoreInfo.getName(), DataStoreInfo.class);
                } else if (sourceStoreInfo instanceof CoverageStoreInfo) {
                    targetStoreInfo = catalog.getStoreByName(sourceStoreInfo.getName(), CoverageStoreInfo.class);
                }
                if (targetStoreInfo != null) {
                    ResourceInfo targetResourceInfo = null;
                    if (sourceStoreInfo instanceof DataStoreInfo) {
                        targetResourceInfo = catalog.getFeatureTypeByName(sourceResourceInfo.getName());
                    } else if (sourceStoreInfo instanceof CoverageStoreInfo) {
                        targetResourceInfo = catalog.getCoverageByName(sourceResourceInfo.getName());
                    }
                    if (targetResourceInfo != null) {
                        targetLayerInfo = clone((LayerInfo) unwrap(l), targetResourceInfo);
                        catalog.add(targetLayerInfo);
                        catalog.save(catalog.getLayer(targetLayerInfo.getId()));
                    }
                }
            }
        }

        // LayerGroups
        try {
            for (LayerGroupInfo lg : srcCatalog.getFacade().getLayerGroups()) {
                LayerGroupInfo targetLayerGroup = catalog.getLayerGroupByName(lg.getName());
                if (targetLayerGroup == null) {
                    WorkspaceInfo targetWorkspace = lg.getWorkspace() != null
                            ? catalog.getWorkspaceByName(lg.getWorkspace().getName())
                            : null;
                    targetLayerGroup = clone((LayerGroupInfo) unwrap(lg), targetWorkspace);
                    catalog.add(targetLayerGroup);
                    catalog.save(catalog.getLayerGroup(targetLayerGroup.getId()));
                }
            }
        } catch (Exception e) {
            if (getCurrentJobExecution() != null) {
                getCurrentJobExecution().addWarningExceptions(List.of(e));
            }
        }

        // Defaults
        if (srcCatalog.getFacade().getDefaultWorkspace() != null) {
            WorkspaceInfo targetDefaultWorkspace = catalog.getWorkspaceByName(
                    srcCatalog.getFacade().getDefaultWorkspace().getName());
            catalog.setDefaultWorkspace(targetDefaultWorkspace);
        }
        if (srcCatalog.getFacade().getDefaultNamespace() != null) {
            NamespaceInfo targetDefaultNameSpace = catalog.getNamespaceByPrefix(
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

    protected StoreInfo clone(StoreInfo source, WorkspaceInfo workspace, Class<?> type) {
        StoreInfo target = null;
        if (type == DataStoreInfo.class) {
            target = catalog.getFactory().createDataStore();
        } else if (type == CoverageStoreInfo.class) {
            target = catalog.getFactory().createCoverageStore();
        } else if (type == WMSStoreInfo.class) {
            target = catalog.getFactory().createWebMapServer();
        } else if (type == WMTSStoreInfo.class) {
            target = catalog.getFactory().createWebMapTileServer();
        }

        if (target != null) {
            target.setWorkspace(workspace);
            target.setEnabled(source.isEnabled());
            target.setName(source.getName());
            target.setDescription(source.getDescription());
            target.setType(source.getType() != null ? source.getType() : "Shapefile");

            if (source instanceof DataStoreInfoImpl impl) {
                ((DataStoreInfoImpl) target).setDefault(((StoreInfoImpl) source).isDefault());
                ((DataStoreInfoImpl) target).setConnectionParameters(impl.getConnectionParameters());
                ((DataStoreInfoImpl) target).setMetadata(((StoreInfoImpl) source).getMetadata());
            }
            if (source instanceof CoverageStoreInfoImpl impl) {
                ((CoverageStoreInfoImpl) target).setURL(impl.getURL());
            }
            if (source instanceof WMSStoreInfoImpl impl) {
                ((WMSStoreInfoImpl) target).setCapabilitiesURL(impl.getCapabilitiesURL());
                ((WMSStoreInfoImpl) target).setUsername(impl.getUsername());
                ((WMSStoreInfoImpl) target).setPassword(impl.getPassword());
                ((WMSStoreInfoImpl) target).setConnectTimeout(impl.getConnectTimeout());
                ((WMSStoreInfoImpl) target).setMaxConnections(impl.getMaxConnections());
                ((WMSStoreInfoImpl) target).setReadTimeout(impl.getReadTimeout());
                ((WMSStoreInfoImpl) target).setUseConnectionPooling(impl.isUseConnectionPooling());
            }
            if (source instanceof WMTSStoreInfoImpl impl) {
                ((WMTSStoreInfoImpl) target).setCapabilitiesURL(impl.getCapabilitiesURL());
                ((WMTSStoreInfoImpl) target).setUsername(impl.getUsername());
                ((WMTSStoreInfoImpl) target).setPassword(impl.getPassword());
                ((WMTSStoreInfoImpl) target).setConnectTimeout(impl.getConnectTimeout());
                ((WMTSStoreInfoImpl) target).setMaxConnections(impl.getMaxConnections());
                ((WMTSStoreInfoImpl) target).setReadTimeout(impl.getReadTimeout());
                ((WMTSStoreInfoImpl) target).setUseConnectionPooling(impl.isUseConnectionPooling());
            }
        }

        if (type == DataStoreInfo.class && target != null && target.isEnabled()) {
            try {
                ((DataStoreInfo) target).getDataStore(null); // validate connection
                LOGGER.config("Processed data store '"
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

    protected FeatureTypeInfo clone(FeatureTypeInfo source, NamespaceInfo namespace, StoreInfo store) {
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

        if (source instanceof FeatureTypeInfoImpl impl) {
            ((FeatureTypeInfoImpl) target).setMetadata(impl.getMetadata());
            ((FeatureTypeInfoImpl) target).setMetadataLinks(impl.getMetadataLinks());
            ((FeatureTypeInfoImpl) target).setAlias(impl.getAlias());
            ((FeatureTypeInfoImpl) target).setAttributes(impl.getAttributes());
            ((FeatureTypeInfoImpl) target).setDataLinks(impl.getDataLinks());
            ((FeatureTypeInfoImpl) target).setKeywords(impl.getKeywords());
            ((FeatureTypeInfoImpl) target).setResponseSRS(impl.getResponseSRS());
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

        if (source instanceof CoverageInfoImpl impl) {
            ((CoverageInfoImpl) target).setDataLinks(impl.getDataLinks());
            ((CoverageInfoImpl) target).setDimensions(impl.getDimensions());
            ((CoverageInfoImpl) target).setInterpolationMethods(impl.getInterpolationMethods());
            ((CoverageInfoImpl) target).setKeywords(impl.getKeywords());
            ((CoverageInfoImpl) target).setMetadata(impl.getMetadata());
            ((CoverageInfoImpl) target).setMetadataLinks(impl.getMetadataLinks());
            ((CoverageInfoImpl) target).setParameters(impl.getParameters());
            ((CoverageInfoImpl) target).setRequestSRS(impl.getRequestSRS());
            ((CoverageInfoImpl) target).setResponseSRS(impl.getResponseSRS());
            ((CoverageInfoImpl) target).setSupportedFormats(impl.getSupportedFormats());
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

        if (source instanceof LayerInfoImpl impl) {
            ((LayerInfoImpl) target).setAuthorityURLs(impl.getAuthorityURLs());
            ((LayerInfoImpl) target).setIdentifiers(impl.getIdentifiers());
            ((LayerInfoImpl) target).setMetadata(impl.getMetadata());
            ((LayerInfoImpl) target).setStyles(impl.getStyles());
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

        List<PublishedInfo> publishables = new ArrayList<>();
        List<StyleInfo> styles = new ArrayList<>();
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
                    styles.add(catalog.getStyleByName(s.getName()));
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
                    StyleInfo s =
                            catalog.getStyleByName(source.getRootLayerStyle().getName());
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
            if (passwordTokens != null) this.properties.putAll(passwordTokens);
        }

        @Override
        public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {}

        @Override
        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
            String tokenizedValue = reader.getValue();
            return properties.getOrDefault(tokenizedValue, tokenizedValue);
        }

        @Override
        public boolean canConvert(@SuppressWarnings("rawtypes") Class type) {
            return BackupRestoreItem.class.equals(type);
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

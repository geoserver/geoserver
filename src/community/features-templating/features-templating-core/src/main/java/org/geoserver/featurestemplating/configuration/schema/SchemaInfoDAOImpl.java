/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.configuration.schema;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogBeforeAddEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resource;
import org.geoserver.security.PropertyFileWatcher;

/** A template info DAO that use a property file for persistence. */
public class SchemaInfoDAOImpl implements SchemaInfoDAO {

    private SortedSet<SchemaInfo> schemaDataSet;

    private PropertyFileWatcher fileWatcher;

    private GeoServerDataDirectory dd;

    private Set<SchemaDAOListener> listeners;

    private static final String PROPERTY_FILE_NAME = "schema-data.properties";

    public SchemaInfoDAOImpl(GeoServerDataDirectory dd) {
        this.dd = dd;
        Resource schemaDir = dd.get(SCHEMA_DIR);
        File dir = schemaDir.dir();
        if (!dir.exists()) dir.mkdir();
        Resource prop = dd.get(SCHEMA_DIR, PROPERTY_FILE_NAME);
        prop.file();
        this.fileWatcher = new PropertyFileWatcher(prop);
        this.schemaDataSet = Collections.synchronizedSortedSet(new TreeSet<>());
        this.listeners = new HashSet<>();
        Catalog catalog = (Catalog) GeoServerExtensions.bean("catalog");
        catalog.addListener(new CatalogListenerSchemaInfo());
    }

    @Override
    public List<SchemaInfo> findAll() {
        reloadIfNeeded();
        return new ArrayList<>(schemaDataSet);
    }

    @Override
    public SchemaInfo saveOrUpdate(SchemaInfo templateData) {
        reloadIfNeeded();
        boolean isUpdate =
                schemaDataSet.stream().anyMatch(ti -> ti.getIdentifier().equals(templateData.getIdentifier()));
        if (isUpdate) {
            fireTemplateUpdateEvent(templateData);
            schemaDataSet.removeIf(ti -> ti.getIdentifier().equals(templateData.getIdentifier()));
        }

        schemaDataSet.add(templateData);
        storeProperties();
        return templateData;
    }

    @Override
    public void delete(SchemaInfo templateData) {
        reloadIfNeeded();
        schemaDataSet.remove(templateData);
        fireSchemaInfoRemoveEvent(templateData);
        storeProperties();
    }

    @Override
    public void delete(List<SchemaInfo> SchemaInfos) {
        reloadIfNeeded();
        schemaDataSet.removeAll(SchemaInfos);
        storeProperties();
        for (SchemaInfo ti : SchemaInfos) fireSchemaInfoRemoveEvent(ti);
    }

    @Override
    public void deleteAll() {
        reloadIfNeeded();
        Set<SchemaInfo> SchemaInfos = schemaDataSet;
        schemaDataSet = Collections.synchronizedSortedSet(new TreeSet<>());
        storeProperties();
        for (SchemaInfo ti : SchemaInfos) fireSchemaInfoRemoveEvent(ti);
    }

    @Override
    public SchemaInfo findById(String id) {
        reloadIfNeeded();
        Optional<SchemaInfo> optional = schemaDataSet.stream()
                .filter(ti -> ti.getIdentifier().equals(id))
                .findFirst();
        if (optional.isPresent()) return optional.get();
        else return null;
    }

    @Override
    public SchemaInfo findByFullName(String fullName) {
        reloadIfNeeded();
        Optional<SchemaInfo> SchemaInfo = schemaDataSet.stream()
                .filter(ti -> ti.getFullName().equals(fullName))
                .findFirst();
        if (SchemaInfo.isPresent()) return SchemaInfo.get();
        return null;
    }

    @Override
    public List<SchemaInfo> findByFeatureTypeInfo(FeatureTypeInfo featureTypeInfo) {
        reloadIfNeeded();
        String workspace = featureTypeInfo.getStore().getWorkspace().getName();
        String name = featureTypeInfo.getName();
        return schemaDataSet.stream()
                .filter(ti -> (ti.getWorkspace() == null && ti.getFeatureType() == null)
                        || ti.getFeatureType() == null && ti.getWorkspace().equals(workspace)
                        || (ti.getWorkspace().equals(workspace)
                                && ti.getFeatureType().equals(name)))
                .collect(Collectors.toList());
    }

    private void fireTemplateUpdateEvent(SchemaInfo SchemaInfo) {
        for (SchemaDAOListener listener : listeners) {
            listener.handleUpdateEvent(new SchemaInfoEvent(SchemaInfo));
        }
    }

    private void fireSchemaInfoRemoveEvent(SchemaInfo SchemaInfo) {
        for (SchemaDAOListener listener : listeners) {
            listener.handleDeleteEvent(new SchemaInfoEvent(SchemaInfo));
        }
    }

    @Override
    public void addTemplateListener(SchemaDAOListener listener) {
        this.listeners.add(listener);
    }

    private SchemaInfo parseProperty(String key, String value) {
        SchemaInfo schemaData = new SchemaInfo();
        schemaData.setIdentifier(key);
        String[] values = value.split(";");
        for (String v : values) {
            String[] attribute = v.split("=");
            String attrName = attribute[0];
            String attrValue = attribute[1];
            if (attrName.equals("schemaName")) schemaData.setSchemaName(attrValue);
            else if (attrName.equals("extension")) schemaData.setExtension(attrValue);
            else if (attrName.equals("workspace")) schemaData.setWorkspace(attrValue);
            else if (attrName.equals("featureTypeInfo")) schemaData.setFeatureType(attrValue);
        }
        schemaData.setIdentifier(key);
        return schemaData;
    }

    private Properties toProperties() {
        Properties properties = new Properties();
        for (SchemaInfo td : schemaDataSet) {
            StringBuilder sb = new StringBuilder();
            sb.append("schemaName=")
                    .append(td.getSchemaName())
                    .append(";extension=")
                    .append(td.getExtension());
            String ws = td.getWorkspace();
            if (ws != null) sb.append(";workspace=").append(td.getWorkspace());
            String fti = td.getFeatureType();
            if (fti != null) sb.append(";featureTypeInfo=").append(td.getFeatureType());
            properties.put(td.getIdentifier(), sb.toString());
        }
        return properties;
    }

    public void storeProperties() {
        Properties p = toProperties();
        Resource propFile = dd.get(SCHEMA_DIR, PROPERTY_FILE_NAME);
        try (OutputStream os = propFile.out()) {
            p.store(os, null);
        } catch (Exception e) {
            throw new RuntimeException("Could not write rules to " + PROPERTY_FILE_NAME);
        }
    }

    private boolean isModified() {
        return fileWatcher != null && fileWatcher.isStale();
    }

    private void loadSchemaInfo() {
        try {
            Properties properties = fileWatcher.getProperties();
            this.schemaDataSet = Collections.synchronizedSortedSet(new TreeSet<>());
            for (Object k : properties.keySet()) {
                SchemaInfo td = parseProperty(k.toString(), properties.getProperty(k.toString()));
                this.schemaDataSet.add(td);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void reloadIfNeeded() {
        if (isModified() || schemaDataSet.isEmpty()) loadSchemaInfo();
    }

    public static class CatalogListenerSchemaInfo implements CatalogListener {
        @Override
        public void handlePreAddEvent(CatalogBeforeAddEvent event) throws CatalogException {}

        @Override
        public void handleAddEvent(CatalogAddEvent event) throws CatalogException {}

        @Override
        public void handleRemoveEvent(CatalogRemoveEvent event) throws CatalogException {
            CatalogInfo source = event.getSource();
            if (source instanceof FeatureTypeInfo) {
                removeFtTemplates((FeatureTypeInfo) source);

            } else if (source instanceof WorkspaceInfo) {
                removeWSTemplates((WorkspaceInfo) source);
            }
        }

        private void removeFtTemplates(FeatureTypeInfo ft) {
            SchemaInfoDAO dao = SchemaInfoDAO.get();
            List<SchemaInfo> SchemaInfos = dao.findByFeatureTypeInfo(ft);
            dao.delete(SchemaInfos.stream()
                    .filter(ti -> ti.getFeatureType() != null)
                    .collect(Collectors.toList()));
        }

        private void removeWSTemplates(WorkspaceInfo ws) {
            SchemaInfoDAO dao = SchemaInfoDAO.get();
            List<SchemaInfo> SchemaInfos = dao.findAll().stream()
                    .filter(ti -> ti.getWorkspace().equals(ws.getName()))
                    .collect(Collectors.toList());
            dao.delete(SchemaInfos);
        }

        @Override
        public void handleModifyEvent(CatalogModifyEvent event) throws CatalogException {
            final CatalogInfo source = event.getSource();
            if (source instanceof FeatureTypeInfo) {
                int nameIdx = event.getPropertyNames().indexOf("name");
                if (nameIdx != -1) {
                    String newName = (String) event.getNewValues().get(nameIdx);
                    updateSchemaInfoLayerName((FeatureTypeInfo) source, newName);
                }
            } else if (source instanceof WorkspaceInfo) {
                int nameIdx = event.getPropertyNames().indexOf("name");
                if (nameIdx != -1) {
                    String oldName = (String) event.getOldValues().get(nameIdx);
                    String newName = (String) event.getNewValues().get(nameIdx);
                    updateWorkspaceNames(oldName, newName);
                }
            }
        }

        private void updateSchemaInfoLayerName(FeatureTypeInfo fti, String newName) {
            SchemaInfoDAO dao = SchemaInfoDAO.get();
            List<SchemaInfo> SchemaInfo = dao.findByFeatureTypeInfo(fti);
            for (SchemaInfo ti : SchemaInfo) {
                ti.setFeatureType(newName);
            }
            ((SchemaInfoDAOImpl) dao).storeProperties();
        }

        private void updateSchemaInfoWorkspace(WorkspaceInfo wi, FeatureTypeInfo fti) {
            SchemaInfoDAO dao = SchemaInfoDAO.get();
            List<SchemaInfo> SchemaInfo = dao.findByFeatureTypeInfo(fti);
            for (SchemaInfo ti : SchemaInfo) {
                ti.setWorkspace(wi.getName());
            }
            ((SchemaInfoDAOImpl) dao).storeProperties();
        }

        private void updateWorkspaceNames(String oldName, String newName) {
            SchemaInfoDAO dao = SchemaInfoDAO.get();
            List<SchemaInfo> infos = dao.findAll();
            for (SchemaInfo ti : infos) {
                if (ti.getWorkspace().equals(oldName)) ti.setWorkspace(newName);
            }
            ((SchemaInfoDAOImpl) dao).storeProperties();
        }

        @Override
        public void handlePostModifyEvent(CatalogPostModifyEvent event) throws CatalogException {
            CatalogInfo source = event.getSource();
            if (source instanceof FeatureTypeInfo) {
                FeatureTypeInfo info = (FeatureTypeInfo) source;
                int wsIdx = event.getPropertyNames().indexOf("workspace");
                if (wsIdx != -1) {
                    WorkspaceInfo newWorkspace =
                            (WorkspaceInfo) event.getNewValues().get(wsIdx);
                    updateSchemaInfoWorkspace(newWorkspace, info);
                }
            }
        }

        @Override
        public void reloaded() {}
    }
}

/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.configuration;

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
public class TemplateInfoDAOImpl implements TemplateInfoDAO {

    private SortedSet<TemplateInfo> templateDataSet;

    private PropertyFileWatcher fileWatcher;

    private GeoServerDataDirectory dd;

    private Set<TemplateDAOListener> listeners;

    private static final String PROPERTY_FILE_NAME = "features-templates-data.properties";

    public TemplateInfoDAOImpl(GeoServerDataDirectory dd) {
        this.dd = dd;
        Resource templateDir = dd.get(TEMPLATE_DIR);
        File dir = templateDir.dir();
        if (!dir.exists()) dir.mkdir();
        Resource prop = dd.get(TEMPLATE_DIR, PROPERTY_FILE_NAME);
        prop.file();
        this.fileWatcher = new PropertyFileWatcher(prop);
        this.templateDataSet = Collections.synchronizedSortedSet(new TreeSet<>());
        this.listeners = new HashSet<>();
        Catalog catalog = (Catalog) GeoServerExtensions.bean("catalog");
        catalog.addListener(new CatalogListenerTemplateInfo());
    }

    @Override
    public List<TemplateInfo> findAll() {
        reloadIfNeeded();
        return new ArrayList<>(templateDataSet);
    }

    @Override
    public TemplateInfo saveOrUpdate(TemplateInfo templateData) {
        reloadIfNeeded();
        boolean isUpdate =
                templateDataSet.stream()
                        .anyMatch(ti -> ti.getIdentifier().equals(templateData.getIdentifier()));
        if (isUpdate) {
            fireTemplateUpdateEvent(templateData);
            templateDataSet.removeIf(ti -> ti.getIdentifier().equals(templateData.getIdentifier()));
        }

        templateDataSet.add(templateData);
        storeProperties();
        return templateData;
    }

    @Override
    public void delete(TemplateInfo templateData) {
        reloadIfNeeded();
        templateDataSet.remove(templateData);
        fireTemplateInfoRemoveEvent(templateData);
        storeProperties();
    }

    @Override
    public void delete(List<TemplateInfo> templateInfos) {
        reloadIfNeeded();
        templateDataSet.removeAll(templateInfos);
        storeProperties();
        for (TemplateInfo ti : templateInfos) fireTemplateInfoRemoveEvent(ti);
    }

    @Override
    public void deleteAll() {
        reloadIfNeeded();
        Set<TemplateInfo> templateInfos = templateDataSet;
        templateDataSet = Collections.synchronizedSortedSet(new TreeSet<>());
        storeProperties();
        for (TemplateInfo ti : templateInfos) fireTemplateInfoRemoveEvent(ti);
    }

    @Override
    public TemplateInfo findById(String id) {
        reloadIfNeeded();
        Optional<TemplateInfo> optional =
                templateDataSet.stream().filter(ti -> ti.getIdentifier().equals(id)).findFirst();
        if (optional.isPresent()) return optional.get();
        else return null;
    }

    @Override
    public TemplateInfo findByFullName(String fullName) {
        reloadIfNeeded();
        Optional<TemplateInfo> templateInfo =
                templateDataSet.stream()
                        .filter(ti -> ti.getFullName().equals(fullName))
                        .findFirst();
        if (templateInfo.isPresent()) return templateInfo.get();
        return null;
    }

    @Override
    public List<TemplateInfo> findByFeatureTypeInfo(FeatureTypeInfo featureTypeInfo) {
        reloadIfNeeded();
        String workspace = featureTypeInfo.getStore().getWorkspace().getName();
        String name = featureTypeInfo.getName();
        return templateDataSet.stream()
                .filter(
                        ti ->
                                (ti.getWorkspace() == null && ti.getFeatureType() == null)
                                        || ti.getFeatureType() == null
                                                && ti.getWorkspace().equals(workspace)
                                        || (ti.getWorkspace().equals(workspace)
                                                && ti.getFeatureType().equals(name)))
                .collect(Collectors.toList());
    }

    private void fireTemplateUpdateEvent(TemplateInfo templateInfo) {
        for (TemplateDAOListener listener : listeners) {
            listener.handleUpdateEvent(new TemplateInfoEvent(templateInfo));
        }
    }

    private void fireTemplateInfoRemoveEvent(TemplateInfo templateInfo) {
        for (TemplateDAOListener listener : listeners) {
            listener.handleDeleteEvent(new TemplateInfoEvent(templateInfo));
        }
    }

    @Override
    public void addTemplateListener(TemplateDAOListener listener) {
        this.listeners.add(listener);
    }

    private TemplateInfo parseProperty(String key, String value) {
        TemplateInfo templateData = new TemplateInfo();
        templateData.setIdentifier(key);
        String[] values = value.split(";");
        for (String v : values) {
            String[] attribute = v.split("=");
            String attrName = attribute[0];
            String attrValue = attribute[1];
            if (attrName.equals("templateName")) templateData.setTemplateName(attrValue);
            else if (attrName.equals("extension")) templateData.setExtension(attrValue);
            else if (attrName.equals("workspace")) templateData.setWorkspace(attrValue);
            else if (attrName.equals("featureTypeInfo")) templateData.setFeatureType(attrValue);
        }
        templateData.setIdentifier(key);
        return templateData;
    }

    private Properties toProperties() {
        Properties properties = new Properties();
        for (TemplateInfo td : templateDataSet) {
            StringBuilder sb = new StringBuilder();
            sb.append("templateName=")
                    .append(td.getTemplateName())
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
        Resource propFile = dd.get(TEMPLATE_DIR, PROPERTY_FILE_NAME);
        try (OutputStream os = propFile.out()) {
            p.store(os, null);
        } catch (Exception e) {
            throw new RuntimeException("Could not write rules to " + PROPERTY_FILE_NAME);
        }
    }

    private boolean isModified() {
        return fileWatcher != null && fileWatcher.isStale();
    }

    private void loadTemplateInfo() {
        try {
            Properties properties = fileWatcher.getProperties();
            this.templateDataSet = Collections.synchronizedSortedSet(new TreeSet<>());
            for (Object k : properties.keySet()) {
                TemplateInfo td = parseProperty(k.toString(), properties.getProperty(k.toString()));
                this.templateDataSet.add(td);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void reloadIfNeeded() {
        if (isModified() || templateDataSet.isEmpty()) loadTemplateInfo();
    }

    public static class CatalogListenerTemplateInfo implements CatalogListener {
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
            TemplateInfoDAO dao = TemplateInfoDAO.get();
            List<TemplateInfo> templateInfos = dao.findByFeatureTypeInfo(ft);
            dao.delete(
                    templateInfos.stream()
                            .filter(ti -> ti.getFeatureType() != null)
                            .collect(Collectors.toList()));
        }

        private void removeWSTemplates(WorkspaceInfo ws) {
            TemplateInfoDAO dao = TemplateInfoDAO.get();
            List<TemplateInfo> templateInfos =
                    dao.findAll().stream()
                            .filter(ti -> ti.getWorkspace().equals(ws.getName()))
                            .collect(Collectors.toList());
            dao.delete(templateInfos);
        }

        @Override
        public void handleModifyEvent(CatalogModifyEvent event) throws CatalogException {
            final CatalogInfo source = event.getSource();
            if (source instanceof FeatureTypeInfo) {
                int nameIdx = event.getPropertyNames().indexOf("name");
                if (nameIdx != -1) {
                    String newName = (String) event.getNewValues().get(nameIdx);
                    updateTemplateInfoLayerName((FeatureTypeInfo) source, newName);
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

        private void updateTemplateInfoLayerName(FeatureTypeInfo fti, String newName) {
            TemplateInfoDAO dao = TemplateInfoDAO.get();
            List<TemplateInfo> templateInfo = dao.findByFeatureTypeInfo(fti);
            for (TemplateInfo ti : templateInfo) {
                ti.setFeatureType(newName);
            }
            ((TemplateInfoDAOImpl) dao).storeProperties();
        }

        private void updateTemplateInfoWorkspace(WorkspaceInfo wi, FeatureTypeInfo fti) {
            TemplateInfoDAO dao = TemplateInfoDAO.get();
            List<TemplateInfo> templateInfo = dao.findByFeatureTypeInfo(fti);
            for (TemplateInfo ti : templateInfo) {
                ti.setWorkspace(wi.getName());
            }
            ((TemplateInfoDAOImpl) dao).storeProperties();
        }

        private void updateWorkspaceNames(String oldName, String newName) {
            TemplateInfoDAO dao = TemplateInfoDAO.get();
            List<TemplateInfo> infos = dao.findAll();
            for (TemplateInfo ti : infos) {
                if (ti.getWorkspace().equals(oldName)) ti.setWorkspace(newName);
            }
            ((TemplateInfoDAOImpl) dao).storeProperties();
        }

        @Override
        public void handlePostModifyEvent(CatalogPostModifyEvent event) throws CatalogException {
            CatalogInfo source = event.getSource();
            if (source instanceof FeatureTypeInfo) {
                FeatureTypeInfo info = (FeatureTypeInfo) source;
                int wsIdx = event.getPropertyNames().indexOf("workspace");
                if (wsIdx != -1) {
                    WorkspaceInfo newWorkspace = (WorkspaceInfo) event.getNewValues().get(wsIdx);
                    updateTemplateInfoWorkspace(newWorkspace, info);
                }
            }
        }

        @Override
        public void reloaded() {}
    }
}

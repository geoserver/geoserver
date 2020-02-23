/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2012 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.store.internal;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.config.GeoServer;
import org.geoserver.csw.GetRecords;
import org.geoserver.csw.records.RecordDescriptor;
import org.geoserver.csw.store.AbstractCatalogStore;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.security.PropertyFileWatcher;
import org.geoserver.util.IOUtils;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.feature.FeatureCollection;
import org.geotools.filter.SortByImpl;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.sort.SortBy;
import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Internal Catalog Store Creates a Catalog Store from a GeoServer Catalog instance and a set of
 * Mappings It can map the internal GS catalog data to 1 or more CSW Record Types, based on one
 * mapping per record type
 *
 * @author Niels Charlier
 */
public class InternalCatalogStore extends AbstractCatalogStore implements ApplicationContextAware {

    protected static final Logger LOGGER = Logging.getLogger(InternalCatalogStore.class);

    protected GeoServer geoServer;

    protected Map<String, CatalogStoreMapping> mappings =
            new HashMap<String, CatalogStoreMapping>();

    protected Map<String, PropertyFileWatcher> watchers =
            new HashMap<String, PropertyFileWatcher>();

    public InternalCatalogStore(GeoServer geoServer) {
        this.geoServer = geoServer;
    }

    /**
     * Add a Mapping to the Internal Catalog Store
     *
     * @param typeName record type name for mapping
     * @param mapping the mapping
     */
    public void addMapping(String typeName, CatalogStoreMapping mapping) {
        mappings.put(typeName, mapping);
    }

    /**
     * Get Mapping
     *
     * @return the mapping
     */
    public CatalogStoreMapping getMapping(String typeName) {
        PropertyFileWatcher watcher = watchers.get(typeName);

        if (watcher != null && watcher.isModified()) {
            try {
                addMapping(
                        typeName,
                        CatalogStoreMapping.parse(
                                new HashMap<String, String>((Map) watcher.getProperties())));
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, e.toString());
            }
        }
        return mappings.get(typeName);
    }

    @Override
    public FeatureCollection getRecordsInternal(
            RecordDescriptor rd, RecordDescriptor rdOutput, Query q, Transaction t)
            throws IOException {

        Map<String, String> interpolationProperties = new HashMap<String, String>();

        String baseUrl = (String) q.getHints().get(GetRecords.KEY_BASEURL);
        if (baseUrl != null) {
            interpolationProperties.put(
                    "url.wfs", ResponseUtils.buildURL(baseUrl, "wfs", null, URLType.SERVICE));
            interpolationProperties.put(
                    "url.wms", ResponseUtils.buildURL(baseUrl, "wms", null, URLType.SERVICE));
            interpolationProperties.put(
                    "url.wcs", ResponseUtils.buildURL(baseUrl, "wcs", null, URLType.SERVICE));
            interpolationProperties.put(
                    "url.wmts",
                    ResponseUtils.buildURL(baseUrl, "gwc/service/wmts", null, URLType.SERVICE));
        }

        CatalogStoreMapping mapping = getMapping(q.getTypeName());
        CatalogStoreMapping outputMapping =
                getMapping(rdOutput.getFeatureDescriptor().getName().getLocalPart());

        int startIndex = 0;
        if (q.getStartIndex() != null) {
            startIndex = q.getStartIndex();
        }

        CSWUnmappingFilterVisitor unmapper = new CSWUnmappingFilterVisitor(mapping, rd);

        Filter unmapped = Filter.INCLUDE;
        // unmap filter
        if (q.getFilter() != null && q.getFilter() != Filter.INCLUDE) {
            Filter filter = q.getFilter();
            unmapped = (Filter) filter.accept(unmapper, null);
        }

        // unmap sortby
        SortBy[] unmappedSortBy = null;
        if (q.getSortBy() != null && q.getSortBy().length > 0) {
            unmappedSortBy = new SortBy[q.getSortBy().length];
            for (int i = 0; i < q.getSortBy().length; i++) {
                SortBy sortby = q.getSortBy()[i];
                Expression expr = (Expression) sortby.getPropertyName().accept(unmapper, null);

                if (!(expr instanceof PropertyName)) {
                    throw new IOException(
                            "Sorting on " + sortby.getPropertyName() + " is not supported.");
                }

                unmappedSortBy[i] = new SortByImpl((PropertyName) expr, sortby.getSortOrder());
            }
        }

        if (q.getProperties() != null && q.getProperties().size() > 0) {
            outputMapping = outputMapping.subMapping(q.getProperties(), rd);
        }

        return new CatalogStoreFeatureCollection(
                startIndex,
                q.getMaxFeatures(),
                unmappedSortBy,
                unmapped,
                geoServer.getCatalog(),
                outputMapping,
                rdOutput,
                interpolationProperties);
    }

    @Override
    public PropertyName translateProperty(RecordDescriptor rd, Name name) {
        return rd.translateProperty(name);
    }

    @Override
    public void setApplicationContext(ApplicationContext appContext) throws BeansException {
        // load record descriptors from application context
        for (RecordDescriptor rd : appContext.getBeansOfType(RecordDescriptor.class).values()) {
            support(rd);
        }
        // load mappings
        GeoServerResourceLoader loader = geoServer.getCatalog().getResourceLoader();
        Resource dir = loader.get("csw");
        try {
            for (Name name : descriptorByType.keySet()) {
                String typeName = name.getLocalPart();
                Resource f = dir.get(typeName + ".properties");

                PropertyFileWatcher watcher = new PropertyFileWatcher(f);
                watchers.put(typeName, watcher);

                if (!Resources.exists(f)) {
                    IOUtils.copy(
                            getClass().getResourceAsStream(typeName + ".default.properties"),
                            f.out());
                }

                addMapping(
                        typeName,
                        CatalogStoreMapping.parse(
                                new HashMap<String, String>((Map) watcher.getProperties())));
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            throw new FatalBeanException(e.getMessage(), e);
        }
    }
}

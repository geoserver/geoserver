/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.records.iso;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geoserver.config.GeoServer;
import org.geoserver.csw.records.AbstractRecordDescriptor;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.util.IOUtils;
import org.geotools.api.data.Query;
import org.geotools.api.feature.type.Name;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.api.filter.sort.SortBy;
import org.geotools.filter.SortByImpl;
import org.geotools.util.logging.Logging;
import org.springframework.beans.FatalBeanException;

/**
 * Abstract class for Record Descriptor that supports configurable Queryables. The queryables
 * mapping is stored in the ${recordtype}.queryables.properties file which is automatically copied
 * to the csw folder in the geoserver data directory.
 */
public abstract class QueryableMappingRecordDescriptor extends AbstractRecordDescriptor {

    private static final Logger LOGGER = Logging.getLogger(QueryableMappingRecordDescriptor.class);

    private GeoServer geoServer;

    protected Map<String, List<PropertyName>> queryableMapping = new HashMap<>();

    public QueryableMappingRecordDescriptor(GeoServer geoServer) {
        this.geoServer = geoServer;
        readMapping();
    }

    public QueryableMappingRecordDescriptor() {
        readMapping();
    }

    public void readMapping() {
        String fileName = getFeatureDescriptor().getLocalName() + ".queryables.properties";

        try {
            Properties props = new Properties();

            if (geoServer != null) {
                GeoServerResourceLoader loader = geoServer.getCatalog().getResourceLoader();
                Resource f = loader.get("csw").get(fileName);

                if (!Resources.exists(f)) {
                    IOUtils.copy(getClass().getResourceAsStream(fileName), f.out());
                }

                try (InputStream in = f.in()) {
                    props.load(in);
                }
            } else {
                try (InputStream in = getClass().getResourceAsStream(fileName)) {
                    props.load(in);
                }
            }

            queryableMapping.putAll(
                    props.entrySet().stream()
                            .collect(
                                    Collectors.toMap(
                                            e -> (String) e.getKey(),
                                            e -> toPropertyNames((String) e.getValue()))));

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            throw new FatalBeanException(e.getMessage(), e);
        }
    }

    @Override
    public List<PropertyName> translateProperty(Name name) {
        return queryableMapping.get(name.getLocalPart());
    }

    @Override
    public Query adaptQuery(Query query) {
        QueryableMappingFilterVisitor visitor =
                new QueryableMappingFilterVisitor(getFeatureDescriptor(), queryableMapping);
        Filter filter = query.getFilter();
        if (filter != null && !Filter.INCLUDE.equals(filter)) {
            query.setFilter((Filter) filter.accept(visitor, null));
        }

        if (query.getSortBy() != null && query.getSortBy().length > 0) {
            List<SortBy> sortBy = Lists.newArrayList();
            for (int i = 0; i < query.getSortBy().length; i++) {
                SortBy sb = query.getSortBy()[i];
                if (!SortBy.NATURAL_ORDER.equals(sb) && !SortBy.REVERSE_ORDER.equals(sb)) {
                    @SuppressWarnings("unchecked")
                    List<PropertyName> properties =
                            (List<PropertyName>) sb.getPropertyName().accept(visitor, null);
                    for (PropertyName property : properties) {
                        sortBy.add(new SortByImpl(property, sb.getSortOrder()));
                    }
                } else {
                    sortBy.add(sb);
                }
            }
            query.setSortBy(sortBy.toArray(new SortBy[sortBy.size()]));
        }

        return query;
    }

    private List<PropertyName> toPropertyNames(String strPropNames) {
        return Arrays.stream(strPropNames.split(";"))
                .map(strPropName -> ff.property(strPropName, getNamespaceSupport()))
                .collect(Collectors.toList());
    }
}

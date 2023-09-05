/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.records.iso;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
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
import org.geotools.data.complex.util.XPathUtil;
import org.geotools.filter.SortByImpl;
import org.geotools.filter.visitor.DuplicatingFilterVisitor;
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

    protected Map<String, PropertyName> queryableMapping = new HashMap<>();

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
                                            e -> toProperty((String) e.getValue()))));

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            throw new FatalBeanException(e.getMessage(), e);
        }
    }

    @Override
    public PropertyName translateProperty(Name name) {
        return queryableMapping.get(name.getLocalPart());
    }

    @Override
    public Query adaptQuery(Query query) {
        Filter filter = query.getFilter();
        if (filter != null && !Filter.INCLUDE.equals(filter)) {
            query.setFilter(
                    (Filter)
                            filter.accept(
                                    new DuplicatingFilterVisitor() {
                                        @Override
                                        public Object visit(
                                                PropertyName expression, Object extraData) {
                                            return adaptProperty(expression);
                                        }
                                    },
                                    null));
        }

        SortBy[] sortBy = query.getSortBy();
        if (sortBy != null && sortBy.length > 0) {
            for (int i = 0; i < sortBy.length; i++) {
                SortBy sb = sortBy[i];
                if (!SortBy.NATURAL_ORDER.equals(sb) && !SortBy.REVERSE_ORDER.equals(sb)) {
                    sortBy[i] =
                            new SortByImpl(adaptProperty(sb.getPropertyName()), sb.getSortOrder());
                }
            }
            query.setSortBy(sortBy);
        }

        return query;
    }

    private PropertyName toProperty(String name) {
        return ff.property(name, getNamespaceSupport());
    }

    /**
     * Helper method to translate propertyname that possibly contains queryable name to xml x-path
     *
     * @param expression property name
     */
    private PropertyName adaptProperty(PropertyName expression) {

        XPathUtil.StepList steps =
                XPathUtil.steps(
                        getFeatureDescriptor(),
                        expression.getPropertyName(),
                        MetaDataDescriptor.NAMESPACES);

        if (steps.size() == 1 && steps.get(0).getName().getNamespaceURI() == null
                || steps.get(0)
                        .getName()
                        .getNamespaceURI()
                        .equals(MetaDataDescriptor.NAMESPACE_APISO)) {
            PropertyName fullPath = queryableMapping.get(steps.get(0).getName().getLocalPart());
            if (fullPath != null) {
                return fullPath;
            }
        }

        return expression;
    }
}

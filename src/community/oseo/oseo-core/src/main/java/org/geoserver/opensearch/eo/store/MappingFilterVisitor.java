/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.store;

import org.geoserver.platform.ServiceException;
import org.geotools.feature.NameImpl;
import org.geotools.filter.visitor.DuplicatingFilterVisitor;
import org.opengis.feature.type.Name;
import org.opengis.filter.expression.PropertyName;

/**
 * Visits a filter and transforms back the properties into
 *
 * @author Andrea Aime - GeoSolutions
 */
class MappingFilterVisitor extends DuplicatingFilterVisitor {

    private SourcePropertyMapper mapper;

    public MappingFilterVisitor(SourcePropertyMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Object visit(PropertyName expression, Object extraData) {
        String name = expression.getPropertyName();
        // special case for "default geometry" property
        if ("".equals(name)) {
            return expression;
        }

        String sourceName;
        if (name.contains(":") && expression.getNamespaceContext() != null) {
            Name qualifiedName = getQualifiedName(expression);
            sourceName = mapper.getSourceName(qualifiedName);
        } else {
            sourceName = mapper.getSourceName(name);
        }

        if (sourceName == null) {
            throw new ServiceException(
                    "Simple feature translation failed, could not back-map '"
                            + name
                            + "' to a source property");
        } else {
            return ff.property(sourceName);
        }
    }

    private Name getQualifiedName(PropertyName expression) {
        String[] split = expression.getPropertyName().split(":");
        String uri = expression.getNamespaceContext().getURI(split[0]);
        return new NameImpl(uri, split[1]);
    }
}

/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2007 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.store.internal;

import java.util.Collection;
import java.util.stream.Collectors;
import org.geoserver.csw.records.RecordDescriptor;
import org.geoserver.csw.store.internal.CatalogStoreMapping.CatalogStoreMappingElement;
import org.geoserver.csw.util.PropertyPath;
import org.geoserver.csw.util.QNameResolver;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.Id;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.data.complex.util.XPathUtil;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.function.FilterFunction_list;
import org.geotools.filter.visitor.DuplicatingFilterVisitor;

/**
 * A Filter visitor that transforms a filter on a CSW Record of the Internal Catalog Store with a particular mapping to
 * a filter that can be applied directly onto Geoserver catalog objects.
 *
 * @author Niels Charlier
 */
public class CSWUnmappingFilterVisitor extends DuplicatingFilterVisitor {

    protected static final FilterFactory ff = CommonFactoryFinder.getFilterFactory();

    protected CatalogStoreMapping mapping;

    protected QNameResolver resolver = new QNameResolver();

    protected RecordDescriptor rd;

    protected boolean needsPostFilter = false;

    /**
     * Create CSW Unmapping Filter Visitor
     *
     * @param mapping The Mapping
     * @param rd The Record Descriptor
     */
    public CSWUnmappingFilterVisitor(CatalogStoreMapping mapping, RecordDescriptor rd) {
        this.mapping = mapping;
        this.rd = rd;
    }

    @Override
    public Object visit(PropertyName expression, Object extraData) {

        XPathUtil.StepList steps =
                XPathUtil.steps(rd.getFeatureDescriptor(), expression.getPropertyName(), rd.getNamespaceSupport());
        if (steps.containsPredicate()) { // predicate not supported by unmapped filter
            needsPostFilter = true;
        }

        if (steps.size() == 1 && steps.get(0).getName().getLocalPart().equalsIgnoreCase("AnyText")) {

            Expression result = ff.literal(" ");

            for (CatalogStoreMappingElement element : mapping.elements()) {
                Expression fieldIgnoreNull = ff.function(
                        "if_then_else",
                        ff.function("isNull", element.getContent()),
                        ff.literal(""),
                        element.getContent());
                result = ff.function("strConcat", result, ff.function("strConcat", ff.literal(" "), fieldIgnoreNull));
            }

            return result;
        }

        PropertyPath path = PropertyPath.fromXPath(steps);

        if (path.toDothPath().equalsIgnoreCase(rd.getBoundingBoxPropertyName())) {
            return ff.property("boundingBox");
        }

        Collection<CatalogStoreMappingElement> elements = mapping.elements(path);
        if (elements.isEmpty()) {
            // try with pattern without indexes
            elements = mapping.elements(path.removeIndexes());
            if (elements.isEmpty()) {
                throw new IllegalArgumentException("Unknown field in mapping: " + expression);
            }
            needsPostFilter = true;
        } else {
            for (CatalogStoreMappingElement element : elements) {
                for (int i : element.splitIndex) {
                    if (path.getIndex(i) != null) {
                        needsPostFilter = true;
                    }
                }
            }
        }
        if (elements.size() == 1) {
            return elements.stream().findFirst().get().getContent();
        } else {
            FilterFunction_list list = new FilterFunction_list();
            list.setParameters(elements.stream().map(el -> el.getContent()).collect(Collectors.toList()));
            return list;
        }
    }

    @Override
    public Object visit(Id filter, Object extraData) {
        return getFactory(extraData)
                .equal(
                        mapping.getIdentifierElement().getContent(),
                        getFactory(extraData).literal(filter.getIDs()),
                        true);
    }

    public boolean needsPostFilter() {
        return needsPostFilter;
    }
}

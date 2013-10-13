/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.store.internal;

import java.util.Iterator;
import org.geoserver.csw.records.RecordDescriptor;
import org.geoserver.csw.store.internal.CatalogStoreMapping.CatalogStoreMappingElement;
import org.geoserver.csw.util.QNameResolver;
import org.geotools.data.complex.filter.XPathUtil;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.visitor.DuplicatingFilterVisitor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.Id;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.PropertyName;

/**
 * A Filter visitor that transforms a filter on a CSW Record of the Internal Catalog Store with a particular mapping
 * to a filter that can be applied directly onto Geoserver catalog objects.
 *
 * 
 * @author Niels Charlier
 */
public class CSWUnmappingFilterVisitor extends DuplicatingFilterVisitor {

    protected static final FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();

    protected CatalogStoreMapping mapping;

    protected QNameResolver resolver = new QNameResolver();
    
    protected RecordDescriptor rd;
    
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
        
        XPathUtil.StepList steps = XPathUtil.steps( rd.getFeatureDescriptor() , expression.getPropertyName(), rd.getNamespaceSupport());
                
        if (steps.size()==1 && steps.get(0).getName().getLocalPart().equalsIgnoreCase("AnyText")) {
            
            Expression result = ff.literal(" ");
             
            for (CatalogStoreMappingElement element : mapping.elements()) {
                Expression fieldIgnoreNull = ff.function( "if_then_else", ff.function("isNull", element.getContent()), ff.literal(""), element.getContent());
                result = ff.function("strConcat", result, ff.function("strConcat", ff.literal(" "),  fieldIgnoreNull ) );
            }
                        
            return result;                    
        } 
                
        String path = CatalogStoreMapping.toDotPath(steps);
                        
        if (path.equalsIgnoreCase(rd.getBoundingBoxPropertyName())) {
            return ff.property("boundingBox");
        }

        CatalogStoreMappingElement element = mapping.getElement(path);
        if (element == null) {
            throw new IllegalArgumentException("Unknown field in mapping: " + expression);
        }
        return element.getContent();
    }
    
    @Override
    public Object visit(Id filter, Object extraData) {
        
        Iterator<Object> it = filter.getIDs().iterator();
        
        if (!it.hasNext()) {
            return Filter.EXCLUDE;
        }
        
        Filter newFilter = getFactory(extraData).equal(mapping.getIdentifierElement().getContent(), getFactory(extraData).literal(it.next()), true);
               
        while (it.hasNext()) {
            newFilter = getFactory(extraData).or(newFilter, getFactory(extraData).equal(mapping.getIdentifierElement().getContent(), getFactory(extraData).literal(it.next()), true));
        }
        
        return newFilter;
    }
}

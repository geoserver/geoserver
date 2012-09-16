/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw;

import java.util.HashSet;
import java.util.Set;

import javax.xml.namespace.QName;

import org.geoserver.csw.util.QNameResolver;
import org.geoserver.platform.ServiceException;
import org.geotools.feature.NameImpl;
import org.geotools.filter.visitor.DefaultFilterVisitor;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.Name;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.spatial.BBOX;
import org.opengis.filter.spatial.Beyond;
import org.opengis.filter.spatial.BinarySpatialOperator;
import org.opengis.filter.spatial.Contains;
import org.opengis.filter.spatial.Crosses;
import org.opengis.filter.spatial.DWithin;
import org.opengis.filter.spatial.Disjoint;
import org.opengis.filter.spatial.Equals;
import org.opengis.filter.spatial.Intersects;
import org.opengis.filter.spatial.Overlaps;
import org.opengis.filter.spatial.Touches;
import org.opengis.filter.spatial.Within;

/**
 * Checks if spatial filters are used against non spatial properties, if so, throws a ServiceException
 * (mandated for CSW cite compliance) 
 * 
 * @author Andrea Aime - GeoSolutions
 *
 */
public class SpatialFilterChecker extends DefaultFilterVisitor {
    
    FeatureType schema;
    QNameResolver resolver = new QNameResolver();
    Set<Name> geometryProperties = new HashSet<Name>();
    
    public SpatialFilterChecker(FeatureType schema) {
        this.schema = schema;
        for (PropertyDescriptor pd : schema.getDescriptors()) {
            if(pd instanceof GeometryDescriptor) {
                geometryProperties.add(pd.getName());
            }
        }
    }
    
    private void checkBinarySpatialOperator(BinarySpatialOperator filter) {
        verifyGeometryProperty(filter.getExpression1());
        verifyGeometryProperty(filter.getExpression2());
    }

    

    private void verifyGeometryProperty(Expression expression) {
        if(expression instanceof PropertyName) {
            PropertyName pn = ((PropertyName) expression);
            String path = pn.getPropertyName();
            // remove an eventual trailing /
            if(path.startsWith("/")) {
                path = path.substring(1);
            }
            // csw:Record specific hack...
            if(path.endsWith("/dc:value")) {
                path = path.substring(0, path.length() - "/dc:value".length());
            }
            QName qualified = resolver.parseQName(path, pn.getNamespaceContext());
            Name name = new NameImpl(qualified);
            if(!geometryProperties.contains(name)) {
                throw new ServiceException("Invalid spatial filter, property " + path + " is not a geometry");
            }
        }
    }

    public Object visit( final BBOX filter, Object data ) {
        checkBinarySpatialOperator(filter);
        return data;
    }

    public Object visit( Beyond filter, Object data ) {
        checkBinarySpatialOperator(filter);
        return data;
    }

    public Object visit( Contains filter, Object data ) {
        checkBinarySpatialOperator(filter);
        return data;
    }

    public Object visit( Crosses filter, Object data ) {
        checkBinarySpatialOperator(filter);
        return data;
    }

    public Object visit( Disjoint filter, Object data ) {
        checkBinarySpatialOperator(filter);
        return data;
    }

    public Object visit( DWithin filter, Object data ) {
        checkBinarySpatialOperator(filter);
        return data;
    }

    public Object visit( Equals filter, Object data ) {
        checkBinarySpatialOperator(filter);
        return data;
    }

    public Object visit( Intersects filter, Object data ) {
        checkBinarySpatialOperator(filter);
        return data;
    }

    public Object visit( Overlaps filter, Object data ) {
        checkBinarySpatialOperator(filter);
        return data;
    }

    public Object visit( Touches filter, Object data ) {
        checkBinarySpatialOperator(filter);
        return data;
    }

    public Object visit( Within filter, Object data ) {
        checkBinarySpatialOperator(filter);        
        return data;
    }


}

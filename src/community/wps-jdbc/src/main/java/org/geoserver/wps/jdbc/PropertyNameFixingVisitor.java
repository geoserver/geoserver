package org.geoserver.wps.jdbc;
/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.visitor.DefaultFilterVisitor;
import org.geotools.filter.visitor.DuplicatingFilterVisitor;
import org.geotools.filter.visitor.FilterVisitorSupport;
import org.geotools.jdbc.SQLDialect;
import org.opengis.filter.BinaryComparisonOperator;
import org.opengis.filter.BinaryLogicOperator;
import org.opengis.filter.ExcludeFilter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.FilterVisitor;
import org.opengis.filter.Id;
import org.opengis.filter.IncludeFilter;
import org.opengis.filter.Not;
import org.opengis.filter.PropertyIsBetween;
import org.opengis.filter.PropertyIsLike;
import org.opengis.filter.PropertyIsNil;
import org.opengis.filter.PropertyIsNull;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.spatial.BinarySpatialOperator;
import org.opengis.filter.temporal.BinaryTemporalOperator;
/**
 * Make sure the property names in a filter are in the case (etc) required for a 
 * datastore based on the supplied dialect.
 * 
 * @author ian
 *
 */
public class PropertyNameFixingVisitor extends DuplicatingFilterVisitor implements FilterVisitor {

    private SQLDialect dialect;
    static private final FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();
    public PropertyNameFixingVisitor(SQLDialect dialect) {
        this.dialect = dialect;
    }

    @Override
    public Object visit(PropertyName expression, Object data) {
        if(dialect!=null) {
            String name = expression.getPropertyName();
            String newName = fixColumnName(name);
            expression = FF.property(newName);
        }
        
        return super.visit(expression, data);
    }

    private String fixColumnName(String processId) {
        String ret = processId;
    
       if(dialect!=null) {
           StringBuffer sql = new StringBuffer();
           dialect.encodeColumnName(null,processId, sql);
           ret = sql.toString();
       }
       return ret;
    }
}

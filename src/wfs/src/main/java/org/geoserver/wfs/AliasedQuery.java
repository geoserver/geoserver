/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.namespace.QName;
import net.opengis.wfs.XlinkPropertyNameType;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.wfs.request.Query;
import org.geotools.filter.visitor.DuplicatingFilterVisitor;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.PropertyIsGreaterThan;
import org.opengis.filter.PropertyIsGreaterThanOrEqualTo;
import org.opengis.filter.PropertyIsLessThan;
import org.opengis.filter.PropertyIsLessThanOrEqualTo;
import org.opengis.filter.PropertyIsNotEqualTo;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.spatial.Beyond;
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
 * Support class checking that the aliases are present, and not in conflict with feature type names
 * or field names (such conflicts might cause issues down the road). In case of conflicts, the class
 * will create/alter the aliases to avoid them, and modify filters and property name selection
 * accordingly
 *
 * @author Andrea Aime - GeoSolutions
 */
class AliasedQuery extends Query {

    /**
     * Renames property names following an alias rename map
     *
     * @author Andrea Aime - GeoSolutions
     */
    class AliasRenameVisitor extends DuplicatingFilterVisitor {

        private Map<String, String> renameMap;

        public AliasRenameVisitor(Map<String, String> renameMap) {
            this.renameMap = renameMap;
        }

        @Override
        public Object visit(PropertyName expression, Object extraData) {
            String name = expression.getPropertyName();
            String renamed = rename(renameMap, name);
            return ff.property(renamed);
        }
    }

    /**
     * Renames property names following an alias rename map
     *
     * @author Andrea Aime - GeoSolutions
     */
    class SelfJoinRenameVisitor extends DuplicatingFilterVisitor {

        private List<String> aliases;

        public SelfJoinRenameVisitor(List<String> aliases) {
            this.aliases = aliases;
        }

        public Object visit(Beyond filter, Object extraData) {
            Expression geometry1 = visitBinaryChild(filter.getExpression1(), extraData, 0);
            Expression geometry2 = visitBinaryChild(filter.getExpression2(), extraData, 1);
            double distance = filter.getDistance();
            String units = filter.getDistanceUnits();
            return getFactory(extraData)
                    .beyond(geometry1, geometry2, distance, units, filter.getMatchAction());
        }

        public Object visit(Contains filter, Object extraData) {
            Expression geometry1 = visitBinaryChild(filter.getExpression1(), extraData, 0);
            Expression geometry2 = visitBinaryChild(filter.getExpression2(), extraData, 1);
            return getFactory(extraData).contains(geometry1, geometry2, filter.getMatchAction());
        }

        public Object visit(Crosses filter, Object extraData) {
            Expression geometry1 = visitBinaryChild(filter.getExpression1(), extraData, 0);
            Expression geometry2 = visitBinaryChild(filter.getExpression2(), extraData, 1);
            return getFactory(extraData).crosses(geometry1, geometry2, filter.getMatchAction());
        }

        public Object visit(Disjoint filter, Object extraData) {
            Expression geometry1 = visitBinaryChild(filter.getExpression1(), extraData, 0);
            Expression geometry2 = visitBinaryChild(filter.getExpression2(), extraData, 1);
            return getFactory(extraData).disjoint(geometry1, geometry2, filter.getMatchAction());
        }

        public Object visit(DWithin filter, Object extraData) {
            Expression geometry1 = visitBinaryChild(filter.getExpression1(), extraData, 0);
            Expression geometry2 = visitBinaryChild(filter.getExpression2(), extraData, 1);
            double distance = filter.getDistance();
            String units = filter.getDistanceUnits();
            return getFactory(extraData)
                    .dwithin(geometry1, geometry2, distance, units, filter.getMatchAction());
        }

        public Object visit(Equals filter, Object extraData) {
            Expression geometry1 = visitBinaryChild(filter.getExpression1(), extraData, 0);
            Expression geometry2 = visitBinaryChild(filter.getExpression2(), extraData, 1);
            return getFactory(extraData).equal(geometry1, geometry2, filter.getMatchAction());
        }

        public Object visit(Intersects filter, Object extraData) {
            Expression geometry1 = visitBinaryChild(filter.getExpression1(), extraData, 0);
            Expression geometry2 = visitBinaryChild(filter.getExpression2(), extraData, 1);
            return getFactory(extraData).intersects(geometry1, geometry2, filter.getMatchAction());
        }

        private Expression visitBinaryChild(Expression ex, Object extraData, int idx) {
            if (ex instanceof PropertyName) {
                String name = ((PropertyName) ex).getPropertyName();
                String renamed = rename(aliases.get(idx), name);
                return ff.property(renamed);
            } else {
                return super.visit(ex, extraData);
            }
        }

        public Object visit(Overlaps filter, Object extraData) {
            Expression geometry1 = visitBinaryChild(filter.getExpression1(), extraData, 0);
            Expression geometry2 = visitBinaryChild(filter.getExpression2(), extraData, 1);
            return getFactory(extraData).overlaps(geometry1, geometry2, filter.getMatchAction());
        }

        public Object visit(Touches filter, Object extraData) {
            Expression geometry1 = visitBinaryChild(filter.getExpression1(), extraData, 0);
            Expression geometry2 = visitBinaryChild(filter.getExpression2(), extraData, 1);
            return getFactory(extraData).touches(geometry1, geometry2, filter.getMatchAction());
        }

        public Object visit(Within filter, Object extraData) {
            Expression geometry1 = visitBinaryChild(filter.getExpression1(), extraData, 0);
            Expression geometry2 = visitBinaryChild(filter.getExpression2(), extraData, 1);
            return getFactory(extraData).within(geometry1, geometry2, filter.getMatchAction());
        }

        public Object visit(PropertyIsEqualTo filter, Object extraData) {
            Expression expr1 = visitBinaryChild(filter.getExpression1(), extraData, 0);
            Expression expr2 = visitBinaryChild(filter.getExpression2(), extraData, 1);
            boolean matchCase = filter.isMatchingCase();
            return getFactory(extraData).equal(expr1, expr2, matchCase, filter.getMatchAction());
        }

        public Object visit(PropertyIsNotEqualTo filter, Object extraData) {
            Expression expr1 = visitBinaryChild(filter.getExpression1(), extraData, 0);
            Expression expr2 = visitBinaryChild(filter.getExpression2(), extraData, 1);
            boolean matchCase = filter.isMatchingCase();
            return getFactory(extraData).notEqual(expr1, expr2, matchCase, filter.getMatchAction());
        }

        public Object visit(PropertyIsGreaterThan filter, Object extraData) {
            Expression expr1 = visitBinaryChild(filter.getExpression1(), extraData, 0);
            Expression expr2 = visitBinaryChild(filter.getExpression2(), extraData, 1);
            return getFactory(extraData)
                    .greater(expr1, expr2, filter.isMatchingCase(), filter.getMatchAction());
        }

        public Object visit(PropertyIsGreaterThanOrEqualTo filter, Object extraData) {
            Expression expr1 = visitBinaryChild(filter.getExpression1(), extraData, 0);
            Expression expr2 = visitBinaryChild(filter.getExpression2(), extraData, 1);
            return getFactory(extraData)
                    .greaterOrEqual(expr1, expr2, filter.isMatchingCase(), filter.getMatchAction());
        }

        public Object visit(PropertyIsLessThan filter, Object extraData) {
            Expression expr1 = visitBinaryChild(filter.getExpression1(), extraData, 0);
            Expression expr2 = visitBinaryChild(filter.getExpression2(), extraData, 1);
            return getFactory(extraData)
                    .less(expr1, expr2, filter.isMatchingCase(), filter.getMatchAction());
        }

        public Object visit(PropertyIsLessThanOrEqualTo filter, Object extraData) {
            Expression expr1 = visitBinaryChild(filter.getExpression1(), extraData, 0);
            Expression expr2 = visitBinaryChild(filter.getExpression2(), extraData, 1);
            return getFactory(extraData)
                    .lessOrEqual(expr1, expr2, filter.isMatchingCase(), filter.getMatchAction());
        }
    }

    /**
     * Checks the aliases in the query are present, and not in conflict with feature type names or
     * field names (such conflicts might cause issues down the road). In case of conflicts a new
     * Query object will be returned in which the conflicts have been resolved.
     */
    static Query fixAliases(List<FeatureTypeInfo> metas, Query query) throws IOException {
        Set<String> reservedWords = new HashSet<>();
        for (FeatureTypeInfo meta : metas) {
            reservedWords.add(meta.getName());
            reservedWords.add(meta.prefixedName());
            FeatureType featureType = meta.getFeatureType();
            for (PropertyDescriptor pd : featureType.getDescriptors()) {
                reservedWords.add(pd.getName().getLocalPart());
                reservedWords.add(pd.getName().getURI());
            }
        }

        // get the starting aliases
        List<String> aliases;
        List<String> originalAliases = query.getAliases();
        boolean replaced = false;
        if (query.getAliases() != null && !query.getAliases().isEmpty()) {
            aliases = new ArrayList<>(query.getAliases());
        } else {
            replaced = true;
            aliases = new ArrayList<>();
            for (int i = 0; i < metas.size(); i++) {
                aliases.add(String.valueOf((char) ('a' + i)));
            }
        }

        // build replacements if necessary
        for (int i = 0; i < aliases.size(); i++) {
            String alias = aliases.get(i);
            String base = alias;
            int j = 0;
            while (reservedWords.contains(alias)) {
                replaced = true;
                alias = base + (j++);
            }
            aliases.set(i, alias);
        }

        if (replaced) {
            return new AliasedQuery(query, originalAliases, aliases);
        } else {
            return query;
        }
    }

    private List<String> aliases;

    private Query delegate;

    private Filter filter;

    private List<String> propertyNames;

    public AliasedQuery(Query query, List<String> originalAliases, List<String> aliases) {
        super(null);
        this.delegate = query;
        this.aliases = aliases;
        if (originalAliases != null && !originalAliases.isEmpty()) {
            Map<String, String> renameMap = buildRenameMap(originalAliases, aliases);
            this.filter =
                    (Filter) query.getFilter().accept(new AliasRenameVisitor(renameMap), null);
            if (query.getPropertyNames() != null) {
                this.propertyNames = new ArrayList<>();
                for (String name : query.getPropertyNames()) {
                    this.propertyNames.add(rename(renameMap, name));
                }
            }
        } else {
            // CITE tests hack, was is a self join query with no aliases?
            List<QName> typeNames = query.getTypeNames();
            if (typeNames.size() == 2 && new HashSet<>(typeNames).size() == 1) {
                this.filter =
                        (Filter) query.getFilter().accept(new SelfJoinRenameVisitor(aliases), null);
            } else {
                this.filter = query.getFilter();
            }
            this.propertyNames = query.getPropertyNames();
        }
    }

    private Map<String, String> buildRenameMap(
            List<String> originalAliases, List<String> newAliases) {
        Map<String, String> map = new HashMap<String, String>();
        for (int i = 0; i < originalAliases.size(); i++) {
            String a1 = originalAliases.get(i);
            String a2 = newAliases.get(i);
            if (!a1.equals(a2)) {
                map.put(a1, a2);
            }
        }

        return map;
    }

    String rename(Map<String, String> renameMap, String name) {
        int idx = name.indexOf('/');
        if (idx > 0) {
            String prefix = name.substring(0, idx);
            String renamed = renameMap.get(prefix);
            if (renamed != null) {
                name = renamed + name.substring(idx);
            }
        }
        return name;
    }

    String rename(String renamedPrefix, String name) {
        int idx = name.indexOf('/');
        if (idx > 0) {
            name = renamedPrefix + name.substring(idx);
        }
        return name;
    }

    @Override
    public List<QName> getTypeNames() {
        return delegate.getTypeNames();
    }

    @Override
    public List<String> getAliases() {
        return aliases;
    }

    @Override
    public List<String> getPropertyNames() {
        return propertyNames;
    }

    @Override
    public Filter getFilter() {
        return filter;
    }

    @Override
    public List<SortBy> getSortBy() {
        return delegate.getSortBy();
    }

    @Override
    public List<XlinkPropertyNameType> getXlinkPropertyNames() {
        return delegate.getXlinkPropertyNames();
    }
}

/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.xml.namespace.QName;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geotools.data.Join;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.FilterAttributeExtractor;
import org.geotools.filter.visitor.DuplicatingFilterVisitor;
import org.geotools.filter.visitor.FilterVisitorSupport;
import org.opengis.filter.And;
import org.opengis.filter.BinaryComparisonOperator;
import org.opengis.filter.BinaryLogicOperator;
import org.opengis.filter.ExcludeFilter;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.Id;
import org.opengis.filter.IncludeFilter;
import org.opengis.filter.Not;
import org.opengis.filter.PropertyIsBetween;
import org.opengis.filter.PropertyIsLike;
import org.opengis.filter.PropertyIsNil;
import org.opengis.filter.PropertyIsNull;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.spatial.BinarySpatialOperator;
import org.opengis.filter.temporal.BinaryTemporalOperator;
import org.xml.sax.helpers.NamespaceSupport;

public class JoinExtractingVisitor extends FilterVisitorSupport {

    static FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);

    FeatureTypeInfo primaryFeatureType;
    String primaryAlias;
    List<FeatureTypeInfo> featureTypes;
    List<String> aliases;

    boolean hadAliases;

    List<Filter> joinFilters = new ArrayList<Filter>();
    List<Filter> filters = new ArrayList<Filter>();
    private List<QName> queriedTypes;

    public JoinExtractingVisitor(List<FeatureTypeInfo> featureTypes, List<String> aliases) {
        this.primaryFeatureType = null;
        this.featureTypes = new ArrayList<>(featureTypes);

        if (aliases == null || aliases.isEmpty()) {
            hadAliases = false;
            // assign prefixes
            aliases = new ArrayList<String>();
            for (int j = 0, i = 0; i < featureTypes.size(); i++) {
                String alias;
                boolean conflictFound;
                do {
                    conflictFound = false;
                    alias = String.valueOf((char) ('a' + (j++)));
                    for (FeatureTypeInfo ft : featureTypes) {
                        if (alias.equals(ft.getName()) || alias.equals(ft.prefixedName())) {
                            conflictFound = true;
                            break;
                        }
                    }
                } while (conflictFound);
                aliases.add(alias);
            }
        } else {
            hadAliases = true;
        }

        this.aliases = new ArrayList(aliases);
    }

    public Object visitNullFilter(Object extraData) {
        return null;
    }

    public Object visit(ExcludeFilter filter, Object extraData) {
        return handleOther(filter, extraData);
    }

    public Object visit(IncludeFilter filter, Object extraData) {
        return handleOther(filter, extraData);
    }

    public Object visit(Id filter, Object extraData) {
        return handleOther(filter, extraData);
    }

    public Object visit(Not filter, Object extraData) {
        if (isJoinFilter(filter.getFilter(), extraData)) {
            checkValidJoinFilter(filter);
            joinFilters.add(filter);
        } else {
            handleOther(filter, extraData);
        }

        return extraData;
    }

    private void checkValidJoinFilter(Filter filter) {
        Set<String> prefixes = getFilterPrefixes(filter);
        if (prefixes.size() > 2) {
            throw new WFSException(
                    "Not subfilter joins against more than one table "
                            + prefixes
                            + ", this kind of filter is not supported: "
                            + filter);
        }
    }

    public Object visit(PropertyIsBetween filter, Object extraData) {
        return handle(
                filter,
                extraData,
                filter.getLowerBoundary(),
                filter.getUpperBoundary(),
                filter.getUpperBoundary());
    }

    public Object visit(PropertyIsLike filter, Object extraData) {
        return handleOther(filter, extraData);
    }

    public Object visit(PropertyIsNull filter, Object extraData) {
        return handleOther(filter, extraData);
    }

    public Object visit(PropertyIsNil filter, Object extraData) {
        return handleOther(filter, extraData);
    }

    @Override
    protected Object visit(BinaryLogicOperator op, Object extraData) {
        if (op instanceof And) {
            for (Filter f : op.getChildren()) {
                f.accept(this, extraData);
            }
        } else {
            boolean joinFilter = false;
            for (Filter child : op.getChildren()) {
                if (isJoinFilter(child, extraData)) {
                    joinFilter = true;
                    break;
                }
            }
            if (joinFilter) {
                checkValidJoinFilter(op);
                joinFilters.add(op);
            } else {
                handleOther(op, extraData);
            }

            return extraData;
        }
        return extraData;
    }

    @Override
    protected Object visit(BinaryComparisonOperator op, Object extraData) {
        return handle(op, extraData, op.getExpression1(), op.getExpression2());
    }

    @Override
    protected Object visit(BinarySpatialOperator op, Object extraData) {
        return handle(op, extraData, op.getExpression1(), op.getExpression2());
    }

    @Override
    protected Object visit(BinaryTemporalOperator op, Object extraData) {
        return handle(op, extraData, op.getExpression1(), op.getExpression2());
    }

    Object handle(Filter f, Object extraData, Expression... expressions) {
        if (isJoinFilter(expressions)) {
            joinFilters.add(f);
        } else {
            handleOther(f, extraData);
        }
        return null;
    }

    Object handleOther(Filter f, Object extraData) {
        filters.add(f);
        return null;
    }

    boolean isJoinFilter(Expression... expressions) {
        // Used to check if the expressions were all property names
        // however, f(t1.x) = t2.y is also a join filter, and t1.x + 2 = t2.y too
        // So, generalized it a bit, it's still not fully correct though,
        // as it can be fooled by a.x = a.y, which is not a join filter... but we
        // can have the full name (no alias) twice in a self join, and that would be a valid join...
        // uff!
        Set<String> prefixes = new HashSet<>();
        for (Expression ex : expressions) {
            FilterAttributeExtractor fae = new FilterAttributeExtractor();
            ex.accept(fae, null);
            Set<String> localAttributes = fae.getAttributeNameSet();
            Set<String> localPrefixes = getPrefixes(localAttributes);
            if (!localPrefixes.isEmpty()) {
                if (prefixes.size() == 0) {
                    // accumulate the prefixes, to see how many tables we're joining
                    prefixes.addAll(localPrefixes);
                } else if (prefixes.size() > 1) {
                    // e.g. f(a.x,b.y)=b.z
                    return true;
                } else {
                    // is it a comparison among attributes of the same table, or not?
                    // e.g., a.x = a.y is not a join filter (a self join would use two different
                    // aliases)
                    localPrefixes.removeAll(prefixes);
                    if (!localPrefixes.isEmpty()) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean isJoinFilter(Filter filter, Object extraData) {
        JoinExtractingVisitor visitor = new JoinExtractingVisitor(featureTypes, aliases);
        filter.accept(visitor, extraData);
        return !visitor.joinFilters.isEmpty();
    }

    private Set<String> getPrefixes(Set<String> attributes) {
        Set<String> result = new HashSet<>();
        for (String attribute : attributes) {
            int idx = attribute.indexOf('/');
            if (idx > 0) {
                String prefix = attribute.substring(0, idx);
                result.add(prefix);
            }
        }

        return result;
    }

    public List<Join> getJoins() {
        List<Join> joins = new ArrayList();

        setupPrimary();

        // unroll the contents of the join filters and rewrite them and and assign to correct
        // feature type
        List<Filter> joinFilters = rewriteAndSortJoinFilters(this.joinFilters);

        // do same for other secondary filters
        List<Filter> otherFilters = rewriteAndSortOtherFilters(this.filters);

        for (int i = 0; i < featureTypes.size(); i++) {
            String nativeName = featureTypes.get(i).getNativeName();
            Join join = new Join(nativeName, joinFilters.get(i + 1));
            if (aliases != null) {
                join.setAlias(aliases.get(i));
            }
            if (otherFilters.get(i + 1) != null) {
                join.setFilter(otherFilters.get(i + 1));
            }
            joins.add(join);
        }

        return joins;
    }

    /**
     * Returns the joined feature types. If called past join extraction, it will return the types in
     * the same order as the joins (which might have been reordered to locate the center of the star
     * join)
     */
    public List<FeatureTypeInfo> getFeatureTypes() {
        if (primaryFeatureType == null) {
            return featureTypes;
        } else {
            List<FeatureTypeInfo> result = new ArrayList<>();
            result.add(primaryFeatureType);
            result.addAll(featureTypes);
            return result;
        }
    }

    /**
     * Find the center of the star join, and remove it from the feature types and aliases arrays the
     * rest of the algorithm is setup to have only the secondary types in these arrays
     */
    private void setupPrimary() {
        if (primaryFeatureType == null) {
            int idx = getPrimaryFeatureTypeIndex(this.joinFilters);
            primaryFeatureType = featureTypes.get(idx);
            primaryAlias = aliases.get(idx);
            featureTypes.remove(idx);
            aliases.remove(idx);
        }
    }

    public Filter getPrimaryFilter() {
        setupPrimary();
        List<Filter> otherFilters = rewriteAndSortOtherFilters(filters);
        return otherFilters.get(0);
    }

    public String getPrimaryAlias() {
        setupPrimary();
        return primaryAlias;
    }

    public FeatureTypeInfo getPrimaryFeatureType() {
        setupPrimary();
        return primaryFeatureType;
    }

    List<Filter> rewriteAndSortJoinFilters(List<Filter> filters) {
        Map<String, FeatureTypeInfo> typeMap = buildTypeMap();
        Map<String, String> nameToAlias = buildNameToAlias();
        String primaryName = primaryFeatureType.prefixedName();
        String primaryUnqualifiedName = primaryFeatureType.getName();

        PropertyNameRewriter rewriter = new PropertyNameRewriter(nameToAlias, true);

        Filter[] sorted = new Filter[featureTypes.size() + 1];
        for (Filter filter : filters) {
            Set<String> prefixes = getFilterPrefixes(filter);
            prefixes.remove(primaryAlias);
            prefixes.remove(primaryName);
            prefixes.remove(primaryUnqualifiedName);
            if (prefixes.size() != 1) {
                throw new WFSException(
                        "Extracted invalid join filter "
                                + filter
                                + ", it joins more than "
                                + "one secondary feature type + "
                                + prefixes
                                + " with the central join feature type "
                                + primaryAlias
                                + "/"
                                + primaryName);
            }

            Filter rewritten = (Filter) filter.accept(rewriter, null);
            String alias = prefixes.iterator().next();
            FeatureTypeInfo ft = typeMap.get(alias);
            int idx = featureTypes.indexOf(ft);
            if (idx == -1) {
                throw new WFSException(
                        "Extracted invalid join filter "
                                + filter
                                + ", it uses the unkonwn alias/typename "
                                + alias);
            }
            updateFilter(sorted, idx + 1, rewritten);
        }

        return Arrays.asList(sorted);
    }

    private Set<String> getFilterPrefixes(Filter filter) {
        FilterAttributeExtractor extractor = new FilterAttributeExtractor();
        filter.accept(extractor, null);
        Set<PropertyName> attributeNames = extractor.getPropertyNameSet();
        Set<String> prefixes = new HashSet<>();
        for (PropertyName attributeName : attributeNames) {
            String name = attributeName.getPropertyName();
            int idx = name.indexOf('/');
            if (idx > 0) {
                String prefix = name.substring(0, idx);
                idx = prefix.indexOf(":");
                if (idx > 0) {
                    String localNsPrefix = prefix.substring(0, idx);
                    NamespaceSupport namespaceSupport = attributeName.getNamespaceContext();
                    if (namespaceSupport != null) {
                        String ns = namespaceSupport.getURI(localNsPrefix);
                        if (ns != null) {
                            Optional<String> wsName =
                                    featureTypes
                                            .stream()
                                            .filter(
                                                    ft ->
                                                            ns.equals(
                                                                    ft.getQualifiedName()
                                                                            .getNamespaceURI()))
                                            .map(ft -> ft.getStore().getWorkspace().getName())
                                            .findFirst();
                            if (wsName.isPresent()) {
                                prefix = wsName.get() + ":" + prefix.substring(idx + 1);
                            }
                        }
                    }
                }
                prefixes.add(prefix);
            }
        }
        return prefixes;
    }

    List<Filter> rewriteAndSortOtherFilters(List<Filter> filters) {
        String primaryName = primaryFeatureType.prefixedName();
        Map<String, FeatureTypeInfo> typeMap = buildTypeMap();
        Map<String, String> nameToAlias = buildNameToAlias();

        PropertyNameRewriter rewriter = new PropertyNameRewriter(nameToAlias, false);

        Filter[] sorted = new Filter[featureTypes.size() + 1];
        for (Filter filter : filters) {
            Set<String> prefixes = getFilterPrefixes(filter);
            prefixes.remove(primaryName);
            if (prefixes.size() != 1) {
                throw new WFSException(
                        "Extracted invalid join sub-filter "
                                + filter
                                + ", it users more than one feature type + "
                                + prefixes);
            }

            Filter rewritten = (Filter) filter.accept(rewriter, null);
            String alias = prefixes.iterator().next();
            FeatureTypeInfo ft = typeMap.get(alias);
            if (primaryFeatureType.equals(ft)) {
                updateFilter(sorted, 0, rewritten);
            } else {
                int idx = featureTypes.indexOf(ft);
                if (idx == -1) {

                    throw new WFSException(
                            "Extracted invalid join filter "
                                    + filter
                                    + ", it uses the unkonwn alias/typename "
                                    + alias);
                }
                updateFilter(sorted, idx + 1, rewritten);
            }
        }

        return Arrays.asList(sorted);
    }

    /**
     * Builds a map going from alias, prefixed type name and simple type name to FeatureTypeInfo. In
     * case of conflicts aliases will override the type names
     */
    private Map<String, FeatureTypeInfo> buildTypeMap() {
        Map<String, FeatureTypeInfo> typeMap = new HashMap<>();
        typeMap.put(primaryFeatureType.prefixedName(), primaryFeatureType);
        typeMap.put(primaryFeatureType.getName(), primaryFeatureType);
        typeMap.put(primaryAlias, primaryFeatureType);
        for (int i = 0; i < aliases.size(); i++) {
            String alias = aliases.get(i);
            FeatureTypeInfo ft = featureTypes.get(i);
            typeMap.put(ft.getName(), ft);
            typeMap.put(ft.prefixedName(), ft);
            String localTypeName = getLocalTypeName(ft);
            if (localTypeName != null) {
                typeMap.put(localTypeName, ft);
            }
            typeMap.put(alias, ft);
        }

        return typeMap;
    }

    private String getLocalTypeName(FeatureTypeInfo ft) {
        if (ft.getNamespace() != null && ft.getNamespace().getURI() != null) {
            String uri = ft.getNamespace().getURI();
            String name = ft.getName();
            if (queriedTypes != null) {
                for (QName type : queriedTypes) {
                    String namespaceURI = type.getNamespaceURI();
                    String prefix = type.getPrefix();
                    if (prefix != null
                            && namespaceURI != null
                            && namespaceURI.equals(uri)
                            && type.getLocalPart().equals(name)) {
                        return prefix + ":" + type.getLocalPart();
                    }
                }
            }
        }

        return null;
    }

    /** Builds a map going from type name, qualified or unqualified, to alias */
    private Map<String, String> buildNameToAlias() {
        Map<String, String> nameToAlias = new HashMap<>();
        nameToAlias.put(primaryFeatureType.prefixedName(), primaryAlias);
        nameToAlias.put(primaryFeatureType.getName(), primaryAlias);
        String localTypeName = getLocalTypeName(primaryFeatureType);
        if (localTypeName != null) {
            nameToAlias.put(localTypeName, primaryAlias);
        }
        for (int i = 0; i < aliases.size(); i++) {
            String alias = aliases.get(i);
            FeatureTypeInfo ft = featureTypes.get(i);
            nameToAlias.put(ft.getName(), alias);
            nameToAlias.put(ft.prefixedName(), alias);
            localTypeName = getLocalTypeName(ft);
            if (localTypeName != null) {
                nameToAlias.put(localTypeName, alias);
            }
        }
        return nameToAlias;
    }

    /**
     * Geotools only support "star" joins with a primary being the center of the join. Figure out if
     * we have one feature type that is acting as the center of the star, or throw an exception if
     * we don't have one.
     */
    private int getPrimaryFeatureTypeIndex(List<Filter> filters) {
        if (featureTypes.size() == 2) {
            return 0;
        }

        List<Integer> connecteds = new ArrayList<>();
        for (int i = 0; i < featureTypes.size(); i++) {
            connecteds.add(i);
        }
        for (Filter filter : filters) {
            Set<String> filterPrefixes = getFilterPrefixes(filter);
            Set<Integer> nameTypes = getPropertyNameTypeIndexes(filterPrefixes);
            connecteds.retainAll(nameTypes);
        }

        if (connecteds.isEmpty()) {
            throw new WFSException(
                    "Cannot run this type of join, at the moment GeoServer only supports "
                            + "joins having a single central feature type joined to all others");
        } else {
            return connecteds.iterator().next();
        }
    }

    private Set<Integer> getPropertyNameTypeIndexes(Set<String> filterPrefixes) {
        Set<Integer> nameTypes = new HashSet<>();
        for (String prefix : filterPrefixes) {
            int aliasIdx = aliases.indexOf(prefix);
            if (aliasIdx >= 0) {
                nameTypes.add(aliasIdx);
            } else {
                for (int i = 0; i < featureTypes.size(); i++) {
                    FeatureTypeInfo ft = featureTypes.get(i);
                    if (prefix.equals(ft.prefixedName()) || prefix.equals(ft.getName())) {
                        nameTypes.add(i);
                        break;
                    }
                }
            }
        }
        return nameTypes;
    }

    void updateFilter(Filter[] filters, int i, Filter filter) {
        if (filters[i] == null) {
            filters[i] = filter;
        } else {
            filters[i] = ff.and(filters[i], filter);
        }
    }

    public void setQueriedTypes(List<QName> queriedTypes) {
        this.queriedTypes = queriedTypes;
    }

    /**
     * Rewrites property names to either remove the join prefixes (for local filters) or replace the
     * <code>alias/attribute</code> or <code>typename/attribute</code> syntax with a <code>
     * alias.attribute</code> syntax
     *
     * @author Andrea Aime - GeoSolutions
     */
    class PropertyNameRewriter extends DuplicatingFilterVisitor {
        Map<String, String> nameToAlias;

        private Set<String> prefixes;

        boolean addPrefix;

        public PropertyNameRewriter(Map<String, String> nameToAlias, boolean prefix) {
            super();
            this.prefixes = new HashSet<>();
            this.prefixes.addAll(nameToAlias.keySet());
            this.prefixes.addAll(nameToAlias.values());
            this.prefixes.add(primaryAlias);
            this.nameToAlias = nameToAlias;
            this.addPrefix = prefix;
        }

        @Override
        public Object visit(PropertyName expression, Object extraData) {
            String n = expression.getPropertyName();
            int idx = n.indexOf('/');
            String prefix = null;
            if (idx > 0) {
                prefix = n.substring(0, idx);
                if (prefixes.contains(prefix)) {
                    if (nameToAlias.get(prefix) != null) {
                        prefix = nameToAlias.get(prefix);
                    }
                    n = n.substring(idx + 1);
                } else {
                    n = null;
                }
            } else {
                n = null;
            }

            if (n != null) {
                // remove the eventual namespace prefix, join are only supported for simple features
                // right now anyways, underlying stores do not understand the prefixes
                int colonIdx = n.indexOf(':');
                if (colonIdx > 0) {
                    n = n.substring(colonIdx + 1);
                }
                if (addPrefix) {
                    n = (prefix != null ? prefix : "") + "." + n;
                }
                return ff.property(n, expression.getNamespaceContext());
            }
            return null;
        }
    }
}

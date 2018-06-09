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
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.sort.SortBy;

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
     * Checks the aliases in the query are present, and not in conflict with feature type names or
     * field names (such conflicts might cause issues down the road). In case of conflicts a new
     * Query object will be returned in which the conflicts have been resolved.
     *
     * @param metas
     * @param query
     * @throws IOException
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
            this.filter = query.getFilter();
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

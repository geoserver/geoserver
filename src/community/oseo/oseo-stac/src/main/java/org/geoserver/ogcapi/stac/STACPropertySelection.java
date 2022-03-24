/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.stac;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.geoserver.featurestemplating.builders.AbstractTemplateBuilder;
import org.geoserver.featurestemplating.builders.visitors.AbstractPropertySelection;

/** A PropertySelection handler that contains STAC specific logic to include,exclude fields. */
public class STACPropertySelection extends AbstractPropertySelection {

    // default fields always included.
    private Set<String> DEFAULT_INCLUDE =
            new HashSet<>(
                    Arrays.asList(
                            "id",
                            "type",
                            "geometry",
                            "bbox",
                            "links",
                            "assets",
                            "properties.datetime",
                            "properties.created"));
    private Set<String> excludedFields;

    private static final String EXCLUDE_PREFIX = "-";

    public STACPropertySelection(String[] fields) {
        populateIncludeExclude(fields);
    }

    private void populateIncludeExclude(String[] fields) {
        Set<String> include = new HashSet<>();
        Set<String> exclude = new HashSet<>();
        if (fields != null) {
            for (String f : fields) {
                if (f.startsWith(EXCLUDE_PREFIX)) exclude.add(f.substring(1));
                else include.add(f);
            }
        }
        populateIncludeExclude(include, exclude);
    }

    private void populateIncludeExclude(Set<String> include, Set<String> exclude) {
        this.includedFields = new HashSet<>();
        this.includedFields.addAll(DEFAULT_INCLUDE);
        if (include != null) this.includedFields.addAll(include);
        if (exclude != null) this.includedFields.removeAll(exclude);
        this.excludedFields = exclude;
    }

    @Override
    protected boolean isKeySelected(String key) {
        boolean result = false;
        for (String included : includedFields) {
            if (key == null || included.equals(key)) {
                result = true;
                break;
            } else if (propFullyContainsOther(key, included)) {
                // the key fully contains one of the included
                // check if it is contained also in one of the excluded fields
                // otherwise it will be added.
                boolean matchExcluded = false;
                String startByExcluded = null;
                for (String excluded : excludedFields) {
                    if (propFullyContainsOther(key, excluded)) {
                        startByExcluded = excluded;
                        break;
                    } else if (key.equals(excluded)) {
                        matchExcluded = true;
                        break;
                    }
                }
                if (!matchExcluded && startByExcluded != null) {
                    // which has the more specific path? included or excluded?
                    int comp = comparePropsPathSize(included, startByExcluded);
                    if (comp >= 0) result = true;
                } else {
                    result = !matchExcluded;
                }
                break;

            } else if (propFullyContainsOther(included, key)) {
                // we need to include the builder because an
                // included one is a child of it.
                result = true;
                break;
            }
        }
        return result;
    }

    private int comparePropsPathSize(String props1, String props2) {
        Long countI = getPropPartsLength(props1);
        Long countE = getPropPartsLength(props2);
        return countI.compareTo(countE);
    }

    private long getPropPartsLength(String prop) {
        if (prop == null) return 0;
        else return prop.chars().filter(ch -> ch == '.').count();
    }

    private boolean propFullyContainsOther(String prop1, String prop2) {
        return prop1.startsWith(prop2)
                && Arrays.asList(prop1.split("\\.")).containsAll(Arrays.asList(prop2.split("\\.")));
    }

    @Override
    public boolean hasSelectableJsonValue(AbstractTemplateBuilder builder) {
        String key = builder.getKey(null);
        boolean result = false;
        if (key != null) {
            // if one of the included fields has a reference to this key
            // we will need to wrap the builder and do evaluation at runtime.
            result = containsKey(includedFields, key);
            if (!result) {
                // the same for an excludeField.
                result = containsKey(excludedFields, key);
            }
        }
        return result;
    }

    private boolean containsKey(Set<String> props, String key) {
        boolean result = false;
        for (String prop : props) {
            if (Arrays.asList(prop.split("\\.")).contains(key)) {
                result = true;
                break;
            }
        }
        return result;
    }
}

/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.feature.sort;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.geoserver.catalog.Info;
import org.geotools.api.filter.sort.SortBy;
import org.geotools.api.filter.sort.SortOrder;

/**
 * Builds comparators against catalog info objects based on {@link SortBy} definitions
 *
 * @author Niels Charlier
 */
public class CatalogComparatorFactory {

    /** Builds a composite comparator matching the specified sortBy array */
    public static Comparator<Info> buildComparator(SortBy... sortBy) {
        if (sortBy.length == 0) {
            throw new IllegalArgumentException("No way to build comparators out of an empty comparator set");
        }

        if (sortBy.length == 1) {
            return buildComparator(sortBy[0]);
        } else {
            List<Comparator<Info>> comparators = new ArrayList<>();
            for (SortBy curr : sortBy) {
                Comparator<Info> comparator = buildComparator(curr);
                comparators.add(comparator);
            }

            return new CompositeComparator<>(comparators);
        }
    }

    /** Builds a single comparator based on the sortBy specification */
    public static Comparator<Info> buildComparator(SortBy sortBy) {
        if (sortBy == null) {
            throw new NullPointerException("The sortBy argument must be not null");
        }

        if (sortBy == SortBy.NATURAL_ORDER) {
            return new InfoComparator(true);
        } else if (sortBy == SortBy.REVERSE_ORDER) {
            return new InfoComparator(false);
        } else {
            return new PropertyComparator<>(sortBy.getPropertyName(), sortBy.getSortOrder() == SortOrder.ASCENDING);
        }
    }
}

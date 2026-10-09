/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rat.web;

import static org.junit.Assert.assertEquals;

import it.geosolutions.imageio.pam.PAMDataset;
import it.geosolutions.imageio.pam.PAMDataset.PAMRasterBand.Row;
import it.geosolutions.imageio.pam.PAMParser;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.junit.Before;
import org.junit.Test;

/** Checks the viewer sorts each column by value, and not as text. */
public class RowProviderTest {

    private RowProvider provider;

    @Before
    public void setupProvider() throws IOException {
        File file = new File("./src/test/resources/org/geoserver/rat/gdal312.xml");
        PAMDataset dataset = new PAMParser().parsePAM(file);
        provider = new RowProvider(dataset, 0);
    }

    /** Sorts the rows on the given column and returns that column, in the sorted order. */
    private List<String> sorted(String column, boolean ascending) {
        Property<Row> property = provider.getProperties().stream()
                .filter(p -> p.getName().equals(column))
                .findFirst()
                .orElseThrow();
        Comparator<Row> comparator = property.getComparator();
        if (!ascending) comparator = comparator.reversed();
        return provider.getItems().stream()
                .sorted(comparator)
                .map(r -> (String) property.getPropertyValue(r))
                .collect(Collectors.toList());
    }

    @Test
    public void testSortByReal() {
        // as text the order would be 0, 12.5, 31, 4.75
        String column = "depthRange.minimumDepth";
        assertEquals(List.of("0", "4.75", "12.5", "31"), sorted(column, true));
        assertEquals(List.of("31", "12.5", "4.75", "0"), sorted(column, false));
    }

    @Test
    public void testSortByDateTime() {
        // the second date is 04:00 UTC, later than the first, but as text it reads a day earlier.
        // The unset date is an empty cell, and those sort first.
        String column = "surveyDateRange.dateStart";
        List<String> ascending = List.of(
                "", "2024-03-01T00:00:00.000+00:00", "2024-02-29T23:00:00.000-05:00", "2025-01-02T23:59:59.000+01:15");
        assertEquals(ascending, sorted(column, true));
        List<String> descending = new ArrayList<>(ascending);
        Collections.reverse(descending);
        assertEquals(descending, sorted(column, false));
    }
}

package org.geoserver.api;

import java.util.List;
import org.geoserver.catalog.LayerInfo;

/**
 * Extension point that provides a Link object to the data (items, coverage, data tiles) of a given
 * layer. Used by the style service to add sample data links when other APIs providing data are
 * available. Implementations must be made available in the Spring application context.
 */
public interface SampleDataProvider {

    /** Returns links to sample data, or an empty list if no sample data link can be produced */
    List<Link> getSampleData(LayerInfo layer);
}

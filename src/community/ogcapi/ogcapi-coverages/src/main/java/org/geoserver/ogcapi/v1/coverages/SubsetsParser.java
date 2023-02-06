/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.coverages;

import java.util.List;
import java.util.stream.Collectors;
import net.opengis.wcs20.DimensionSubsetType;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.wcs2_0.kvp.SubsetKvpParser;

/** Parses a comma separated list of subset definitions */
class SubsetsParser {

    SubsetKvpParser parser = new SubsetKvpParser(":");

    List<DimensionSubsetType> parse(String spec) {
        return KvpUtils.readFlat(spec).stream()
                .map(s -> parser.parse(s))
                .collect(Collectors.toList());
    }
}

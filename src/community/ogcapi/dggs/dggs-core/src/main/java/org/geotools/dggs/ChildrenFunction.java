package org.geotools.dggs;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import org.opengis.filter.capability.FunctionName;

/**
 * Checks if the given zoneId is a children of the given referenceZoneId, within a certain distance.
 * This function is meant to be used against a {@link org.geotools.dggs.gstore.DGGSStore} that will
 * fill in the {@link DGGSInstance} to use, usage in any other context will throw an exception.
 *
 * <p>TODO: add some limit to the cached children ids, use a different approach in case the set is
 * large
 */
public class ChildrenFunction extends DGGSSetFunctionBase {

    Set<String> zoneIds;

    public static FunctionName NAME =
            functionName(
                    "children",
                    "result:Boolean",
                    "testedZoneId:String",
                    "referenceZoneId:String",
                    "resolution:Integer",
                    "dggs:org.geotools.dggs.DGGSInstance");

    public ChildrenFunction() {
        super(NAME);
    }

    @Override
    public Object evaluate(Object object) {
        // get the zone being tested
        String testedZoneId = (String) getParameterValue(object, 0);
        if (testedZoneId == null) return false;

        return matches(
                testedZoneId,
                () -> {
                    // check params
                    String referenceZoneId = (String) getParameterValue(object, 1);
                    Integer resolution = (Integer) getParameterValue(object, 2);
                    DGGSInstance dggs = (DGGSInstance) getParameterValue(object, 3);
                    if (referenceZoneId == null || resolution == null || dggs == null)
                        return Collections.emptyIterator();

                    // check resolution first
                    if (dggs.getZone(testedZoneId).getResolution() != resolution)
                        return Collections.emptyIterator();
                    return dggs.children(referenceZoneId, resolution);
                });
    }

    @Override
    public Iterator<Zone> getMatchedZones() {
        if (!isStable()) throw new IllegalStateException("Source parameters are not stable");
        String referenceZoneId = (String) getParameterValue(null, 1);
        Integer resolution = (Integer) getParameterValue(null, 2);
        DGGSInstance dggs = (DGGSInstance) getParameterValue(null, 3);

        return dggs.children(referenceZoneId, resolution);
    }

    @Override
    public long countMatched() {
        if (!isStable()) throw new IllegalStateException("Source parameters are not stable");
        String referenceZoneId = (String) getParameterValue(null, 1);
        Integer resolution = (Integer) getParameterValue(null, 2);
        DGGSInstance dggs = (DGGSInstance) getParameterValue(null, 3);

        return dggs.countChildren(referenceZoneId, resolution);
    }
}

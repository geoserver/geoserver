package org.geotools.dggs;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.locationtech.jts.geom.Polygon;
import org.opengis.filter.capability.FunctionName;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Literal;

/**
 * Checks if the given zoneId is inside the target polygon and at the target resolution. This
 * function is meant to be used against a {@link org.geotools.dggs.gstore.DGGSStore} that will fill
 * in the {@link DGGSInstance} to use, usage in any other context will throw an exception.
 *
 * <p>TODO: add some limit to the polygon cell ids, use a different approach in case the set is too
 * large
 */
public class PolygonFunction extends DGGSSetFunctionBase {

    Set<String> zoneIds;

    public static FunctionName NAME =
            functionName(
                    "dggsPolygon",
                    "result:Boolean",
                    "zoneId:String",
                    "polygon:org.locationtech.jts.geom.Polygon",
                    "resolution:Integer",
                    "compact:Boolean",
                    "dggs:org.geotools.dggs.DGGSInstance");

    public PolygonFunction() {
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
                    Polygon polygon = (Polygon) getParameterValue(object, 1);
                    Integer resolution = (Integer) getParameterValue(object, 2);
                    Boolean compact =
                            Optional.of((Boolean) getParameterValue(null, 3)).orElse(false);
                    DGGSInstance dggs = (DGGSInstance) getParameterValue(object, 4);
                    if (polygon == null || resolution == null || dggs == null)
                        return Collections.emptyIterator();

                    // check resolution first
                    if (dggs.getZone(testedZoneId).getResolution() != resolution)
                        return Collections.emptyIterator();
                    return dggs.polygon(polygon, resolution, compact);
                });
    }

    @Override
    public void setDGGSInstance(DGGSInstance dggs) {
        Literal dggsLiteral = FF.literal(dggs);
        List<Expression> parameters = getParameters();
        if (parameters.size() == 4) {
            parameters.add(dggsLiteral);
        } else {
            parameters.set(4, dggsLiteral);
        }
        setParameters(parameters);
    }

    @Override
    public Iterator<Zone> getMatchedZones() {
        if (!isStable()) throw new IllegalStateException("Source parameters are not stable");
        Polygon polygon = (Polygon) getParameterValue(null, 1);
        Integer resolution = (Integer) getParameterValue(null, 2);
        Boolean compact = Optional.of((Boolean) getParameterValue(null, 3)).orElse(false);
        DGGSInstance dggs = (DGGSInstance) getParameterValue(null, 4);

        return dggs.polygon(polygon, resolution, compact);
    }

    @Override
    public long countMatched() {
        if (!isStable()) throw new IllegalStateException("Source parameters are not stable");
        Polygon polygon = (Polygon) getParameterValue(null, 1);
        Integer resolution = (Integer) getParameterValue(null, 2);
        Boolean compact = Optional.of((Boolean) getParameterValue(null, 3)).orElse(false);
        DGGSInstance dggs = (DGGSInstance) getParameterValue(null, 4);

        return dggs.countPolygon(polygon, resolution);
    }
}

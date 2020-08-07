package org.geotools.dggs;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.opengis.filter.capability.FunctionName;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Literal;

/**
 * Checks if the given zoneId is a parent of the given referenceZoneId. This function is meant to be
 * used against a {@link org.geotools.dggs.gstore.DGGSStore} that will fill in the {@link
 * DGGSInstance} to use, usage in any other context will throw an exception. large
 */
public class ParentsFunction extends DGGSSetFunctionBase {

    Set<String> zoneIds;

    public static FunctionName NAME =
            functionName(
                    "parents",
                    "result:Boolean",
                    "testedZoneId:String",
                    "referenceZoneId:String",
                    "dggs:org.geotools.dggs.DGGSInstance");

    public ParentsFunction() {
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
                    DGGSInstance dggs = (DGGSInstance) getParameterValue(object, 2);
                    if (referenceZoneId == null || dggs == null) return Collections.emptyIterator();

                    // check resolution first
                    return dggs.parents(referenceZoneId);
                });
    }

    @Override
    public void setDGGSInstance(DGGSInstance dggs) {
        Literal dggsLiteral = FF.literal(dggs);
        List<Expression> parameters = getParameters();
        if (parameters.size() == 2) {
            parameters.add(dggsLiteral);
        } else {
            parameters.set(2, dggsLiteral);
        }
        setParameters(parameters);
    }

    @Override
    public Iterator<Zone> getMatchedZones() {
        if (!isStable()) throw new IllegalStateException("Source parameters are not stable");
        String referenceZoneId = (String) getParameterValue(null, 1);
        DGGSInstance dggs = (DGGSInstance) getParameterValue(null, 2);

        return dggs.parents(referenceZoneId);
    }

    @Override
    public long countMatched() {
        if (!isStable()) throw new IllegalStateException("Source parameters are not stable");
        String referenceZoneId = (String) getParameterValue(null, 1);
        DGGSInstance dggs = (DGGSInstance) getParameterValue(null, 2);

        return dggs.countParents(referenceZoneId);
    }
}

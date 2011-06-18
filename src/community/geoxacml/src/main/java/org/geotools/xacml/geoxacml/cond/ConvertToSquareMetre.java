/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2008, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.xacml.geoxacml.cond;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.DoubleAttribute;
import com.sun.xacml.attr.StringAttribute;
import com.sun.xacml.cond.EvaluationResult;
import com.sun.xacml.cond.Expression;

/**
 * Converts units to square metres
 * 
 * @author Christian Mueller
 * 
 */
public class ConvertToSquareMetre extends ConvertFunction {

    public static final String NAME = NAME_PREFIX + "convert-to-square-metre";

    private static Map<String, Double> UnitToSquareMetre;

    static {
        UnitToSquareMetre = new HashMap<String, Double>();
        UnitToSquareMetre.put("acre", 4.046873E+03);

        UnitToSquareMetre.put("are", 1.0E+02);
        UnitToSquareMetre.put("a", 1.0E+02);

        UnitToSquareMetre.put("barn", 1.0E-28);
        UnitToSquareMetre.put("b", 1.0E-28);

        UnitToSquareMetre.put("circular mil", 5.067075E-10);

        UnitToSquareMetre.put("hectare", 1.0E+04);
        UnitToSquareMetre.put("ha", 1.0E+04);

        UnitToSquareMetre.put("ft2", 9.290304E-02);
        UnitToSquareMetre.put("in2", 6.4516E-04);
        UnitToSquareMetre.put("mi2", 2.589988E+06);
        UnitToSquareMetre.put("yd2", 8.361274E-01);

    }

    public ConvertToSquareMetre() {
        super(NAME);

    }

    public EvaluationResult evaluate(List<? extends Expression> inputs, EvaluationCtx context) {

        AttributeValue[] argValues = new AttributeValue[inputs.size()];
        EvaluationResult result = evalArgs(inputs, context, argValues);
        if (result != null)
            return result;

        DoubleAttribute value = (DoubleAttribute) (argValues[0]);
        StringAttribute unit = (StringAttribute) (argValues[1]);

        Double multiplyBy = UnitToSquareMetre.get(unit.getValue());
        if (multiplyBy == null) {
            exceptionError(new Exception("Unit" + unit + " not supported"));
        }
        double resultValue = value.getValue() * multiplyBy;

        return new EvaluationResult(new DoubleAttribute(resultValue));
    }

}

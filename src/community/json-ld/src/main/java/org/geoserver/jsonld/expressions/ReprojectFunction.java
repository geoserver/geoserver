/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jsonld.expressions;

import static org.geotools.filter.capability.FunctionNameImpl.parameter;

import org.geotools.filter.FunctionExpressionImpl;
import org.geotools.filter.capability.FunctionNameImpl;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.factory.epsg.CartesianAuthorityFactory;
import org.locationtech.jts.geom.Geometry;
import org.opengis.filter.capability.FunctionName;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

/** Cql function that allows to reproject geometry towards targetCRS */
public class ReprojectFunction extends FunctionExpressionImpl {
    public static FunctionName NAME =
            new FunctionNameImpl(
                    "reproject",
                    parameter("reprojected", Geometry.class),
                    parameter("targetCRS", String.class),
                    parameter("geometry", Geometry.class));

    public ReprojectFunction() {
        super(NAME);
    }

    public Object evaluate(Object feature) {
        CoordinateReferenceSystem sourceCRS;
        CoordinateReferenceSystem targetCRS;
        Geometry arg1;
        try { // attempt to get value and perform conversion
            targetCRS = getExpression(0).evaluate(feature, CoordinateReferenceSystem.class);
            if (targetCRS == null) {
                String strCrs = getExpression(0).evaluate(feature, String.class);
                try {
                    targetCRS = CRS.decode(strCrs);
                } catch (FactoryException e) {
                    targetCRS = CRS.parseWKT(strCrs);
                }
            }
        } catch (Exception e) // probably a type error
        {
            throw new IllegalArgumentException(
                    "Expected argument of type CoordinateReferenceSystem, WKT or valid EPSG code for argument #0");
        }

        try { // attempt to get value and perform conversion
            arg1 = (Geometry) getExpression(1).evaluate(feature, Geometry.class); // extra

        } catch (Exception e) // probably a type error
        {
            throw new IllegalArgumentException(
                    "Filter Function problem for function reproject argument #1 - expected type Geometry");
        }
        try {

            if (arg1.getSRID() != 0) {
                // sourceCRS = evaluate(feature, CoordinateReferenceSystem.class);
                sourceCRS = CRS.decode("EPSG:" + (arg1.getSRID()));
            } else {
                sourceCRS = CartesianAuthorityFactory.GENERIC_2D;
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to retrieve source CRS");
        }
        try {
            MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS, true);
            return JTS.transform(arg1, transform);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to transform geometry");
        }
    }
}

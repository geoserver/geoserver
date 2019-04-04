/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.kvp;

import net.opengis.wcs20.InterpolationAxesType;
import net.opengis.wcs20.InterpolationAxisType;
import net.opengis.wcs20.InterpolationMethodType;
import net.opengis.wcs20.InterpolationType;
import net.opengis.wcs20.Wcs20Factory;
import org.geoserver.ows.KvpParser;
import org.geoserver.wcs2_0.exception.WCS20Exception;

/**
 * KVP parser for the WCS 2.0 {@link InterpolationType}
 *
 * @author Andrea Aime - GeoSolutions
 */
public class InterpolationKvpParser extends KvpParser {

    public InterpolationKvpParser() {
        super("interpolation", InterpolationType.class);
    }

    @Override
    public Object parse(String value) throws Exception {
        InterpolationType result = Wcs20Factory.eINSTANCE.createInterpolationType();

        // remove space
        value = value.trim();

        // single value?
        if (value.matches("http://www.opengis.net/def/interpolation/OGC/1/[^,:]*")) {
            // single value then
            InterpolationMethodType method = Wcs20Factory.eINSTANCE.createInterpolationMethodType();
            method.setInterpolationMethod(value);
            result.setInterpolationMethod(method);
            return result;
        }

        // minimal validation of the multi-axis case
        if (value.matches(".*,\\s*,.*")) {
            // two consequent commas
            throwInvalidSyntaxException();
        } else if (value.startsWith(",") || value.endsWith(",")) {
            throwInvalidSyntaxException();
        }

        InterpolationAxesType axes = Wcs20Factory.eINSTANCE.createInterpolationAxesType();
        result.setInterpolationAxes(axes);
        String[] components = value.split("\\s*,\\s*");
        for (String component : components) {
            // minimal validation of the content
            if (!component.matches(".*:http://www.opengis.net/def/interpolation/OGC/1/.*")) {
                // not a regular axis:interpolation structure
                throwInvalidSyntaxException();
            } else if (component.matches(".*:\\s*:.*")) {
                // two consequent columns
                throwInvalidSyntaxException();
            }

            int idx = component.lastIndexOf(":", component.lastIndexOf(":") - 1);
            InterpolationAxisType ia = Wcs20Factory.eINSTANCE.createInterpolationAxisType();
            ia.setAxis(component.substring(0, idx));
            ia.setInterpolationMethod(component.substring(idx + 1));

            axes.getInterpolationAxis().add(ia);
        }

        return result;
    }

    protected void throwInvalidSyntaxException() {
        throw new WCS20Exception(
                "Invalid Interpolation syntax, expecting either a single interpolation value, "
                        + "or a comma separated list of axis:interpolation specs",
                WCS20Exception.WCS20ExceptionCode.InvalidEncodingSyntax,
                "interpolation");
    }
}

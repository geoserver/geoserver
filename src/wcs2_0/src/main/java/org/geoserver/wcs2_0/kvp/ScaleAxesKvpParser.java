/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.kvp;

import java.util.List;
import net.opengis.wcs20.ScaleAxisByFactorType;
import net.opengis.wcs20.ScaleAxisType;
import net.opengis.wcs20.Wcs20Factory;
import org.geoserver.wcs2_0.exception.WCS20Exception;

/**
 * Parses the WCS 2.0 {@link ScaleAxisType} from KVP
 *
 * @author Andrea Aime - GeoSolutions
 */
public class ScaleAxesKvpParser extends AbstractAxisValueKvpParser<ScaleAxisType> {

    public ScaleAxesKvpParser() {
        super("scaleaxes", ScaleAxisType.class);
    }

    @Override
    public Object parse(String value) throws Exception {
        ScaleAxisByFactorType sabf = Wcs20Factory.eINSTANCE.createScaleAxisByFactorType();

        List<ScaleAxisType> items = parseItem(value);
        sabf.getScaleAxis().addAll(items);

        return sabf;
    }

    @Override
    protected ScaleAxisType buildItem(String axisName, String value) {
        ScaleAxisType sa = Wcs20Factory.eINSTANCE.createScaleAxisType();
        try {
            sa.setAxis(axisName.trim());
            sa.setScaleFactor(Double.valueOf(value));
        } catch (NumberFormatException e) {
            throwInvalidSyntaxException(null);
        }

        return sa;
    }

    @Override
    protected void throwInvalidSyntaxException(Exception e) {
        WCS20Exception ex =
                new WCS20Exception(
                        "Invalid ScaleAxes syntax, expecting a comma separate list of axisName(scale)*",
                        WCS20Exception.WCS20ExceptionCode.InvalidEncodingSyntax,
                        "scaleAxes");
        if (e != null) {
            ex.initCause(e);
        }
        throw ex;
    }
}

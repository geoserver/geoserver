/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.kvp;

import java.util.List;
import net.opengis.wcs20.ScaleToSizeType;
import net.opengis.wcs20.TargetAxisSizeType;
import net.opengis.wcs20.Wcs20Factory;
import org.geoserver.wcs2_0.exception.WCS20Exception;

/**
 * Parses the WCS 2.0 {@link ScaleToSizeType} from KVP
 *
 * @author Andrea Aime - GeoSolutions
 */
public class ScaleSizeKvpParser extends AbstractAxisValueKvpParser<TargetAxisSizeType> {

    public ScaleSizeKvpParser() {
        super("scalesize", ScaleToSizeType.class);
    }

    @Override
    public Object parse(String value) throws Exception {
        ScaleToSizeType sabf = Wcs20Factory.eINSTANCE.createScaleToSizeType();

        List<TargetAxisSizeType> items = parseItem(value);
        sabf.getTargetAxisSize().addAll(items);

        return sabf;
    }

    @Override
    protected TargetAxisSizeType buildItem(String axisName, String value) {
        TargetAxisSizeType tas = Wcs20Factory.eINSTANCE.createTargetAxisSizeType();
        try {
            tas.setAxis(axisName.trim());
            tas.setTargetSize(Double.valueOf(value));
        } catch (NumberFormatException e) {
            throwInvalidSyntaxException(null);
        }

        return tas;
    }

    @Override
    protected void throwInvalidSyntaxException(Exception e) {
        WCS20Exception ex =
                new WCS20Exception(
                        "Invalid ScaleSize syntax, expecting a comma separate list of axisName(size)*",
                        WCS20Exception.WCS20ExceptionCode.InvalidEncodingSyntax,
                        "scaleSize");
        if (e != null) {
            ex.initCause(e);
        }
        throw ex;
    }
}

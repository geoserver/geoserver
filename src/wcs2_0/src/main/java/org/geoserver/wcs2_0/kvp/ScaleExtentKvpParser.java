/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.kvp;

import net.opengis.wcs20.ScaleToExtentType;
import net.opengis.wcs20.TargetAxisExtentType;
import net.opengis.wcs20.Wcs20Factory;
import org.geoserver.ows.KvpParser;
import org.geoserver.wcs2_0.exception.WCS20Exception;

/**
 * Parses the WCS 2.0 {@link ScaleToExtentType} from KVP
 *
 * @author Andrea Aime - GeoSolutions
 */
public class ScaleExtentKvpParser extends KvpParser {

    public ScaleExtentKvpParser() {
        super("scaleExtent", ScaleToExtentType.class);
    }

    @Override
    public Object parse(String value) throws Exception {
        // clean up extra space
        value = value.trim();

        ScaleToExtentType se = Wcs20Factory.eINSTANCE.createScaleToExtentType();

        int base = 0;
        for (; ; ) {
            // search the open parenthesis
            int idxOpen = value.indexOf("(", base);
            if (idxOpen == -1) {
                throw new WCS20Exception(
                        "Invalid ScaleExtent syntax, expecting a comma separate list of axisName(min,max)*",
                        WCS20Exception.WCS20ExceptionCode.InvalidEncodingSyntax,
                        "scaleExtent");
            }
            int idxNextOpen = value.indexOf("(", idxOpen + 1);

            // search the closed parens
            int idxClosed = value.indexOf(")", idxOpen);
            if (idxClosed == -1 || (idxNextOpen > 0 && idxClosed > idxNextOpen)) {
                throw new WCS20Exception(
                        "Invalid ScaleExtent syntax, expecting a comma separate list of axisName(min,max)*",
                        WCS20Exception.WCS20ExceptionCode.InvalidEncodingSyntax,
                        "scaleExtent");
            }
            int idxNextClosed = value.indexOf(")", idxClosed + 1);

            // the comma between the parens (we start from base to make sure it's actually between
            // the parens)
            int idxMid = value.indexOf(",", base);
            if (idxMid == -1 || idxMid >= idxClosed - 1 || idxMid <= idxOpen + 1) {
                throw new WCS20Exception(
                        "Invalid ScaleExtent syntax, expecting a comma separate list of axisName(min,max)*",
                        WCS20Exception.WCS20ExceptionCode.InvalidEncodingSyntax,
                        "scaleExtent");
            }
            int idxNextMid = value.indexOf(",", idxMid + 1);
            if (idxNextMid != -1 && idxNextMid < idxClosed) {
                throw new WCS20Exception(
                        "Invalid ScaleExtent syntax, expecting a comma separate list of axisName(min,max)*",
                        WCS20Exception.WCS20ExceptionCode.InvalidEncodingSyntax,
                        "scaleExtent");
            }

            // extract the three components
            String axisName = value.substring(base, idxOpen);
            String low = value.substring(idxOpen + 1, idxMid);
            String high = value.substring(idxMid + 1, idxClosed);

            try {
                TargetAxisExtentType te = Wcs20Factory.eINSTANCE.createTargetAxisExtentType();
                te.setAxis(axisName.trim());
                te.setLow(Double.valueOf(low));
                te.setHigh(Double.valueOf(high));

                se.getTargetAxisExtent().add(te);
            } catch (NumberFormatException e) {
                WCS20Exception ex =
                        new WCS20Exception(
                                "Invalid ScaleExtent syntax, expecting a comma separate list of axisName(min,max)*",
                                WCS20Exception.WCS20ExceptionCode.InvalidEncodingSyntax,
                                "scaleExtent");
                ex.initCause(e);
                throw ex;
            }

            // we should also have a comma after the closed parens
            int idxSeparator = value.indexOf(",", idxClosed);
            if (idxSeparator == -1) {
                if (idxClosed == value.length() - 1) {
                    return se;
                } else {
                    throw new WCS20Exception(
                            "Invalid ScaleExtent syntax, expecting a comma separate list of axisName(min,max)*",
                            WCS20Exception.WCS20ExceptionCode.InvalidEncodingSyntax,
                            "scaleExtent");
                }
            } else {
                if (idxSeparator > idxNextClosed) {
                    throw new WCS20Exception(
                            "Invalid ScaleExtent syntax, expecting a comma separate list of axisName(min,max)*",
                            WCS20Exception.WCS20ExceptionCode.InvalidEncodingSyntax,
                            "scaleExtent");
                }
                base = idxSeparator + 1;
            }
        }
    }
}

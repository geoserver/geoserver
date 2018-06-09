/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.kvp;

import java.util.List;
import net.opengis.wcs10.AxisSubsetType;
import net.opengis.wcs10.IntervalType;
import net.opengis.wcs10.TypedLiteralType;
import net.opengis.wcs10.Wcs10Factory;
import org.geoserver.ows.KvpParser;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.ows.util.KvpUtils.Tokenizer;
import org.vfny.geoserver.wcs.WcsException;
import org.vfny.geoserver.wcs.WcsException.WcsExceptionCode;

/**
 * WCS 1.0.0 BBoxKvpParser.
 *
 * @author Alessio Fabiani, GeoSolutions
 */
public class AxisSubsetKvpParser extends KvpParser {
    public AxisSubsetKvpParser() {
        super("band", AxisSubsetType.class);
        setService("wcs");
    }

    public Object parse(String value) throws Exception {
        final AxisSubsetType axisSubset = Wcs10Factory.eINSTANCE.createAxisSubsetType();

        axisSubset.setName("Band");

        if (value.contains("/")) {
            List<String> unparsed = KvpUtils.readFlat(value, new Tokenizer("/"));

            IntervalType interval = Wcs10Factory.eINSTANCE.createIntervalType();
            TypedLiteralType min = Wcs10Factory.eINSTANCE.createTypedLiteralType();
            TypedLiteralType max = Wcs10Factory.eINSTANCE.createTypedLiteralType();
            TypedLiteralType res = Wcs10Factory.eINSTANCE.createTypedLiteralType();
            if (unparsed.size() == 2) {
                min.setValue(unparsed.get(0));
                max.setValue(unparsed.get(1));

                interval.setMin(min);
                interval.setMax(max);
            } else {
                min.setValue(unparsed.get(0));
                max.setValue(unparsed.get(1));
                res.setValue(unparsed.get(2));

                interval.setMin(min);
                interval.setMax(max);
                interval.setRes(res);
            }

            axisSubset.getInterval().add(interval);
        } else {
            List<String> unparsed = KvpUtils.readFlat(value, KvpUtils.INNER_DELIMETER);

            if (unparsed.size() == 0) {
                throw new WcsException(
                        "Requested axis subset contains wrong number of values (should have at least 1): "
                                + unparsed.size(),
                        WcsExceptionCode.InvalidParameterValue,
                        "band");
            }

            for (String bandValue : unparsed) {
                TypedLiteralType singleValue = Wcs10Factory.eINSTANCE.createTypedLiteralType();
                singleValue.setValue(bandValue);

                axisSubset.getSingleValue().add(singleValue);
            }
        }

        return axisSubset;
    }
}

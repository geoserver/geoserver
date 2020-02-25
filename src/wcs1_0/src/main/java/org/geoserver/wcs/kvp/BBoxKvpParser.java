/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.kvp;

import static org.vfny.geoserver.wcs.WcsException.WcsExceptionCode.InvalidParameterValue;

import java.util.List;
import org.geoserver.ows.util.KvpUtils;
import org.geotools.geometry.GeneralEnvelope;
import org.vfny.geoserver.wcs.WcsException;

/**
 * Parsing a BBOX for WCS.
 *
 * <p>Notice that we make sure tht the BBOX is 2D since we support elevation only as a band of the
 * range!
 *
 * @author Simone Giannecchini, GeoSolutions SAS
 */
public class BBoxKvpParser extends Wcs10KvpParser {
    public BBoxKvpParser() {
        super("bbox", GeneralEnvelope.class);
    }

    @SuppressWarnings("unchecked")
    public GeneralEnvelope parse(String value) throws Exception {
        List unparsed = KvpUtils.readFlat(value, KvpUtils.INNER_DELIMETER);
        final int size = unparsed.size();
        // check to make sure that the bounding box has 4 coordinates
        if (unparsed.size() != 4) {
            throw new WcsException(
                    "Requested bounding box contains wrong"
                            + "number of coordinates: "
                            + unparsed.size(),
                    InvalidParameterValue,
                    "bbox");
        }

        // if it does, store them in an array of doubles
        final double[] bbox = new double[size];
        for (int i = 0; i < size; i++) {
            try {
                bbox[i] = Double.parseDouble((String) unparsed.get(i));
            } catch (NumberFormatException e) {
                throw new WcsException(
                        "Bounding box coordinate " + i + " is not parsable:" + unparsed.get(i),
                        InvalidParameterValue,
                        "bbox");
            }
        }

        // ensure the values are sane
        double minx = bbox[0];
        double miny = bbox[1];
        double maxx = bbox[2];
        double maxy = bbox[3];
        //    	double minz = Double.NaN;
        //    	double maxz = Double.NaN;
        //        if(size==6){
        //        	minz = bbox[4];
        //        	maxz = bbox[5];
        //        }
        if (minx > maxx) {
            throw new WcsException(
                    "illegal bbox, minX: " + minx + " is " + "greater than maxX: " + maxx,
                    InvalidParameterValue,
                    "bbox");
        }

        if (miny > maxy) {
            throw new WcsException(
                    "illegal bbox, minY: " + miny + " is " + "greater than maxY: " + maxy,
                    InvalidParameterValue,
                    "bbox");
        }

        //        if (size== 6 &&minz > maxz) {
        //            throw new ServiceException("illegal bbox, minz: " + minz + " is "
        //                    + "greater than maxz: " + maxz);
        //        }

        // build the final envelope with no CRS
        final GeneralEnvelope envelope = new GeneralEnvelope(size / 2);
        //        if(size==4)
        envelope.setEnvelope(minx, miny, maxx, maxy);
        //        else
        //        	envelope.setEnvelope(minx,miny,minz,maxx,maxy,maxz);
        return envelope;
    }
}

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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.xacml.geoxacml.attr.GeometryAttribute;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import com.sun.xacml.cond.EvaluationResult;
import com.sun.xacml.cond.FunctionBase;
import com.sun.xacml.ctx.Status;
import com.vividsolutions.jts.geom.Geometry;

/**
 * @author Christian Mueller
 * 
 *         Abstract base class for GeoXACML functions.
 * 
 *         Also responsible for coordinate transformations. WGS84 is the common CRS, if
 *         transformation has to be done, the target CRS is always WGS84
 * 
 */
public abstract class GeoXACMLFunctionBase extends FunctionBase {

    protected static final String NAME_PREFIX = "urn:ogc:def:function:geoxacml:1.0:";

    protected static final String COMMON_CRS_NAME = "EPSG:4326";

    protected static CoordinateReferenceSystem COMMON_CRS = null;

    public GeoXACMLFunctionBase(String functionName, int functionId, String paramType,
            boolean paramIsBag, int numParams, int minParams, String returnType, boolean returnsBag) {
        super(functionName, functionId, paramType, paramIsBag, numParams, minParams, returnType,
                returnsBag);
    }

    public GeoXACMLFunctionBase(String functionName, int functionId, String paramType,
            boolean paramIsBag, int numParams, String returnType, boolean returnsBag) {
        super(functionName, functionId, paramType, paramIsBag, numParams, returnType, returnsBag);
    }

    public GeoXACMLFunctionBase(String functionName, int functionId, String returnType,
            boolean returnsBag) {
        super(functionName, functionId, returnType, returnsBag);
    }

    public GeoXACMLFunctionBase(String functionName, int functionId, String[] paramTypes,
            boolean[] paramIsBag, String returnType, boolean returnsBag) {
        super(functionName, functionId, paramTypes, paramIsBag, returnType, returnsBag);
    }

    /**
     * @param array
     *            array of GeometryAttributes, length == 2
     * @return array with possible replaced GeometryAttributes (transformed to WGS84)
     * @throws GeoXACMLException
     * 
     *             This method tries to avoid transformations.
     * 
     *             No transformation in the following situations:
     * 
     *             1) Both have no srsName (null) 2) Both have srsNames and equalsIgnoreCase is true
     *             3) Both CRS are decodeable and equalsIgnoreMetaData is true
     * 
     *             Error situations are
     * 
     *             1) One attribute has a srsName, the other not 2) a CRS is not decodeable
     * 
     *             If we need a transformation, both geometries are transformd to WGS84
     */
    protected String transformOnDemand(GeometryAttribute array[]) throws GeoXACMLException {
        GeometryAttribute attr1 = array[0];
        GeometryAttribute attr2 = array[1];

        if (attr1.getSrsName() == null && attr2.getSrsName() == null)
            return null;

        if ((attr1.getSrsName() == null && attr2.getSrsName() != null)
                || (attr1.getSrsName() != null && attr2.getSrsName() == null)) {
            throw new GeoXACMLException("Missing srsName");
        }

        if (attr1.getSrsName().equalsIgnoreCase(attr2.getSrsName()))
            return attr1.getSrsName();

        CoordinateReferenceSystem[] crsArray = new CoordinateReferenceSystem[2];
        crsArray[0] = decodeCRS(array[0].getSrsName());
        crsArray[1] = decodeCRS(array[1].getSrsName());

        for (int i = 0; i < crsArray.length; i++) { // check if not null
            if (crsArray[i] == null)
                throw new GeoXACMLException("Cannod decode " + array[i].getSrsName());
        }

        if (CRS.equalsIgnoreMetadata(crsArray[0], crsArray[1])) // CRS are compatible
            return array[0].getSrsName();

        try {
            for (int i = 0; i < array.length; i++) {
                Geometry newGeom = transformToCommonCRS(array[i].getGeometry(), array[i]
                        .getSrsName(), crsArray[i]);
                if (newGeom != array[i].getGeometry()) { // geometry changed
                    GeometryAttribute newGeomAttr = new GeometryAttribute(newGeom, COMMON_CRS_NAME,
                            array[i].getGid(), array[i].getGmlVersion(), array[i].getType()
                                    .toString());
                    newGeomAttr.setSrsDimension(array[i].getSrsDimension());
                    array[i] = newGeomAttr;
                }
            }
        } catch (Exception e) {
            throw new GeoXACMLException(e);
        }
        return COMMON_CRS_NAME;

    }

    /**
     * @param g
     * @param srsName
     * @param sourceCRS
     * @return
     * @throws GeoXACMLException
     * 
     *             Transformation of a geomtry to WGS84
     * 
     *             No transformation in the following situations
     * 
     *             1) the srsName equalsIgnoreCase with EPSG:4326 is true 2) equalsIgnorMetaData
     *             returns true
     */
    protected Geometry transformToCommonCRS(Geometry g, String srsName,
            CoordinateReferenceSystem sourceCRS) throws GeoXACMLException {

        try {
            if (COMMON_CRS == null) {
                synchronized (COMMON_CRS_NAME) {
                    COMMON_CRS = CRS.decode(COMMON_CRS_NAME, true);
                }
            }

            if (COMMON_CRS_NAME.equalsIgnoreCase(srsName))
                return g;

            if (CRS.equalsIgnoreMetadata(sourceCRS, COMMON_CRS))
                return g;

            MathTransform transform = CRS.findMathTransform(sourceCRS, COMMON_CRS);
            return JTS.transform(g, transform);
        } catch (Exception e) {
            throw new GeoXACMLException(e);
        }
    }

    /**
     * @param t
     *            a Throwable
     * @return an EvaluationResult indicating a processing error
     */
    protected EvaluationResult exceptionError(Throwable t) {

        Logger log = Logger.getLogger(this.getClass().getName());
        log.log(Level.SEVERE, t.getMessage(), t);

        List<String> codeList = new ArrayList<String>();
        codeList.add(Status.STATUS_PROCESSING_ERROR);
        return new EvaluationResult(new Status(codeList, t.getLocalizedMessage()));
    }

    /**
     * @param srsName
     * @return CoordinateRefernceSystem
     * @throws GeoXACMLException
     * 
     *             try to decode the value of the GML srsName attribute
     */
    protected CoordinateReferenceSystem decodeCRS(String srsName) throws GeoXACMLException {

        URI srs = null;

        try {
            srs = new URI(srsName);
        } catch (URISyntaxException e) { // failed, continue on
        }

        if (srs != null) {
            // TODO: JD, this is a hack until GEOT-1136 has been resolved
            if ("http".equals(srs.getScheme()) && "www.opengis.net".equals(srs.getAuthority())
                    && "/gml/srs/epsg.xml".equals(srs.getPath()) && (srs.getFragment() != null)) {
                try {
                    return CRS.decode("EPSG:" + srs.getFragment(), true);
                } catch (Exception e) {
                    // failed, try as straight up uri
                    try {
                        return CRS.decode(srs.toString(), true);
                    } catch (Exception e1) {
                        // failed again, do nothing ,should fail below as well
                    }
                }
            }
        }

        try {
            return CRS.decode(srsName, true);
        } catch (NoSuchAuthorityCodeException e) {
            // HACK HACK HACK!: remove when
            // https://osgeo-org.atlassian.net/browse/GEOT-1659 is fixed

            if (srsName.toUpperCase().startsWith("URN")) {
                String code = srsName.substring(srsName.lastIndexOf(":") + 1);
                try {
                    return CRS.decode("EPSG:" + code, true);
                } catch (Exception e1) {
                    throw new GeoXACMLException("Could not create crs: " + srs, e);
                }
            }
        } catch (FactoryException e) {
            throw new GeoXACMLException("Could not create crs: " + srs, e);
        }

        return null;
    }

}

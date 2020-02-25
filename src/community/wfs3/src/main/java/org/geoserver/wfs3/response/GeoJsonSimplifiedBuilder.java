/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3.response;

import java.io.Writer;
import javax.measure.Unit;
import net.sf.json.JSONException;
import net.sf.json.util.JSONBuilder;
import org.geoserver.wfs.json.GeoJSONBuilder;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.precision.CoordinatePrecisionReducerFilter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import si.uom.SI;

/** GeoJsonBuilder extension that writes simplified coordinates decimals on geometries */
public class GeoJsonSimplifiedBuilder extends GeoJSONBuilder {

    private CoordinateReferenceSystem mapCrs;
    private CoordinatePrecisionReducerFilter precisionReducerFilter;

    public GeoJsonSimplifiedBuilder(Writer w, CoordinateReferenceSystem mapCrs) {
        super(w);
        this.mapCrs = mapCrs;
        // initialize precisionReducerFilter
        Unit<?> unit = this.mapCrs.getCoordinateSystem().getAxis(0).getUnit();
        Unit<?> standardUnit = unit.getSystemUnit();
        PrecisionModel pm = null;
        if (SI.RADIAN.equals(standardUnit)) {
            pm = new PrecisionModel(1e6); // truncate coords at 6 decimals
        } else if (SI.METRE.equals(standardUnit)) {
            pm = new PrecisionModel(100); // truncate coords at 2 decimals
        }
        if (pm != null) {
            precisionReducerFilter = new CoordinatePrecisionReducerFilter(pm);
        }
    }

    @Override
    public JSONBuilder writeGeom(Geometry geometry) throws JSONException {
        if (precisionReducerFilter != null) {
            geometry.apply(precisionReducerFilter);
        }
        return super.writeGeom(geometry);
    }
}

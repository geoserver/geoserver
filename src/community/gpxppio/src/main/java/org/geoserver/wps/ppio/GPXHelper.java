/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import java.util.List;
import org.geoserver.wps.ppio.gpx.GpxType;
import org.geoserver.wps.ppio.gpx.RteType;
import org.geoserver.wps.ppio.gpx.WptType;
import org.geotools.api.feature.Property;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.type.Name;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.Point;

/** Small helper class to convert from JTS Geometry to GPX types */
public class GPXHelper {
    private GpxType gpxType;

    public GPXHelper(GpxType gpx) {
        this.gpxType = gpx;
    }

    public void addFeature(SimpleFeature f) {

        Object defaultGeometry = f.getDefaultGeometryProperty();
        if (defaultGeometry == null) {
            return;
        }
        String nameStr = null;
        String commentStr = null;
        String descriptionStr = null;

        for (Property p : f.getProperties()) {
            Object object = p.getValue();
            if (object instanceof Geometry) {
                continue;
            } else {
                Name name = p.getName();
                if (name.getLocalPart().equalsIgnoreCase("name")
                        || name.getLocalPart().equalsIgnoreCase("geographicalName")) {
                    nameStr = p.getValue().toString();
                } else if (name.getLocalPart().equalsIgnoreCase("description")) {
                    descriptionStr = p.getValue().toString();
                } else if (name.getLocalPart().equalsIgnoreCase("comment")) {
                    commentStr = p.getValue().toString();
                }
            }
        }

        Object go = ((Property) defaultGeometry).getValue();
        if (go instanceof MultiLineString string1) {
            int nrls = string1.getNumGeometries();
            for (int li = 0; li < nrls; li++) {
                Geometry ls = string1.getGeometryN(li);
                RteType rte = toRte((LineString) ls);
                if (nameStr != null) rte.setName(nameStr);
                if (commentStr != null) rte.setCmt(commentStr);
                if (descriptionStr != null) rte.setDesc(descriptionStr);
                gpxType.getRte().add(rte);
            }
        } else if (go instanceof LineString string) {
            RteType rte = toRte(string);
            if (nameStr != null) rte.setName(nameStr);
            if (commentStr != null) rte.setCmt(commentStr);
            if (descriptionStr != null) rte.setDesc(descriptionStr);
            gpxType.getRte().add(rte);
        } else if (go instanceof MultiPoint point1) {
            int nrpt = point1.getNumGeometries();
            for (int pi = 0; pi < nrpt; pi++) {
                Geometry pt = point1.getGeometryN(pi);
                WptType wpt = toWpt((Point) pt);
                if (nameStr != null) wpt.setName(nameStr);
                if (commentStr != null) wpt.setCmt(commentStr);
                if (descriptionStr != null) wpt.setDesc(descriptionStr);
                gpxType.getWpt().add(wpt);
            }
        } else if (go instanceof Point point) {
            WptType wpt = toWpt(point);
            if (nameStr != null) wpt.setName(nameStr);
            if (commentStr != null) wpt.setCmt(commentStr);
            if (descriptionStr != null) wpt.setDesc(descriptionStr);
            gpxType.getWpt().add(wpt);
        }
    }

    public WptType toWpt(Point p) {
        return coordToWpt(p.getX(), p.getY());
    }

    private WptType coordToWpt(double x, double y) {
        WptType wpt = new WptType();
        wpt.setLon(x);
        wpt.setLat(y);
        return wpt;
    }

    public RteType toRte(LineString ls) {
        RteType rte = new RteType();
        List<WptType> rtePts = rte.getRtept();

        Coordinate[] coordinates = ls.getCoordinates();
        for (Coordinate coordinate : coordinates) {
            rtePts.add(coordToWpt(coordinate.x, coordinate.y));
        }
        return rte;
    }
}

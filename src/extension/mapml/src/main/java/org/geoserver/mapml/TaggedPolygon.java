/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.mapml;

import java.util.List;
import org.locationtech.jts.geom.Coordinate;

/**
 * A class to represent a clipped and tagged polygon, that is, a Polygon that has been clipped on
 * the server side, introducing extra vertices to the boundary of the polygon. The polygon boundary
 * and holes are represented as a list of visible and invisible coordinate lists, which the
 * generator will translate into a MapML document making sure the invisible ones are associated to
 * an invisible <code>map-span</code>.
 */
class TaggedPolygon {

    /**
     * A class to represent a tagged coordinate sequence, that is, a list of coordinates with a
     * visibility flag.
     */
    static class TaggedCoordinateSequence {

        boolean visible;

        List<Coordinate> coordinates;

        public TaggedCoordinateSequence(boolean visible, List<Coordinate> coordinates) {
            this.visible = visible;
            this.coordinates = coordinates;
        }

        public boolean isVisible() {
            return visible;
        }

        public List<Coordinate> getCoordinates() {
            return coordinates;
        }

        @Override
        public String toString() {
            return "TaggedCoordinateSequence{"
                    + "visible="
                    + visible
                    + ", coordinates="
                    + coordinates
                    + '}';
        }
    }

    /**
     * A class to represent a clipped linestring, that is, a LineString made of visible and
     * invisible coordinate sequences.
     */
    static class TaggedLineString {
        List<TaggedCoordinateSequence> coordinates;

        public TaggedLineString(List<TaggedCoordinateSequence> coordinates) {
            this.coordinates = coordinates;
        }

        public List<TaggedCoordinateSequence> getCoordinates() {
            return coordinates;
        }

        @Override
        public String toString() {
            return "TaggedLineString{" + "coordinates=" + coordinates + '}';
        }
    }

    TaggedLineString boundary;
    List<TaggedLineString> holes;

    public TaggedPolygon(TaggedLineString boundary, List<TaggedLineString> holes) {
        this.boundary = boundary;
        this.holes = holes;
    }

    public TaggedLineString getBoundary() {
        return boundary;
    }

    public List<TaggedLineString> getHoles() {
        return holes;
    }

    @Override
    public String toString() {
        return "TaggedPolygon{" + "boundary=" + boundary + ", holes=" + holes + '}';
    }
}

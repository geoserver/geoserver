package org.geoserver.bxml.gml_3_1;

import static org.geotools.gml3.GML.coordinates;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import org.geoserver.bxml.Decoder;
import org.geoserver.bxml.base.StringDecoder;
import org.gvsig.bxml.stream.BxmlStreamReader;
import org.gvsig.bxml.stream.EventType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.impl.PackedCoordinateSequence;

/**
 * The Class CoordinatesDecoder.
 * 
 * @author cfarina
 */
public class CoordinatesDecoder implements Decoder<CoordinateSequence> {

    /**
     * Decode.
     * 
     * @param r
     *            the r
     * @return the coordinate sequence
     * @throws Exception
     *             the exception
     */
    @Override
    public CoordinateSequence decode(BxmlStreamReader r) throws Exception {
        r.require(EventType.START_ELEMENT, coordinates.getNamespaceURI(),
                coordinates.getLocalPart());

        String decimal = r.getAttributeValue(null, "decimal") != null ? r.getAttributeValue(null,
                "decimal") : ".";
        char cs = r.getAttributeValue(null, "cs") != null ? r.getAttributeValue(null, "cs").charAt(
                0) : ',';
        char ts = r.getAttributeValue(null, "ts") != null ? r.getAttributeValue(null, "ts").charAt(
                0) : ' ';

        String value = new StringDecoder(coordinates).decode(r);

        Coordinate[] coordinates = parse(value, decimal, cs, ts);
        return new PackedCoordinateSequence.Double(coordinates);
    }

    /**
     * Parses the.
     * 
     * @param string
     *            the string
     * @param decimal
     *            the decimal
     * @param cs
     *            the cs
     * @param ts
     *            the ts
     * @return the coordinate[]
     */
    private Coordinate[] parse(String string, String decimal, char cs, char ts) {
        String trim = string.trim();
        final int len = trim.length();

        StringBuilder curr = new StringBuilder();

        List<Coordinate> coordinates = new ArrayList<Coordinate>();
        List<String> strings = new ArrayList<String>();
        Boolean isCs = true;
        for (int i = 0; i < len; i++) {
            char c = trim.charAt(i);
            if (c != ts && c != cs) {
                curr.append(c);
            } else if (curr.length() > 0) {
                if (isCs) {
                    if (curr.length() > 0) {
                        strings.add(curr.toString().replace(decimal, "."));
                    }
                    curr.setLength(0);
                } else {
                    coordinates.add(buildCoordinate(strings, decimal));
                    strings = new ArrayList<String>();
                    if (curr.length() > 0) {
                        strings.add(curr.toString().replace(decimal, "."));
                    }
                    curr.setLength(0);
                }
                isCs = false;
            }
            if (c == cs) {
                isCs = true;
            }
            if (i == (len - 1)) {
                if (curr.length() > 0) {
                    strings.add(curr.toString().replace(decimal, "."));
                }
                coordinates.add(buildCoordinate(strings, decimal));
            }
        }

        return coordinates.toArray(new Coordinate[coordinates.size()]);
    }

    /**
     * Builds the coordinate.
     * 
     * @param strings
     *            the strings
     * @param decimal
     *            the decimal
     * @return the coordinate
     */
    private Coordinate buildCoordinate(List<String> strings, String decimal) {
        Coordinate coordinate = new Coordinate();
        coordinate.x = Double.parseDouble(strings.get(0));
        coordinate.y = Double.parseDouble(strings.get(1));
        if (strings.size() > 2) {
            coordinate.z = Double.parseDouble(strings.get(2));
        }
        return coordinate;
    }

    /**
     * Can handle.
     * 
     * @param name
     *            the name
     * @return true, if successful
     */
    @Override
    public boolean canHandle(QName name) {
        return coordinates.equals(name);
    }

    /**
     * Gets the targets.
     * 
     * @return the targets
     */
    @Override
    public Set<QName> getTargets() {
        return Collections.singleton(coordinates);
    }

}

package org.geoserver.bxml.gml_3_1;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import org.geoserver.bxml.SequenceDecoder;
import org.geotools.gml3.GML;
import org.gvsig.bxml.stream.BxmlStreamReader;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;

/**
 * The Class MultiLineStringDecoder.
 * 
 * @author cfarina
 */
public class MultiLineStringDecoder extends AbstractGeometryDecoder<Geometry> {

    /**
     * Instantiates a new multi line string decoder.
     */
    public MultiLineStringDecoder() {
        super(GML.MultiLineString);
    }

    /**
     * Decode internal.
     * 
     * @param r
     *            the r
     * @param name
     *            the name
     * @return the multi line string
     * @throws Exception
     *             the exception
     */
    @Override
    protected MultiLineString decodeInternal(BxmlStreamReader r, QName name) throws Exception {
        SequenceDecoder<Geometry> seq = new SequenceDecoder<Geometry>(1, Integer.MAX_VALUE);
        seq.add(new GeometryMemberDecoder(GML.lineStringMember, new LineStringDecoder()), 1, 1);

        r.nextTag();
        Iterator<Geometry> iterator = seq.decode(r);
        List<LineString> lines = new ArrayList<LineString>();
        while (iterator.hasNext()) {
            lines.add((LineString) iterator.next());

        }
        MultiLineString multiLineString = new GeometryFactory().createMultiLineString(lines
                .toArray(new LineString[lines.size()]));
        multiLineString.setUserData(getCrs());
        return multiLineString;
    }

}

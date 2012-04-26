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
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.Point;

/**
 * The Class MultiPointDecoder.
 * 
 * @author cfarina
 */
public class MultiPointDecoder extends AbstractGeometryDecoder<Geometry> {

    /**
     * Instantiates a new multi point decoder.
     */
    public MultiPointDecoder() {
        super(GML.MultiPoint);
    }

    /**
     * Decode internal.
     * 
     * @param r
     *            the r
     * @param name
     *            the name
     * @return the multi point
     * @throws Exception
     *             the exception
     */
    @Override
    protected MultiPoint decodeInternal(BxmlStreamReader r, QName name) throws Exception {
        SequenceDecoder<Geometry> seq = new SequenceDecoder<Geometry>(1, Integer.MAX_VALUE);
        seq.add(new GeometryMemberDecoder(GML.pointMember, new PointDecoder()), 1, 1);

        r.nextTag();
        Iterator<Geometry> iterator = seq.decode(r);
        List<Point> points = new ArrayList<Point>();
        while (iterator.hasNext()) {
            points.add((Point) iterator.next());

        }
        MultiPoint multiPoint = new GeometryFactory().createMultiPoint(points
                .toArray(new Point[points.size()]));
        multiPoint.setUserData(getCrs());
        return multiPoint;
    }

}

package org.geoserver.bxml.gml_3_1;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import org.geoserver.bxml.ChoiceDecoder;
import org.geoserver.bxml.SequenceDecoder;
import org.geotools.gml3.GML;
import org.gvsig.bxml.stream.BxmlStreamReader;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

/**
 * The Class MultiPolygonDecoder.
 * 
 * @author cfarina
 */
public class MultiPolygonDecoder extends AbstractGeometryDecoder<Geometry> {

    /**
     * Instantiates a new multi polygon decoder.
     */
    public MultiPolygonDecoder() {
        super(GML.MultiPolygon, GML.MultiSurface);
    }

    /**
     * Decode internal.
     * 
     * @param r
     *            the r
     * @param name
     *            the name
     * @return the multi polygon
     * @throws Exception
     *             the exception
     */
    @Override
    protected MultiPolygon decodeInternal(BxmlStreamReader r, QName name) throws Exception {
        SequenceDecoder<Geometry> seq = new SequenceDecoder<Geometry>(1, Integer.MAX_VALUE);
        ChoiceDecoder<Geometry> choice = new ChoiceDecoder<Geometry>();
        choice.addOption(new GeometryMemberDecoder(GML.polygonMember, new PolygonDecoder()));
        choice.addOption(new GeometryMemberDecoder(GML.surfaceMember, new PolygonDecoder()));
        seq.add(choice, 1, 1);

        r.nextTag();
        Iterator<Geometry> iterator = seq.decode(r);
        List<Polygon> polygons = new ArrayList<Polygon>();
        while (iterator.hasNext()) {
            polygons.add((Polygon) iterator.next());

        }
        MultiPolygon multiPolygon = new GeometryFactory().createMultiPolygon(polygons
                .toArray(new Polygon[polygons.size()]));
        multiPolygon.setUserData(getCrs());
        return multiPolygon;
    }

}

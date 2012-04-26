package org.geoserver.bxml.gml_3_1;

import static org.geotools.gml3.GML.Polygon;
import static org.geotools.gml3.GML.exterior;
import static org.geotools.gml3.GML.interior;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import org.geoserver.bxml.ChoiceDecoder;
import org.geoserver.bxml.SequenceDecoder;
import org.geoserver.bxml.SetterDecoder;
import org.gvsig.bxml.stream.BxmlStreamReader;

import com.google.common.collect.Iterators;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;

/**
 * The Class PolygonDecoder.
 * 
 * @author cfarina
 */
public class PolygonDecoder extends AbstractGeometryDecoder<Geometry> {

    /**
     * Instantiates a new polygon decoder.
     */
    public PolygonDecoder() {
        super(Polygon);
    }

    /**
     * Decode internal.
     * 
     * @param r
     *            the r
     * @param name
     *            the name
     * @return the geometry
     * @throws Exception
     *             the exception
     */
    @Override
    public Geometry decodeInternal(BxmlStreamReader r, QName name) throws Exception {
        ChoiceDecoder<Object> choice = new ChoiceDecoder<Object>();

        PolygonRings polygonRing = new PolygonRings();

        choice.addOption(new SetterDecoder<Object>(new PolygonLineRingDecoder(getCrs(),
                getDimension(), interior), polygonRing, "interior"));
        choice.addOption(new SetterDecoder<Object>(new PolygonLineRingDecoder(getCrs(),
                getDimension(), exterior), polygonRing, "exterior"));

        SequenceDecoder<Object> seq = new SequenceDecoder<Object>(1, 1);
        seq.add(choice, 0, Integer.MAX_VALUE);

        r.nextTag();
        final Iterator<Object> iterator = seq.decode(r);
        Iterators.toArray(iterator, Object.class);

        Polygon geometry = new GeometryFactory()
                .createPolygon(
                        polygonRing.getExterior(),
                        polygonRing.getInterior().toArray(
                                new LinearRing[polygonRing.getInterior().size()]));
        geometry.setUserData(getCrs());

        return geometry;
    }

    /**
     * The Class PolygonRings.
     */
    public class PolygonRings {

        /** The exterior. */
        private LinearRing exterior;

        /** The interior. */
        private List<LinearRing> interior;

        /**
         * Gets the exterior.
         * 
         * @return the exterior
         */
        public LinearRing getExterior() {
            return exterior;
        }

        /**
         * Sets the exterior.
         * 
         * @param exterior
         *            the new exterior
         */
        public void setExterior(LinearRing exterior) {
            this.exterior = exterior;
        }

        /**
         * Gets the interior.
         * 
         * @return the interior
         */
        public List<LinearRing> getInterior() {
            if (interior == null) {
                interior = new ArrayList<LinearRing>(2);
            }
            return interior;
        }

        /**
         * Sets the interior.
         * 
         * @param interior
         *            the new interior
         */
        public void setInterior(List<LinearRing> interior) {
            this.interior = interior;
        }

    }

}

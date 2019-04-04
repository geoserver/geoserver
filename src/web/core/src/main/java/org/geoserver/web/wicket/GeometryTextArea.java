/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import java.util.Locale;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.convert.ConversionException;
import org.apache.wicket.util.convert.IConverter;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

/**
 * A form component for a {@link Geometry} object, expressed either as 2-3 space separated ordinates
 * or a WKT formatted {@link Geometry}
 *
 * @author Andrea Aime, GeoSolutions
 */
public class GeometryTextArea extends TextArea<Geometry> {
    private static final long serialVersionUID = 1L;

    protected TextArea<String> geometry;

    public GeometryTextArea(String id) {
        this(id, new Model<Geometry>(null));
    }

    public GeometryTextArea(String id, Geometry g) {
        this(id, new Model<Geometry>(g));
    }

    public GeometryTextArea(String id, IModel<Geometry> model) {
        super(id, model);
        setType(Geometry.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <C> IConverter<C> getConverter(Class<C> type) {
        return (IConverter<C>) new GeometryConverter();
    }
    /**
     * Converts between String and Geometry
     *
     * @author Andrea Aime - GeoSolutions
     */
    private class GeometryConverter implements IConverter<Object> {
        private static final long serialVersionUID = 5868644160487841740L;

        transient GeometryFactory gf = new GeometryFactory();

        transient WKTReader reader = new WKTReader(gf);

        @Override
        public Object convertToObject(String value, Locale locale) {
            try {
                return reader.read(value);
            } catch (ParseException e) {
                try {
                    String[] values = value.split("\\s+");
                    if (values.length > 0 && values.length < 3) {
                        Coordinate c = new Coordinate();
                        c.x = Double.parseDouble(values[0]);
                        c.y = Double.parseDouble(values[1]);
                        return gf.createPoint(c);
                    }
                } catch (NumberFormatException nfe) {
                    // fall through
                }

                ConversionException ce = new ConversionException(e.getMessage());
                ce.setResourceKey(GeometryTextArea.class.getSimpleName() + ".parseError");
                throw ce;
            }
        }

        @Override
        public String convertToString(Object value, Locale locale) {
            if (value instanceof Point) {
                Coordinate c = ((Point) value).getCoordinate();
                return c.x + " " + c.y;
            } else {
                return value.toString();
            }
        }
    }
}

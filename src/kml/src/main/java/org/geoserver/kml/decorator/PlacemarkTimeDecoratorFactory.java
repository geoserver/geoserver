/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml.decorator;

import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.TimeSpan;
import de.micromata.opengis.kml.v_2_2_0.TimeStamp;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import org.geoserver.kml.KmlEncodingContext;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.featureinfo.FeatureTemplate;
import org.geotools.feature.type.DateUtil;
import org.geotools.util.logging.Logging;
import org.geotools.xs.bindings.XSDateTimeBinding;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * Template driven decorator setting the name in Placemark objects
 *
 * @author Andrea Aime - GeoSolutions
 * @author Wayne Fang, Refractions Research, wfang@refractions.net
 * @author Arne Kepp - OpenGeo
 * @author Justin Deoliveira - OpenGeo
 */
public class PlacemarkTimeDecoratorFactory implements KmlDecoratorFactory {

    static final Logger LOGGER = Logging.getLogger(PlacemarkTimeDecorator.class);

    /**
     * list of formats which correspond to the default formats in which freemarker outputs dates
     * when a user calls the ?datetime(),?date(),?time() fuctions.
     */
    List<DateFormat> dtformats = new ArrayList<DateFormat>();

    List<DateFormat> dformats = new ArrayList<DateFormat>();

    List<DateFormat> tformats = new ArrayList<DateFormat>();

    public PlacemarkTimeDecoratorFactory() {
        // add default freemarker ones first since they are likely to be used
        // first, the order of this list matters.
        // this is done in the constructor because otherwise there will be timezone
        // contaminations between junit tests, and the factory is a singleton in the GS lifetime
        // anyways

        dtformats.add(DateFormat.getDateTimeInstance());
        dtformats.add(DateFormat.getInstance());

        dtformats.add(new SimpleDateFormat(FeatureTemplate.DATETIME_FORMAT_PATTERN));
        addFormats(dtformats, "dd%MM%yy hh:mm:ss");
        addFormats(dtformats, "MM%dd%yy hh:mm:ss");
        // addFormats(formats,"yy%MM%dd hh:mm:ss" );
        addFormats(dtformats, "dd%MMM%yy hh:mm:ss");
        addFormats(dtformats, "MMM%dd%yy hh:mm:ss");
        // addFormats(formats,"yy%MMM%dd hh:mm:ss" );

        addFormats(dtformats, "dd%MM%yy hh:mm");
        addFormats(dtformats, "MM%dd%yy hh:mm");
        // addFormats(formats,"yy%MM%dd hh:mm" );
        addFormats(dtformats, "dd%MMM%yy hh:mm");
        addFormats(dtformats, "MMM%dd%yy hh:mm");
        // addFormats(formats,"yy%MMM%dd hh:mm" );

        dformats.add(DateFormat.getDateInstance());
        dformats.add(new SimpleDateFormat(FeatureTemplate.DATE_FORMAT_PATTERN));
        addFormats(dformats, "dd%MM%yy");
        addFormats(dformats, "MM%dd%yy");
        // addFormats(formats,"yy%MM%dd" );
        addFormats(dformats, "dd%MMM%yy");
        addFormats(dformats, "MMM%dd%yy");
        // addFormats(formats,"yy%MMM%dd" );

        tformats.add(DateFormat.getTimeInstance());
        tformats.add(new SimpleDateFormat(FeatureTemplate.TIME_FORMAT_PATTERN));
    }

    void addFormats(List<DateFormat> formats, String pattern) {
        formats.add(new SimpleDateFormat(pattern.replaceAll("%", "-")));
        formats.add(new SimpleDateFormat(pattern.replaceAll("%", "/")));
        formats.add(new SimpleDateFormat(pattern.replaceAll("%", ".")));
        formats.add(new SimpleDateFormat(pattern.replaceAll("%", " ")));
        formats.add(new SimpleDateFormat(pattern.replaceAll("%", ",")));
    }

    @Override
    public KmlDecorator getDecorator(
            Class<? extends Feature> featureClass, KmlEncodingContext context) {
        // this decorator is used only for WMS
        if (!(context.getService() instanceof WMSInfo)) {
            return null;
        }

        if (Placemark.class.isAssignableFrom(featureClass) && hasTimeTemplate(context)) {
            return new PlacemarkTimeDecorator();
        } else {
            return null;
        }
    }

    private boolean hasTimeTemplate(KmlEncodingContext context) {
        try {
            SimpleFeatureType schema = context.getCurrentFeatureCollection().getSchema();
            return !context.getTemplate()
                    .isTemplateEmpty(schema, "time.ftl", FeatureTemplate.class, null);
        } catch (IOException e) {
            throw new ServiceException("Failed to apply time template during kml generation", e);
        }
    }

    class PlacemarkTimeDecorator implements KmlDecorator {

        @Override
        public Feature decorate(Feature feature, KmlEncodingContext context) {
            Placemark pm = (Placemark) feature;

            // try with the template
            SimpleFeature sf = context.getCurrentFeature();
            try {
                String[] times = execute(context.getTemplate(), sf);
                if (times != null && times.length > 0) {
                    if (times.length == 1) {
                        TimeStamp stamp = pm.createAndSetTimeStamp();
                        stamp.setWhen(times[0]);
                    } else {
                        TimeSpan span = pm.createAndSetTimeSpan();
                        span.setBegin(times[0]);
                        span.setEnd(times[1]);
                    }
                }
            } catch (IOException e) {
                throw new ServiceException(
                        "Failed to apply KML time template to the current feature", e);
            }

            return pm;
        }

        /**
         * Executes the template against the feature.
         *
         * <p>This method returns:
         *
         * <ul>
         *   <li><code>{"01/01/07"}</code>: timestamp as 1 element array
         *   <li><code>{"01/01/07","01/12/07"}</code>: timespan as 2 element array
         *   <li><code>{null,"01/12/07"}</code>: open ended (start) timespan as 2 element array
         *   <li><code>{"01/12/07",null}</code>: open ended (end) timespan as 2 element array
         *   <li><code>{}</code>: no timestamp information as empty array
         * </ul>
         *
         * @param feature The feature to execute against.
         */
        public String[] execute(FeatureTemplate delegate, SimpleFeature feature)
                throws IOException {
            String output = delegate.template(feature, "time.ftl", FeatureTemplate.class);

            if (output != null) {
                output = output.trim();
            }

            // case of nothing specified
            if (output == null || "".equals(output)) {
                return new String[] {};
            }

            // JD: split() returns a single value when the delimiter is at the
            // end... but two when at the start do another check
            String[] timespan = output.split("\\|\\|");
            if (output.endsWith("||")) {
                timespan = new String[] {timespan[0], null};
            }

            if (timespan.length > 2) {
                String msg = "Incorrect time syntax. Should be: <date>||<date>";
                throw new IllegalArgumentException(msg);
            }

            if (timespan.length > 1) {
                // case of open ended timespan
                if (timespan[0] == null || "".equals(timespan[0].trim())) {
                    timespan[0] = null;
                }
                if (timespan[1] == null || "".equals(timespan[1].trim())) {
                    timespan[1] = null;
                }
            }

            // re-encode the times in the proper format
            for (int i = 0; i < timespan.length; i++) {
                if (timespan[i] != null) {
                    Date d = parseDateTime(timespan[i]);
                    timespan[i] = encodeDateTime(d);
                }
            }

            return timespan;
        }

        protected String encodeDateTime(Date date) {
            if (date != null) {
                Calendar c = Calendar.getInstance();
                c.setTime(date);
                return new XSDateTimeBinding().encode(c, null);
            } else {
                return null;
            }
        }

        /** Encodes a date as an xs:dateTime. */
        protected Date parseDateTime(String date) {

            // first try as date time
            Date d = parseDate(dtformats, date);
            if (d == null) {
                // then try as date
                d = parseDate(dformats, date);
            }
            if (d == null) {
                // try as time
                d = parseDate(tformats, date);
            }

            if (d == null) {
                // last ditch effort, try to parse as xml dates
                try {
                    // try as xml date time
                    d = DateUtil.deserializeDateTime(date);
                } catch (Exception e1) {
                    try {
                        // try as xml date
                        d = DateUtil.deserializeDate(date);
                    } catch (Exception e2) {
                    }
                }
            }

            if (d != null) {
                return d;
            }

            LOGGER.warning("Could not parse date: " + date);
            return null;
        }

        /** Parses a date as a string into a well-known format. */
        protected Date parseDate(List formats, String date) {
            for (Iterator f = formats.iterator(); f.hasNext(); ) {
                SimpleDateFormat format = (SimpleDateFormat) f.next();
                Date d = null;
                try {
                    d = format.parse(date);
                } catch (ParseException e) {
                    // fine, we have many templates to try against
                    // e.printStackTrace();
                }

                if (d != null) {
                    return d;
                }
            }

            return null;
        }
    }
}

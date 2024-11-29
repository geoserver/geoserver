/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import java.text.ParseException;
import java.util.List;
import org.geoserver.ows.kvp.TimeParser;
import org.geoserver.platform.ServiceException;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Converts a string formatted according to the OGC APIs "datetime" parameter into a list of
 * objects, which can be either a DateRange or a Date.
 *
 * <p>The OGC specification has the following ABNF: <br>
 * <br>
 * <code>
 * interval-closed     = date-time "/" date-time<br>
 * interval-open-start = [".."] "/" date-time<br>
 * interval-open-end   = date-time "/" [".."]<br>
 * interval            = interval-closed / interval-open-start / interval-open-end<br>
 * datetime            = date-time / interval<br>
 * </code>
 */
@Component
public class DateTimeConverter implements Converter<String, DateTimeList> {

    TimeParser parser = new TimeParser();

    @Override
    public DateTimeList convert(String s) {
        // TODO: extend the TimeParser to accept ".." for open ended itervals
        try {
            @SuppressWarnings("unchecked")
            List<Object> parsed = (List<Object>) parser.parse(s);
            return new DateTimeList(parsed);
        } catch (ParseException e) {
            throw new APIException(
                    ServiceException.INVALID_PARAMETER_VALUE,
                    "Un-recognized time specification: " + s,
                    HttpStatus.BAD_REQUEST,
                    e);
        }
    }
}

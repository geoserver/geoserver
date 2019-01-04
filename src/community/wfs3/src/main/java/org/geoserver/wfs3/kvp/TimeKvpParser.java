package org.geoserver.wfs3.kvp;

import java.text.ParseException;
import java.util.Collection;
import org.geoserver.ows.KvpParser;
import org.geoserver.ows.kvp.TimeParser;
import org.geoserver.platform.ServiceException;
import org.geotools.util.Version;

/** WFS specific version of time parsing, turns a time spec into a single time or date range */
public class TimeKvpParser extends KvpParser {

    TimeParser parser = new TimeParser();

    /**
     * Creates the parser specifying the name of the key to latch to.
     *
     * @param key The key whose associated value to parse.
     */
    public TimeKvpParser() {
        super("time", Object.class);
        setVersion(new Version("3.0.0"));
        setService("WFS");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public Object parse(String value) throws ParseException {
        Collection times = parser.parse(value);
        if (times.isEmpty() || times.size() > 1) {
            throw new ServiceException(
                    "Invalid time specification, must be a single time, or a time range",
                    ServiceException.INVALID_PARAMETER_VALUE,
                    "time");
        }

        return times.iterator().next();
    }
}

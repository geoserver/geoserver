/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.kvp;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import org.geoserver.ows.KvpParser;
import org.geoserver.ows.util.CaseInsensitiveMap;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Parses the format options parameter which is of the form:
 *
 * <pre>FORMAT_OPTIONS=opt1:val1,val2;opt2:val1;opt3:...</pre>
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 */
public class FormatOptionsKvpParser extends KvpParser implements ApplicationContextAware {
    /** application context used to lookup KvpParsers */
    ApplicationContext applicationContext;

    public FormatOptionsKvpParser() {
        this("format_options");
    }

    /**
     * Builds a {@link FormatOptionsKvpParser} with a user specified key (for params that have the
     * syntax of format_options, but not the same name)
     */
    public FormatOptionsKvpParser(String key) {
        super(key, Map.class);
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public Object parse(String value) throws Exception {
        List parsers = GeoServerExtensions.extensions(KvpParser.class, applicationContext);
        Map formatOptions = new CaseInsensitiveMap(new TreeMap());

        List<String> kvps = KvpUtils.escapedTokens(value, ';');

        for (String kvp : kvps) {
            List<String> kv = KvpUtils.escapedTokens(kvp, ':', 2);
            String key = kv.get(0);
            String raw = kv.size() == 1 ? "true" : KvpUtils.unescape(kv.get(1));

            Object parsed = null;

            for (Iterator p = parsers.iterator(); p.hasNext(); ) {
                KvpParser parser = (KvpParser) p.next();
                if (key.equalsIgnoreCase(parser.getKey())) {
                    parsed = parser.parse(raw);
                    if (parsed != null) {

                        break;
                    }
                }
            }

            if (parsed == null) {
                if (LOGGER.isLoggable(Level.FINER))
                    LOGGER.finer(
                            "Could not find kvp parser for: '" + key + "'. Storing as raw string.");
                parsed = raw;
            }

            formatOptions.put(key, parsed);
        }

        return formatOptions;
    }
}

/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.kvp;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;

import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.KvpParser;
import org.geoserver.ows.util.CaseInsensitiveMap;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;


/**
 * Parses the format options parameter which is of the form:
 * <pre>FORMAT_OPTIONS=opt1:val1,val2;opt2:val1;opt3:...</pre>
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 *
 */
public class FormatOptionsKvpParser extends KvpParser implements ApplicationContextAware {
    /**
     * application context used to lookup KvpParsers
     */
    ApplicationContext applicationContext;
    
    public FormatOptionsKvpParser() {
        this("format_options");
    }
 
    /**
     * Builds a {@link FormatOptionsKvpParser} with a user specified key (for params that have the
     * syntax of format_options, but not the same name)
     * 
     * @param key
     */
    public FormatOptionsKvpParser(String key) {
        super(key, Map.class);
    }

    public void setApplicationContext(ApplicationContext applicationContext)
        throws BeansException {
        this.applicationContext = applicationContext;
    }

    public Object parse(String value) throws Exception {
        Map formatOptions = new CaseInsensitiveMap(new HashMap());

        List<String> kvps = KvpUtils.escapedTokens(value, ';');
        
        
        // build map of unparsed key - value pairs
        for (String kvp : kvps) {
            List<String> kv = KvpUtils.escapedTokens(kvp, ':', 2);
            String key = kv.get(0);
            String raw = kv.size() == 1 ? "true" : KvpUtils.unescape(kv.get(1));
            formatOptions.put(key, raw);
        }
        // we use service, request, version info for this parser (if available)
        // to restrict the parsers list used for options parsing (see GEOS-6555)
        List<Throwable> errors = KvpUtils
                .parse(formatOptions, getService(), getRequest(),
                        getVersion() == null ? null : getVersion().toString());
        
        if(errors != null && errors.size() > 0) {
            throw new Exception(errors.get(0));
        }
        
        return formatOptions;
    }

}

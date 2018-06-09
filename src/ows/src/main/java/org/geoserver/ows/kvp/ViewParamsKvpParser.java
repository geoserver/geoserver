/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.kvp;

import java.util.ArrayList;
import java.util.List;
import org.geoserver.ows.KvpParser;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Parses view parameters which are of the form:
 *
 * <pre>VIEWPARAMS=opt1:val1,val2;opt2:val1;opt3:...[,opt1:val1,val2;opt2:val1;opt3:...]</pre>
 *
 * @see FormatOptionsKvpParser
 */
public class ViewParamsKvpParser extends KvpParser implements ApplicationContextAware {
    /** application context used to lookup KvpParsers */
    ApplicationContext applicationContext;

    public ViewParamsKvpParser() {
        super("viewparams", List.class);
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public Object parse(String value) throws Exception {
        List ret = new ArrayList();
        List parsers = GeoServerExtensions.extensions(KvpParser.class, applicationContext);
        KvpParser formatOptionsParser = null;
        for (Object o : parsers) {
            KvpParser parser = (KvpParser) o;
            if (parser.getKey().equalsIgnoreCase("format_options")) {
                formatOptionsParser = parser;
                break;
            }
        }
        if (formatOptionsParser == null) {
            throw new IllegalStateException("Missing format options parser.");
        }
        for (String kvp : KvpUtils.escapedTokens(value, ',')) {
            ret.add(formatOptionsParser.parse(kvp));
        }

        return ret;
    }
}

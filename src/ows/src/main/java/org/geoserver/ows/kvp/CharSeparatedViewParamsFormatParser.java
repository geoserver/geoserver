/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.kvp;

import java.util.ArrayList;
import java.util.List;
import org.geoserver.ows.KvpParser;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.platform.GeoServerExtensions;

/**
 * This is the default view params format parser. Parses view parameters which are of the form:
 *
 * <pre>VIEWPARAMS=opt1:val1,val2;opt2:val1;opt3:...[,opt1:val1,val2;opt2:val1;opt3:...]</pre>
 *
 * @see FormatOptionsKvpParser
 */
public class CharSeparatedViewParamsFormatParser implements ViewParamsFormatParser {

    public static final String CHAR_SEPARATED_IDENTIFIER = "CharSeparated";

    @Override
    public String getIdentifier() {
        return CHAR_SEPARATED_IDENTIFIER;
    }

    @Override
    public List<Object> parse(String value) throws Exception {
        List<Object> ret = new ArrayList<>();
        List<KvpParser> parsers = GeoServerExtensions.extensions(KvpParser.class);
        KvpParser formatOptionsParser = null;
        for (KvpParser parser : parsers) {
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

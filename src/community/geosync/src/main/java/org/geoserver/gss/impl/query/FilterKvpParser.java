package org.geoserver.gss.impl.query;

import java.util.List;

import org.geoserver.ows.KvpParser;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.kvp.Filter_1_1_0_KvpParser;
import org.opengis.filter.Filter;

public class FilterKvpParser extends KvpParser {

    private Filter_1_1_0_KvpParser filterListParser;

    public FilterKvpParser() {
        super("FILTER", Filter.class);
        filterListParser = new Filter_1_1_0_KvpParser();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Filter parse(final String value) throws Exception {
        List<Filter> parsed = (List<Filter>) filterListParser.parse(value);
        if (parsed == null || parsed.size() == 0) {
            return null;
        }
        if (parsed.size() != 1) {
            throw new ServiceException("Parameter parsed to more than one filter",
                    "InvalidParameterValue", "FILTER");
        }
        return parsed.get(0);
    }

}

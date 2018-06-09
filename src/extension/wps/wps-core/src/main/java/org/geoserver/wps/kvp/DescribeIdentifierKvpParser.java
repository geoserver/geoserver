/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps.kvp;

import java.util.ArrayList;
import java.util.List;
import net.opengis.ows11.CodeType;
import net.opengis.ows11.Ows11Factory;
import net.opengis.ows11.impl.Ows11FactoryImpl;
import org.geoserver.ows.KvpParser;
import org.geoserver.ows.util.KvpUtils;

/**
 * Identifier attribute KVP parser
 *
 * @author Lucas Reed, Refractions Research Inc
 */
public class DescribeIdentifierKvpParser extends KvpParser {
    public DescribeIdentifierKvpParser() {
        super("identifier", CodeType.class);

        this.setService("wps");
        this.setRequest("DescribeProcess");
    }

    @SuppressWarnings("unchecked")
    public Object parse(String value) throws Exception {

        List<CodeType> values = new ArrayList<CodeType>();

        Ows11Factory owsFactory = new Ows11FactoryImpl();

        for (String str : (List<String>) KvpUtils.readFlat(value)) {
            CodeType codeType = owsFactory.createCodeType();
            codeType.setValue(str);
            values.add(codeType);
        }

        return values;
    }
}

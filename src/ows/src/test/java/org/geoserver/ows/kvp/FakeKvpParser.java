/* Copyright (c) 2014 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.kvp;

import java.util.Arrays;

import org.geoserver.ows.KvpParser;

/**
 * Fake KvpParser implementation, used to test parsers filtering
 * by service - request - version.
 * 
 * @author mauro.bartolomeoli at geo-solutions.it
 *
 */
public class FakeKvpParser extends KvpParser {

    public FakeKvpParser(String key, Class binding) {
        super(key, binding);
    }

    @Override
    public Object parse(String value) throws Exception {
        return Arrays.asList(value);
    }
}

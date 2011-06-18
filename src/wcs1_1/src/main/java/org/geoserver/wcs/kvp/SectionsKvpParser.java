/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.kvp;

import java.util.List;

import net.opengis.ows11.Ows11Factory;
import net.opengis.ows11.SectionsType;

import org.geoserver.ows.KvpParser;
import org.geoserver.ows.util.KvpUtils;

/**
 * Parses the "sections" GetCapabilities kvp argument
 * @author Andrea Aime - TOPP
 */
public class SectionsKvpParser extends KvpParser {

    public SectionsKvpParser() {
        super("sections", SectionsType.class);
        
    }

    @Override
    public Object parse(String value) throws Exception {
        List<String> sectionNames = KvpUtils.readFlat(value);
        SectionsType sections = Ows11Factory.eINSTANCE.createSectionsType();
        sections.getSection().addAll(sectionNames);
        return sections;
    }

}

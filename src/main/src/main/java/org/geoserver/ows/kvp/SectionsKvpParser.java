/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.kvp;

import java.util.Collection;
import org.eclipse.emf.ecore.EObject;
import org.geoserver.ows.KvpParser;
import org.geoserver.ows.util.KvpUtils;
import org.geotools.xsd.EMFUtils;

/**
 * Parses the "sections" GetCapabilities kvp argument
 *
 * @author Andrea Aime - TOPP
 */
public abstract class SectionsKvpParser extends KvpParser {

    public SectionsKvpParser(Class target) {
        super("sections", target);
        setService("wcs");
    }

    public Object parse(String value) throws Exception {
        EObject sections = createObject();
        ((Collection) EMFUtils.get(sections, "section"))
                .addAll(KvpUtils.readFlat(value, KvpUtils.INNER_DELIMETER));
        return sections;
    }

    protected abstract EObject createObject();
}

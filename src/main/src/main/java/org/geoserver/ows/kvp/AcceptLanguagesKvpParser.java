/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.kvp;

import java.util.Collection;
import java.util.List;
import org.eclipse.emf.ecore.EObject;
import org.geoserver.ows.KvpParser;
import org.geoserver.ows.util.KvpUtils;
import org.geotools.xsd.EMFUtils;

/**
 * Parses a kvp of the form "acceptLanguages=lang1 lang2 langN" into an instance of OWS
 * AcceptVersionsType. This class is version independent, the subclasses bind it to a specific OWS
 * version.
 */
public abstract class AcceptLanguagesKvpParser extends KvpParser {

    public AcceptLanguagesKvpParser(Class<?> clazz) {
        super("acceptLanguages", clazz);
    }

    @Override
    public Object parse(String value) throws Exception {
        EObject acceptLanguages = createObject();
        List<String> values = KvpUtils.readFlat(value, KvpUtils.SPACE_DELIMETER);
        if (!values.isEmpty() && values.size() == 1) {
            values = KvpUtils.readFlat(value, KvpUtils.INNER_DELIMETER);
        }
        for (String v : values) {
            @SuppressWarnings("unchecked")
            Collection<String> of = (Collection<String>) EMFUtils.get(acceptLanguages, "language");
            of.add(v);
        }
        return acceptLanguages;
    }

    protected abstract EObject createObject();
}

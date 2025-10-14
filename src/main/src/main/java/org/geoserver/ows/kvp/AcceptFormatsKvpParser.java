/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
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
 * Parses a kvp of the form {@code acceptFormats=format1,format2,...,formatN} into an instance of OWS AcceptFormatsType.
 * This class is version independent, the subclasses bind it to a specific OWS version.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public abstract class AcceptFormatsKvpParser extends KvpParser {

    public AcceptFormatsKvpParser(Class<?> clazz) {
        super("acceptFormats", clazz);
    }

    @Override
    public Object parse(String value) throws Exception {
        List<String> values = KvpUtils.readFlat(value);

        EObject acceptFormats = createObject();

        for (String v : values) {
            @SuppressWarnings("unchecked")
            Collection<String> of = (Collection<String>) EMFUtils.get(acceptFormats, "outputFormat");
            of.add(v);
        }

        return acceptFormats;
    }

    /** Creates the AcceptsFormatType */
    protected abstract EObject createObject();
}

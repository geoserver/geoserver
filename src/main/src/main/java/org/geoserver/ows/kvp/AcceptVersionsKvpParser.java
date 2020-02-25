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
 * Parses a kvp of the form "acceptVersions=version1,version2,...,versionN" into an instance of OWS
 * AcceptVersionsType. This class is version independent, the subclasses bind it to a specific OWS
 * version.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public abstract class AcceptVersionsKvpParser extends KvpParser {

    public AcceptVersionsKvpParser(Class clazz) {
        super("acceptversions", clazz);
    }

    public Object parse(String value) throws Exception {
        EObject acceptVersions = createObject();
        ((Collection) EMFUtils.get(acceptVersions, "version"))
                .addAll(KvpUtils.readFlat(value, KvpUtils.INNER_DELIMETER));
        return acceptVersions;
    }

    protected abstract EObject createObject();
}

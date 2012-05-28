/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfs.kvp;

import java.util.Collection;

import net.opengis.ows10.AcceptVersionsType;
import net.opengis.ows10.Ows10Factory;

import org.eclipse.emf.ecore.EObject;
import org.geoserver.ows.KvpParser;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.wfs.WFSInfo;
import org.geotools.xml.EMFUtils;


/**
 * Parses a kvp of the form "acceptVersions=version1,version2,...,versionN" into
 * an instance of {@link net.opengis.ows.v1_0_0.AcceptVersionsType}.
 *
 * @author Justin Deoliveira, The Open Planning Project
 *
 */
public class AcceptVersionsKvpParser extends KvpParser {
    public AcceptVersionsKvpParser() {
        this(AcceptVersionsType.class);
    }

    public AcceptVersionsKvpParser(Class clazz) {
        super("acceptversions", clazz);
        setVersion(WFSInfo.Version.V_11.getVersion());
    }

    public Object parse(String value) throws Exception {
        EObject acceptVersions = createObject();
        ((Collection)EMFUtils.get(acceptVersions, "version")).addAll(KvpUtils.readFlat(value, KvpUtils.INNER_DELIMETER));
        return acceptVersions;
    }

    protected EObject createObject() {
        return Ows10Factory.eINSTANCE.createAcceptVersionsType();
    }
}

/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfs.xml;

import java.util.HashSet;
import java.util.Set;

import org.geotools.feature.NameImpl;
import org.geotools.feature.type.ProfileImpl;
import org.geotools.gml2.GML;
import org.geotools.gml2.GMLSchema;


public class GML2Profile extends TypeMappingProfile {
    static Set profiles = new HashSet();

    static {
        //profile.add(new Name(GML.NAMESPACE, GML.POINTTYPE.getLocalPart()));
        Set profile = new HashSet();
        profile.add(new NameImpl(GML.NAMESPACE, GML.PointPropertyType.getLocalPart()));
        //profile.add(new Name(GML.NAMESPACE, GML.MULTIPOINTTYPE.getLocalPart()));
        profile.add(new NameImpl(GML.NAMESPACE, GML.MultiPointPropertyType.getLocalPart()));

        //profile.add(new Name(GML.NAMESPACE, GML.LINESTRINGTYPE.getLocalPart()));
        profile.add(new NameImpl(GML.NAMESPACE, GML.LineStringPropertyType.getLocalPart()));
        //profile.add(new Name(GML.NAMESPACE, GML.MULTILINESTRINGTYPE.getLocalPart()));
        profile.add(new NameImpl(GML.NAMESPACE, GML.MultiLineStringPropertyType.getLocalPart()));

        //profile.add(new Name(GML.NAMESPACE, GML.POLYGONTYPE.getLocalPart()));
        profile.add(new NameImpl(GML.NAMESPACE, GML.PolygonPropertyType.getLocalPart()));
        //profile.add(new Name(GML.NAMESPACE, GML.MULTIPOLYGONTYPE.getLocalPart()));
        profile.add(new NameImpl(GML.NAMESPACE, GML.MultiPolygonPropertyType.getLocalPart()));

        profile.add(new NameImpl(GML.NAMESPACE, GML.GeometryPropertyType.getLocalPart()));
        profiles.add( new ProfileImpl( new GMLSchema(), profile ) );
    }

    public GML2Profile() {
        super(profiles);
    }
}

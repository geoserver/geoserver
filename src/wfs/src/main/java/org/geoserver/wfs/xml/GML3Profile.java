/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import org.geotools.feature.NameImpl;
import org.geotools.feature.type.ProfileImpl;
import org.geotools.gml3.GML;
import org.geotools.gml3.GMLSchema;

public class GML3Profile extends TypeMappingProfile {
    static Set profiles = new HashSet();

    static {
        // set with guaranteed iteration order, so that we can put deprecated elements only
        // after the ones that replaced them
        Set profile = new LinkedHashSet();

        // basic
        profile.add(new NameImpl(GML.NAMESPACE, GML.MeasureType.getLocalPart()));

        // geomtetries
        // profile.add( new NameImpl(  GML.NAMESPACE, GML.PointType.getLocalPart() ) );
        profile.add(new NameImpl(GML.NAMESPACE, GML.PointPropertyType.getLocalPart()));
        // profile.add( new NameImpl(  GML.NAMESPACE, GML.MultiPointType.getLocalPart() ) );
        profile.add(new NameImpl(GML.NAMESPACE, GML.MultiPointPropertyType.getLocalPart()));

        // profile.add( new NameImpl(  GML.NAMESPACE, GML.LineStringType.getLocalPart() ) );
        profile.add(new NameImpl(GML.NAMESPACE, GML.LineStringPropertyType.getLocalPart()));
        // profile.add( new NameImpl(  GML.NAMESPACE, GML.MultiLineStringType.getLocalPart() ) );
        profile.add(new NameImpl(GML.NAMESPACE, GML.MultiLineStringPropertyType.getLocalPart()));

        // profile.add( new NameImpl(  GML.NAMESPACE, GML.CurveType.getLocalPart() ) );
        profile.add(new NameImpl(GML.NAMESPACE, GML.CurvePropertyType.getLocalPart()));
        // profile.add( new NameImpl(  GML.NAMESPACE, GML.MultiCurveType.getLocalPart() ) );
        profile.add(new NameImpl(GML.NAMESPACE, GML.MultiCurvePropertyType.getLocalPart()));

        profile.add(new NameImpl(GML.NAMESPACE, GML.SurfacePropertyType.getLocalPart()));
        // profile.add( new NameImpl(  GML.NAMESPACE, GML.MultiSurfaceType.getLocalPart() ) );
        profile.add(new NameImpl(GML.NAMESPACE, GML.MultiSurfacePropertyType.getLocalPart()));

        // register polygon and multipolygon only after surface, the iteration order
        // will make sure surface is found before in any encoding attempt, this way we
        // are still able to handle polygons, but we don't use them by default
        // profile.add( new NameImpl(  GML.NAMESPACE, GML.PolygonType.getLocalPart() ) );
        profile.add(new NameImpl(GML.NAMESPACE, GML.PolygonPropertyType.getLocalPart()));
        // profile.add( new NameImpl(  GML.NAMESPACE, GML.MultiPolygonType.getLocalPart() ) );
        profile.add(new NameImpl(GML.NAMESPACE, GML.MultiPolygonPropertyType.getLocalPart()));

        // profile.add( new NameImpl(  GML.NAMESPACE, GML.AbstractGeometryType.getLocalPart() ) );
        profile.add(new NameImpl(GML.NAMESPACE, GML.GeometryPropertyType.getLocalPart()));
        profile.add(new NameImpl(GML.NAMESPACE, GML.MultiGeometryPropertyType.getLocalPart()));

        profiles.add(new ProfileImpl(new GMLSchema(), profile));
    }

    public GML3Profile() {
        super(profiles);
    }
}

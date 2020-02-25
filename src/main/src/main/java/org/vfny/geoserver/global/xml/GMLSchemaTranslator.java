/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.global.xml;

import java.util.HashSet;
import java.util.Set;
import org.geotools.feature.FeatureCollection;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

/**
 * XMLSchemaTranslator purpose.
 *
 * <p>This instance of the NameSpaceTranslator should be used with http://www.opengis.net/gml
 * namespace.
 *
 * <p>Instances of this object should always be retrieved through the NameSpaceTranslatorFactory.
 *
 * @see NameSpaceTranslatorFactory
 * @author dzwiers, Refractions Research, Inc.
 * @author $Author: dmzwiers $ (last modification)
 * @version $Id$
 */
public class GMLSchemaTranslator extends NameSpaceTranslator {
    private HashSet elements;

    /**
     * XMLSchemaTranslator constructor.
     *
     * <p>Description
     */
    public GMLSchemaTranslator(String prefix) {
        super(prefix);
        elements = new HashSet();
        /*elements.add(new PointElement(prefix));
        elements.add(new LineStringElement(prefix));
        elements.add(new LinearRingElement(prefix));
        elements.add(new BoxElement(prefix));
        elements.add(new PolygonElement(prefix));
        elements.add(new GeometryCollectionElement(prefix));
        elements.add(new MultiPointElement(prefix));
        elements.add(new MultiLineStringElement(prefix));
        elements.add(new MultiPolygonElement(prefix));
        elements.add(new CoordElement(prefix));
        elements.add(new CoordinatesElement(prefix));*/
        elements.add(new PointPropertyElement(prefix));
        elements.add(new PolygonPropertyElement(prefix));
        elements.add(new LineStringPropertyElement(prefix));
        elements.add(new MultiPointPropertyElement(prefix));
        elements.add(new MultiLineStringPropertyElement(prefix));
        elements.add(new MultiPolygonPropertyElement(prefix));
        elements.add(new MultiGeometryPropertyElement(prefix));
        elements.add(new NullElement(prefix));
        elements.add(new AbstractFeatureElement(prefix));
        elements.add(new AbstractFeatureCollectionBaseElement(prefix));
        elements.add(new AbstractFeatureCollectionElement(prefix));
        elements.add(new GeometryPropertyElement(prefix));

        /*elements.add(new GeometryPropertyElement(prefix));
        elements.add(new FeatureAssociationElement(prefix));
        elements.add(new BoundingShapeElement(prefix));
        elements.add(new AbstractGeometryElement(prefix));
        elements.add(new AbstractGeometryCollectionBaseElement(prefix));
        elements.add(new AssociationAttributeGroupElement(prefix));
        elements.add(new GeometryAssociationElement(prefix));
        elements.add(new PointMemberElement(prefix));
        elements.add(new LineStringMemberElement(prefix));
        elements.add(new PolygonMemberElement(prefix));
        elements.add(new LinearRingMemberElement(prefix));*/
    }

    /**
     * Implementation of getElements.
     *
     * @see org.vfny.geoserver.global.xml.NameSpaceTranslator#getElements()
     */
    public Set getElements() {
        return elements;
    }

    /**
     * Implementation of getNameSpace.
     *
     * @see org.vfny.geoserver.global.xml.NameSpaceTranslator#getNameSpace()
     */
    public String getNameSpace() {
        return "http://www.opengis.net/gml";
    }
}

class AbstractFeatureElement extends NameSpaceElement {
    public AbstractFeatureElement(String prefix) {
        super(prefix);
    }

    public String getTypeDefName() {
        return "AbstractFeatureType";
    }

    public String getTypeRefName() {
        return null;
    }

    public String getQualifiedTypeDefName() {
        return prefix + ":AbstractFeatureType";
    }

    public String getQualifiedTypeRefName() {
        return null;
    }

    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":AbstractFeatureType";
        }

        if (this.prefix != null) {
            return this.prefix + ":AbstractFeatureType";
        }

        return null;
    }

    public String getQualifiedTypeRefName(String prefix) {
        return null;
    }

    public Class getJavaClass() {
        return Object.class;
    }

    public boolean isAbstract() {
        return true;
    }
}

class AbstractFeatureCollectionBaseElement extends NameSpaceElement {
    public AbstractFeatureCollectionBaseElement(String prefix) {
        super(prefix);
    }

    public String getTypeDefName() {
        return "AbstractFeatureCollectionBaseType";
    }

    public String getTypeRefName() {
        return null;
    }

    public String getQualifiedTypeDefName() {
        return prefix + ":AbstractFeatureCollectionBaseType";
    }

    public String getQualifiedTypeRefName() {
        return null;
    }

    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":AbstractFeatureCollectionBaseType";
        }

        if (this.prefix != null) {
            return this.prefix + ":AbstractFeatureCollectionBaseType";
        }

        return null;
    }

    public String getQualifiedTypeRefName(String prefix) {
        return null;
    }

    public Class getJavaClass() {
        return FeatureCollection.class;
    }

    public boolean isAbstract() {
        return true;
    }
}

class AbstractFeatureCollectionElement extends NameSpaceElement {
    public AbstractFeatureCollectionElement(String prefix) {
        super(prefix);
    }

    public String getTypeDefName() {
        return "AbstractFeatureCollectionType";
    }

    public String getTypeRefName() {
        return null;
    }

    public String getQualifiedTypeDefName() {
        return prefix + ":AbstractFeatureCollectionType";
    }

    public String getQualifiedTypeRefName() {
        return null;
    }

    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":AbstractFeatureCollectionType";
        }

        if (this.prefix != null) {
            return this.prefix + ":AbstractFeatureCollectionType";
        }

        return null;
    }

    public String getQualifiedTypeRefName(String prefix) {
        return null;
    }

    public Class getJavaClass() {
        return FeatureCollection.class;
    }

    public boolean isAbstract() {
        return true;
    }
}

// I don't think this big chunk of junk is useful, as I don't think you're
// going to define a schema with it, unless you do something more complicated
// then what we are doing.  We only want the GeometryPropertyType stuff, which
// allows us to name the element as we will.  The rest is only useful if you
// are doing more complex stuff than we allow.  But I will leave this in
// and commented out, in case I am wrong and this junk does have a use.

// I think perhaps it may just need better objects, that can actually use
// these?  Since the geometry objects they are given can't use them.
class GeometryPropertyElement extends NameSpaceElement {
    public GeometryPropertyElement(String prefix) {
        super(prefix);
    }

    public String getTypeDefName() {
        return "GeometryPropertyType";
    }

    public String getTypeRefName() {
        return null;
    }

    public String getQualifiedTypeDefName() {
        return prefix + ":GeometryPropertyType";
    }

    public String getQualifiedTypeRefName() {
        return null;
    }

    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":GeometryPropertyType";
        }

        if (this.prefix != null) {
            return this.prefix + ":GeometryPropertyType";
        }

        return null;
    }

    public String getQualifiedTypeRefName(String prefix) {
        return null;
    }

    public Class getJavaClass() {
        return Geometry.class;
    }

    public boolean isAbstract() {
        return true;
    }
}

/*
class FeatureAssociationElement extends NameSpaceElement{
        public FeatureAssociationElement(String prefix){super(prefix);}
        public String getTypeDefName(){return "FeatureAssociationType";}
        public String getTypeRefName(){return null;}
        public String getQualifiedTypeDefName(){return prefix+":FeatureAssociationType";}
        public String getQualifiedTypeRefName(){return null;}
        public String getQualifiedTypeDefName(String prefix){
                if(prefix!=null)
                        return prefix+":FeatureAssociationType";
                if(this.prefix!=null)
                        return this.prefix+":FeatureAssociationType";
                return null;
        }
        public String getQualifiedTypeRefName(String prefix){return null;}
        public Class getJavaClass(){return Feature.class;}
        public boolean isAbstract(){return true;}
}

class BoundingShapeElement extends NameSpaceElement{
        public BoundingShapeElement(String prefix){super(prefix);}
        public String getTypeDefName(){return "BoundingShapeType";}
        public String getTypeRefName(){return null;}
        public String getQualifiedTypeDefName(){return prefix+":BoundingShapeType";}
        public String getQualifiedTypeRefName(){return null;}
        public String getQualifiedTypeDefName(String prefix){
                if(prefix!=null)
                        return prefix+":BoundingShapeType";
                if(this.prefix!=null)
                        return this.prefix+":BoundingShapeType";
                return null;
        }
        public String getQualifiedTypeRefName(String prefix){return null;}
        public Class getJavaClass(){return Filter.class;}
        public boolean isAbstract(){return false;}
}

class AbstractGeometryElement extends NameSpaceElement{
        public AbstractGeometryElement(String prefix){super(prefix);}
        public String getTypeDefName(){return "AbstractGeometryType";}
        public String getTypeRefName(){return null;}
        public String getQualifiedTypeDefName(){return prefix+":AbstractGeometryType";}
        public String getQualifiedTypeRefName(){return null;}
        public String getQualifiedTypeDefName(String prefix){
                if(prefix!=null)
                        return prefix+":AbstractGeometryType";
                if(this.prefix!=null)
                        return this.prefix+":AbstractGeometryType";
                return null;
        }
        public String getQualifiedTypeRefName(String prefix){return null;}
        public Class getJavaClass(){return Geometry.class;}
        public boolean isAbstract(){return true;}
}

class AbstractGeometryCollectionBaseElement extends NameSpaceElement{
        public AbstractGeometryCollectionBaseElement(String prefix){super(prefix);}
        public String getTypeDefName(){return "AbstractGeometryCollectionBase";}
        public String getTypeRefName(){return null;}
        public String getQualifiedTypeDefName(){return prefix+":AbstractGeometryCollectionBase";}
        public String getQualifiedTypeRefName(){return null;}
        public String getQualifiedTypeDefName(String prefix){
                if(prefix!=null)
                        return prefix+":AbstractGeometryCollectionBase";
                if(this.prefix!=null)
                        return this.prefix+":AbstractGeometryCollectionBase";
                return null;
        }
        public String getQualifiedTypeRefName(String prefix){return null;}
        public Class getJavaClass(){return GeometryCollection.class;}
        public boolean isAbstract(){return true;}
}

class AssociationAttributeGroupElement extends NameSpaceElement{
        public AssociationAttributeGroupElement(String prefix){super(prefix);}
        public String getTypeDefName(){return "AssociationAttributeGroup";}
        public String getTypeRefName(){return null;}
        public String getQualifiedTypeDefName(){return prefix+":AssociationAttributeGroup";}
        public String getQualifiedTypeRefName(){return null;}
        public String getQualifiedTypeDefName(String prefix){
                if(prefix!=null)
                        return prefix+":AssociationAttributeGroup";
                if(this.prefix!=null)
                        return this.prefix+":AssociationAttributeGroup";
                return null;
        }
        public String getQualifiedTypeRefName(String prefix){return null;}
        public Class getJavaClass(){return Collection.class;}
        public boolean isAbstract(){return true;}
}

class GeometryAssociationElement extends NameSpaceElement{
        public GeometryAssociationElement(String prefix){super(prefix);}
        public String getTypeDefName(){return "GeometryAssociationType";}
        public String getTypeRefName(){return null;}
        public String getQualifiedTypeDefName(){return prefix+":GeometryAssociationType";}
        public String getQualifiedTypeRefName(){return null;}
        public String getQualifiedTypeDefName(String prefix){
                if(prefix!=null)
                        return prefix+":GeometryAssociationType";
                if(this.prefix!=null)
                        return this.prefix+":GeometryAssociationType";
                return null;
        }
        public String getQualifiedTypeRefName(String prefix){return null;}
        public Class getJavaClass(){return Object.class;}
        public boolean isAbstract(){return true;}
}

class PointMemberElement extends NameSpaceElement{
        public PointMemberElement(String prefix){super(prefix);}
        public String getTypeDefName(){return "PointMemberType";}
        public String getTypeRefName(){return null;}
        public String getQualifiedTypeDefName(){return prefix+":PointMemberType";}
        public String getQualifiedTypeRefName(){return null;}
        public String getQualifiedTypeDefName(String prefix){
                if(prefix!=null)
                        return prefix+":PointMemberType";
                if(this.prefix!=null)
                        return this.prefix+":PointMemberType";
                return null;
        }
        public String getQualifiedTypeRefName(String prefix){return null;}
        public Class getJavaClass(){return Object.class;}
        public boolean isAbstract(){return false;}
}

class LineStringMemberElement extends NameSpaceElement{
        public LineStringMemberElement(String prefix){super(prefix);}
        public String getTypeDefName(){return "LineStringMemberType";}
        public String getTypeRefName(){return null;}
        public String getQualifiedTypeDefName(){return prefix+":LineStringMemberType";}
        public String getQualifiedTypeRefName(){return null;}
        public String getQualifiedTypeDefName(String prefix){
                if(prefix!=null)
                        return prefix+":LineStringMemberType";
                if(this.prefix!=null)
                        return this.prefix+":LineStringMemberType";
                return null;
        }
        public String getQualifiedTypeRefName(String prefix){return null;}
        public Class getJavaClass(){return Object.class;}
        public boolean isAbstract(){return false;}
}

class PolygonMemberElement extends NameSpaceElement{
        public PolygonMemberElement(String prefix){super(prefix);}
        public String getTypeDefName(){return "PolygonMemberType";}
        public String getTypeRefName(){return null;}
        public String getQualifiedTypeDefName(){return prefix+":PolygonMemberType";}
        public String getQualifiedTypeRefName(){return null;}
        public String getQualifiedTypeDefName(String prefix){
                if(prefix!=null)
                        return prefix+":PolygonMemberType";
                if(this.prefix!=null)
                        return this.prefix+":PolygonMemberType";
                return null;
        }
        public String getQualifiedTypeRefName(String prefix){return null;}
        public Class getJavaClass(){return Object.class;}
        public boolean isAbstract(){return false;}
}

class LinearRingMemberElement extends NameSpaceElement{
        public LinearRingMemberElement(String prefix){super(prefix);}
        public String getTypeDefName(){return "LinearRingMemberType";}
        public String getTypeRefName(){return null;}
        public String getQualifiedTypeDefName(){return prefix+":LinearRingMemberType";}
        public String getQualifiedTypeRefName(){return null;}
        public String getQualifiedTypeDefName(String prefix){
                if(prefix!=null)
                        return prefix+":LinearRingMemberType";
                if(this.prefix!=null)
                        return this.prefix+":LinearRingMemberType";
                return null;
        }
        public String getQualifiedTypeRefName(String prefix){return null;}
        public Class getJavaClass(){return Object.class;}
        public boolean isAbstract(){return false;}
}

class PointElement extends NameSpaceElement{
        public PointElement(String prefix){super(prefix);}
        public String getTypeDefName(){return "PointType";}
        public String getTypeRefName(){return "point";}
        public String getQualifiedTypeDefName(){return prefix+":PointType";}
        public String getQualifiedTypeRefName(){return prefix+":point";}
        public String getQualifiedTypeDefName(String prefix){
                if(prefix!=null)
                        return prefix+":PointType";
                if(this.prefix!=null)
                        return this.prefix+":PointType";
                return null;
        }
        public String getQualifiedTypeRefName(String prefix){
                if(prefix!=null)
                        return prefix+":point";
                if(this.prefix!=null)
                        return this.prefix+":point";
                return null;
        }
        public Class getJavaClass(){return Point.class;}
        public boolean isAbstract(){return false;}
}

class LineStringElement extends NameSpaceElement{
        public LineStringElement(String prefix){super(prefix);}
        public String getTypeDefName(){return "LineStringType";}
        public String getTypeRefName(){return "lineStringType";}
        public String getQualifiedTypeDefName(){return prefix+":LineStringType";}
        public String getQualifiedTypeRefName(){return prefix+":lineStringType";}
        public String getQualifiedTypeDefName(String prefix){
                if(prefix!=null)
                        return prefix+":LineStringType";
                if(this.prefix!=null)
                        return this.prefix+":LineStringType";
                return null;
        }
        public String getQualifiedTypeRefName(String prefix){
                if(prefix!=null)
                        return prefix+":lineStringType";
                if(this.prefix!=null)
                        return this.prefix+":lineStringType";
                return null;
        }
        public Class getJavaClass(){return LineString.class;}
        public boolean isAbstract(){return false;}
}

class LinearRingElement extends NameSpaceElement{
        public LinearRingElement(String prefix){super(prefix);}
        public String getTypeDefName(){return "LinearRingType";}
        public String getTypeRefName(){return "LinearRingType";}
        public String getQualifiedTypeDefName(){return prefix+":LinearRingType";}
        public String getQualifiedTypeRefName(){return prefix+":linearRingType";}
        public String getQualifiedTypeDefName(String prefix){
                if(prefix!=null)
                        return prefix+":LinearRingType";
                if(this.prefix!=null)
                        return this.prefix+":LinearRingType";
                return null;
        }
        public String getQualifiedTypeRefName(String prefix){
                if(prefix!=null)
                        return prefix+":linearRingType";
                if(this.prefix!=null)
                        return this.prefix+":linearRingType";
                return null;
        }
        public Class getJavaClass(){return LinearRing.class;}
        public boolean isAbstract(){return false;}
}

class BoxElement extends NameSpaceElement{
        public BoxElement(String prefix){super(prefix);}
        public String getTypeDefName(){return "BoxType";}
        public String getTypeRefName(){return "boxType";}
        public String getQualifiedTypeDefName(){return prefix+":BoxType";}
        public String getQualifiedTypeRefName(){return prefix+":bBoxType";}
        public String getQualifiedTypeDefName(String prefix){
                if(prefix!=null)
                        return prefix+":BoxType";
                if(this.prefix!=null)
                        return this.prefix+":BoxType";
                return null;
        }
        public String getQualifiedTypeRefName(String prefix){
                if(prefix!=null)
                        return prefix+":boxType";
                if(this.prefix!=null)
                        return this.prefix+":boxType";
                return null;
        }
        public Class getJavaClass(){return Envelope.class;}
        public boolean isAbstract(){return false;}
}

class PolygonElement extends NameSpaceElement{
        public PolygonElement(String prefix){super(prefix);}
        public String getTypeDefName(){return "PolygonType";}
        public String getTypeRefName(){return "polygonType";}
        public String getQualifiedTypeDefName(){return prefix+":PolygonType";}
        public String getQualifiedTypeRefName(){return prefix+":pPolygonType";}
        public String getQualifiedTypeDefName(String prefix){
                if(prefix!=null)
                        return prefix+":PolygonType";
                if(this.prefix!=null)
                        return this.prefix+":PolygonType";
                return null;
        }
        public String getQualifiedTypeRefName(String prefix){
                if(prefix!=null)
                        return prefix+":polygonType";
                if(this.prefix!=null)
                        return this.prefix+":polygonType";
                return null;
        }
        public Class getJavaClass(){return Polygon.class;}
        public boolean isAbstract(){return false;}
}

class GeometryCollectionElement extends NameSpaceElement{
        public GeometryCollectionElement(String prefix){super(prefix);}
        public String getTypeDefName(){return "GeometryCollectionType";}
        public String getTypeRefName(){return "GeometryCollectionType";}
        public String getQualifiedTypeDefName(){return prefix+":GeometryCollectionType";}
        public String getQualifiedTypeRefName(){return prefix+":geometryCollectionType";}
        public String getQualifiedTypeDefName(String prefix){
                if(prefix!=null)
                        return prefix+":GeometryCollectionType";
                if(this.prefix!=null)
                        return this.prefix+":GeometryCollectionType";
                return null;
        }
        public String getQualifiedTypeRefName(String prefix){
                if(prefix!=null)
                        return prefix+":geometryCollectionType";
                if(this.prefix!=null)
                        return this.prefix+":geometryCollectionType";
                return null;
        }
        public Class getJavaClass(){return GeometryCollection.class;}
        public boolean isAbstract(){return true;}
}

class MultiPointElement extends NameSpaceElement{
        public MultiPointElement(String prefix){super(prefix);}
        public String getTypeDefName(){return "MultiPointType";}
        public String getTypeRefName(){return "multiPointType";}
        public String getQualifiedTypeDefName(){return prefix+":MultiPointType";}
        public String getQualifiedTypeRefName(){return prefix+":multiPointType";}
        public String getQualifiedTypeDefName(String prefix){
                if(prefix!=null)
                        return prefix+":MultiPointType";
                if(this.prefix!=null)
                        return this.prefix+":MultiPointType";
                return null;
        }
        public String getQualifiedTypeRefName(String prefix){
                if(prefix!=null)
                        return prefix+":multiPointType";
                if(this.prefix!=null)
                        return this.prefix+":multiPointType";
                return null;
        }
        public Class getJavaClass(){return MultiPoint.class;}
        public boolean isAbstract(){return false;}
}

class MultiLineStringElement extends NameSpaceElement{
        public MultiLineStringElement(String prefix){super(prefix);}
        public String getTypeDefName(){return "MultiLineStringType";}
        public String getTypeRefName(){return "multiLineStringType";}
        public String getQualifiedTypeDefName(){return prefix+":MultiLineStringType";}
        public String getQualifiedTypeRefName(){return prefix+":multiLineStringType";}
        public String getQualifiedTypeDefName(String prefix){
                if(prefix!=null)
                        return prefix+":MultiLineStringType";
                if(this.prefix!=null)
                        return this.prefix+":MultiLineStringType";
                return null;
        }
        public String getQualifiedTypeRefName(String prefix){
                if(prefix!=null)
                        return prefix+":multiLineStringType";
                if(this.prefix!=null)
                        return this.prefix+":multiLineStringType";
                return null;
        }
        public Class getJavaClass(){return MultiLineString.class;}
        public boolean isAbstract(){return false;}
}

class MultiPolygonElement extends NameSpaceElement{
        public MultiPolygonElement(String prefix){super(prefix);}
        public String getTypeDefName(){return "MultiPolygonType";}
        public String getTypeRefName(){return "multiPolygonType";}
        public String getQualifiedTypeDefName(){return prefix+":MultiPolygonType";}
        public String getQualifiedTypeRefName(){return prefix+":multiPolygonType";}
        public String getQualifiedTypeDefName(String prefix){
                if(prefix!=null)
                        return prefix+":MultiPolygonType";
                if(this.prefix!=null)
                        return this.prefix+":MultiPolygonType";
                return null;
        }
        public String getQualifiedTypeRefName(String prefix){
                if(prefix!=null)
                        return prefix+":multiPolygonType";
                if(this.prefix!=null)
                        return this.prefix+":multiPolygonType";
                return null;
        }
        public Class getJavaClass(){return MultiPolygon.class;}
        public boolean isAbstract(){return false;}
}

class CoordElement extends NameSpaceElement{
        public CoordElement(String prefix){super(prefix);}
        public String getTypeDefName(){return "CoordType";}
        public String getTypeRefName(){return "coordType";}
        public String getQualifiedTypeDefName(){return prefix+":CoordType";}
        public String getQualifiedTypeRefName(){return prefix+":coordType";}
        public String getQualifiedTypeDefName(String prefix){
                if(prefix!=null)
                        return prefix+":CoordType";
                if(this.prefix!=null)
                        return this.prefix+":CoordType";
                return null;
        }
        public String getQualifiedTypeRefName(String prefix){
                if(prefix!=null)
                        return prefix+":coordType";
                if(this.prefix!=null)
                        return this.prefix+":coordType";
                return null;
        }
        public Class getJavaClass(){return Coordinate.class;}
        public boolean isAbstract(){return false;}
}

class CoordinatesElement extends NameSpaceElement{
        public CoordinatesElement(String prefix){super(prefix);}
        public String getTypeDefName(){return "CoordinatesType";}
        public String getTypeRefName(){return "coordinatesType";}
        public String getQualifiedTypeDefName(){return prefix+":CoordinatesType";}
        public String getQualifiedTypeRefName(){return prefix+":coordinatesType";}
        public String getQualifiedTypeDefName(String prefix){
                if(prefix!=null)
                        return prefix+":CoordinatesType";
                if(this.prefix!=null)
                        return this.prefix+":CoordinatesType";
                return null;
        }
        public String getQualifiedTypeRefName(String prefix){
                if(prefix!=null)
                        return prefix+":coordinatesType";
                if(this.prefix!=null)
                        return this.prefix+":coordinatesType";
                return null;
        }
        public Class getJavaClass(){return Coordinate.class;}
        public boolean isAbstract(){return false;}
        }*/
class PointPropertyElement extends NameSpaceElement {
    public PointPropertyElement(String prefix) {
        super(prefix);
    }

    public String getTypeDefName() {
        return "PointPropertyType";
    }

    public String getTypeRefName() {
        return "pointProperty";
    }

    public String getQualifiedTypeDefName() {
        return prefix + ":PointPropertyType";
    }

    public String getQualifiedTypeRefName() {
        return prefix + ":pointProperty";
    }

    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":PointPropertyType";
        }

        if (this.prefix != null) {
            return this.prefix + ":PointPropertyType";
        }

        return null;
    }

    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":pointProperty";
        }

        if (this.prefix != null) {
            return this.prefix + ":pointProperty";
        }

        return null;
    }

    public Class getJavaClass() {
        return Point.class;
    }

    public boolean isAbstract() {
        return false;
    }

    public boolean isDefault() {
        return true;
    }
}

class PolygonPropertyElement extends NameSpaceElement {
    public PolygonPropertyElement(String prefix) {
        super(prefix);
    }

    public String getTypeDefName() {
        return "PolygonPropertyType";
    }

    public String getTypeRefName() {
        return "polygonProperty";
    }

    public String getQualifiedTypeDefName() {
        return prefix + ":PolygonPropertyType";
    }

    public String getQualifiedTypeRefName() {
        return prefix + ":polygonProperty";
    }

    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":PolygonPropertyType";
        }

        if (this.prefix != null) {
            return this.prefix + ":PolygonPropertyType";
        }

        return null;
    }

    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":polygonProperty";
        }

        if (this.prefix != null) {
            return this.prefix + ":polygonProperty";
        }

        return null;
    }

    public boolean isAbstract() {
        return false;
    }

    public Class getJavaClass() {
        return Polygon.class;
    }

    public boolean isDefault() {
        return true;
    }
}

class LineStringPropertyElement extends NameSpaceElement {
    public LineStringPropertyElement(String prefix) {
        super(prefix);
    }

    public String getTypeDefName() {
        return "LineStringPropertyType";
    }

    public String getTypeRefName() {
        return "lineStringProperty";
    }

    public String getQualifiedTypeDefName() {
        return prefix + ":LineStringPropertyType";
    }

    public String getQualifiedTypeRefName() {
        return prefix + ":lineStringProperty";
    }

    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":LineStringPropertyType";
        }

        if (this.prefix != null) {
            return this.prefix + ":LineStringPropertyType";
        }

        return null;
    }

    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":lineStringProperty";
        }

        if (this.prefix != null) {
            return this.prefix + ":lineStringProperty";
        }

        return null;
    }

    public Class getJavaClass() {
        return LineString.class;
    }

    public boolean isAbstract() {
        return false;
    }

    public boolean isDefault() {
        return true;
    }
}

class MultiPointPropertyElement extends NameSpaceElement {
    public MultiPointPropertyElement(String prefix) {
        super(prefix);
    }

    public String getTypeDefName() {
        return "MultiPointPropertyType";
    }

    public String getTypeRefName() {
        return "multiPointProperty";
    }

    public String getQualifiedTypeDefName() {
        return prefix + ":MultiPointPropertyType";
    }

    public String getQualifiedTypeRefName() {
        return prefix + ":multiPointProperty";
    }

    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":MultiPointPropertyType";
        }

        if (this.prefix != null) {
            return this.prefix + ":MultiPointPropertyType";
        }

        return null;
    }

    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":multiPointProperty";
        }

        if (this.prefix != null) {
            return this.prefix + ":multiPointProperty";
        }

        return null;
    }

    public Class getJavaClass() {
        return MultiPoint.class;
    }

    public boolean isAbstract() {
        return false;
    }

    public boolean isDefault() {
        return true;
    }
}

class MultiLineStringPropertyElement extends NameSpaceElement {
    public MultiLineStringPropertyElement(String prefix) {
        super(prefix);
    }

    public String getTypeDefName() {
        return "MultiLineStringPropertyType";
    }

    public String getTypeRefName() {
        return "multiLineStringProperty";
    }

    public String getQualifiedTypeDefName() {
        return prefix + ":MultiLineStringPropertyType";
    }

    public String getQualifiedTypeRefName() {
        return prefix + ":multiLineStringProperty";
    }

    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":MultiLineStringPropertyType";
        }

        if (this.prefix != null) {
            return this.prefix + ":MultiLineStringPropertyType";
        }

        return null;
    }

    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":multiLineStringProperty";
        }

        if (this.prefix != null) {
            return this.prefix + ":multiLineStringProperty";
        }

        return null;
    }

    public Class getJavaClass() {
        return MultiLineString.class;
    }

    public boolean isAbstract() {
        return false;
    }

    public boolean isDefault() {
        return true;
    }
}

class MultiPolygonPropertyElement extends NameSpaceElement {
    public MultiPolygonPropertyElement(String prefix) {
        super(prefix);
    }

    public String getTypeDefName() {
        return "MultiPolygonPropertyType";
    }

    public String getTypeRefName() {
        return "multiPolygonProperty";
    }

    public String getQualifiedTypeDefName() {
        return prefix + ":MultiPolygonPropertyType";
    }

    public String getQualifiedTypeRefName() {
        return prefix + ":multiPolygonProperty";
    }

    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":MultiPolygonPropertyType";
        }

        if (this.prefix != null) {
            return this.prefix + ":MultiPolygonPropertyType";
        }

        return null;
    }

    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":multiPolygonProperty";
        }

        if (this.prefix != null) {
            return this.prefix + ":multiPolygonProperty";
        }

        return null;
    }

    public Class getJavaClass() {
        return MultiPolygon.class;
    }

    public boolean isAbstract() {
        return false;
    }

    public boolean isDefault() {
        return true;
    }
}

class MultiGeometryPropertyElement extends NameSpaceElement {
    public MultiGeometryPropertyElement(String prefix) {
        super(prefix);
    }

    public String getTypeDefName() {
        return "MultiGeometryPropertyType";
    }

    public String getTypeRefName() {
        return "multiGeometryProperty";
    }

    public String getQualifiedTypeDefName() {
        return prefix + ":MultiGeometryPropertyType";
    }

    public String getQualifiedTypeRefName() {
        return prefix + ":multiGeometryProperty";
    }

    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":MultiGeometryPropertyType";
        }

        if (this.prefix != null) {
            return this.prefix + ":MultiGeometryPropertyType";
        }

        return null;
    }

    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":multiGeometryProperty";
        }

        if (this.prefix != null) {
            return this.prefix + ":multiGeometryProperty";
        }

        return null;
    }

    public Class getJavaClass() {
        return GeometryCollection.class;
    }

    public boolean isAbstract() {
        return false;
    }

    public boolean isDefault() {
        return true;
    }
}

class NullElement extends NameSpaceElement {
    public NullElement(String prefix) {
        super(prefix);
    }

    public String getTypeDefName() {
        return "NullType";
    }

    public String getTypeRefName() {
        return "null";
    }

    public String getQualifiedTypeDefName() {
        return prefix + ":NullType";
    }

    public String getQualifiedTypeRefName() {
        return prefix + ":null";
    }

    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":NullType";
        }

        if (this.prefix != null) {
            return this.prefix + ":NullType";
        }

        return null;
    }

    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":null";
        }

        if (this.prefix != null) {
            return this.prefix + ":null";
        }

        return null;
    }

    public boolean isAbstract() {
        return false;
    }

    public Class getJavaClass() {
        return null;
    }
}

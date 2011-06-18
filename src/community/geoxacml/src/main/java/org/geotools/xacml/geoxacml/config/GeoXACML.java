/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2008, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */

package org.geotools.xacml.geoxacml.config;

import java.net.URL;

import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.geotools.xacml.extensions.WildCardFunctionCluster;
import org.geotools.xacml.geoxacml.attr.GeometryAttribute;
import org.geotools.xacml.geoxacml.attr.proxy.GeometryAttributeProxy;
import org.geotools.xacml.geoxacml.cond.ConvertToMetre;
import org.geotools.xacml.geoxacml.cond.ConvertToSquareMetre;
import org.geotools.xacml.geoxacml.cond.GeometryArea;
import org.geotools.xacml.geoxacml.cond.GeometryBoundary;
import org.geotools.xacml.geoxacml.cond.GeometryBuffer;
import org.geotools.xacml.geoxacml.cond.GeometryCentroid;
import org.geotools.xacml.geoxacml.cond.GeometryContains;
import org.geotools.xacml.geoxacml.cond.GeometryConvexHull;
import org.geotools.xacml.geoxacml.cond.GeometryCrosses;
import org.geotools.xacml.geoxacml.cond.GeometryDifference;
import org.geotools.xacml.geoxacml.cond.GeometryDisjoint;
import org.geotools.xacml.geoxacml.cond.GeometryDistance;
import org.geotools.xacml.geoxacml.cond.GeometryEquals;
import org.geotools.xacml.geoxacml.cond.GeometryIntersection;
import org.geotools.xacml.geoxacml.cond.GeometryIntersects;
import org.geotools.xacml.geoxacml.cond.GeometryIsClosed;
import org.geotools.xacml.geoxacml.cond.GeometryIsSimple;
import org.geotools.xacml.geoxacml.cond.GeometryIsValid;
import org.geotools.xacml.geoxacml.cond.GeometryIsWithinDistance;
import org.geotools.xacml.geoxacml.cond.GeometryLength;
import org.geotools.xacml.geoxacml.cond.GeometryOverlaps;
import org.geotools.xacml.geoxacml.cond.GeometrySymDifference;
import org.geotools.xacml.geoxacml.cond.GeometryTouches;
import org.geotools.xacml.geoxacml.cond.GeometryUnion;
import org.geotools.xacml.geoxacml.cond.GeometryWithin;
import org.xml.sax.SAXException;

import com.sun.xacml.attr.AttributeFactory;
import com.sun.xacml.attr.AttributeFactoryProxy;
import com.sun.xacml.attr.StandardAttributeFactory;
import com.sun.xacml.cond.BagFunction;
import com.sun.xacml.cond.Function;
import com.sun.xacml.cond.FunctionFactory;
import com.sun.xacml.cond.FunctionFactoryProxy;
import com.sun.xacml.cond.SetFunction;
import com.sun.xacml.cond.StandardFunctionFactory;

/**
 * @author Christian Mueller
 * 
 *         Global class for configuration. The initialize method has to be called for
 * 
 *         1) registering the new GeometryAttribute as XACML attribute 2) registering a lot of
 *         geometry functions according to the GeoXACML spec.
 * 
 *         All functions for conformance level STANDARD are implemented which is a superset of
 *         conformance level BASIC.
 * 
 */
public class GeoXACML {

    private static boolean initialized = false;

    static Schema XACMLPolicySchema, XACMLContextSchema;

    public static URL getPolicyXMLSchemaURL() {
        return GeoXACML.class
                .getResource("/xsd/xacml/access_control-xacml-2.0-policy-schema-cd-04.xsd");
        // http://docs.oasis-open.org/xacml/2.0/access_control-xacml-2.0-policy-schema-os.xsd
    }

    public static URL getContextXMLSchemaURL() {
        return GeoXACML.class
                .getResource("/xsd/xacml/access_control-xacml-2.0-context-schema-cd-04.xsd");
        // http://docs.oasis-open.org/xacml/2.0/access_control-xacml-2.0-context-schema-os.xsd
    }

    public static synchronized Schema getPolicySchema() {

        if (XACMLPolicySchema != null)
            return XACMLPolicySchema;
        SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");

        URL url = getPolicyXMLSchemaURL();
        try {
            XACMLPolicySchema = factory.newSchema(url);
        } catch (SAXException e) {
            // should not happened
        }

        return XACMLPolicySchema;
    }

    public static synchronized Schema getContextSchema() {

        if (XACMLContextSchema != null)
            return XACMLContextSchema;
        SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");

        URL url = getContextXMLSchemaURL();
        try {
            XACMLContextSchema = factory.newSchema(url);

        } catch (SAXException e) {
            // should not happened
        }

        return XACMLContextSchema;
    }

    public static synchronized void initialize() {
        if (initialized)
            return;
        registerGeoXACMLAttributes();
        registerGeoXACMLBaseFunctions();
        initialized = true;

    }

    private static void registerGeoXACMLBaseFunctions() {

        FunctionFactoryProxy factoryProxy = StandardFunctionFactory.getNewFactoryProxy();
        FunctionFactory factory = factoryProxy.getTargetFactory();

        // FunctionFactory factory = FunctionFactory.getTargetInstance();

        // add wildcard functions
        for (Function wildCardFunction : new WildCardFunctionCluster().getSupportedFunctions()) {
            factory.addFunction(wildCardFunction);
        }

        factory.addFunction(new GeometryEquals());
        factory.addFunction(new GeometryDisjoint());
        factory.addFunction(new GeometryTouches());
        factory.addFunction(new GeometryCrosses());
        factory.addFunction(new GeometryWithin());
        factory.addFunction(new GeometryContains());
        factory.addFunction(new GeometryOverlaps());
        factory.addFunction(new GeometryIntersects());

        factory.addFunction(new GeometryIsClosed());
        factory.addFunction(new GeometryIsValid());
        factory.addFunction(new GeometryIsSimple());

        factory.addFunction(new GeometryArea());
        factory.addFunction(new GeometryDistance());
        factory.addFunction(new GeometryIsWithinDistance());
        factory.addFunction(new GeometryLength());

        factory.addFunction(new GeometryBuffer());
        factory.addFunction(new GeometryUnion());
        factory.addFunction(new GeometryIntersection());
        factory.addFunction(new GeometryDifference());
        factory.addFunction(new GeometrySymDifference());
        factory.addFunction(new GeometryBoundary());
        factory.addFunction(new GeometryCentroid());
        factory.addFunction(new GeometryConvexHull());

        factory.addFunction(new ConvertToMetre());
        factory.addFunction(new ConvertToSquareMetre());

        String bagPrefix = "urn:ogc:def:function:geoxacml:1.0:geometry";

        String functionName;
        functionName = bagPrefix + BagFunction.NAME_BASE_ONE_AND_ONLY;
        factory.addFunction(BagFunction.getOneAndOnlyInstance(functionName,
                GeometryAttribute.identifier));

        functionName = bagPrefix + BagFunction.NAME_BASE_IS_IN;
        factory
                .addFunction(BagFunction
                        .getIsInInstance(functionName, GeometryAttribute.identifier));

        functionName = bagPrefix + BagFunction.NAME_BASE_BAG_SIZE;
        factory.addFunction(BagFunction.getBagSizeInstance(functionName,
                GeometryAttribute.identifier));

        functionName = bagPrefix + BagFunction.NAME_BASE_BAG;
        factory.addFunction(BagFunction.getBagInstance(functionName, GeometryAttribute.identifier));

        String setPrefix = "urn:ogc:def:function:geoxacml:1.0:geometry";

        functionName = setPrefix + SetFunction.NAME_BASE_AT_LEAST_ONE_MEMBER_OF;
        factory.addFunction(SetFunction.getAtLeastOneInstance(functionName,
                GeometryAttribute.identifier));

        functionName = setPrefix + SetFunction.NAME_BASE_SET_EQUALS;
        factory.addFunction(SetFunction.getSetEqualsInstance(functionName,
                GeometryAttribute.identifier));

        setPrefix = "urn:ogc:def:function:geoxacml:1.0:geometry-bag";

        functionName = setPrefix + SetFunction.NAME_BASE_INTERSECTION;
        factory.addFunction(SetFunction.getIntersectionInstance(functionName,
                GeometryAttribute.identifier));

        functionName = setPrefix + SetFunction.NAME_BASE_SUBSET;
        factory.addFunction(SetFunction.getSubsetInstance(functionName,
                GeometryAttribute.identifier));

        functionName = setPrefix + SetFunction.NAME_BASE_UNION;
        factory.addFunction(SetFunction
                .getUnionInstance(functionName, GeometryAttribute.identifier));

        FunctionFactory.setDefaultFactory(factoryProxy);
    }

    private static void registerGeoXACMLAttributes() {

        final AttributeFactory fac = StandardAttributeFactory.getNewFactory();

        fac.addDatatype(GeometryAttribute.identifier, new GeometryAttributeProxy());

        AttributeFactory.setDefaultFactory(new AttributeFactoryProxy() {
            public AttributeFactory getFactory() {
                return fac;
            }
        });

    }

}

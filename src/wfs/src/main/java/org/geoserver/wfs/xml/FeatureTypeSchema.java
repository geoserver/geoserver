/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml;

import java.io.IOException;
import org.eclipse.xsd.XSDSchema;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.wfs.WFSInfo;

/**
 * An xml schema describing a wfs feature type.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public abstract class FeatureTypeSchema {
    /** The feature type metadata object. */
    protected FeatureTypeInfo featureType;

    /** The xsd schema builder. */
    protected FeatureTypeSchemaBuilder builder;

    /** The catalog */
    protected Catalog catalog;

    /** WFS configuration */
    protected WFSInfo wfs;

    protected FeatureTypeSchema(FeatureTypeInfo featureType, WFSInfo wfs, Catalog catalog) {
        this.featureType = featureType;
        this.catalog = catalog;
        this.wfs = wfs;
    }

    /** @return The feautre type info. */
    FeatureTypeInfo getFeatureType() {
        return featureType;
    }

    /** @return The {@link XSDSchema} representation of the schema. */
    public XSDSchema schema(String baseUrl) throws IOException {
        return builder.build(new FeatureTypeInfo[] {featureType}, baseUrl);
    }

    /** Converts the schema to a gml2 schema. */
    public FeatureTypeSchema toGML2() {
        if (this instanceof GML2) {
            return this;
        }

        return new GML2(featureType, wfs, catalog);
    }

    /** Converts the schema to a gml3 schema. */
    public FeatureTypeSchema toGML3() {
        if (this instanceof GML3) {
            return this;
        }

        return new GML3(featureType, wfs, catalog);
    }

    /**
     * GML2 based wfs feature type schema.
     *
     * @author Justin Deoliveira, The Open Planning Project
     */
    public static final class GML2 extends FeatureTypeSchema {
        public GML2(FeatureTypeInfo featureType, WFSInfo wfs, Catalog catalog) {
            super(featureType, wfs, catalog);
            builder = new FeatureTypeSchemaBuilder.GML2(wfs.getGeoServer());
        }
    }

    /**
     * GML3 based wfs feature type schema.
     *
     * @author Justin Deoliveira, The Open Planning Project
     */
    public static final class GML3 extends FeatureTypeSchema {
        protected GML3(FeatureTypeInfo featureType, WFSInfo wfs, Catalog catalog) {
            super(featureType, wfs, catalog);
            builder = new FeatureTypeSchemaBuilder.GML3(wfs.getGeoServer());
        }
    }
}

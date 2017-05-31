/* Copyright (c) 2013 - 2014 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.core.format;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;

import org.geoserver.rest.format.ReflectiveJSONFormat;
import com.boundlessgeo.gsr.core.feature.AttributeListConverter;
import com.boundlessgeo.gsr.core.feature.FieldTypeConverter;
import com.boundlessgeo.gsr.core.font.FontDecorationEnumConverter;
import com.boundlessgeo.gsr.core.font.FontStyleEnumConverter;
import com.boundlessgeo.gsr.core.font.FontWeightEnumConverter;
import com.boundlessgeo.gsr.core.geometry.Geometry;
import com.boundlessgeo.gsr.core.geometry.GeometryTypeConverter;
import com.boundlessgeo.gsr.core.geometry.Point;
import com.boundlessgeo.gsr.core.geometry.SpatialReference;
import com.boundlessgeo.gsr.core.geometry.SpatialReferenceWKID;
import com.boundlessgeo.gsr.core.label.LineLabelPlacementEnumConverter;
import com.boundlessgeo.gsr.core.label.PointLabelPlacementEnumConverter;
import com.boundlessgeo.gsr.core.label.PolygonLabelPlacementEnumConverter;
import com.boundlessgeo.gsr.core.symbol.HorizontalAlignmentEnumConverter;
import com.boundlessgeo.gsr.core.symbol.SimpleFillSymbol;
import com.boundlessgeo.gsr.core.symbol.SimpleFillSymbolEnumConverter;
import com.boundlessgeo.gsr.core.symbol.SimpleLineSymbol;
import com.boundlessgeo.gsr.core.symbol.SimpleLineSymbolEnumConverter;
import com.boundlessgeo.gsr.core.symbol.SimpleMarkerSymbol;
import com.boundlessgeo.gsr.core.symbol.SimpleMarkerSymbolEnumConverter;
import com.boundlessgeo.gsr.core.symbol.Symbol;
import com.boundlessgeo.gsr.core.symbol.VerticalAlignmentEnumConverter;
import com.boundlessgeo.gsr.service.CatalogService;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.json.JsonHierarchicalStreamDriver;
import com.thoughtworks.xstream.io.json.JsonWriter;

/**
 * 
 * @author Juan Marin, OpenGeo
 * 
 */
public class GeoServicesJsonFormat extends ReflectiveJSONFormat {

    XStream xStream;

    public XStream getXStream() {
        return xStream;
    }

    public void setXStream(XStream xStream) {
        this.xStream = xStream;
    }

    public GeoServicesJsonFormat() {
        super();
        configureXStream();
    }

    private void configureXStream() {
        XStream xstream = new XStream(new JsonHierarchicalStreamDriver() {
            public HierarchicalStreamWriter createWriter(Writer writer) {
                return new JsonWriter(writer, JsonWriter.DROP_ROOT_MODE);
            }
        });

        // alias
        xstream.alias("", Geometry.class);
        xstream.alias("", Point.class);
        xstream.alias("", SpatialReference.class);
        xstream.alias("", SpatialReferenceWKID.class);
        xstream.alias("", SimpleFillSymbol.class);
        xstream.alias("", SimpleLineSymbol.class);
        xstream.alias("", SimpleMarkerSymbol.class);
        xstream.alias("", Symbol.class);

        // omit fields
        xstream.omitField(CatalogService.class, "name");
        xstream.omitField(CatalogService.class, "type");
        xstream.omitField(CatalogService.class, "serviceType");
        xstream.omitField(CatalogService.class, "specVersion");
        xstream.omitField(CatalogService.class, "productName");

        xstream.omitField(Point.class, "geometryType");
        xstream.omitField(SpatialReferenceWKID.class, "geometryType");

        // converters
        xstream.registerConverter(new AttributeListConverter());
        xstream.registerConverter(new FieldTypeConverter());
        xstream.registerConverter(new GeometryTypeConverter());
        xstream.registerConverter(new SimpleMarkerSymbolEnumConverter());
        xstream.registerConverter(new SimpleLineSymbolEnumConverter());
        xstream.registerConverter(new SimpleFillSymbolEnumConverter());
        xstream.registerConverter(new VerticalAlignmentEnumConverter());
        xstream.registerConverter(new HorizontalAlignmentEnumConverter());
        xstream.registerConverter(new FontWeightEnumConverter());
        xstream.registerConverter(new FontDecorationEnumConverter());
        xstream.registerConverter(new FontStyleEnumConverter());
        xstream.registerConverter(new PointLabelPlacementEnumConverter());
        xstream.registerConverter(new LineLabelPlacementEnumConverter());
        xstream.registerConverter(new PolygonLabelPlacementEnumConverter());

        this.xStream = xstream;
    }

    @Override
    protected Object read(InputStream input) throws IOException {
        return xStream.fromXML(input);
    }

    @Override
    protected void write(Object data, OutputStream output) throws IOException {
        xStream.toXML(data, output);
    }

}

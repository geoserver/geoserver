package com.boundlessgeo.gsr.api;

import com.boundlessgeo.gsr.core.GSRModel;
import com.boundlessgeo.gsr.core.feature.AttributeListConverter;
import com.boundlessgeo.gsr.core.feature.FieldTypeConverter;
import com.boundlessgeo.gsr.core.font.FontDecorationEnumConverter;
import com.boundlessgeo.gsr.core.font.FontStyleEnumConverter;
import com.boundlessgeo.gsr.core.font.FontWeightEnumConverter;
import com.boundlessgeo.gsr.core.geometry.*;
import com.boundlessgeo.gsr.core.label.LineLabelPlacementEnumConverter;
import com.boundlessgeo.gsr.core.label.PointLabelPlacementEnumConverter;
import com.boundlessgeo.gsr.core.label.PolygonLabelPlacementEnumConverter;
import com.boundlessgeo.gsr.core.service.CatalogService;
import com.boundlessgeo.gsr.core.symbol.*;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.json.JsonHierarchicalStreamDriver;
import com.thoughtworks.xstream.io.json.JsonWriter;
import org.geoserver.rest.converters.BaseMessageConverter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Writer;

/**
 * Converts a {@link GSRModel} to a JSON string
 */
@Component
public class GeoServicesJSONConverter extends BaseMessageConverter<GSRModel> {

    protected XStream xStream;

    public GeoServicesJSONConverter() {
        super(MediaType.APPLICATION_JSON);
        configureXStream();
    }

    protected boolean supports(Class<?> clazz) {
        return GSRModel.class.isAssignableFrom(clazz);
    }

    public XStream getXStream() {
        return xStream;
    }

    public void setXStream(XStream xStream) {
        this.xStream = xStream;
    }

    @Override
    public GSRModel readInternal(Class<? extends GSRModel> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException
    {
        return (GSRModel) xStream.fromXML(inputMessage.getBody());
    }

    @Override
    public void writeInternal(GSRModel o, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {

        xStream.toXML(o, outputMessage.getBody());
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
}

/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.acl.plugin.web.components;

import com.google.common.collect.Iterators;
import java.util.Iterator;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AbstractAutoCompleteTextRenderer;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.string.Strings;
import org.geoserver.acl.plugin.web.support.SerializableFunction;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.PublishedType;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

@SuppressWarnings("serial")
public class PublishedInfoAutoCompleteTextField extends ModelUpdatingAutoCompleteTextField<String> {

    public PublishedInfoAutoCompleteTextField(
            String id, IModel<String> model, SerializableFunction<String, Iterator<PublishedInfo>> choiceResolver) {
        super(id, model, adapt(choiceResolver), LayerAutoCompleteRenderer.INSTANCE, defaultSettings());
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        response.render(CssHeaderItem.forReference(
                new PackageResourceReference(getClass(), "PublishedInfoAutoCompleteTextField.css")));
    }

    private static SerializableFunction<String, Iterator<String>> adapt(
            SerializableFunction<String, Iterator<PublishedInfo>> choiceResolver) {

        return input -> {
            Iterator<PublishedInfo> infos = choiceResolver.apply(input);
            return Iterators.transform(infos, info -> {
                String cssType = getCssClass(info);
                String name = info.getName();
                return name + "#" + cssType;
            });
        };
    }

    private static class LayerAutoCompleteRenderer extends AbstractAutoCompleteTextRenderer<String> {

        static final LayerAutoCompleteRenderer INSTANCE = new LayerAutoCompleteRenderer();

        protected @Override String getTextValue(final String object) {
            return getName(object);
        }

        protected @Override void renderChoice(
                final String object, final org.apache.wicket.request.Response response, final String criteria) {
            String textValue = getTextValue(object);
            String type = getClassName(object);
            textValue = Strings.escapeMarkup(textValue).toString();
            response.write("<span class=");
            response.write(type);
            response.write(">");
            response.write(textValue);
            response.write("</span>");
        }

        private String getName(String cssQualifiedName) {
            return cssQualifiedName.substring(0, cssQualifiedName.indexOf('#'));
        }

        private String getClassName(String cssQualifiedName) {
            return cssQualifiedName.substring(1 + cssQualifiedName.indexOf('#'));
        }
    }

    private static String getCssClass(PublishedInfo object) {
        PublishedType type = object.getType();
        switch (type) {
            case VECTOR:
                return getSpecificVectorType((LayerInfo) object);
            case GROUP:
                return "group";
            case RASTER:
            case REMOTE:
            case WMS:
            case WMTS:
                return "raster";
            default:
                return "unknown";
        }
    }

    private static String getSpecificVectorType(LayerInfo object) {
        try {
            FeatureTypeInfo fti = (FeatureTypeInfo) object.getResource();
            GeometryDescriptor gd = fti.getFeatureType().getGeometryDescriptor();
            return getVectoryClass(gd);
        } catch (Exception e) {
            return "vector";
        }
    }

    public static String getVectoryClass(GeometryDescriptor gd) {
        if (gd == null) {
            return "unknown";
        }
        Class<?> geom = gd.getType().getBinding();
        if (Point.class.isAssignableFrom(geom) || MultiPoint.class.isAssignableFrom(geom)) {
            return "point";
        } else if (LineString.class.isAssignableFrom(geom) || MultiLineString.class.isAssignableFrom(geom)) {
            return "line";
        } else if (Polygon.class.isAssignableFrom(geom) || MultiPolygon.class.isAssignableFrom(geom)) {
            return "polygon";
        } else {
            return "geometry";
        }
    }
}

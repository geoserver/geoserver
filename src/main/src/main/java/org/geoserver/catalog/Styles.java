/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.NamedLayer;
import org.geotools.styling.NamedStyle;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.styling.UserLayer;
import org.geotools.util.Version;
import org.geotools.util.logging.Logging;

/**
 * Provides methods to parse/encode style documents.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class Styles {

    /** logger */
    static Logger LOGGER = Logging.getLogger("org.geoserver.wms");

    static StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory(null);

    /**
     * Encodes the specified SLD as a string.
     *
     * @param sld The sld to encode.
     * @param handler The handler to use to encode.
     * @param ver Version of the style to encode, may be <code>null</code>.
     * @param pretty Whether to format the style.
     * @return The encoded style.
     */
    public static String string(
            StyledLayerDescriptor sld, SLDHandler handler, Version ver, boolean pretty)
            throws IOException {

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        handler.encode(sld, ver, pretty, bout);

        return new String(bout.toByteArray());
    }

    /**
     * Convenience method to pull a UserSyle from a StyledLayerDescriptor.
     *
     * <p>This method will return the first UserStyle it encounters in the StyledLayerDescriptor
     * tree.
     *
     * @param sld The StyledLayerDescriptor object.
     * @return The UserStyle, or <code>null</code> if no such style could be found.
     */
    public static Style style(StyledLayerDescriptor sld) {
        for (int i = 0; i < sld.getStyledLayers().length; i++) {
            Style[] styles = null;

            if (sld.getStyledLayers()[i] instanceof NamedLayer) {
                NamedLayer layer = (NamedLayer) sld.getStyledLayers()[i];
                styles = layer.getStyles();
            } else if (sld.getStyledLayers()[i] instanceof UserLayer) {
                UserLayer layer = (UserLayer) sld.getStyledLayers()[i];
                styles = layer.getUserStyles();
            }

            if (styles != null) {
                for (int j = 0; j < styles.length; j++) {
                    if (!(styles[j] instanceof NamedStyle)) {
                        return styles[j];
                    }
                }
            }
        }

        return null;
    }

    /**
     * Convenience method to wrap a UserStyle in a StyledLayerDescriptor object.
     *
     * <p>This method wraps the UserStyle in a NamedLayer, and wraps the result in a
     * StyledLayerDescriptor.
     *
     * @param style The UserStyle.
     * @return The StyledLayerDescriptor.
     */
    public static StyledLayerDescriptor sld(Style style) {
        StyledLayerDescriptor sld = styleFactory.createStyledLayerDescriptor();

        NamedLayer layer = styleFactory.createNamedLayer();
        layer.setName(style.getName());
        sld.addStyledLayer(layer);

        layer.addStyle(style);

        return sld;
    }

    /**
     * Looks up a style handler by format, file extension, or mime type.
     *
     * @param format The format, file extension, or mime type.
     * @see StyleHandler#getFormat()
     * @see StyleHandler#getFileExtension()
     * @see StyleHandler#mimeType(org.geotools.util.Version)
     */
    public static StyleHandler handler(String format) {
        if (format == null) {
            throw new IllegalArgumentException("Style format must not be null");
        }

        List<StyleHandler> allHandlers = handlers();
        List<StyleHandler> matches = new ArrayList();

        // look by format
        for (StyleHandler h : allHandlers) {
            if (format.equalsIgnoreCase(h.getFormat())) {
                matches.add(h);
            }
        }

        if (matches.isEmpty()) {
            // look by mime type
            for (StyleHandler h : allHandlers) {
                for (Version ver : h.getVersions()) {
                    if (h.mimeType(ver).equals(format)) {
                        matches.add(h);
                    }
                }
            }
        }

        if (matches.isEmpty()) {
            // look by file extension
            for (StyleHandler h : allHandlers) {
                if (format.equalsIgnoreCase(h.getFileExtension())) {
                    matches.add(h);
                }
            }
        }

        if (matches.isEmpty()) {
            throw new RuntimeException("No such style handler: format = " + format);
        }

        if (matches.size() == 1) {
            return matches.get(0);
        }

        List<String> handlerNames =
                Lists.transform(
                        matches,
                        new Function<StyleHandler, String>() {
                            @Nullable
                            @Override
                            public String apply(@Nullable StyleHandler styleHandler) {
                                if (styleHandler == null) {
                                    throw new RuntimeException(
                                            "Got a null style handler, unexpected");
                                }
                                return styleHandler.getName();
                            }
                        });
        throw new IllegalArgumentException(
                "Multiple style handlers: " + handlerNames + " found for format: " + format);
    }

    /** Returns all registered style handlers. */
    public static List<StyleHandler> handlers() {
        return GeoServerExtensions.extensions(StyleHandler.class);
    }
}

/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.Nullable;
import javax.xml.transform.TransformerException;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.geoserver.ows.util.RequestUtils;
import org.geoserver.platform.resource.Resource;
import org.geotools.data.DataUtilities;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.sld.v1_1.SLDConfiguration;
import org.geotools.styling.*;
import org.geotools.util.Version;
import org.geotools.util.logging.Logging;
import org.geotools.xml.Parser;
import org.vfny.geoserver.util.SLDValidator;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

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
     * Convenience method to pull a UserSyle from a StyledLayerDescriptor.
     * <p>
     * This method will return the first UserStyle it encounters in the StyledLayerDescriptor tree.
     * </p>
     * @param sld The StyledLayerDescriptor object.
     * 
     * @return The UserStyle, or <code>null</code> if no such style could be found.
     */
    public static Style style(StyledLayerDescriptor sld) {
        for (int i = 0; i < sld.getStyledLayers().length; i++) {
            Style[] styles = null;
            
            if (sld.getStyledLayers()[i] instanceof NamedLayer) {
                NamedLayer layer = (NamedLayer) sld.getStyledLayers()[i];
                styles = layer.getStyles();
            }
            else if(sld.getStyledLayers()[i] instanceof UserLayer) {
                UserLayer layer = (UserLayer) sld.getStyledLayers()[i];
                styles = layer.getUserStyles();
            }
            
            if (styles != null) {
                for (int j = 0; j < styles.length; i++) {
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
     * <p>
     * This method wraps the UserStyle in a NamedLayer, and wraps the result in a StyledLayerDescriptor.
     * </p>
     * @param style The UserStyle.
     * 
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
     *
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
            throw new RuntimeException(
                "No such style handler: format = " + format);
        }

        if (matches.size() == 1) {
            return matches.get(0);
        }

        List<String> handlerNames = Lists.transform(matches, new Function<StyleHandler, String>() {
            @Nullable
            @Override
            public String apply(@Nullable StyleHandler styleHandler) {
                return styleHandler.getName();
            }
        });
        throw new IllegalArgumentException("Multiple style handlers: " + handlerNames + " found for format: " + format);
    }

    /**
     * Returns all registered style handlers.
     */
    public static List<StyleHandler> handlers() {
        return GeoServerExtensions.extensions(StyleHandler.class);
    }
}

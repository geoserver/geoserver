/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.admin;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.geoserver.web.wicket.GeoServerDataProvider;

/**
 * Provides a filtered, sorted view over the fonts that are available to the JVM
 *
 * @author Miles Jordan, Australian Antarctic Division
 */
@SuppressWarnings({"serial"})
public class PreviewFontProvider extends GeoServerDataProvider<PreviewFont> {
    public static final Property<PreviewFont> NAME =
            new BeanProperty<PreviewFont>("name", "fontName");

    public static final Property<PreviewFont> PREVIEW_IMAGE =
            new BeanProperty<PreviewFont>("previewImage", "previewImage") {
                public boolean isSearchable() {
                    return false;
                }
            };

    public static final List<Property<PreviewFont>> PROPERTIES = Arrays.asList(NAME, PREVIEW_IMAGE);

    @Override
    protected List<PreviewFont> getItems() {
        List<PreviewFont> result = new ArrayList<PreviewFont>();

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Font fonts[] = ge.getAllFonts();
        for (Font font : fonts) {
            result.add(new PreviewFont(font.deriveFont(12f)));
        }

        return result;
    }

    @Override
    protected List<Property<PreviewFont>> getProperties() {
        return PROPERTIES;
    }
}

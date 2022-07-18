/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.Styles;
import org.geotools.util.Version;
import org.geotools.util.logging.Logging;

public class StyleFormatLabel extends Panel {

    private static final long serialVersionUID = 6348703587354586691L;
    static final Logger LOGGER = Logging.getLogger(StyleFormatLabel.class);

    public StyleFormatLabel(String id, IModel<?> formatModel, IModel<?> versionModel) {

        super(id, formatModel);
        String formatDisplayName = getFormatDisplayName(formatModel);
        String majorMinorVersion = getMajorMinorVersionString(versionModel);

        String formatNameAndVersion =
                concateFormatNameAndVersion(formatDisplayName, majorMinorVersion);

        Label formatLabel = new Label("styleFormatLabel", formatNameAndVersion);
        formatLabel.add(new AttributeModifier("title", formatNameAndVersion));
        add(formatLabel);
    }

    private String concateFormatNameAndVersion(String formatName, String formatVersion) {
        if (formatName == null || formatName.trim().isEmpty()) {
            return "";
        }

        if (formatVersion == null || formatVersion.trim().isEmpty()) {
            return formatName;
        }

        return (formatName + " " + formatVersion);
    }

    private String getFormatDisplayName(IModel<?> formatModel) {

        if (formatModel == null || formatModel.getObject() == null) {
            return null;
        }

        String format = (String) formatModel.getObject();
        try {
            return Styles.handler(format).getName();
        } catch (Exception e) {
            LOGGER.log(
                    Level.FINE,
                    "Go an exception looking up the style handler, using the raw format instead",
                    e);
            return format;
        }
    }

    private String getMajorMinorVersionString(IModel<?> versionModel) {

        if (versionModel == null || versionModel.getObject() == null) {
            return null;
        }

        Version formatVersion = (Version) versionModel.getObject();

        Comparable<?> major = formatVersion.getMajor();
        Comparable<?> minor = formatVersion.getMinor();

        if (major == null) {
            return null;
        }

        if (minor == null) {
            return major.toString();
        }

        return major.toString() + "." + minor.toString();
    }
}

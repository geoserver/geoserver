/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.response.dxf;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

/**
 * Class storing a linetype description.
 *
 * @author Mauro Bartolomeoli, mbarto@infosia.it
 */
public class LineType {
    String name = "";

    String description = "";

    LineTypeItem[] items = new LineTypeItem[0];

    public LineType(String name, String description) {
        super();
        this.name = name;
        this.description = description;
    }

    /** Gets the DXF name of the line type. */
    public String getName() {
        return name;
    }

    /** Sets DXF name of the line type. */
    public void setName(String name) {
        this.name = name;
    }

    /** Gets the pattern description for the line type. */
    public String getDescription() {
        return description;
    }

    /** Sets the pattern description for the line type. */
    public void setDescription(String description) {
        this.description = description;
    }

    /** Gets the pattern items. */
    public LineTypeItem[] getItems() {
        return items;
    }

    /** Sets the pattern items. */
    public void setItems(LineTypeItem[] items) {
        this.items = items;
    }

    /** Gets the complete length of the pattern. */
    public double getLength() {
        double len = 0.0;
        for (LineTypeItem item : items) len += Math.abs(item.getLength());
        return len;
    }

    /**
     * Parse a line type descriptor and returns a fully configured LineType object. A descriptor has
     * the following format: <name>!<repeatable pattern>[!<base length>], where <name> is the name
     * assigned to the line type, <base length> (optional) is a real number that tells how long is
     * each part of the line pattern (defaults to 0.125), and <repeatable pattern> is a visual
     * description of the repeatable part of the line pattern, as a sequence of - (solid line), *
     * (dot) and _ (empty space).
     */
    public static LineType parse(String ltype) {
        // split the descriptor in 2/3 parts
        String[] parts = ltype.split("!");
        // get the name
        String name = parts[0];
        // get the pattern/description
        String description = name;
        List<LineTypeItem> items = new ArrayList<LineTypeItem>();
        // default base length
        double baseLen = 0.125;
        if (parts.length > 1) {
            // put spaces instead of underscores in description
            description = StringUtils.repeat(parts[1].replace('_', ' '), 5);
            // get the custom base length, if available
            if (parts.length > 2) baseLen = Double.parseDouble(parts[2]);
            // split the pattern using a regular expression
            Pattern p = Pattern.compile("[-]+|[*]+|[_]+");
            Matcher m = p.matcher(parts[1]);
            // analyze each part and build a LineTypeItem
            while (m.find()) {
                String piece = m.group(0);
                int type =
                        piece.startsWith("-")
                                ? LineTypeItem.DASH
                                : (piece.startsWith("*") ? LineTypeItem.DOT : LineTypeItem.EMPTY);
                LineTypeItem item = new LineTypeItem(type, piece.length() * baseLen);
                items.add(item);
            }
        }
        LineType result = new LineType(name, description);
        result.setItems(items.toArray(new LineTypeItem[] {}));
        return result;
    }
}

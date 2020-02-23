/* (c) 2015 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.utfgrid;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.sf.json.util.JSONBuilder;
import net.sf.json.util.JSONStringer;
import org.geoserver.wms.map.RawMap;
import org.geoserver.wms.utfgrid.UTFGridEntries.UTFGridEntry;
import org.geotools.util.Converters;
import org.geotools.xml.gml.GMLComplexTypes.GeometryPropertyType;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;

public class UTFGridMap extends RawMap {

    private RenderedImage image;

    public UTFGridMap(final UTFGridMapContent mapContent, RenderedImage image) {
        super(mapContent, (byte[]) null, UTFGridMapOutputFormat.MIME_TYPE);
        this.image = image;
    }

    public void writeTo(java.io.OutputStream out) throws java.io.IOException {
        UTFGridEntries entries = getEntries();

        PrintWriter pw = new PrintWriter(out);
        pw.println("{");
        pw.println("\"grid\": [");
        List<UTFGridEntry> encodedEntries = writeGrid(pw, image, entries);
        pw.println("],");
        pw.println("\"keys\": [");
        if (encodedEntries.isEmpty()) {
            pw.println("  \"\"");
        } else {
            pw.println("  \"\",");
            for (Iterator<UTFGridEntry> it = encodedEntries.iterator(); it.hasNext(); ) {
                UTFGridEntry entry = (UTFGridEntry) it.next();
                pw.print("  \"");
                pw.print(entry.getKey());
                if (it.hasNext()) {
                    pw.println("\",");
                } else {
                    pw.println("\"");
                }
            }
        }
        pw.println("],");
        pw.println("\"data\": {");
        for (Iterator<UTFGridEntry> it = encodedEntries.iterator(); it.hasNext(); ) {
            UTFGridEntry entry = (UTFGridEntry) it.next();
            pw.print("  \"");
            pw.print(entry.getKey());
            pw.print("\" : ");
            pw.print(getAttributesJson(entry.getFeature()));
            if (it.hasNext()) {
                pw.println(",");
            }
        }
        pw.println("}");
        pw.println("}");
        pw.flush();
    }

    private String getAttributesJson(Feature feature) {
        JSONBuilder builder = new JSONStringer().object();
        builder.key("id").value(feature.getIdentifier().toString());
        if (feature instanceof SimpleFeature) {
            SimpleFeature sf = (SimpleFeature) feature;
            for (AttributeDescriptor ad : sf.getFeatureType().getAttributeDescriptors()) {
                if (ad instanceof GeometryDescriptor) {
                    continue;
                } else {
                    String name = ad.getLocalName();
                    Object value = sf.getAttribute(name);
                    addAttribute(builder, name, value);
                }
            }
        } else {
            for (Property p : feature.getProperties()) {
                if (p.getType() instanceof GeometryPropertyType) {
                    continue;
                }
                String name = p.getName().getLocalPart();
                Object value = p.getValue();
                addAttribute(builder, name, value);
            }
        }

        builder.endObject();
        return builder.toString();
    }

    private void addAttribute(JSONBuilder builder, String name, Object value) {
        if (value instanceof java.util.Date || value instanceof Calendar) {
            value = Converters.convert(value, String.class);
        }
        builder.key(name).value(value);
    }

    /**
     * Writes the grid, and maps the original values into a compact sequence of keys (the original
     * values might be sparse due to features being fully overwritten by other features)
     */
    private List<UTFGridEntry> writeGrid(
            PrintWriter pw, RenderedImage image, UTFGridEntries entries) {
        Map<Integer, UTFGridEntry> keyToFeature = entries.getEntryMap();
        List<UTFGridEntry> result = new ArrayList<UTFGridEntry>();

        int key = 1;
        Raster data = getData(image);
        int width = data.getWidth();
        int[] pixels = new int[width];
        int height = data.getHeight();
        for (int r = 0; r < height; r++) {
            data.getDataElements(0, r, width, 1, pixels);
            pw.print("\"");
            for (int i = 0; i < pixels.length; i++) {
                int pixel = pixels[i] & 0xFFFFFF;
                if (pixel == 0) {
                    pw.print(" ");
                } else {
                    UTFGridEntry entry = keyToFeature.get(pixel);
                    if (entry == null) {
                        throw new RuntimeException(
                                "Could not find entry for pixel value "
                                        + pixel
                                        + ". This normally means there is some color altering option at work "
                                        + "that the UTFGrid code failed to remove, like opacity, blending and the like");
                    }
                    int entryKey = entry.getKey();
                    if (entryKey == -1) {
                        entryKey = key++;
                        entry.setKey(entryKey);
                        result.add(entry);
                    }
                    pw.print(getGridChar(entryKey));
                }
            }
            if (r < height - 1) {
                pw.println("\",");
            } else {
                pw.println("\"");
            }
        }

        return result;
    }

    private Raster getData(RenderedImage image) {
        if (image instanceof BufferedImage) {
            // copy-less version of data access
            return ((BufferedImage) image).getRaster();
        } else {
            return image.getData();
        }
    }

    /**
     * From the spec, the encoding works as follows:
     *
     * <ul>
     *   <li>Add 32.
     *   <li>If the result is >= 34, add 1.
     *   <li>If the result is >= 92, add 1.
     * </ul>
     */
    private char getGridChar(int val) {
        int result = val + 32;
        if (result >= 34) {
            result++;
        }
        if (result >= 92) {
            result++;
        }
        return (char) result;
    }

    UTFGridEntries getEntries() {
        UTFGridMapContent mc = (UTFGridMapContent) mapContent;
        return mc.getEntries();
    }
}

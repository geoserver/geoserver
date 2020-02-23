/* (c) 2014 - 2017 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.response.dxf;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.text.NumberFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.geoserver.wfs.response.dxf.util.JulianDate;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Logging;

/**
 * Basic, abstract implementation of DXFWriter. Implements a common base of export functions useful
 * for any implementations (groups, tables, etc.).
 *
 * @author Mauro Bartolomeoli, mbarto@infosia.it
 */
public abstract class AbstractDXFWriter implements DXFWriter {

    private static final Logger LOGGER = Logging.getLogger(AbstractDXFWriter.class);

    // physical writer onto which the dxf will be written
    protected Writer writer = null;

    // numeric format used for real numbers (expecially coordinates)
    protected NumberFormat format = null;

    // numeric format used for julian dates
    protected NumberFormat dateFormat = null;

    // numeric format used for real numbers in ltype definition
    protected NumberFormat ltypeFormat = null;

    // end of line marker
    protected String EOL = "\n";

    // default encoding
    protected String encoding = "ANSI_1252";

    // flag to choose if all geometries should be written as blocks
    // (faster) or only when they are too complex (slower)
    protected boolean geometryAsBlock = false;

    // flag to choose if feature attributes should be written to file
    // This forces all features to be converted to blocks, since
    // attributes are tight to BLOCK/INSERT
    protected boolean writeAttributes = false;

    // array of cyclically used colors (specified as autocad color indexes)
    // each color is assigned to a layer until there are elements
    // available, then they are reused again
    protected int[] colors = new int[] {7, 1, 2, 3, 4, 5, 6, 8, 9};

    // array of cyclically used line types
    protected LineType[] lineTypes = new LineType[] {new LineType("CONTINUOUS", "Solid line")};

    // current layer color (index in the colors array)
    private int colorPos = 0;

    // current layer line type (index in the lineTypes array)
    private int ltypePos = 0;

    // list of names to be assigned to export layers
    private String[] layerNames = null;

    // counter used to name layers, if no layer name list is given
    // as an option
    int layerCounter = 0;

    // assigned names (id -> name), used to reference layers
    // in different steps of the dxf writing
    private Map<String, String> cachedNames = new HashMap<String, String>();

    // cached global envelope
    private ReferencedEnvelope e = null;

    // handle counters
    // each type of object needs a separate handle space, to
    // avoid conflicts, so we mantain a set of counters
    // for different kink of objects
    Map<String, Integer> handles = new HashMap<String, Integer>();

    /** Create a new instance of the writer, using the given underlying writer. */
    public abstract DXFWriter newInstance(Writer writer);

    /** Verifies if the writer supports the request dxf version. */
    public boolean supportsVersion(String version) {
        if (version == null) return true;
        return false;
    }

    public AbstractDXFWriter() {}

    /**
     * Full constructor. Needs a writer, to write the dxf out. It permits to specify an encoding for
     * the dxf.
     */
    public AbstractDXFWriter(Writer writer, String encoding) {
        // initialize handle counters
        handles.put("LType", 22); // max 19 linetypes
        handles.put("VPort", 41); // max 5 vports
        handles.put("Layer", 46); // max 154 layers
        handles.put("BlockRecord", 200); // max 299800 blocks
        handles.put("Block", 300000);
        handles.put("Geometry", 700000); // max 395075 geometries

        this.writer = writer;
        if (encoding != null) this.encoding = encoding;
        // initialize number formats
        format = NumberFormat.getInstance(Locale.US);
        // this may not be enough for latlon. At the equator, 0.01° approx. 1100 m
        // to have an accuracy of cm, 7 is needed
        // TODO: use CRS information to adapt this value
        format.setMaximumFractionDigits(7);
        format.setGroupingUsed(false);
        format.setMinimumFractionDigits(1);
        ltypeFormat = NumberFormat.getInstance(Locale.US);
        ltypeFormat.setMaximumFractionDigits(4);
        ltypeFormat.setGroupingUsed(false);
        ltypeFormat.setMinimumFractionDigits(1);
        dateFormat = NumberFormat.getInstance(Locale.US);
        dateFormat.setMaximumFractionDigits(10);
        dateFormat.setGroupingUsed(false);
        dateFormat.setMinimumFractionDigits(1);
    }

    /** Simple constructor. Needs a writer, to write the dxf out. */
    public AbstractDXFWriter(Writer writer) {
        this(writer, null);
    }

    /** Performs the actual writing. Override it in the actual implementation class. */
    public abstract void write(List featureList, String version) throws IOException;

    /** Extracts and cache the global ReferenceEnvelope for the given feature list. */
    protected ReferencedEnvelope getEnvelope(List featureList) {
        if (e == null) {
            for (int i = 0; i < featureList.size(); i++) {
                FeatureCollection collection = (FeatureCollection) featureList.get(i);
                if (e == null) {
                    e = collection.getBounds();
                } else {
                    e.expandToInclude(collection.getBounds());
                }
            }
        }
        return normalizeEnvelope(e);
    }

    /** Normalizes an envelope to get a usable viewport. */
    private ReferencedEnvelope normalizeEnvelope(ReferencedEnvelope pEnv) {
        if (pEnv != null) {
            // if it's empty, get a 1 meter envelope around it
            if (pEnv.getWidth() == 0 && pEnv.getHeight() == 0) {
                // no features or no geom, enable creation of valid empty file at least
                pEnv.init(0d, 1d, 0d, 1d);
            }
        } else {
            // no data, enable creation of empty file at least
            pEnv = new ReferencedEnvelope();
            pEnv.init(0d, 1d, 0d, 1d);
        }
        return pEnv;
    }

    /**
     * Writes the simplest dxf object, a group, composed of a numeric code and a value. The value
     * type can be interpreted looking at the code.
     */
    protected void writeGroup(int code, String value) throws IOException {
        writer.write(StringUtils.leftPad(code + "", 3) + EOL);
        writer.write(value + EOL);
    }

    /** Writes the End of file group. */
    protected void writeEof() throws IOException {
        writeStart("EOF");
    }

    /** Writes a start (section, etc.) group. */
    protected void writeStart(String entity) throws IOException {
        writeGroup(0, entity);
    }

    /**
     * Loads a static section from a resource/file. Some parts of the dxf can be really static, so
     * it's easier to load them from files.
     */
    protected void loadFromResource(String resource) throws IOException {
        final InputStream tpl =
                this.getClass().getClassLoader().getResourceAsStream(resource + ".dxf");

        if (tpl != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(tpl));
            String line = null;
            while ((line = reader.readLine()) != null) writer.write(line + EOL);
        }
    }

    /** Writes a section start. */
    protected void writeSectionStart(String name) throws IOException {
        writeStart("SECTION");
        writeName(name);
    }

    /** Writes a section end. */
    protected void writeSectionEnd() throws IOException {
        writeGroup(0, "ENDSEC");
    }

    /** Writes a table start. */
    protected void writeTableStart(String table) throws IOException {
        writeGroup(0, "TABLE");
        writeName(table);
    }

    /** Writes a table end. */
    protected void writeTableEnd() throws IOException {
        writeGroup(0, "ENDTAB");
    }

    /**
     * Writes an handle of the given type. The type is used to generate handles in different numeric
     * spaces, for different entities.
     */
    protected String writeHandle(String type) throws IOException {
        String handle = getNewHandle(type);
        writeGroup(5, handle);
        return handle;
    }

    /**
     * Gets a new handle of the given type. The type is used to generate handles in different
     * numeric spaces, for different entities.
     */
    protected String getNewHandle(String type) {
        int currentHandle = handles.get(type);
        String handle = Integer.toHexString(currentHandle).toUpperCase();
        currentHandle++;
        handles.put(type, currentHandle);
        return handle;
    }

    /** Gets a name for the layer represented by the given collection. */
    protected String getLayerName(FeatureCollection coll) {
        String name = getCachedName(coll.hashCode() + "");
        if (name == null) {
            name = layerNames[layerCounter];
            if (name.equals("")) name = "LAYER" + layerCounter;
            layerCounter++;
            storeCachedName(coll.hashCode() + "", name);
        }

        return name;
    }

    /** Store a layer name for future use. */
    private void storeCachedName(String id, String name) {
        cachedNames.put(id, name);
    }

    /** Gets a stored layer name. */
    private String getCachedName(String id) {
        return cachedNames.get(id);
    }

    /** Assign a color to the collection, cycling through the available color list. */
    protected int getColor(FeatureCollection coll) {
        int color = colors[colorPos];
        if (colorPos < (colors.length - 1)) colorPos++;
        else colorPos = 0;
        return color;
    }

    /** Assign a line type to the collection, cycling through the available line types list. */
    protected int getLineType(FeatureCollection coll) {
        int ltype = ltypePos;
        if (ltypePos < (lineTypes.length - 1)) ltypePos++;
        else ltypePos = 0;
        return ltype;
    }

    /** Writes a layer group. */
    protected void writeLayer(String layer) throws IOException {
        writeGroup(8, layer);
    }

    /** Writes an xref path group. */
    protected void writePath(String path) throws IOException {
        writeGroup(1, path);
    }

    /**
     * Writes a geometry start, for the given geometry name (line, etc.). The geometry belongs to
     * the given layer and has an owner handle. The geometry is assigned a line type and color, if
     * specified.
     */
    protected void writeGeometryStart(
            String geometryName, String layer, String ownerHandle, int lineType, int color)
            throws IOException {
        writeGroup(0, geometryName);
        writeHandle("Geometry");
        if (ownerHandle != null) writeOwnerHandle(ownerHandle);
        writeSubClass("AcDbEntity");
        writeLayer(layer);
        if (lineType != -1) writeLineType(lineType);
        if (color != -1) writeColor(7);
    }

    /**
     * Writes a geometry start, for the given geometry name (line, etc.). The geometry belongs to
     * the given layer and has an owner handle. The geometry is assigned layer line type and color.
     */
    protected void writeGeometryStart(String geometryName, String layer, String ownerHandle)
            throws IOException {
        writeGeometryStart(geometryName, layer, ownerHandle, -1, -1);
    }

    /** Writes a color group. */
    protected void writeColor(int color) throws IOException {
        writeIntegerGroup(62, color);
    }

    /** Writes a line type group. */
    protected void writeLineType(int lineType) throws IOException {
        writeGroup(6, lineTypes[lineType].getName());
    }

    /** Writes an owner handle group (assigns an owner to the object via its handle). */
    protected void writeOwnerHandle(String handle) throws IOException {
        writeGroup(330, handle);
    }

    /** Writes a subclass marker group. */
    protected void writeSubClass(String subclass) throws IOException {
        writeGroup(100, subclass);
    }

    /** Writes a size (number of following objects) group. */
    protected void writeSize(int size) throws IOException {
        writeIntegerGroup(70, size);
    }

    /** Writes a name group. */
    protected void writeName(String name) throws IOException {
        writeGroup(2, name);
    }

    /** Writes an header variable. */
    protected void writeVariable(String varName) throws IOException {
        writeGroup(9, "$" + varName);
    }

    /** Writes a group having an integer value. */
    protected void writeIntegerGroup(int code, int value) throws IOException {
        writeGroup(code, StringUtils.leftPad(value + "", 6));
    }

    protected void writeFlags(int code, int value) throws IOException {
        writeGroup(code, StringUtils.leftPad(value + "", 9));
    }

    /**
     * Writes a point with the given coordinates. Use NaN to exclude a coordinate (tipically z) from
     * output. Uses the standard (10,20,30) codes.
     */
    protected void writePoint(double x, double y, double z) throws IOException {
        writePoint(10, x, y, z);
    }

    /**
     * Writes a point with the given coordinates. Use NaN to exclude a coordinate (tipically z) from
     * output. Uses the codes given by baseCode (baseCode,baseCode+10,baseCode+20)
     */
    protected void writePoint(int baseCode, double x, double y, double z) throws IOException {
        writeDoubleGroup(baseCode, x);
        writeDoubleGroup(baseCode + 10, y);
        if (!Double.isNaN(z)) writeDoubleGroup(baseCode + 20, z);
    }

    /** Writes a group having a double value. */
    protected void writeDoubleGroup(int code, double val) throws IOException {
        writeGroup(code, format.format(val));
    }

    /** Writes a group having a double value. */
    protected void writeLength(int code, double val) throws IOException {
        writeGroup(code, ltypeFormat.format(val));
    }

    /** Writes a group representing a date in julian format. */
    protected void writeJulianDate(Date dt) throws IOException {
        writeGroup(40, dateFormat.format(JulianDate.toJulian(dt)));
    }

    /** Configure an option (usually got as a format option). */
    public void setOption(String optionName, Object optionValue) {
        if (optionName.equalsIgnoreCase("geometryasblock")) {
            setGeometryAsBlock(((Boolean) optionValue).booleanValue());
        } else if (optionName.equalsIgnoreCase("colors")) {
            setColors((int[]) optionValue);
        } else if (optionName.equalsIgnoreCase("linetypes")) {
            setLineTypes((LineType[]) optionValue);
        } else if (optionName.equalsIgnoreCase("layers")) {
            setLayerNames((String[]) optionValue);
        } else if (optionName.equalsIgnoreCase("writeattributes")) {
            setWriteAttributes((Boolean) optionValue);
        } else {
            LOGGER.severe("unknown option " + optionName);
        }
    }
    /** Sets the "write attributes to file" flag. */
    private void setWriteAttributes(boolean writeAttributes) {
        this.writeAttributes = writeAttributes;
    }

    /** Sets the "all geometries as blocks" flag. */
    public void setGeometryAsBlock(boolean geometryAsBlock) {
        this.geometryAsBlock = geometryAsBlock;
    }

    /** Set custom array of colors to assign to written layers. */
    public void setColors(int[] colors) {
        this.colors = colors;
    }

    /** Set custom array of line types to assign to written layers. */
    public void setLineTypes(LineType[] lineTypes) {
        this.lineTypes = lineTypes;
    }

    /** Set list of names to be used for layers. */
    public void setLayerNames(String[] layerNames) {
        this.layerNames = layerNames;
    }
}

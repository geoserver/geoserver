/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.response.dxf;

import java.io.IOException;
import java.io.Writer;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.type.FeatureTypeImpl;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.GeometryType;
import org.opengis.feature.type.Name;
import org.opengis.feature.type.PropertyDescriptor;

/**
 * DXFWriter for the release 14 of DXF. see
 * http://www.autodesk.com/techpubs/autocad/acadr14/dxf/index.htm
 *
 * @author Mauro Bartolomeoli, mbarto@infosia.it
 */
public class Rel14DXFWriter extends AbstractDXFWriter {
    private static final Logger LOGGER = Logging.getLogger(Rel14DXFWriter.class);

    // cache for block names
    Map<String, String> blockNames = null;

    // cache for block handles
    Map<String, String> blockHandles = null;

    // cache for block handles
    Map<String, Object> textConfig = null;

    // block name counter (blocks will be names "0", "1", ...
    int blockCounter = 0;

    // DXF version
    protected String version = "AC1014";

    public Rel14DXFWriter() {
        super();
        textConfig = new HashMap<String, Object>();
        textConfig.put("height", 0.72);
    }

    public Rel14DXFWriter(Writer writer) {
        super(writer);
    }

    /** Supports version if it's a number and equals to 14. */
    public boolean supportsVersion(String version) {
        if (super.supportsVersion(version)) return true;
        try {
            int v = Integer.parseInt(version);
            return v == 14;
        } catch (Exception e) {
            return false;
        }
    }

    /** Creates a new writer, using the given underlying writer. */
    public DXFWriter newInstance(Writer writer) {
        return new Rel14DXFWriter(writer);
    }

    /** Writes the DXF for the given feature list. */
    @Override
    public void write(List featureList, String version) throws IOException {
        // DXF General Structure
        writeHeader(featureList);
        writeClasses(featureList);
        writeTables(featureList);
        writeBlocks(featureList);
        writeEntities(featureList);
        writeObjects(featureList);
        writeEof();
        blockNames.clear();
        blockHandles.clear();
    }

    /** Writes the Header section. */
    private void writeHeader(List featureList) throws IOException {
        writeSectionStart("HEADER");
        // writes a list of variables
        writeVariables(featureList);
        writeSectionEnd();
    }

    /** Writes the classes section. */
    private void writeClasses(List featureList) throws IOException {
        writeSectionStart("CLASSES");
        writeClass(
                "ACDBDICTIONARYWDFLT",
                "AcDbDictionaryWithDefault",
                "ObjectDBX Classes",
                0,
                false,
                false);
        writeClass("TABLESTYLE", "AcDbTableStyle", "ObjectDBX Classes", 2047, false, false);
        writeClass("DICTIONARYVAR", "AcDbDictionaryVar", "ObjectDBX Classes", 0, false, false);
        writeClass("XRECORD", "AcDbXrecord", "AutoCAD 2000", 0, false, false);
        writeClass("LWPOLYLINE", "AcDbPolyline", "AutoCAD 2000", 0, false, true);
        writeClass("HATCH", "AcDbHatch", "AutoCAD 2000", 0, false, true);
        writeClass("ACDBPLACEHOLDER", "AcDbPlaceHolder", "ObjectDBX Classes", 0, false, false);
        writeClass("LAYOUT", "AcDbLayout", "ObjectDBX Classes", 0, false, false);
        writeSectionEnd();
    }

    /** Writes a class definition. */
    private void writeClass(
            String name,
            String devname,
            String description,
            int flags,
            boolean proxy,
            boolean entities)
            throws IOException {
        writeGroup(0, "CLASS");
        writeGroup(1, name);
        writeGroup(2, devname);
        writeGroup(3, description);
        writeFlags(90, flags);
        writeIntegerGroup(280, proxy ? 1 : 0);
        writeIntegerGroup(281, entities ? 1 : 0);
    }

    /** Writes the tables section */
    private void writeTables(List featureList) throws IOException {
        LOGGER.warning("Rel14DXFWriter.writeTables");
        writeSectionStart("TABLES");
        // Tables structure
        writeViewPort(featureList);
        writeLineTypes();
        writeLayers(featureList);
        writeStyles();
        writeView();
        writeUCS();
        writeApplications();
        writeDimensionStyles();
        writeBlockRecords(featureList);
        writeSectionEnd();
    }

    /** Writes the blocks section */
    private void writeBlocks(List featureList) throws IOException {
        writeSectionStart("BLOCKS");
        // static blocks (model space and paper space)
        writeModelSpaceBlock();
        writePaperSpaceBlock();
        // blocks computed from the feature list
        // (complex geometries)
        writeEntityBlocks(featureList);
        writeAttributeDefinitionBlocks(featureList);
        writeSectionEnd();
    }

    /** Writes the entities section */
    private void writeEntities(List featureList) throws IOException {
        writeSectionStart("ENTITIES");

        // entities computed from the feature list
        // (simple geometries or insert of blocks)
        for (Object coll : featureList) writeEntity((FeatureCollection) coll);
        writeSectionEnd();
    }

    /** Writes the objects section */
    private void writeObjects(List featureList) throws IOException {
        loadFromResource("objects");
    }

    /** Writes entities representing the given collection. */
    private void writeEntity(FeatureCollection coll) throws IOException {
        String layer = getLayerName(coll);
        if (geometryAsBlock) {
            for (String name : blockNames.values()) writeInsert(layer, name);
        } else {
            // iterates through all the items
            FeatureIterator<SimpleFeature> iter = coll.features();
            try {
                while (iter.hasNext()) {
                    SimpleFeature f = iter.next();
                    String fid = f.getID();

                    // if it's a block insert it, else write the geometry
                    // directly
                    if (blockNames.containsKey(fid)) {
                        String name = blockNames.get(fid);
                        writeInsert(layer, name);
                    } else {
                        writeGeometry(layer, "1F", (Geometry) f.getDefaultGeometry());
                        String name = blockNames.get(coll.hashCode() + "");
                        if (writeAttributes) {
                            String ownerHandle = blockHandles.get(coll.hashCode() + "");
                            String attributesLayer = layer + "_attributes";
                            // writeInsert(layer, name);
                            writeInsertWithAttributes(attributesLayer, ownerHandle, name, f);
                        }
                    }
                }
            } finally {
                iter.close();
            }
        }
    }

    private void writeAttributes(String layer, String ownerHandle, SimpleFeature f)
            throws IOException {
        Geometry geometry = (Geometry) f.getDefaultGeometry();
        Point intPoint = geometry.getInteriorPoint();
        for (Property p : f.getProperties()) {
            Name name = p.getName();
            LOGGER.warning("    attr: " + name.getLocalPart() + " = " + p.getValue());
            if (!(p.getValue() instanceof Geometry)) {
                writeAttribute(layer, ownerHandle, name.getLocalPart(), p.getValue(), intPoint);
            }
        }
    }

    private void writeAttribute(
            String layer, String ownerHandle, String attribName, Object value, Point intPoint)
            throws IOException {
        writeGroup(0, "ATTRIB");
        writeHandle("Geometry");
        // String handle = getNewHandle("Attrib");
        // writeGroup(5, handle);
        writeOwnerHandle(ownerHandle);
        writeSubClass("AcDbEntity");
        writeLayer(layer);
        writeSubClass("AcDbText");
        writeDoubleGroup(10, intPoint.getX());
        writeDoubleGroup(20, intPoint.getY());
        writeDoubleGroup(30, 0.0);
        writeDoubleGroup(40, 0.72 /*(Double) textConfig.get("height")*/);
        String valueString = "";
        if (value != null) {
            valueString = value.toString();
        }
        writeGroup(1, valueString);
        writeSubClass("AcDbAttribute");
        writeGroup(2, attribName);
        writeGroup(70, "     0");
    }

    /** Writes a block insert entity. */
    private void writeInsert(String layer, String name) throws IOException {
        writeGroup(0, "INSERT");
        writeHandle("Geometry");
        writeSubClass("AcDbEntity");
        writeLayer(layer);
        writeSubClass("AcDbBlockReference");
        writeName(name);
        writePoint(0.0, 0.0, 0.0);
    }

    private void writeInsertWithAttributes(
            String layer, String ownerHandle, String name, SimpleFeature f) throws IOException {
        writeGroup(0, "INSERT");
        writeOwnerHandle(ownerHandle);
        writeHandle("Geometry");
        writeSubClass("AcDbEntity");
        writeLayer(layer);
        writeSubClass("AcDbBlockReference");
        writeGroup(66, "     1");
        writeName(name);
        Geometry geometry = (Geometry) f.getDefaultGeometry();
        Point intPoint = geometry.getInteriorPoint();
        writePoint(intPoint.getX(), intPoint.getY(), 0.0);
        writeAttributes(layer, ownerHandle, f);
        writeGroup(0, "SEQEND");
        writeHandle("Geometry");
        // String handle = getNewHandle("AttDef");
        // writeGroup(5, handle);
        writeOwnerHandle(ownerHandle);
        writeSubClass("AcDbEntity");
        writeLayer(layer);
    }

    /** Writes all the given feature list associated blocks. */
    private void writeEntityBlocks(List featureList) throws IOException {
        for (Object coll : featureList) writeFeatureBlocks((FeatureCollection) coll);
    }

    /** Writes all the given feature collection associated blocks. */
    private void writeFeatureBlocks(FeatureCollection coll) throws IOException {
        LOGGER.warning("Rel14DXFWriter.writeFeatureBlocks");
        String layer = getLayerName(coll);

        FeatureIterator<SimpleFeature> iter = coll.features();
        try {
            while (iter.hasNext()) {
                SimpleFeature f = iter.next();
                String fid = f.getID();

                // consider only items cached to be treated as
                // blocks (by the previous block_record analysis)
                if (blockNames.containsKey(fid)) {
                    String ownerHandle = blockHandles.get(fid);
                    String name = blockNames.get(fid);
                    String startHandle = getNewHandle("Block");
                    String endHandle = getNewHandle("Block");
                    writeStartBlock(startHandle, ownerHandle, false, "0", name);
                    writeGeometry(layer, ownerHandle, (Geometry) f.getDefaultGeometry());
                    writeEndBlock(endHandle, ownerHandle, false, "0", name);
                }
            }
        } finally {
            iter.close();
        }
    }

    /** Writes all the given attribute definition blocks to be used for later INSERT entities */
    private void writeAttributeDefinitionBlocks(List<FeatureCollection> featureList)
            throws IOException {
        LOGGER.warning("Rel14DXFWriter.writeAttributeDefinitionBlocks");
        for (FeatureCollection coll : featureList) {

            // consider only items cached to be treated as
            // blocks (by the previous block_record analysis)
            if (blockNames.containsKey(coll.hashCode() + "")) {
                String ownerHandle = blockHandles.get(coll.hashCode() + "");
                String name = blockNames.get(coll.hashCode() + "");
                String startHandle = getNewHandle("Block");
                String endHandle = getNewHandle("Block");
                writeStartBlock(startHandle, ownerHandle, false, "0", name);
                String attributesLayer = getLayerName(coll) + "_attributes";
                writeGeometryStart("POINT", attributesLayer, ownerHandle);
                // writeGeometryStart("CIRCLE", attributesLayer, ownerHandle);
                writeSubClass("AcDbPoint");
                // writeSubClass("AcDbCircle");
                writePoint(0.0, 0.0, 0.0);
                // writeDoubleGroup(40, 1.0);
                writeAttributeDefinitions(attributesLayer, ownerHandle, coll);
                writeEndBlock(endHandle, ownerHandle, false, "0", name);
            }
        }
    }

    private void writeAttributeDefinitions(String layer, String ownerHandle, FeatureCollection fc)
            throws IOException {
        FeatureTypeImpl schema = (FeatureTypeImpl) fc.getSchema();
        for (PropertyDescriptor p : schema.getDescriptors()) {
            Name name = p.getName();
            LOGGER.warning("    attr: " + name.getLocalPart());
            if (!(p.getType() instanceof GeometryType)) {
                writeAttrDef(layer, ownerHandle, name.getLocalPart());
            }
        }
    }

    private void writeAttrDef(String layer, String ownerHandle, String attribName)
            throws IOException {
        // http://www.autodesk.com/techpubs/autocad/acad2000/dxf/attdef_dxf_06.htm
        writeGroup(0, "ATTDEF");
        writeHandle("Geometry");
        // String handle = getNewHandle("AttDef");
        // writeGroup(5, handle);
        writeOwnerHandle(ownerHandle);
        writeSubClass("AcDbEntity");
        writeLayer(layer);
        writeSubClass("AcDbText");
        writeDoubleGroup(10, 0.0);
        writeDoubleGroup(20, 0.0);
        writeDoubleGroup(30, 0.0);
        writeDoubleGroup(40, 2.0);
        writeGroup(1, "");
        writeSubClass("AcDbAttributeDefinition");
        writeGroup(3, attribName);
        writeGroup(2, attribName);
        writeGroup(70, "     0");
    }

    /** Writes a given geometry. */
    private void writeGeometry(String layer, String ownerHandle, Geometry geom) throws IOException {
        if (geom instanceof GeometryCollection) {
            GeometryCollection coll = (GeometryCollection) geom;
            for (int count = 0; count < coll.getNumGeometries(); count++)
                writeGeometry(layer, ownerHandle, coll.getGeometryN(count));
        } else if (geom instanceof Polygon) {
            Polygon p = (Polygon) geom;
            writeGeometry(layer, ownerHandle, p.getExteriorRing());
            for (int count = 0; count < p.getNumInteriorRing(); count++)
                writeGeometry(layer, ownerHandle, p.getInteriorRingN(count));
        } else if (geom instanceof LineString) {
            LineString l = (LineString) geom;
            Coordinate[] coords = l.getCoordinates();
            writePolylineGeometry(layer, ownerHandle, coords, false);
        } else if (geom instanceof Point) {
            Point p = (Point) geom;
            writePointGeometry(layer, ownerHandle, p);
        }
    }

    /** Writes a point geometry. */
    private void writePointGeometry(String layer, String ownerHandle, Point p) throws IOException {
        writeGeometryStart("POINT", layer, ownerHandle);
        writeSubClass("AcDbPoint");
        writePoint(p.getX(), p.getY(), 0.0);
    }

    /** Writes a polyline geometry. */
    private void writePolylineGeometry(
            String layer, String ownerHandle, Coordinate[] coords, boolean closed)
            throws IOException {
        writeGeometryStart("LWPOLYLINE", layer, ownerHandle);
        writeSubClass("AcDbPolyline");
        writeIntegerGroup(90, coords.length);
        writeDoubleGroup(43, 0.0);
        if (closed) writeIntegerGroup(70, 1);
        for (Coordinate coord : coords) writePoint(coord.x, coord.y, Double.NaN);
    }

    /** Writes the static model space block. */
    private void writeModelSpaceBlock() throws IOException {
        writeStartBlock("20", "1F", false, "0", "*MODEL_SPACE");
        writeEndBlock("21", "1F", false, "0", "*MODEL_SPACE");
    }

    /** Writes the static paper space block. */
    private void writePaperSpaceBlock() throws IOException {
        writeStartBlock("1C", "1B", true, "0", "*PAPER_SPACE");
        writeEndBlock("1D", "1B", true, "0", "*MODEL_SPACE");
    }

    /** Writes a start block section. */
    private void writeStartBlock(
            String handle, String ownerHandle, boolean paperSpace, String layer, String name)
            throws IOException {
        writeGroup(0, "BLOCK");
        writeGroup(5, handle);
        writeOwnerHandle(ownerHandle);
        writeSubClass("AcDbEntity");
        writeLayer(layer);
        if (paperSpace) writeIntegerGroup(67, 1);
        writeSubClass("AcDbBlockBegin");
        writeName(name);
        writeIntegerGroup(70, 0);
        writeIntegerGroup(71, 0);
        writePoint(0.0, 0.0, 0.0);
        writeGroup(3, name);
        writePath("");
    }

    /** Writes an end block section. */
    private void writeEndBlock(
            String handle, String ownerHandle, boolean paperSpace, String layer, String name)
            throws IOException {
        writeGroup(0, "ENDBLK");
        writeGroup(5, handle);
        writeOwnerHandle(ownerHandle);
        writeSubClass("AcDbEntity");
        if (paperSpace) writeIntegerGroup(67, 1);
        writeLayer(layer);
        writeSubClass("AcDbBlockEnd");
    }

    /** Writes block references table. */
    private void writeBlockRecords(List featureList) throws IOException {
        writeTableStart("BLOCK_RECORD");
        writeGroup(5, "1");
        writeOwnerHandle("0");
        writeSubClass("AcDbSymbolTable");
        // 2 fixed blocks (paper space and model space)
        // N dynamic blocks for complex geometries
        writeSize(2 + countBlocks(featureList));
        // writes the 2 fixed block references
        writeModelSpaceBlockRecord();
        writePaperSpaceBlockRecord();
        // write each dynamic block reference, saving handles
        // for further reference (they will be assigned as owner
        // handles for the real blocks)
        if (blockNames != null) {
            for (String fid : blockNames.keySet())
                blockHandles.put(fid, writeBlockRecord(blockNames.get(fid)));
        }
        writeTableEnd();
    }

    /** Writes a block reference, given the desired name. The handle is dinamically created. */
    private String writeBlockRecord(String blockName) throws IOException {
        String handle = getNewHandle("BlockRecord");
        writeBlockRecord(handle, "1", blockName);
        return handle;
    }

    /** Writes the model space fixed block reference. */
    private void writeModelSpaceBlockRecord() throws IOException {
        writeBlockRecord("1F", "1", "*MODEL_SPACE");
    }

    /** Writes the paper space fixed block reference. */
    private void writePaperSpaceBlockRecord() throws IOException {
        writeBlockRecord("1B", "1", "*PAPER_SPACE");
    }

    /** Writes a block reference, using given handles. */
    private void writeBlockRecord(String handle, String ownerHandle, String name)
            throws IOException {
        writeGroup(0, "BLOCK_RECORD");
        writeGroup(5, handle);
        writeOwnerHandle(ownerHandle);
        writeSubClass("AcDbSymbolTableRecord");
        writeSubClass("AcDbBlockTableRecord");
        writeName(name);
    }

    /** Builds a block list for the given feature list. */
    private int countBlocks(List featureList) {
        if (blockNames == null) {
            // initializes block cache
            // for names and handles
            blockNames = new HashMap<String, String>();
            blockHandles = new HashMap<String, String>();
            // cycle through feature to accumulate
            // blocks
            for (Object coll : featureList) addBlocks((FeatureCollection) coll);
        }
        return blockNames.size();
    }

    /** Add blocks for the given collection. */
    private void addBlocks(FeatureCollection coll) {
        FeatureIterator<SimpleFeature> iter = coll.features();
        try {
            while (iter.hasNext()) {
                SimpleFeature f = iter.next();
                Geometry geom = (Geometry) f.getDefaultGeometry();
                // if the geometry is complex, it will be
                // exported as a block; we use fid to cache
                // this for further using (blocks and entities
                // sections)
                if (geometryAsBlock || isBlockGeometry(geom)) {
                    LOGGER.warning("    added");
                    blockNames.put(f.getID(), (blockCounter++) + "");
                }
            }
        } finally {
            iter.close();
        }
        if (writeAttributes) {
            // add attribute definition blocks
            blockNames.put(coll.hashCode() + "", (blockCounter++) + "");
        }
    }

    /** Checks if a geometry is complex and should be exported as a block. */
    private boolean isBlockGeometry(Geometry geom) {
        if (geom != null) {
            // collections are exported as blocks
            // quick fix: generates a false reference to a block "0"
            /* if (GeometryCollection.class.isAssignableFrom(geom.getClass()))
            return true; */
            // polygons with holes are exported as blocks
            if (Polygon.class.isAssignableFrom(geom.getClass())) {
                return ((Polygon) geom).getNumInteriorRing() > 0;
            }
        }
        return false;
    }

    /** Writes the dimstyle table. */
    private void writeDimensionStyles() throws IOException {
        writeTableStart("DIMSTYLE");
        writeGroup(5, "A");
        writeOwnerHandle("0");
        writeSubClass("AcDbSymbolTable");
        writeSize(1);
        writeDimensionStyle("27", "A", "11", "STANDARD", 0);
        writeTableEnd();
    }

    /** Writes a dimstyle item. */
    private void writeDimensionStyle(
            String handle, String ownerHandle, String styleHandle, String name, int flags)
            throws IOException {
        writeGroup(0, "DIMSTYLE");
        writeGroup(105, handle);
        writeOwnerHandle(ownerHandle);
        writeSubClass("AcDbSymbolTableRecord");
        writeSubClass("AcDbDimStyleTableRecord");
        writeName(name);
        writeIntegerGroup(70, flags);
        writeGroup(3, "");
        writeGroup(4, "");
        writeGroup(5, "");
        writeGroup(6, "");
        writeGroup(7, "");
        writeDoubleGroup(40, 1.0);
        writeDoubleGroup(41, 0.18);
        writeLength(42, 0.0625);
        writeDoubleGroup(43, 0.38);
        writeDoubleGroup(44, 0.18);
        writeDoubleGroup(45, 0.0);
        writeDoubleGroup(46, 0.0);
        writeDoubleGroup(47, 0.0);
        writeDoubleGroup(48, 0.0);
        writeDoubleGroup(140, 0.18);
        writeDoubleGroup(141, 0.09);
        writeDoubleGroup(142, 0.0);
        writeDoubleGroup(143, 25.4);
        writeDoubleGroup(144, 1.0);
        writeDoubleGroup(145, 0.0);
        writeDoubleGroup(146, 1.0);
        writeDoubleGroup(147, 0.09);
        writeIntegerGroup(71, 0);
        writeIntegerGroup(72, 0);
        writeIntegerGroup(73, 1);
        writeIntegerGroup(74, 1);
        writeIntegerGroup(75, 0);
        writeIntegerGroup(76, 0);
        writeIntegerGroup(77, 0);
        writeIntegerGroup(78, 0);
        writeIntegerGroup(170, 0);
        writeIntegerGroup(171, 2);
        writeIntegerGroup(172, 0);
        writeIntegerGroup(173, 0);
        writeIntegerGroup(174, 0);
        writeIntegerGroup(175, 0);
        writeIntegerGroup(176, 0);
        writeIntegerGroup(177, 0);
        writeIntegerGroup(178, 0);
        writeIntegerGroup(270, 2);
        writeIntegerGroup(271, 4);
        writeIntegerGroup(272, 4);
        writeIntegerGroup(273, 2);
        writeIntegerGroup(274, 2);
        writeGroup(340, styleHandle);
        writeIntegerGroup(275, 0);
        writeIntegerGroup(280, 0);
        writeIntegerGroup(281, 0);
        writeIntegerGroup(282, 0);
        writeIntegerGroup(283, 1);
        writeIntegerGroup(284, 0);
        writeIntegerGroup(285, 0);
        writeIntegerGroup(286, 0);
        writeIntegerGroup(287, 3);
        writeIntegerGroup(288, 0);
    }

    /** Writes the view table. */
    private void writeView() throws IOException {
        writeTableStart("VIEW");
        writeGroup(5, "6");
        writeOwnerHandle("0");
        writeSubClass("AcDbSymbolTable");
        writeSize(0);
        writeTableEnd();
    }

    /** Writes the layer table. */
    private void writeLayers(List featureList) throws IOException {
        writeTableStart("LAYER");
        writeGroup(5, "2");
        writeOwnerHandle("0");
        writeSubClass("AcDbSymbolTable");
        writeSize(featureList.size() + 1);
        writeBgLayer();
        // write a layer for each feature collection
        for (Object coll : featureList) {
            writeLayer((FeatureCollection) coll);
        }
        if (writeAttributes) {
            for (Object coll : featureList) {
                writeAttributeLayer((FeatureCollection) coll);
            }
        }
        writeTableEnd();
    }

    /** Writes a layer with the given properties */
    private void writeLayerItem(
            String handle, String ownerHandle, String name, boolean frozen, int color, int ltype)
            throws IOException {
        writeGroup(0, "LAYER");
        writeGroup(5, handle);
        writeOwnerHandle(ownerHandle);
        writeSubClass("AcDbSymbolTableRecord");
        writeSubClass("AcDbLayerTableRecord");
        // layer name
        writeName(name);
        // flags
        writeIntegerGroup(70, frozen ? 2 : 0);
        // color
        if (color != -1) writeColor(color);
        // line type
        if (ltype != -1) writeLineType(ltype);
    }

    /** Writes the background ("0") layer. */
    private void writeBgLayer() throws IOException {
        writeLayerItem("10", "2", "0", false, 7, 0);
    }

    /** Writes a layer for the given featurecollection. */
    private void writeLayer(FeatureCollection coll) throws IOException {
        writeLayerItem(
                getNewHandle("Layer"),
                "2",
                getLayerName(coll),
                false,
                getColor(coll),
                getLineType(coll));
    }

    /** Writes a layer for the given featurecollection. */
    private void writeAttributeLayer(FeatureCollection coll) throws IOException {
        String attributesLayer = getLayerName(coll) + "_attributes";
        writeLayerItem(
                getNewHandle("Layer"),
                "2",
                attributesLayer,
                false,
                getColor(coll),
                getLineType(coll));
    }

    /** Writes the line types table. */
    private void writeLineTypes() throws IOException {
        writeTableStart("LTYPE");
        writeGroup(5, "5");
        writeOwnerHandle("0");
        writeSubClass("AcDbSymbolTable");
        writeSize(lineTypes.length);
        writeLineType("14", "5", "BYBLOCK", "", 0.0, new LineTypeItem[] {});
        writeLineType("15", "5", "BYLAYER", "", 0.0, new LineTypeItem[] {});
        for (LineType ltype : lineTypes)
            writeLineType(
                    getNewHandle("LType"),
                    "5",
                    ltype.getName(),
                    ltype.getDescription(),
                    ltype.getLength(),
                    ltype.getItems());
        writeTableEnd();
    }

    /** Writes a LineType definition. */
    private void writeLineType(
            String handle,
            String ownerHandle,
            String name,
            String description,
            double length,
            LineTypeItem[] items)
            throws IOException {
        writeGroup(0, "LTYPE");
        writeGroup(5, handle);
        writeOwnerHandle(ownerHandle);
        writeSubClass("AcDbSymbolTableRecord");
        writeSubClass("AcDbLinetypeTableRecord");
        writeName(name);
        writeIntegerGroup(70, 0);
        writeGroup(3, description);
        writeIntegerGroup(72, 65);
        writeIntegerGroup(73, items.length);
        writeLength(40, length);
        for (LineTypeItem item : items) {
            writeLength(49, item.getLength());
            writeIntegerGroup(74, 0);
        }
    }

    /** Writes the appid tables. */
    private void writeApplications() throws IOException {
        writeTableStart("APPID");
        writeGroup(5, "9");
        writeOwnerHandle("0");
        writeSubClass("AcDbSymbolTable");
        writeSize(1);
        writeApplication("12", "9", "ACAD");
        writeTableEnd();
    }

    /** Writes the default ACAD appid. */
    private void writeApplication(String handle, String ownerHandle, String name)
            throws IOException {
        writeGroup(0, "APPID");
        writeGroup(5, handle);
        writeOwnerHandle(ownerHandle);
        writeSubClass("AcDbSymbolTableRecord");
        writeSubClass("AcDbRegAppTableRecord");
        // app name
        writeName(name);
        // flags
        writeIntegerGroup(70, 0);
    }

    /** Writes the UCS table. */
    private void writeUCS() throws IOException {
        writeTableStart("UCS");
        writeGroup(5, "7");
        writeOwnerHandle("0");
        writeSubClass("AcDbSymbolTable");
        writeSize(0);
        writeTableEnd();
    }

    /** Writes the styles table. */
    private void writeStyles() throws IOException {
        writeTableStart("STYLE");
        writeGroup(5, "3");
        writeOwnerHandle("0");
        writeSubClass("AcDbSymbolTable");
        writeSize(1);
        writeStyleItem("11", "3", "STANDARD", 0);
        writeTableEnd();
    }

    /** Writes a style item. */
    private void writeStyleItem(String handle, String ownerHandle, String name, int flags)
            throws IOException {
        writeGroup(0, "STYLE");
        writeGroup(5, handle);
        writeOwnerHandle(ownerHandle);
        writeSubClass("AcDbSymbolTableRecord");
        writeSubClass("AcDbTextStyleTableRecord");
        writeName(name);
        writeIntegerGroup(70, flags);
        writeDoubleGroup(40, 0.0);
        writeDoubleGroup(41, 1.0);
        writeDoubleGroup(50, 0.0);
        writeIntegerGroup(71, 0);
        writeDoubleGroup(42, 0.2);
        writeGroup(3, "txt");
        writeGroup(4, "");
    }

    /** Writes the viewport table. */
    private void writeViewPort(List featureList) throws IOException {
        writeTableStart("VPORT");
        writeGroup(5, "8");
        writeOwnerHandle("0");
        writeSubClass("AcDbSymbolTable");
        writeSize(1);
        writeViewPortItem("*ACTIVE", featureList);
        writeTableEnd();
    }

    /** Writes a viewport framing the given feature list. */
    private void writeViewPortItem(String name, List featureList) throws IOException {
        writeGroup(0, "VPORT");
        writeHandle("VPort");
        writeSubClass("AcDbSymbolTableRecord");
        writeSubClass("AcDbViewportTableRecord");
        writeName(name);

        // flags
        writeIntegerGroup(70, 0);
        // lower-left point
        writePoint(10, 0.0, 0.0, Double.NaN);
        // upper right point
        writePoint(11, 1.0, 1.0, Double.NaN);
        ReferencedEnvelope env = getEnvelope(featureList);
        // center point
        writePoint(12, env.getMedian(0), env.getMedian(1), Double.NaN);
        // snap point
        writePoint(13, 0.0, 0.0, Double.NaN);
        // snap spacing
        writePoint(14, 0.5, 0.5, Double.NaN);
        // grid spacing
        writePoint(15, 0.5, 0.5, Double.NaN);
        // view direction
        writePoint(16, 0.0, 0.0, 1.0);
        // view target point
        writePoint(17, 0.0, 0.0, 0.0);
        // view height
        writeDoubleGroup(40, env.getHeight());
        // view aspect ratio
        writeDoubleGroup(41, env.getWidth() / env.getHeight());
        // lens length
        writeDoubleGroup(42, 50.0);
        // front clipping plane
        writeDoubleGroup(43, 0.0);
        // back clipping plane
        writeDoubleGroup(44, 0.0);
        // snap rotation angle
        writeDoubleGroup(50, 0.0);
        // view twist angle
        writeDoubleGroup(51, 0.0);
        // view mode
        writeIntegerGroup(71, 0);
        // circle zoom percent
        writeIntegerGroup(72, 100);
        // fast zoom setting
        writeIntegerGroup(73, 1);
        // ucs icon
        writeIntegerGroup(74, 3);
        // snap on/off
        writeIntegerGroup(75, 0);
        // grid on/off
        writeIntegerGroup(76, 0);
        // snap style
        writeIntegerGroup(77, 0);
        // snap isopair
        writeIntegerGroup(78, 0);
    }

    /** Writes variables for the header section. */
    protected void writeVariables(List featureList) throws IOException {
        // version
        writeVariable("ACADVER");
        writeGroup(1, version);
        // internal version
        writeVariable("ACADMAINTVER");
        writeIntegerGroup(70, 9);
        // codepage
        writeVariable("DWGCODEPAGE");
        // encoding
        writeGroup(3, encoding);
        // insertion point
        writeVariable("INSBASE");
        writePoint(0.0, 0.0, 0.0);
        // extracts global envelope
        ReferencedEnvelope e = getEnvelope(featureList);
        // drawing extension
        if (e != null) {
            writeVariable("EXTMIN");
            writePoint(e.getMinX(), e.getMinY(), 0.0);
            writeVariable("EXTMAX");
            writePoint(e.getMaxX(), e.getMaxY(), 0.0);
        }
        // creation/update dates
        writeVariable("TDCREATE");
        writeJulianDate((new GregorianCalendar()).getTime());
        writeVariable("TDUPDATE");
        writeJulianDate((new GregorianCalendar()).getTime());
        // fixed variables, read from resource
        loadFromResource("header");
    }

    /** Description for the writer. */
    public String getDescription() {
        return "DXF Release 14";
    }
}

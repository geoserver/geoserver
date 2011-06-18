package org.geoserver.wfs.response;

import java.io.IOException;
import java.io.Writer;
import java.util.Calendar;
import java.util.Date;

import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.util.CellReference;
import org.apache.xerces.util.XMLChar;

/**
 * Spreadsheet writer to directly generate OOXML spreadsheets instead of builing them in the Apache
 * POI object model, which eats up tons of memory. Taken from existing example by Yegor Kozlov
 * 
 * @see <a
 *      href="http://svn.apache.org/repos/asf/poi/trunk/src/examples/src/org/apache/poi/xssf/usermodel/examples/BigGridDemo.java">Code
 *      by Yegor Kozlov</a>
 * 
 * @author Shane StClair, Axiom Consulting and Design, shane@axiomalaska.com
 */
public class SpreadsheetWriter {
    private final Writer _out;

    private int _rownum;

    private String xmlEncoding = "UTF-8";

    public SpreadsheetWriter(Writer out) {
        _out = out;
    }

    public SpreadsheetWriter(Writer out, String xmlEncoding) {
        _out = out;
        this.xmlEncoding = xmlEncoding;
    }

    public void beginSheet() throws IOException {
        _out.write("<?xml version=\"1.0\" encoding=\"" + xmlEncoding + "\"?>"
                + "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">");
        _out.write("<sheetData>\n");
    }

    public void endSheet() throws IOException {
        _out.write("</sheetData>");
        _out.write("</worksheet>");
    }

    /**
     * Insert a new row
     * 
     * @param rownum
     *            0-based row number
     */
    public void insertRow(int rownum) throws IOException {
        _out.write("<row r=\"" + (rownum + 1) + "\">\n");
        this._rownum = rownum;
    }

    /**
     * Insert row end marker
     */
    public void endRow() throws IOException {
        _out.write("</row>\n");
    }

    public void createCell(int columnIndex, String value, int styleIndex) throws IOException {
        String ref = new CellReference(_rownum, columnIndex).formatAsString();
        _out.write("<c r=\"" + ref + "\" t=\"inlineStr\"");
        if (styleIndex != -1)
            _out.write(" s=\"" + styleIndex + "\"");
        _out.write(">");
        _out.write("<is><t>" + santizeForXml(value) + "</t></is>");
        _out.write("</c>");
    }

    public void createCell(int columnIndex, String value) throws IOException {
        createCell(columnIndex, value, -1);
    }

    public void createCell(int columnIndex, double value, int styleIndex) throws IOException {
        String ref = new CellReference(_rownum, columnIndex).formatAsString();
        _out.write("<c r=\"" + ref + "\" t=\"n\"");
        if (styleIndex != -1)
            _out.write(" s=\"" + styleIndex + "\"");
        _out.write(">");
        _out.write("<v>" + value + "</v>");
        _out.write("</c>");
    }

    public void createCell(int columnIndex, double value) throws IOException {
        createCell(columnIndex, value, -1);
    }

    public void createCell(int columnIndex, boolean value) throws IOException {
        createCell(columnIndex, value, -1);
    }

    public void createCell(int columnIndex, boolean value, int styleIndex) throws IOException {
        String ref = new CellReference(_rownum, columnIndex).formatAsString();
        _out.write("<c r=\"" + ref + "\" t=\"b\"");
        if (styleIndex != -1)
            _out.write(" s=\"" + styleIndex + "\"");
        _out.write(">");
        _out.write("<v>" + (value ? 1 : 0) + "</v>");
        _out.write("</c>");
    }

    public void createCell(int columnIndex, Calendar value, int styleIndex) throws IOException {
        createCell(columnIndex, DateUtil.getExcelDate(value, false), styleIndex);
    }

    public void createCell(int columnIndex, Date value, int styleIndex) throws IOException {
        createCell(columnIndex, DateUtil.getExcelDate(value, false), styleIndex);
    }

    private String cData(String str) {
        return "<![CDATA[" + str + "]]>";
    }

    private String santizeForXml(String str) {
        StringBuilder strBuilder = new StringBuilder();

        boolean stringHasSpecial = false;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (!XMLChar.isInvalid(c)) {
                strBuilder.append(c);
                stringHasSpecial = stringHasSpecial || charIsSpecial(c);
            }
        }

        if (stringHasSpecial) {
            return cData(strBuilder.toString());
        }

        return strBuilder.toString();
    }

    private boolean charIsSpecial(char c) {
        if (c == '&' || c == '<' || c == '>')
            return true;
        return false;
    }
}
package org.geoserver.wfs.response;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import net.opengis.wfs.FeatureCollectionType;

import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

/**
 * Excel 2007 WFS output format
 * 
 * @author Shane StClair, Axiom Consulting, shane@axiomalaska.com
 */
public class Excel2007OutputFormat extends ExcelOutputFormat {
    private static Logger log = Logger.getLogger(Excel2007OutputFormat.class);

    private static final String XML_ENCODING = "UTF-8";

    /**
     * Constructor setting the format type as "excel2007" in addition to file extension, mime type,
     * and row and column limits
     * 
     * @param gs
     */
    public Excel2007OutputFormat(GeoServer gs) {
        super(gs, "excel2007");
        rowLimit = (int) Math.pow(2, 20); // 1,048,576
        colLimit = (int) Math.pow(2, 14); // 16,384
        fileExtension = "xlsx";
        mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    }

    /**
     * Returns a new XSSFWorkbook workbook
     */
    @Override
    protected Workbook getNewWorkbook() {
        return new XSSFWorkbook();
    }

    /**
     * We override the write method of the ExcelOutputFormat base class here as a workaround. In a
     * perfect world, we could use the same code for generating Excel 97 (binary) and Excel
     * 2007/OOXML (xml based) files. In reality, generating OOXML spreadsheets with Apache POI uses
     * a lot of memory overhead (3.5 GB for 80,000 rows in one test).
     * 
     * For now, we use a workaround by Yegor Kozlov that creates a template workbook, writes rows
     * direclty to an XML temp file, and then slices the temp file into the workbook.
     * 
     * If POI's memory performance when creating OOXML spreadsheet improves in future versions, we
     * should be able to just remove this override method to use the ss usermodel (ideal).
     * 
     * @see <a href=
     *      "http://svn.apache.org/repos/asf/poi/trunk/src/examples/src/org/apache/poi/xssf/usermodel/examples/BigGridDemo.java"
     *      >Yegor Kozlov's code</a>.
     */
    @Override
    protected void write(FeatureCollectionType featureCollection, OutputStream output,
            Operation getFeature) throws IOException, ServiceException {
        // Create the workbook
        Workbook wb = getNewWorkbook();
        ExcelCellStyles styles = new ExcelCellStyles(wb);

        Map<String, File> sheetMap = new HashMap<String, File>();

        for (Iterator it = featureCollection.getFeature().iterator(); it.hasNext();) {
            SimpleFeatureCollection fc = (SimpleFeatureCollection) it.next();

            // create the sheet for this feature collection
            Sheet sheet = wb.createSheet(fc.getSchema().getTypeName());
            String sheetName = ((XSSFSheet) sheet).getPackagePart().getPartName().getName();
            File rawSheet = File.createTempFile("Excel2007TempSheet", ".xml");
            sheetMap.put(sheetName, rawSheet);

            Writer rawWriter = new OutputStreamWriter(new FileOutputStream(rawSheet), XML_ENCODING);
            SpreadsheetWriter sw = new SpreadsheetWriter(rawWriter, XML_ENCODING);
            sw.beginSheet();

            // write out the header
            sw.insertRow(0);
            int headerStyleIndex = styles.getHeaderStyle().getIndex();
            SimpleFeatureType ft = fc.getSchema();
            sw.createCell(0, "FID");
            for (int i = 0; i < ft.getAttributeCount() && i < colLimit; i++) {
                AttributeDescriptor ad = ft.getDescriptor(i);
                sw.createCell(i + 1, ad.getLocalName(), headerStyleIndex);
            }
            sw.endRow();

            // write out the features
            SimpleFeatureIterator i = fc.features();
            int r = 0; // row index
            try {
                while (i.hasNext()) {
                    r++; // start at 1, since header is at 0
                    sw.insertRow(r);

                    if (r == (rowLimit - 1) && i.hasNext()) {
                        // there are more features than rows available in this
                        // Excel format. write out a warning line and break
                        String rowWarning = TRUNCATE_WARNING + ": ROWS " + r + " - " + fc.size()
                                + " NOT SHOWN";
                        sw.createCell(0, rowWarning);
                        sw.endRow();
                        break;
                    }

                    SimpleFeature f = i.next();
                    sw.createCell(0, f.getID());
                    for (int j = 0; j < f.getAttributeCount() && j < colLimit; j++) {
                        Object att = f.getAttribute(j);
                        if (att != null) {
                            if (att instanceof Number) {
                                sw.createCell(j + 1, ((Number) att).doubleValue());
                            } else if (att instanceof Date) {
                                sw.createCell(j + 1, (Date) att, styles.getDateStyle().getIndex());
                            } else if (att instanceof Calendar) {
                                sw.createCell(j + 1, (Calendar) att, styles.getDateStyle()
                                        .getIndex());
                            } else if (att instanceof Boolean) {
                                sw.createCell(j + 1, (Boolean) att);
                            } else {
                                // ok, it seems we have no better way than dump it as a string
                                String stringVal = att.toString();

                                // if string length > excel cell limit, truncate it and warn the
                                // user, otherwise excel workbook will be corrupted
                                if (stringVal.length() > CELL_CHAR_LIMIT) {
                                    stringVal = TRUNCATE_WARNING
                                            + " "
                                            + stringVal.substring(0, CELL_CHAR_LIMIT
                                                    - TRUNCATE_WARNING.length() - 1);
                                    sw.createCell(j + 1, stringVal, styles.getWarningStyle()
                                            .getIndex());
                                } else {
                                    sw.createCell(j + 1, stringVal);
                                }
                            }
                        }
                    }
                    sw.endRow();
                }
            } finally {
                fc.close(i);
            }

            sw.endSheet();
            rawWriter.close();
        }

        // save the template
        File template = File.createTempFile("Excel2007TempTemplate", ".xlsx");
        FileOutputStream os = new FileOutputStream(template);
        wb.write(os);
        os.close();

        // swap out sheets
        for (Entry<String, File> sheetEntry : sheetMap.entrySet()) {
            String sheetName = sheetEntry.getKey();
            File tempXml = sheetEntry.getValue();
            File newTemplate = File.createTempFile("Excel2007TempTemplate", ".xlsx");
            FileOutputStream out = new FileOutputStream(newTemplate);
            BigGridUtil.substitute(template, tempXml, sheetName.substring(1), out);
            out.close();
            template = newTemplate;
        }

        // stream generated file to output
        FileInputStream fis = new FileInputStream(template);
        BigGridUtil.copyStream(fis, output);
        fis.close();
    }
}

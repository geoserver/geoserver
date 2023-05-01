/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test.onlineTest.setup;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.geotools.data.property.PropertyFeatureReader;
import org.geotools.geopkg.geom.GeoPkgGeomWriter;
import org.geotools.util.Classes;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.IllegalAttributeException;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.filter.identity.FeatureId;

/** Geopackage data setup for app-schema-test with online mode. */
public class AppSchemaTestGeopackageSetup extends ReferenceDataGeopackageSetup {

    // predefined sridList in the gpkg_spatial_ref_sys table.
    private List<Integer> sridList = Arrays.asList(4326, -1, 0);

    /** Mapping file database parameters */
    public static String DB_PARAMS =
            "<parameters>"
                    + "\n<Parameter>\n"
                    + "<name>dbtype</name>\n"
                    + "<value>geopkg</value>"
                    + "\n</Parameter>"
                    + "\n<Parameter>\n"
                    + "<name>database</name>\n"
                    + "<value>PATH_TO_BE_REPLACED</value>\n"
                    + "</Parameter>\n"
                    + "<Parameter>\n"
                    + "<name>Expose primary keys</name>\n"
                    + "<value>true</value>"
                    + "\n</Parameter>"
                    + "</parameters>"; //

    private String sql;

    /**
     * Ensure the app-schema properties file is loaded with the database parameters. Also create
     * corresponding tables on the database based on data from properties files.
     *
     * @param propertyFiles Property file name and its feature type directory map
     * @param geopkgDir geopkg file path
     */
    public static AppSchemaTestGeopackageSetup getInstance(
            Map<String, File> propertyFiles, String geopkgDir) throws Exception {
        return new AppSchemaTestGeopackageSetup(propertyFiles, geopkgDir);
    }

    /**
     * Factory method.
     *
     * @param propertyFiles Property file name and its parent directory map
     * @param geopkgDir geopkg file path
     */
    public AppSchemaTestGeopackageSetup(Map<String, File> propertyFiles, String geopkgDir)
            throws Exception {
        this.geopkgDir = geopkgDir;
        configureFixture();
        createTables(propertyFiles);
    }

    /**
     * Write SQL string to create tables in the test database based on the property files.
     *
     * @param propertyFiles Property files from app-schema-test suite.
     */
    private void createTables(Map<String, File> propertyFiles)
            throws IllegalAttributeException, NoSuchElementException, IOException {
        StringBuffer buf = new StringBuffer();
        buf.append("DELETE FROM gpkg_spatial_ref_sys where srs_id not in (4326, -1, 0);\n");
        List<GeometryDescriptor> geoms;
        for (String fileName : propertyFiles.keySet()) {
            geoms = new ArrayList<>();
            File file = new File(propertyFiles.get(fileName), fileName);
            try (PropertyFeatureReader reader = new PropertyFeatureReader("test", file)) {
                SimpleFeatureType schema = reader.getFeatureType();
                String tableName = schema.getName().getLocalPart().toUpperCase();
                removeGeometryColumnsFromTable(tableName, buf);
                // create the table
                buf.append("DROP TABLE IF EXISTS ").append(tableName).append(";\n");
                buf.append("CREATE TABLE ").append(tableName).append(" (");

                int size = schema.getAttributeCount() + 1;
                String[] fieldNames = new String[size];
                List<String> createParams = new ArrayList<>();
                int j = 0;
                String field;
                String type;
                for (PropertyDescriptor desc : schema.getDescriptors()) {
                    if (desc instanceof GeometryDescriptor) {
                        geoms.add((GeometryDescriptor) desc);
                    } else {
                        field = "\"" + desc.getName() + "\" ";
                        type = Classes.getShortName(desc.getType().getBinding());
                        if (type.equalsIgnoreCase("String")) {
                            type = "TEXT";
                        } else if (type.equalsIgnoreCase("Double")) {
                            type = "DOUBLE PRECISION";
                        }
                        field += type;
                        createParams.add(field);
                    }
                    fieldNames[j] = desc.getName().toString();
                    j++;
                }
                // Add numeric PK for sorting
                String pkFieldName = schema.getTypeName() + "_PKEY";
                fieldNames[j] = pkFieldName;
                createParams.add("\"" + pkFieldName + "\" TEXT");
                buf.append(StringUtils.join(createParams.iterator(), ", "));
                buf.append(", PRIMARY KEY (").append(pkFieldName).append("));\n");

                // add geometry columns
                geoms = geoms.stream().distinct().collect(Collectors.toList());
                int count = 0;
                for (GeometryDescriptor geom : geoms) {
                    String geomColumnName = geom.getName().toString();
                    buf.append(
                            "ALTER TABLE "
                                    + tableName
                                    + " add "
                                    + "\""
                                    + geomColumnName
                                    + "\""
                                    + " GEOMETRY;\n");
                    // does not support multiple geometry columns in the same table
                    if (count == 0) {
                        addGeometryColumnsToTable(tableName, buf, geomColumnName, geom);
                        count++;
                    }
                }

                // this block of code exists to not fail from GeopkgDialect.includeTable() function
                if (geoms.isEmpty()) {
                    buf.append(
                            "INSERT INTO gpkg_contents (table_name, data_type, identifier, srs_id) VALUES "
                                    + "('"
                                    + tableName
                                    + "', 'features', '"
                                    + tableName
                                    + "', 4326);\n");
                }

                // then insert rows
                SimpleFeature feature;
                FeatureId id;
                while (reader.hasNext()) {
                    buf.append("INSERT INTO ").append(tableName).append(" (\"");
                    feature = reader.next();
                    moveGeometryColumnInFieldNames(geoms, fieldNames);
                    buf.append(StringUtils.join(fieldNames, "\", \""));
                    buf.append("\") ");
                    buf.append("VALUES (");
                    Collection<Property> properties = feature.getProperties();
                    String[] values = new String[size];
                    int valueIndex = 0;
                    for (Property prop : properties) {
                        Object value = prop.getValue();
                        if (value instanceof Geometry) {
                            Geometry geom = (Geometry) value;

                            String s = toString(geom);

                            String geomValue = "GEOM_INDEX" + "X'" + s + "'";
                            values[valueIndex] = geomValue;
                            value = geomValue;
                        }
                        if (value == null || value.toString().equalsIgnoreCase("null")) {
                            values[valueIndex] = "null";
                        } else {
                            values[valueIndex] = "'" + value + "'";
                        }
                        valueIndex++;
                    }

                    id = feature.getIdentifier();
                    values[valueIndex] = "'" + id.toString() + "'";
                    moveGeometryValueInValues(values);
                    buf.append(StringUtils.join(values, ","));
                    buf.append(");\n");
                }
            }
            if (buf.length() > 0) {
                this.sql = buf.toString();
            }
        }
    }

    /** Since we can add column only to the last index, we need to move other columns too. */
    private void moveGeometryColumnInFieldNames(
            List<GeometryDescriptor> geoms, String[] fieldNames) {
        int geomIndex;
        ArrayList<String> fieldNamesList = new ArrayList<>(Arrays.asList(fieldNames));
        String geometryName = "";
        if (geoms.size() > 1) {
            for (String fieldName : fieldNamesList) {
                for (GeometryDescriptor geom : geoms) {
                    if (geom.getName().toString().equals(fieldName)) {
                        geometryName = fieldName;
                    }
                }
            }
        } else if (geoms.size() == 1) {
            geometryName = geoms.get(0).getName().toString();
        }

        if (fieldNamesList.contains(geometryName)) {
            geomIndex = fieldNamesList.indexOf(geometryName);
            String temp = fieldNames[geomIndex];
            fieldNames[geomIndex] = fieldNames[fieldNames.length - 1];
            fieldNames[fieldNames.length - 1] = temp;
        }
    }

    private void moveGeometryValueInValues(String[] values) {
        Optional<String> optGeomString =
                Arrays.stream(values).filter(value -> value.startsWith("'GEOM_INDEX")).findFirst();
        if (optGeomString.isPresent()) {
            String geomString = optGeomString.get();
            ArrayList<String> valuesList = new ArrayList<>(Arrays.asList(values));
            int index = valuesList.indexOf(geomString);
            String temp = values[index].substring(11, values[index].length() - 1);
            values[index] = values[values.length - 1];
            values[values.length - 1] = temp;
        }
    }

    @Override
    protected void runSqlInsertScript() throws Exception {
        this.run(sql, false);
    }

    String toString(Geometry g) throws IOException {
        byte[] bytes = new GeoPkgGeomWriter().write(g);
        return toHexString(bytes);
    }

    /** Convert geom to a hex string for saving to the DB. */
    public static String toHexString(byte[] bytes) {
        final char[] hexArray = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
        };
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    /** Clear the gpkg related tables. */
    private void removeGeometryColumnsFromTable(String tableName, StringBuffer buf) {
        buf.append("DELETE FROM gpkg_contents where table_name ='" + tableName + "';\n");
        buf.append("DELETE FROM gpkg_extensions where table_name ='" + tableName + "';\n");
        buf.append("DELETE FROM gpkg_geometry_columns where table_name ='" + tableName + "';\n");
    }

    /** Adding to the geopackage tables for geometry definition. */
    private void addGeometryColumnsToTable(
            String tableName, StringBuffer buf, String columnName, GeometryDescriptor geom) {
        int srid = getSrid(geom.getType());
        buf.append(
                "INSERT INTO gpkg_contents (table_name, data_type, identifier, srs_id) VALUES "
                        + "('"
                        + tableName
                        + "', 'features', '"
                        + tableName
                        + "', "
                        + srid
                        + ");\n");
        buf.append(
                "INSERT INTO gpkg_geometry_columns VALUES ('"
                        + tableName
                        + "', '"
                        + columnName
                        + "', 'GEOMETRY', "
                        + srid
                        + " , 2, 0);\n");

        buf.append(
                "INSERT INTO gpkg_extensions VALUES('"
                        + tableName
                        + "', '"
                        + columnName
                        + "', 'gpkg_rtree_index', 'http://www.geopackage.org/spec120/#extension_rtree', 'write-only');\n");

        if (!sridList.contains(srid)) {
            buf.append(
                    "INSERT INTO gpkg_spatial_ref_sys VALUES('"
                            + tableName
                            + "', '"
                            + srid
                            + "', 'EPSG"
                            + "', '"
                            + srid
                            + "', '"
                            + tableName
                            + "', '"
                            + tableName
                            + "' );\n");
        }
    }
}

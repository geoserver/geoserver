/*
 * GeoTools - The Open Source Java GIS Toolkit http://geotools.org
 *
 * (C) 2008, Open Source Geospatial Foundation (OSGeo)
 *
 * This library is free software; you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation; version 2.1 of
 * the License.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */
package org.geotools.gce.pgraster.config;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.geotools.data.jdbc.datasource.DBCPDataSourceFactory;
import org.geotools.data.jdbc.datasource.JNDIDataSourceFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Class for holding the config info read from the xml config file
 *
 * @author mcr
 */
public class Config {
    private static Map<String, Config> configMap = new HashMap<>();

    // is
    // synchronized
    private String xmlUrl;

    private String coverageName;

    private String geoRasterAttribute;

    private String coordsys;

    private SpatialExtension spatialExtension;

    private String dstype;

    private String username;

    private String password;

    private String jdbcUrl;

    private String driverClassName;

    private Integer maxActive;

    private Integer maxIdle;

    private String jndiReferenceName;

    private String coverageNameAttribute;

    private String blobAttributeNameInTileTable;

    private String keyAttributeNameInTileTable;

    private String keyAttributeNameInSpatialTable;

    private String geomAttributeNameInSpatialTable;

    private String maxXAttribute;

    private String maxYAttribute;

    private String minXAttribute;

    private String minYAttribute;

    private String masterTable;

    private String resXAttribute;

    private String resYAttribute;

    private String tileTableNameAtribute;

    private String spatialTableNameAtribute;

    private String sqlUpdateMosaicStatement;

    private String sqlSelectCoverageStatement;

    private String sqlUpdateResStatement;

    private Boolean verifyCardinality;

    private Boolean ignoreAxisOrder;

    private Integer interpolation;

    private String tileMaxXAttribute;

    private String tileMaxYAttribute;

    private String tileMinXAttribute;

    private String tileMinYAttribute;

    private String jdbcAccessClassName;

    protected Config() {}

    public static Config readFrom(URL xmlURL) throws Exception {
        Config result = configMap.get(xmlURL.toString());

        if (result != null) {
            return result;
        }

        Document dom = getDocument(xmlURL);

        result = new Config();

        result.xmlUrl = xmlURL.toString();

        result.dstype = readValueString(dom, "dstype");
        result.username = readValueString(dom, "username");
        result.password = readValueString(dom, "password");
        result.jdbcUrl = readValueString(dom, "jdbcUrl");
        result.driverClassName = readValueString(dom, "driverClassName");
        result.jndiReferenceName = readValueString(dom, "jndiReferenceName");
        result.maxActive = readValueInteger(dom, "maxActive");
        result.maxIdle = readValueInteger(dom, "maxIdle");

        result.coordsys = readNameString(dom.getDocumentElement(), "coordsys");
        result.coverageName = readNameString(dom.getDocumentElement(), "coverageName");

        Node tmp = dom.getElementsByTagName("scaleop").item(0);
        NamedNodeMap map = tmp.getAttributes();
        String s = map.getNamedItem("interpolation").getNodeValue();
        result.interpolation = Integer.valueOf(s);

        result.ignoreAxisOrder = Boolean.FALSE;
        tmp = dom.getElementsByTagName("axisOrder").item(0);
        if (tmp != null) {
            map = tmp.getAttributes();
            s = map.getNamedItem("ignore").getNodeValue();
            result.ignoreAxisOrder = Boolean.valueOf(s);
        }

        // db mapping
        String name = readNameString(dom.getDocumentElement(), "spatialExtension");
        Objects.requireNonNull(name, "spatialExtension is null");
        result.spatialExtension = SpatialExtension.valueOf(name.toUpperCase());
        readMapping(result, dom);
        result.initStatements();
        configMap.put(xmlURL.toString(), result);

        return result;
    }

    public static Document getDocument(URL xmlURL)
            throws IOException, ParserConfigurationException, SAXException {
        try (InputStream in = xmlURL.openStream()) {
            InputSource input = new InputSource(xmlURL.toString());

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setIgnoringElementContentWhitespace(true);
            dbf.setIgnoringComments(true);

            DocumentBuilder db = dbf.newDocumentBuilder();

            // db.setEntityResolver(new ConfigEntityResolver(xmlURL));
            return db.parse(input);
        }
    }

    static void readMapping(Config result, Document dom) {
        result.masterTable = readNameString(dom.getDocumentElement(), "masterTable");

        Element masterTableElem = (Element) dom.getElementsByTagName("masterTable").item(0);
        result.coverageNameAttribute = readNameString(masterTableElem, "coverageNameAttribute");

        result.maxXAttribute = readNameString(masterTableElem, "maxXAttribute");
        result.maxYAttribute = readNameString(masterTableElem, "maxYAttribute");
        result.minXAttribute = readNameString(masterTableElem, "minXAttribute");
        result.minYAttribute = readNameString(masterTableElem, "minYAttribute");
        result.resXAttribute = readNameString(masterTableElem, "resXAttribute");
        result.resYAttribute = readNameString(masterTableElem, "resYAttribute");

        result.tileTableNameAtribute =
                readNameString(masterTableElem, "tileTableNameAtribute"); // typo
        if (result.tileTableNameAtribute == null)
            result.tileTableNameAtribute =
                    readNameString(masterTableElem, "tileTableNameAttribute"); // correct name

        result.spatialTableNameAtribute =
                readNameString(masterTableElem, "spatialTableNameAtribute"); // typo
        if (result.spatialTableNameAtribute == null)
            result.spatialTableNameAtribute =
                    readNameString(masterTableElem, "spatialTableNameAttribute"); // correct name

        Element tileTableElem = (Element) dom.getElementsByTagName("tileTable").item(0);
        if (tileTableElem != null) {
            result.blobAttributeNameInTileTable =
                    readNameString(tileTableElem, "blobAttributeName");
            result.keyAttributeNameInTileTable = readNameString(tileTableElem, "keyAttributeName");
        }

        Element spatialTableElem = (Element) dom.getElementsByTagName("spatialTable").item(0);
        if (spatialTableElem != null) {
            result.keyAttributeNameInSpatialTable =
                    readNameString(spatialTableElem, "keyAttributeName");
            result.geomAttributeNameInSpatialTable =
                    readNameString(spatialTableElem, "geomAttributeName");
            result.tileMaxXAttribute = readNameString(spatialTableElem, "tileMaxXAttribute");
            result.tileMaxYAttribute = readNameString(spatialTableElem, "tileMaxYAttribute");
            result.tileMinXAttribute = readNameString(spatialTableElem, "tileMinXAttribute");
            result.tileMinYAttribute = readNameString(spatialTableElem, "tileMinYAttribute");
        }

        result.verifyCardinality = Boolean.FALSE;
        Node tmp = dom.getElementsByTagName("verify").item(0);
        if (tmp != null) {
            NamedNodeMap map = tmp.getAttributes();
            String s = map.getNamedItem("cardinality").getNodeValue();
            result.verifyCardinality = Boolean.valueOf(s);
        }
    }

    private void initStatements() {
        StringBuffer buff = new StringBuffer("update ").append(masterTable).append(" set ");
        buff.append(maxXAttribute).append(" = ?,");
        buff.append(maxYAttribute).append(" = ?,");
        buff.append(minXAttribute).append(" = ?,");
        buff.append(minYAttribute).append(" = ?");
        buff.append(" where ").append(coverageNameAttribute).append(" = ? ");
        if (tileTableNameAtribute != null)
            buff.append(" and ").append(tileTableNameAtribute).append(" = ? ");
        if (spatialTableNameAtribute != null)
            buff.append(" and ").append(spatialTableNameAtribute).append(" = ? ");
        sqlUpdateMosaicStatement = buff.toString();

        buff =
                new StringBuffer("select * from ")
                        .append(masterTable)
                        .append(" where ")
                        .append(coverageNameAttribute)
                        .append(" = ? ");
        sqlSelectCoverageStatement = buff.toString();

        buff = new StringBuffer("update ").append(masterTable).append(" set ");
        buff.append(resXAttribute).append(" = ?,");
        buff.append(resYAttribute).append(" = ? ");
        buff.append(" where ").append(coverageNameAttribute).append(" = ? ");
        if (tileTableNameAtribute != null)
            buff.append(" and ").append(tileTableNameAtribute).append(" = ? ");
        if (spatialTableNameAtribute != null)
            buff.append(" and ").append(spatialTableNameAtribute).append(" = ? ");
        sqlUpdateResStatement = buff.toString();
    }

    private static String readValueString(Document dom, String elemName) {
        Node n = readValueAttribute(dom, elemName);

        if (n == null) {
            return null;
        }

        return n.getNodeValue();
    }

    private static String readNameString(Element elem, String elemName) {
        Node n = readNameAttribute(elem, elemName);

        if (n == null) {
            return null;
        }

        return n.getNodeValue();
    }

    private static Integer readValueInteger(Document dom, String elemName) {
        Node n = readValueAttribute(dom, elemName);

        if (n == null) {
            return null;
        }

        return Integer.valueOf(n.getNodeValue());
    }

    private static Node readValueAttribute(Document dom, String elemName) {
        NodeList list = dom.getElementsByTagName(elemName);
        Node n = list.item(0);

        if (n == null) {
            return null;
        }

        return n.getAttributes().getNamedItem("value");
    }

    private static Node readNameAttribute(Element elem, String elemName) {
        NodeList list = elem.getElementsByTagName(elemName);
        Node n = list.item(0);

        if (n == null) {
            return null;
        }

        return n.getAttributes().getNamedItem("name");
    }

    public Map<String, Object> getDataSourceParams() {
        Map<String, Object> result = new HashMap<>();

        if ("DBCP".equals(dstype)) {
            result.put(DBCPDataSourceFactory.DSTYPE.key, dstype);
            result.put(DBCPDataSourceFactory.USERNAME.key, username);
            result.put(DBCPDataSourceFactory.PASSWORD.key, password);
            result.put(DBCPDataSourceFactory.JDBC_URL.key, jdbcUrl);
            result.put(DBCPDataSourceFactory.DRIVERCLASS.key, driverClassName);
            result.put(DBCPDataSourceFactory.MAXACTIVE.key, maxActive);
            result.put(DBCPDataSourceFactory.MAXIDLE.key, maxIdle);
        }

        if ("JNDI".equals(dstype)) {
            result.put(JNDIDataSourceFactory.DSTYPE.key, dstype);
            result.put(JNDIDataSourceFactory.JNDI_REFNAME.key, jndiReferenceName);
        }

        return result;
    }

    public String getBlobAttributeNameInTileTable() {
        return blobAttributeNameInTileTable;
    }

    public String getKeyAttributeNameInSpatialTable() {
        return keyAttributeNameInSpatialTable;
    }

    public String getKeyAttributeNameInTileTable() {
        return keyAttributeNameInTileTable;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public String getJndiReferenceName() {
        return jndiReferenceName;
    }

    public String getSqlUpdateMosaicStatement() {
        return sqlUpdateMosaicStatement;
    }

    public String getSqlSelectCoverageStatement() {
        return sqlSelectCoverageStatement;
    }

    public String getSpatialTableNameAtribute() {
        return spatialTableNameAtribute;
    }

    public String getTileTableNameAtribute() {
        return tileTableNameAtribute;
    }

    public String getSqlUpdateResStatement() {
        return sqlUpdateResStatement;
    }

    // String getTileTableSelectString(String tileTableName) {
    // StringBuffer buff = new StringBuffer ("select
    // ").append(blobAttributeNameInTileTable ).append(" from ")
    // .append(tileTableName).append(" where ")
    // .append(keyAttributeNameInTileTable).append( " = ? ");
    // return buff.toString();
    // }
    public String getMaxXAttribute() {
        return maxXAttribute;
    }

    public String getMaxYAttribute() {
        return maxYAttribute;
    }

    public String getMinXAttribute() {
        return minXAttribute;
    }

    public String getMinYAttribute() {
        return minYAttribute;
    }

    public String getResXAttribute() {
        return resXAttribute;
    }

    public String getResYAttribute() {
        return resYAttribute;
    }

    public String getCoordsys() {
        return coordsys;
    }

    public String getCoverageName() {
        return coverageName;
    }

    public Integer getInterpolation() {
        return interpolation;
    }

    public String getXmlUrl() {
        return xmlUrl;
    }

    public Boolean getVerifyCardinality() {
        return verifyCardinality;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public String getMasterTable() {
        return masterTable;
    }

    public String getCoverageNameAttribute() {
        return coverageNameAttribute;
    }

    public String getGeoRasterAttribute() {
        return geoRasterAttribute;
    }

    public String getPassword() {
        return password;
    }

    public String getUsername() {
        return username;
    }

    public String getTileMaxXAttribute() {
        return tileMaxXAttribute;
    }

    public String getTileMaxYAttribute() {
        return tileMaxYAttribute;
    }

    public String getTileMinXAttribute() {
        return tileMinXAttribute;
    }

    public String getTileMinYAttribute() {
        return tileMinYAttribute;
    }

    public String getGeomAttributeNameInSpatialTable() {
        return geomAttributeNameInSpatialTable;
    }

    public SpatialExtension getSpatialExtension() {
        return spatialExtension;
    }

    public String getJdbcAccessClassName() {
        return jdbcAccessClassName;
    }

    public Boolean getIgnoreAxisOrder() {
        return ignoreAxisOrder;
    }
}

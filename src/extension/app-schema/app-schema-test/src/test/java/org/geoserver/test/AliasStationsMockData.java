package org.geoserver.test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.geoserver.data.test.MockData;
import org.locationtech.jts.geom.Envelope;

/** Mock data for testing Type Alias on complex features. */
public class AliasStationsMockData extends StationsMockData {

    private String layerNamePrefix;

    @Override
    public void addContent() {
        setLayerNamePrefix("lyr");
        super.addContent();
    }

    private String getLayerName(String typeName) {
        if (layerNamePrefix != null) return layerNamePrefix + "_" + typeName;
        return typeName;
    }

    @Override
    public void addFeatureType(
            String namespacePrefix,
            String typeName,
            String mappingFileName,
            String... supportFileNames) {
        File featureTypeDir = getFeatureTypeDir(featureTypesBaseDir, namespacePrefix, typeName);
        String dataStoreName = getDataStoreName(namespacePrefix, typeName);
        try {
            writeInfoFileInternal(namespacePrefix, typeName, featureTypeDir, dataStoreName);
            copyMappingAndSupportFiles(
                    namespacePrefix, typeName, mappingFileName, supportFileNames);
            // if mappingFileName contains directory, eg, dir1/dir2/file.xml, we will ignore the
            // directory from here on
            addDataStore(
                    dataStoreName,
                    namespacePrefix,
                    AbstractAppSchemaMockData.buildAppSchemaDatastoreParams(
                            namespacePrefix,
                            typeName,
                            getFileNamePart(mappingFileName),
                            featureTypesBaseDir,
                            dataStoreName));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Write an info.xml file describing a feature type to the feature type directory.
     *
     * <p>Stolen from {@link MockData}.
     *
     * @param namespacePrefix namespace prefix of the WFS feature type
     * @param typeName namespace prefix of the WFS feature type
     * @param featureTypeDir feature type directory
     * @param dataStoreName data store directory name
     */
    protected void writeInfoFileInternal(
            String namespacePrefix, String typeName, File featureTypeDir, String dataStoreName) {
        // prepare extra params default
        Map<String, Object> params = new HashMap<String, Object>();
        params.put(KEY_STYLE, "Default");
        params.put(KEY_SRS_HANDLINGS, 2);
        params.put(KEY_ALIAS, null);
        Integer srs = 4326;
        params.put(KEY_SRS_NUMBER, srs);
        try {
            featureTypeDir.mkdir();
            File info = new File(featureTypeDir, "info.xml");
            info.delete();
            info.createNewFile();
            FileWriter writer = new FileWriter(info);
            writer.write("<featureType datastore=\"" + dataStoreName + "\">");
            writer.write("<name>" + getLayerName(typeName) + "</name>");
            writer.write("<nativeName>" + typeName + "</nativeName>");
            if (params.get(KEY_ALIAS) != null)
                writer.write("<alias>" + params.get(KEY_ALIAS) + "</alias>");
            writer.write("<SRS>" + params.get(KEY_SRS_NUMBER) + "</SRS>");
            // this mock type may have wrong SRS compared to the actual one in the property files...
            // let's configure SRS handling not to alter the original one, and have 4326 used only
            // for capabilities
            writer.write("<SRSHandling>" + params.get(KEY_SRS_HANDLINGS) + "</SRSHandling>");
            writer.write("<title>" + typeName + "</title>");
            writer.write("<abstract>abstract about " + typeName + "</abstract>");
            writer.write("<numDecimals value=\"8\"/>");
            writer.write("<keywords>" + typeName + "</keywords>");
            Envelope llEnvelope = (Envelope) params.get(KEY_LL_ENVELOPE);
            if (llEnvelope == null) llEnvelope = DEFAULT_ENVELOPE;
            writer.write(
                    "<latLonBoundingBox dynamic=\"false\" minx=\""
                            + llEnvelope.getMinX()
                            + "\" miny=\""
                            + llEnvelope.getMinY()
                            + "\" maxx=\""
                            + llEnvelope.getMaxX()
                            + "\" maxy=\""
                            + llEnvelope.getMaxY()
                            + "\"/>");
            Envelope nativeEnvelope = (Envelope) params.get(KEY_NATIVE_ENVELOPE);
            if (nativeEnvelope != null)
                writer.write(
                        "<nativeBBox dynamic=\"false\" minx=\""
                                + nativeEnvelope.getMinX()
                                + "\" miny=\""
                                + nativeEnvelope.getMinY()
                                + "\" maxx=\""
                                + nativeEnvelope.getMaxX()
                                + "\" maxy=\""
                                + nativeEnvelope.getMaxY()
                                + "\"/>");
            String style = (String) params.get(KEY_STYLE);
            if (style == null) style = "Default";
            writer.write("<styles default=\"" + style + "\"/>");
            writer.write("</featureType>");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected String getDataStoreName(String namespacePrefix, String typeName) {
        return namespacePrefix + "_" + getLayerName(typeName);
    }

    /**
     * Prefix for layer name, to test name vs nativeName
     *
     * @return layer name prefix
     */
    public String getLayerNamePrefix() {
        return layerNamePrefix;
    }

    public void setLayerNamePrefix(String layerNamePrefix) {
        this.layerNamePrefix = layerNamePrefix;
    }
}

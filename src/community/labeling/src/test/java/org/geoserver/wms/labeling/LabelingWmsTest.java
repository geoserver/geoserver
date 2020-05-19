/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.labeling;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.geoserver.wms.labeling.AttributesGlobeKvpParser.ATTRIBUTE_NAME_SEPARATOR;
import static org.geoserver.wms.labeling.AttributesGlobeKvpParser.RULE_SEPARATOR;
import static org.geoserver.wms.labeling.AttributesGlobeKvpParser.VALUE_SEPARATOR;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.xml.namespace.QName;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.SystemTestData.LayerProperty;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.image.test.ImageAssert;
import org.junit.Test;

public class LabelingWmsTest extends WMSTestSupport {

    static final String DELIVERY_URI = "http://delivery.org";
    static final String DELIVERY_PREFIX = "dlv";
    static final QName DRIVERS_LAYER = new QName(DELIVERY_URI, "Drivers", DELIVERY_PREFIX);
    static final QName CUSTOMERS_LAYER = new QName(DELIVERY_URI, "Customers", DELIVERY_PREFIX);
    static final String DRIVERS_STYLE = "drivers";
    static final String DRIVERS_STYLE_GRAPHIC = "drivers_graphic";

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        // add the layers
        @SuppressWarnings("rawtypes")
        Map<LayerProperty, Object> props = new HashMap<LayerProperty, Object>();
        props.put(LayerProperty.STYLE, "point");
        props.put(LayerProperty.NAME, DRIVERS_LAYER.getLocalPart());
        testData.setUpVectorLayer(DRIVERS_LAYER, props, getClass());
        props.put(LayerProperty.NAME, CUSTOMERS_LAYER.getLocalPart());
        testData.setUpVectorLayer(CUSTOMERS_LAYER, props, getClass());
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        // add the styles
        testData.addStyle(DRIVERS_STYLE, "drivers.sld", LabelingWmsTest.class, getCatalog());
        testData.addStyle(
                DRIVERS_STYLE_GRAPHIC, "drivers_graphic.sld", LabelingWmsTest.class, getCatalog());
    }

    @Test
    public void testAllFeatures() throws Exception {
        String bbox = "-81.1,-2.0,-77.4,0.0";
        BufferedImage image =
                getAsImage(
                        "wms?bbox="
                                + bbox
                                + "&styles="
                                + DRIVERS_STYLE
                                + "&layers="
                                + DRIVERS_LAYER.getPrefix()
                                + ":"
                                + DRIVERS_LAYER.getLocalPart()
                                + "&Format=image/png"
                                + "&request=GetMap"
                                + "&width=750"
                                + "&height=750"
                                + "&srs=EPSG:4326&transparent=true"
                                + "&renderlabel="
                                + encodeRenderLabelParameter(
                                        "dlv:Drivers",
                                        "1=1",
                                        Arrays.asList("code", "description", "time")),
                        "image/png");
        imageAssert("drivers_normal.png", image, 20);
    }

    @Test
    public void testOneFeature() throws Exception {
        String bbox = "-81.1,-2.0,-77.4,0.0";
        BufferedImage image =
                getAsImage(
                        "wms?bbox="
                                + bbox
                                + "&styles="
                                + DRIVERS_STYLE
                                + "&layers="
                                + DRIVERS_LAYER.getPrefix()
                                + ":"
                                + DRIVERS_LAYER.getLocalPart()
                                + "&Format=image/png"
                                + "&request=GetMap"
                                + "&width=750"
                                + "&height=750"
                                + "&srs=EPSG:4326&transparent=true"
                                + "&renderlabel="
                                + encodeRenderLabelParameter(
                                        "dlv:Drivers",
                                        "gid IN ('a01')",
                                        Arrays.asList("code", "description")),
                        "image/png");
        imageAssert("drivers_one.png", image, 20);
    }

    @Test
    public void testNoStyle() throws Exception {
        String bbox = "-81.1,-2.0,-77.4,0.0";
        BufferedImage image =
                getAsImage(
                        "wms?bbox="
                                + bbox
                                + "&styles=point"
                                + "&layers="
                                + DRIVERS_LAYER.getPrefix()
                                + ":"
                                + DRIVERS_LAYER.getLocalPart()
                                + "&Format=image/png"
                                + "&request=GetMap"
                                + "&width=750"
                                + "&height=750"
                                + "&srs=EPSG:4326&transparent=true"
                                + "&renderlabel="
                                + encodeRenderLabelParameter(
                                        "dlv:Drivers",
                                        "gid IN ('a01')",
                                        Arrays.asList("code", "description")),
                        "image/png");
        imageAssert("drivers_nostyle.png", image, 20);
    }

    @Test
    public void testStyleConfigurations() throws Exception {
        String bbox = "-81.1,-2.0,-77.4,0.0";
        BufferedImage image =
                getAsImage(
                        "wms?bbox="
                                + bbox
                                + "&styles="
                                + DRIVERS_STYLE_GRAPHIC
                                + "&layers="
                                + DRIVERS_LAYER.getPrefix()
                                + ":"
                                + DRIVERS_LAYER.getLocalPart()
                                + "&Format=image/png"
                                + "&request=GetMap"
                                + "&width=750"
                                + "&height=750"
                                + "&srs=EPSG:4326&transparent=true"
                                + "&renderlabel="
                                + encodeRenderLabelParameter(
                                        "dlv:Drivers",
                                        "gid IN ('a01')",
                                        Arrays.asList("time", "code", "description")),
                        "image/png");
        imageAssert("drivers_style_configs.png", image, 20);
    }

    @Test
    public void testTwoLayers() throws Exception {
        String bbox = "-81.1,-2.0,-77.4,0.0";
        // define layers and styles
        String driversLayer = DRIVERS_LAYER.getPrefix() + ":" + DRIVERS_LAYER.getLocalPart();
        String customersLayer = CUSTOMERS_LAYER.getPrefix() + ":" + CUSTOMERS_LAYER.getLocalPart();
        String layers = driversLayer + "," + customersLayer;
        String styles = DRIVERS_STYLE_GRAPHIC + ",point";
        // define render label
        String driversRenderLabel =
                encodeRenderLabelParameter(
                        "dlv:Drivers",
                        "gid IN ('a01')",
                        Arrays.asList("time", "code", "description"));
        String customersRenderLabel =
                encodeRenderLabelParameter(
                        "dlv:Customers", "gid IN ('c01')", Arrays.asList("code", "description"));
        String renderLabelValue = driversRenderLabel + RULE_SEPARATOR + customersRenderLabel;
        BufferedImage image =
                getAsImage(
                        "wms?bbox="
                                + bbox
                                + "&styles="
                                + styles
                                + "&layers="
                                + layers
                                + "&Format=image/png"
                                + "&request=GetMap"
                                + "&width=750"
                                + "&height=750"
                                + "&srs=EPSG:4326&transparent=true"
                                + "&renderlabel="
                                + renderLabelValue,
                        "image/png");
        imageAssert("drivers_2layers.png", image, 20);
    }

    void imageAssert(String expectedFileName, RenderedImage actualImage, int threshold) {
        try {
            InputStream inputStream = getClass().getResourceAsStream(expectedFileName);
            BufferedImage expectedImage = ImageIO.read(inputStream);
            ImageAssert.assertEquals(expectedImage, actualImage, threshold);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    String encodeRenderLabelParameter(String layerName, String filter, List<String> attributes) {
        StringBuilder builder = new StringBuilder(layerName);
        builder.append(VALUE_SEPARATOR);
        builder.append(filter);
        builder.append(VALUE_SEPARATOR);
        for (int i = 0; i < attributes.size(); i++) {
            if (i > 0) builder.append(ATTRIBUTE_NAME_SEPARATOR);
            builder.append(attributes.get(i));
        }
        try {
            return URLEncoder.encode(builder.toString(), UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    void saveImage(BufferedImage image, String filePath) throws Exception {
        File outputfile = new File(filePath);
        ImageIO.write(image, "png", outputfile);
    }
}

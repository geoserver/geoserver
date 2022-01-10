/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.worldwind.web;

import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.logging.Logger;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.resource.loader.IStringResourceLoader;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.web.GeoServerStringResourceLoader;
import org.geoserver.web.publish.PublishedConfigurationPanel;
import org.geoserver.web.util.MapModel;
import org.geoserver.wms.worldwind.BilConfig;
import org.geotools.util.logging.Logging;

/**
 * UI panel to configure a the BIL output format for a layer. This panel appears in the Publishing
 * tab of the layer configuration for a raster layer.
 *
 * @author Parker Abercrombie
 */
public class BilLayerConfigPanel extends PublishedConfigurationPanel<LayerInfo> {
    private static final long serialVersionUID = -7082211085566621848L;

    private static final Logger LOGGER = Logging.getLogger(BilLayerConfigPanel.class);

    public BilLayerConfigPanel(String id, IModel<LayerInfo> model) {
        super(id, model);

        PropertyModel<Object> metadata = new PropertyModel<Object>(model, "resource.metadata");

        add(
                new DropDownChoice<String>(
                        BilConfig.DEFAULT_DATA_TYPE,
                        new MapModel(metadata, BilConfig.DEFAULT_DATA_TYPE),
                        new ListModel<>(
                                Arrays.asList(
                                        "application/bil8",
                                        "application/bil16",
                                        "application/bil32"))));

        add(
                new DropDownChoice<String>(
                        BilConfig.BYTE_ORDER,
                        new MapModel(metadata, BilConfig.BYTE_ORDER),
                        new ListModel<>(
                                Arrays.asList(
                                        ByteOrder.BIG_ENDIAN.toString(),
                                        ByteOrder.LITTLE_ENDIAN.toString())),
                        new ByteOrderRenderer()));

        add(
                new TextField<Double>(
                        BilConfig.NO_DATA_OUTPUT,
                        new MapModel(metadata, BilConfig.NO_DATA_OUTPUT),
                        Double.class));
    }

    /** Renderer to display a localized string for the Byte Order drop down. */
    private class ByteOrderRenderer extends ChoiceRenderer<String> {

        private static final long serialVersionUID = 9198622236589910965L;

        public Object getDisplayValue(String str) {
            IStringResourceLoader loader = new GeoServerStringResourceLoader();
            if (ByteOrder.LITTLE_ENDIAN.toString().equals(str)) {
                return new StringResourceModel("byteOrderLittleEndian", BilLayerConfigPanel.this)
                        .getObject();
            } else if (ByteOrder.BIG_ENDIAN.toString().equals(str)) {
                return new StringResourceModel("byteOrderBigEndian", BilLayerConfigPanel.this)
                        .getObject();
            }

            LOGGER.warning("Unknown byte order: " + str);
            return str;
        }

        public String getIdValue(String str, int index) {
            return str;
        }
    }
}

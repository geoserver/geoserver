/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.netcdf.layer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.web.ComponentBuilder;
import org.geoserver.web.FormTestPage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.netcdf.DataPacking;
import org.geoserver.web.netcdf.NetCDFSettingsContainer;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class for the {@link NetCDFOutTabPanel}.
 * 
 */
public class NetCDFOutTabPanelTest extends GeoServerWicketTestSupport {
    /** LayerInfo model */
    private Model<LayerInfo> layerModel;

    /** CoverageInfo model */
    private Model<CoverageInfo> resourceModel;

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        testData.setUpDefaultRasterLayers();
    }

    @Before
    public void setUpInternal() throws Exception {
        // Creatign models
        LayerInfo layerInfo = getCatalog().getLayerByName(getLayerId(MockData.TASMANIA_DEM));
        layerModel = new Model<LayerInfo>(layerInfo);
        ResourceInfo resource = layerInfo.getResource();
        resourceModel = new Model<CoverageInfo>((CoverageInfo) resource);
        // Add Element to MetadataMap
        MetadataMap metadata = resource.getMetadata();
        if (!metadata.containsKey(NetCDFSettingsContainer.NETCDFOUT_KEY)) {
            metadata.put(NetCDFSettingsContainer.NETCDFOUT_KEY, new NetCDFLayerSettingsContainer());
        }
    }

    @Test
    public void testComponent() {
        login();
        // Opening the selected page
        tester.startPage(new FormTestPage(new ComponentBuilder() {
            private static final long serialVersionUID = -6705646666953650890L;

            public Component buildComponent(final String id) {
                return new NetCDFOutTabPanel(id, layerModel, resourceModel);
            }
        }));

        tester.assertComponent("form:panel", NetCDFOutTabPanel.class);

        // Checking Components and their default values
        tester.assertComponent("form:panel:netcdfeditor", NetCDFOutSettingsEditor.class);

        // getting component
        NetCDFOutSettingsEditor editor = (NetCDFOutSettingsEditor) tester
                .getComponentFromLastRenderedPage("form:panel:netcdfeditor");

        // Getting Model Object
        NetCDFLayerSettingsContainer container = editor.getModelObject();
        // Ensure the model is not null
        assertNotNull(container);
        // Ensure the container is equal to the one inside the MetadataMap
        NetCDFLayerSettingsContainer actualContainer = resourceModel.getObject().getMetadata()
                .get(NetCDFSettingsContainer.NETCDFOUT_KEY, NetCDFLayerSettingsContainer.class);
        assertEquals(container, actualContainer);

        // Ensure the Shuffle Component value is correct
        tester.assertComponent("form:panel:netcdfeditor:container:shuffle", CheckBox.class);
        CheckBox shuffle = (CheckBox) tester
                .getComponentFromLastRenderedPage("form:panel:netcdfeditor:container:shuffle");
        assertEquals(shuffle.getModelObject(), container.isShuffle());

        // Ensure the Compression Component value is correct
        tester.assertComponent("form:panel:netcdfeditor:container:compressionLevel",
                TextField.class);
        TextField<Integer> compressionLevel = (TextField<Integer>) tester
                .getComponentFromLastRenderedPage("form:panel:netcdfeditor:container:compressionLevel");
        assertEquals(compressionLevel.getModelObject().intValue(), container.getCompressionLevel());

        // Ensure the DataPacking Component value is correct
        tester.assertComponent("form:panel:netcdfeditor:container:dataPacking",
                DropDownChoice.class);
        DropDownChoice<DataPacking> dataPacking = (DropDownChoice<DataPacking>) tester
                .getComponentFromLastRenderedPage("form:panel:netcdfeditor:container:dataPacking");
        assertEquals(dataPacking.getModelObject(), container.getDataPacking());

        // Ensure the form is updated correctly
        FormTester formTester = tester.newFormTester("form");
        formTester.setValue("panel:netcdfeditor:container:standardName", "test");
        formTester.setValue("panel:netcdfeditor:container:uom", "test");
        formTester.submit();

        // Ensure no error
        tester.assertNoErrorMessage();

        // Check the container
        assertEquals(actualContainer.getLayerName(), "test");
        assertEquals(actualContainer.getLayerUOM(), "test");
    }

}

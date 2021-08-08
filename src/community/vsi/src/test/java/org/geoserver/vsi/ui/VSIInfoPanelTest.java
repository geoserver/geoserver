package org.geoserver.vsi.ui;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.vsi.VSIState;
import org.geoserver.vsi.VSITestHelper;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Before;
import org.junit.Test;

public class VSIInfoPanelTest extends GeoServerWicketTestSupport {

    private StoreInfo storeInfo;
    private Form form;
    private VSIInfoPanel panel;
    private VSITestHelper helper = new VSITestHelper();

    @Before
    public void setUp() throws Exception {
        // Sets up a mock instance of VSIInfoPanel
        storeInfo = helper.mockStoreInfo();
        form = mock(Form.class);
        IModel model = mock(IModel.class);
        when(model.getObject()).thenReturn(storeInfo);
        when(form.getModel()).thenReturn(model);
        when(form.getModelObject()).thenReturn(storeInfo);

        panel = new VSIInfoPanel("ID", form);
    }

    @Test
    public void testVSIInfoPanelConstructor() {
        // Check that methods are being called correctly
        // Note getModel() is also called in the super constructor
        verify(form, times(2)).getModel();
        verify(form, times(1)).getModelObject();

        // Check that the static variable lastStoreInfo is set correctly
        assertEquals(storeInfo, VSIState.getStoreInfo());
    }
}

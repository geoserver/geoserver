/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.autopopulate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.xml.namespace.QName;
import net.opengis.wfs.PropertyType;
import net.opengis.wfs.WfsFactory;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.test.SystemTest;
import org.geoserver.test.TestSetup;
import org.geoserver.test.TestSetupFrequency;
import org.geoserver.util.IOUtils;
import org.geoserver.wfs.request.Property;
import org.geoserver.wfs.request.TransactionElement;
import org.geoserver.wfs.request.TransactionRequest;
import org.geoserver.wfs.request.TransactionResponse;
import org.geoserver.wfs.request.Update;
import org.geotools.util.logging.Logging;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.mock.web.MockHttpServletResponse;

@Category(SystemTest.class)
@TestSetup(run = TestSetupFrequency.REPEAT)
public class AutopopulateTransactionCallbackTest extends GeoServerSystemTestSupport {

    /** logger */
    private static final Logger log = Logging.getLogger(AutopopulateTransactionCallbackTest.class);

    private AutopopulateTransactionCallback listener;

    private AutopopulateTemplate template;

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        testData.setUpDefaultRasterLayers();

        GeoServerExtensions extension = GeoServerExtensions.bean(GeoServerExtensions.class);
        if (extension == null) {
            GeoServerExtensionsHelper.init(this.applicationContext);
        }
    }

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        listener = new AutopopulateTransactionCallback(getCatalog());
        File f = new File(
                this.getTestData().getDataDirectoryRoot() + "/workspaces/cite/cite/NamedPlaces/",
                "transactionCustomizer.properties");
        f.deleteOnExit();
        try (FileOutputStream fout = new FileOutputStream(f)) {
            IOUtils.copy(this.getClass().getResourceAsStream("test-data/transactionCustomizer.properties"), fout);
        }
        template =
                new AutopopulateTemplate(getDataDirectory().get("cite/NamedPlaces/transactionCustomizer.properties"));
        Map templateCache = mock(Map.class);
        when(templateCache.get(any())).thenReturn(template);
        listener.setTemplateCache(templateCache);
    }

    @Test
    public void testAutopopulateTemplateResolvesProperties() {
        String key = "UPDATED";
        String value = "now()";
        String resolved = template.getAllProperties().get(key);
        log.info("Resolved value: " + resolved);
        assertNotSame(value, resolved);
    }

    @Test
    public void testNoInteractionsInUnusedMethods() {
        TransactionRequest request = mock(TransactionRequest.class);

        TransactionRequest returned = listener.beforeTransaction(request);
        assertSame(request, returned);

        TransactionResponse result = mock(TransactionResponse.class);
        listener.afterTransaction(request, result, false);
        verifyNoMoreInteractions(result);
    }

    @Test
    public void testBeforeTransactionDoesNotPropagateExceptions() {
        TransactionRequest request = mock(TransactionRequest.class);
        when(request.getElements()).thenThrow(new RuntimeException("fake"));
        try {
            listener.beforeTransaction(request);
        } catch (RuntimeException e) {
            fail(
                    "Exception should have been eaten to prevent the transaction from failing due to a gwc integration error");
        }
    }

    @Test
    public void testBeforeTransactionOfNoInterest() {
        TransactionRequest request = mock(TransactionRequest.class);
        when(request.getVersion()).thenReturn("1.1.0");
        when(request.getElements()).thenReturn(new ArrayList<>());
        TransactionRequest returned = listener.beforeTransaction(request);
        assertSame(request, returned);

        verify(request, times(1)).getElements();
        verify(request, times(1)).getVersion();
    }

    @Test
    public void testAfterTransactionUncommitted() {
        TransactionRequest request = mock(TransactionRequest.class);
        TransactionResponse result = mock(TransactionResponse.class);
        boolean committed = false;

        listener.afterTransaction(request, result, committed);

        verifyNoMoreInteractions(result);
    }

    @Test
    public void testUpdateTransactionElement() throws Exception {
        Update element = mock(Update.class);
        List<Property> properties = new ArrayList<>();
        PropertyType property = WfsFactory.eINSTANCE.createPropertyType();
        Property.WFS11 updateProperty = new Property.WFS11(property);
        when(element.createProperty()).thenReturn(updateProperty);
        when(element.getUpdateProperties()).thenReturn(properties);

        @SuppressWarnings("MockNotUsedInProduction")
        FeatureTypeInfo featureTypeInfo = mock(FeatureTypeInfo.class);
        when(element.getTypeName()).thenReturn(new QName("NamedPlaces"));
        NamespaceInfo nameSpaceInfo = mock(NamespaceInfo.class);
        when(nameSpaceInfo.getURI()).thenReturn("http://www.opengis.net/cite");
        when(featureTypeInfo.getNamespace()).thenReturn(nameSpaceInfo);

        TransactionRequest request = mock(TransactionRequest.class);
        List<TransactionElement> transactionElements = new ArrayList<>();
        transactionElements.add(element);
        when(request.getElements()).thenReturn(transactionElements);
        when(request.getVersion()).thenReturn("1.1.0");

        listener.beforeTransaction(request);

        StringBuilder sb = new StringBuilder("wfs?request=GetFeature&version=2.0")
                .append("&TYPENAME=cite:NamedPlaces&outputFormat=")
                .append("application/json");
        JSONObject result = (JSONObject) getJson(sb.toString());
        JSONArray features = (JSONArray) result.get("features");
        assertEquals(2, features.size());
        verify(element, times(features.size())).getTypeName();

        assertTrue(properties.stream().anyMatch(p -> p.getName().getLocalPart().equals("NAME")));
        assertFalse(properties.isEmpty());
        while (properties.iterator().hasNext()) {
            Property p = properties.iterator().next();
            if (p.getName().getLocalPart().equals("NAME")) {
                assertEquals("Foo", p.getValue(), p.getValue());
                break;
            }
        }
    }

    protected JSON getJson(String path) throws Exception {
        MockHttpServletResponse response = getAsServletResponse(path);
        String contentType = response.getContentType();
        // in the case of GeoJSON response with ogcapi, the output format is not
        // set to MockHttpServlet request, so skipping
        if (contentType != null) assertEquals("application/json;charset=UTF-8", contentType);
        return json(response);
    }
}

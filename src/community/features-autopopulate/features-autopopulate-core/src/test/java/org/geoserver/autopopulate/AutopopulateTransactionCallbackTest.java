/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.autopopulate;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.xml.namespace.QName;
import net.opengis.wfs.PropertyType;
import net.opengis.wfs.WfsFactory;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.test.SystemTest;
import org.geoserver.test.TestSetup;
import org.geoserver.test.TestSetupFrequency;
import org.geoserver.wfs.request.*;
import org.geotools.util.logging.Logging;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

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
    public void setUp() throws Exception {
        listener = new AutopopulateTransactionCallback(getCatalog());
        template = new AutopopulateTemplate("src/test/resources/transactionCustomizer.properties");
        Map templateCache = mock(Map.class);
        when(templateCache.get(any())).thenReturn(template);
        listener.setTemplateCache(templateCache);
    }

    @Test
    public void testAutopopulateTemplateResolvesProperties() {
        String key = "UPDATED";
        String value = "now()";
        String resolved = template.getProperty(key);
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
        when(request.getElements()).thenReturn(new ArrayList<TransactionElement>());
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
    public void testUpdateTransactionElement() {
        Update element = mock(Update.class);
        List<Property> properties = new ArrayList<Property>();
        PropertyType property = WfsFactory.eINSTANCE.createPropertyType();
        Property.WFS11 updateProperty = new Property.WFS11(property);
        when(element.createProperty()).thenReturn(updateProperty);
        when(element.getUpdateProperties()).thenReturn(properties);
        FeatureTypeInfo featureTypeInfo = mock(FeatureTypeInfo.class);
        when(element.getTypeName()).thenReturn(new QName("NamedPlaces"));
        NamespaceInfo nameSpaceInfo = mock(NamespaceInfo.class);
        when(nameSpaceInfo.getURI()).thenReturn("http://www.opengis.net/cite");
        when(featureTypeInfo.getNamespace()).thenReturn(nameSpaceInfo);

        TransactionRequest request = mock(TransactionRequest.class);
        List<TransactionElement> transactionElements = new ArrayList<TransactionElement>();
        transactionElements.add(element);
        when(request.getElements()).thenReturn(transactionElements);
        when(request.getVersion()).thenReturn("1.1.0");

        listener.beforeTransaction(request);

        verify(element, times(6)).getUpdateProperties();
        assertTrue(properties.stream().anyMatch(p -> p.getName().getLocalPart().equals("NAME")));
        while (properties.iterator().hasNext()) {
            Property p = properties.iterator().next();
            if (p.getName().getLocalPart().equals("NAME")) {
                assertEquals("Foo", p.getValue(), p.getValue());
                break;
            }
        }
    }
}

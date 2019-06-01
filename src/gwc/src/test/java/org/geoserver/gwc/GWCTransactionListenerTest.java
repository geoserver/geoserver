/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc;

import static junit.framework.TestCase.assertTrue;
import static org.geotools.referencing.crs.DefaultGeographicCRS.WGS84;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import net.opengis.wfs.InsertElementType;
import net.opengis.wfs.TransactionType;
import org.geoserver.wfs.TransactionEvent;
import org.geoserver.wfs.TransactionEventType;
import org.geoserver.wfs.request.TransactionRequest;
import org.geoserver.wfs.request.TransactionResponse;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope3D;
import org.geotools.referencing.CRS;
import org.junit.Before;
import org.junit.Test;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class GWCTransactionListenerTest {

    private GWC mediator;

    private GWCTransactionListener listener;

    @Before
    public void setUp() throws Exception {
        mediator = mock(GWC.class);
        listener = new GWCTransactionListener(mediator);
    }

    @Test
    public void testNoInteractionsInUnusedMethods() {

        TransactionRequest request = mock(TransactionRequest.class);

        TransactionRequest returned = listener.beforeTransaction(request);
        assertSame(request, returned);
        verifyNoMoreInteractions(request, mediator);

        listener.beforeCommit(request);
        verifyNoMoreInteractions(request, mediator);
    }

    @Test
    public void testAfterTransactionUncommitted() {

        TransactionRequest request = mock(TransactionRequest.class);
        TransactionResponse result = mock(TransactionResponse.class);
        boolean committed = false;

        listener.afterTransaction(request, result, committed);

        verifyNoMoreInteractions(request, result, mediator);
    }

    @Test
    public void testDataStoreChangeDoesNotPropagateExceptions() {

        TransactionEvent event = mock(TransactionEvent.class);
        when(event.getSource()).thenThrow(new RuntimeException("fake"));
        try {
            listener.dataStoreChange(event);
        } catch (RuntimeException e) {
            fail(
                    "Exception should have been eaten to prevent the transaction from failing due to a gwc integration error");
        }
    }

    @Test
    public void testDataStoreChangeOfNoInterest() {
        TransactionEvent event = mock(TransactionEvent.class);
        when(event.getSource()).thenReturn(new Object());
        listener.dataStoreChange(event);

        verify(event, times(1)).getLayerName();
        verify(event, times(1)).getType();
        verify(event, times(1)).getSource();

        verifyNoMoreInteractions(event, mediator);
    }

    @Test
    public void testDataStoreChangePostInsert() {

        InsertElementType insert = mock(InsertElementType.class);
        TransactionEvent event = mock(TransactionEvent.class);

        QName layerName = new QName("testType");
        when(event.getLayerName()).thenReturn(layerName);
        when(event.getSource()).thenReturn(insert);
        when(event.getType()).thenReturn(TransactionEventType.POST_INSERT);

        listener.dataStoreChange(event);
        // no need to do anything at post insert, bounds computed at pre_insert
        verifyNoMoreInteractions(mediator);
    }

    @Test
    public void testDataStoreChangeDoesNotAffectTileLayer() {

        InsertElementType insert = mock(InsertElementType.class);
        TransactionEvent event = mock(TransactionEvent.class);

        QName layerName = new QName("testType");
        when(event.getLayerName()).thenReturn(layerName);
        when(event.getSource()).thenReturn(insert);
        when(event.getType()).thenReturn(TransactionEventType.PRE_INSERT);

        when(mediator.getTileLayersByFeatureType(
                        eq(layerName.getNamespaceURI()), eq(layerName.getLocalPart())))
                .thenReturn(Collections.EMPTY_SET);

        listener.dataStoreChange(event);
        // nothing else to do
        verify(mediator, times(1))
                .getTileLayersByFeatureType(
                        eq(layerName.getNamespaceURI()), eq(layerName.getLocalPart()));
        verifyNoMoreInteractions(mediator);
    }

    @Test
    public void testDataStoreChangeInsert() {

        Map<Object, Object> extendedProperties = new HashMap<Object, Object>();
        ReferencedEnvelope affectedBounds = new ReferencedEnvelope(-180, 0, 0, 90, WGS84);

        issueInsert(extendedProperties, affectedBounds);

        assertTrue(
                extendedProperties.containsKey(
                        GWCTransactionListener.GWC_TRANSACTION_INFO_PLACEHOLDER));

        @SuppressWarnings("unchecked")
        Map<String, List<ReferencedEnvelope>> placeHolder =
                (Map<String, List<ReferencedEnvelope>>)
                        extendedProperties.get(
                                GWCTransactionListener.GWC_TRANSACTION_INFO_PLACEHOLDER);

        assertNotNull(placeHolder.get("theLayer"));

        assertSame(affectedBounds, placeHolder.get("theLayer").get(0));
        assertSame(affectedBounds, placeHolder.get("theGroup").get(0));
    }

    @Test
    public void testAfterTransactionCompoundCRS() throws Exception {
        Map<Object, Object> extendedProperties = new HashMap<Object, Object>();
        final CoordinateReferenceSystem compoundCrs = CRS.decode("EPSG:7415");
        ReferencedEnvelope3D transactionBounds =
                new ReferencedEnvelope3D(142892, 470783, 142900, 470790, 16, 20, compoundCrs);

        issueInsert(extendedProperties, transactionBounds);

        TransactionRequest request = mock(TransactionRequest.class);
        TransactionResponse result = mock(TransactionResponse.class);
        when(request.getExtendedProperties()).thenReturn(extendedProperties);

        when(mediator.getDeclaredCrs(anyString())).thenReturn(compoundCrs);
        listener.afterTransaction(request, result, true);

        ReferencedEnvelope expectedBounds =
                new ReferencedEnvelope(transactionBounds, CRS.getHorizontalCRS(compoundCrs));

        verify(mediator, times(1)).truncate(eq("theLayer"), eq(expectedBounds));
        verify(mediator, times(1)).truncate(eq("theGroup"), eq(expectedBounds));
    }

    @Test
    public void testAfterTransaction() throws Exception {
        Map<Object, Object> extendedProperties = new HashMap<Object, Object>();
        ReferencedEnvelope affectedBounds1 = new ReferencedEnvelope(-180, 0, 0, 90, WGS84);
        ReferencedEnvelope affectedBounds2 = new ReferencedEnvelope(0, 180, 0, 90, WGS84);

        issueInsert(extendedProperties, affectedBounds1);

        issueInsert(extendedProperties, affectedBounds2);

        TransactionRequest request = mock(TransactionRequest.class);
        TransactionResponse result = mock(TransactionResponse.class);
        when(request.getExtendedProperties()).thenReturn(extendedProperties);

        when(mediator.getDeclaredCrs(anyString())).thenReturn(WGS84);
        listener.afterTransaction(request, result, true);

        ReferencedEnvelope expectedEnv = new ReferencedEnvelope(affectedBounds1);
        expectedEnv.expandToInclude(affectedBounds2);

        verify(mediator, times(1)).truncate(eq("theLayer"), eq(expectedEnv));
        verify(mediator, times(1)).truncate(eq("theGroup"), eq(expectedEnv));
    }

    /**
     * Issues a fake dataStoreChange insert event that affects two tile layers: "theLayer" and
     * "theGroup"
     */
    private void issueInsert(
            Map<Object, Object> extendedProperties, ReferencedEnvelope affectedBounds) {

        TransactionType transaction = mock(TransactionType.class);
        when(transaction.getExtendedProperties()).thenReturn(extendedProperties);

        TransactionEvent event = mock(TransactionEvent.class);

        when(event.getRequest()).thenReturn(transaction);

        QName layerName = new QName("testType");
        when(event.getLayerName()).thenReturn(layerName);

        InsertElementType insert = mock(InsertElementType.class);

        when(event.getSource()).thenReturn(insert);
        when(event.getType()).thenReturn(TransactionEventType.PRE_INSERT);

        when(mediator.getTileLayersByFeatureType(
                        eq(layerName.getNamespaceURI()), eq(layerName.getLocalPart())))
                .thenReturn(ImmutableSet.of("theLayer", "theGroup"));

        SimpleFeatureCollection affectedFeatures = mock(SimpleFeatureCollection.class);
        when(affectedFeatures.getBounds()).thenReturn(affectedBounds);
        when(event.getAffectedFeatures()).thenReturn(affectedFeatures);

        listener.dataStoreChange(event);
    }
}

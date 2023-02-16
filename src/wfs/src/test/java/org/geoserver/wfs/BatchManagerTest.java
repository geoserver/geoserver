/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.wfs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.namespace.QName;
import net.opengis.wfs20.DeleteType;
import net.opengis.wfs20.InsertType;
import net.opengis.wfs20.NativeType;
import net.opengis.wfs20.ReplaceType;
import net.opengis.wfs20.TransactionType;
import net.opengis.wfs20.UpdateType;
import net.opengis.wfs20.Wfs20Factory;
import org.geoserver.wfs.Transaction.BatchManager;
import org.geoserver.wfs.request.Delete;
import org.geoserver.wfs.request.Insert;
import org.geoserver.wfs.request.Native;
import org.geoserver.wfs.request.Replace;
import org.geoserver.wfs.request.TransactionElement;
import org.geoserver.wfs.request.TransactionRequest;
import org.geoserver.wfs.request.TransactionResponse;
import org.geoserver.wfs.request.Update;
import org.geotools.data.FeatureStore;
import org.geotools.util.MapEntry;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
import org.opengis.filter.Or;

/**
 * Tests for {@link BatchManager}.
 *
 * @author awaterme
 */
public class BatchManagerTest {

    private TransactionListener transactionListener = mock(TransactionListener.class);
    private TransactionResponse transactionResponse = mock(TransactionResponse.class);

    private SimpleFeature feature1 = mock(SimpleFeature.class);
    private SimpleFeature feature2 = mock(SimpleFeature.class);
    private SimpleFeature feature3 = mock(SimpleFeature.class);
    private SimpleFeature feature4 = mock(SimpleFeature.class);

    private QName type1Name = new QName("t1");
    private QName type2Name = new QName("t2");

    private Filter filter1 = mock(Filter.class);
    private Filter filter2 = mock(Filter.class);
    private Filter filter3 = mock(Filter.class);

    @SuppressWarnings("rawtypes")
    private Map<QName, FeatureStore> stores = Collections.emptyMap();

    /**
     * Verifies that Transaction:
     *
     * <pre>
     * INSERT-1
     *      FEATURE-1
     * INSERT-2
     *      FEATURE-2
     * UPDATE-1
     *      ANYTHING
     * INSERT-3
     *      FEATURE-3
     * DELETE-1 for TYPE-1
     *      FILTER-1
     * DELETE-2 for TYPE-1
     *      FILTER-2
     * DELETE-3 for TYPE-2
     *      FILTER-3
     * REPLACE-1
     *      FEATURE-4
     * NATIVE-1
     *      ANYTHING
     * </pre>
     *
     * with delete batch size 100 gets aggregated as
     *
     * <pre>
     * INSERT-1
     *      FEATURE-1
     *      FEATURE-2
     * UPDATE-1
     *      ANYTHING
     * INSERT-3
     *      FEATURE-3
     * DELETE-1 for TYPE-1
     *      FILTER-1
     *      FILTER-2
     * DELETE-3 for TYPE-2
     *      FILTER-3
     * REPLACE-1
     *      FEATURE-4
     * NATIVE-1
     *      ANYTHING
     * </pre>
     *
     * @throws Exception
     */
    @Test
    public void testAggregation() throws Exception {
        int lDeleteBatchSize = 100;
        testAggregationWithDeleteBatchSize(lDeleteBatchSize);
    }

    /**
     * Verifies that Transaction:
     *
     * <pre>
     * INSERT-1
     *      FEATURE-1
     * INSERT-2
     *      FEATURE-2
     * UPDATE-1
     *      ANYTHING
     * INSERT-3
     *      FEATURE-3
     * DELETE-1 for TYPE-1
     *      FILTER-1
     * DELETE-2 for TYPE-1
     *      FILTER-2
     * DELETE-3 for TYPE-2
     *      FILTER-3
     * REPLACE-1
     *      FEATURE-4
     * NATIVE-1
     *      ANYTHING
     * </pre>
     *
     * with delete batch size 1 or 0 or smaller 0 gets aggregated as
     *
     * <pre>
     * INSERT-1
     *      FEATURE-1
     *      FEATURE-2
     * UPDATE-1
     *      ANYTHING
     * INSERT-3
     *      FEATURE-3
     * DELETE-1 for TYPE-1
     *      FILTER-1
     * DELETE-1 for TYPE-1
     *      FILTER-2
     * DELETE-3 for TYPE-2
     *      FILTER-3
     * REPLACE-1
     *      FEATURE-4
     * NATIVE-1
     *      ANYTHING
     * </pre>
     *
     * @throws Exception
     */
    @Test
    public void testAggregationNoDeleteAgg() throws Exception {
        testAggregationWithDeleteBatchSize(1);
        testAggregationWithDeleteBatchSize(0);
        testAggregationWithDeleteBatchSize(-1);
    }

    @Test
    public void testLargeBatchDeletion() {

        final int batchSizeForDeletion = 40;

        Map<TransactionElement, TransactionElementHandler> element2Handlers = new LinkedHashMap<>();

        Delete delete1 = newDelete(type1Name, filter1);
        TransactionElementHandler delete1Handler = mock(TransactionElementHandler.class);

        element2Handlers.put(delete1, delete1Handler);

        for (int i = 1; i < batchSizeForDeletion; i++) {
            Delete delete = newDelete(type1Name, filter1);
            TransactionElementHandler deleteHandler = mock(TransactionElementHandler.class);
            element2Handlers.put(delete, deleteHandler);
        }

        TransactionRequest lTransaction = transactionRequest(element2Handlers.keySet());

        BatchManager sut =
                new BatchManager(
                        lTransaction,
                        transactionListener,
                        stores,
                        transactionResponse,
                        element2Handlers,
                        batchSizeForDeletion);
        sut.run();

        String message = String.format("First %d DELETEs have been merged", batchSizeForDeletion);
        assertTrue(message, delete1.getFilter() instanceof Or);
        int lOrCount = ((Or) delete1.getFilter()).getChildren().size();
        assertEquals(message, 40, lOrCount);

        verify(delete1Handler, times(1)).execute(same(delete1), any(), any(), any(), any());
    }

    private void testAggregationWithDeleteBatchSize(int pDeleteBatchSize) {
        // given: test transactions contents...
        Insert insert1 = newInsert(feature1);
        TransactionElementHandler insert1Handler = mock(TransactionElementHandler.class);

        Insert insert2 = newInsert(feature2);
        TransactionElementHandler insert2Handler = mock(TransactionElementHandler.class);

        Update update1 = newUpdate();
        TransactionElementHandler update1Handler = mock(TransactionElementHandler.class);

        Insert insert3 = newInsert(feature3);
        TransactionElementHandler insert3Handler = mock(TransactionElementHandler.class);

        Delete delete1 = newDelete(type1Name, filter1);
        TransactionElementHandler delete1Handler = mock(TransactionElementHandler.class);

        Delete delete2 = newDelete(type1Name, filter2);
        TransactionElementHandler delete2Handler = mock(TransactionElementHandler.class);

        Delete delete3 = newDelete(type2Name, filter3);
        TransactionElementHandler delete3Handler = mock(TransactionElementHandler.class);

        Replace replace1 = newReplace(feature4);
        TransactionElementHandler replace1Handler = mock(TransactionElementHandler.class);

        Native native1 = newNative();
        TransactionElementHandler native1Handler = mock(TransactionElementHandler.class);

        Map<TransactionElement, TransactionElementHandler> element2Handlers =
                asMap( //
                        keyValue(insert1, insert1Handler), //
                        keyValue(insert2, insert2Handler), //
                        keyValue(update1, update1Handler), //
                        keyValue(insert3, insert3Handler), //
                        keyValue(delete1, delete1Handler), //
                        keyValue(delete2, delete2Handler), //
                        keyValue(delete3, delete3Handler), //
                        keyValue(replace1, replace1Handler), //
                        keyValue(native1, native1Handler)
                        //
                        );

        TransactionRequest lTransaction = transactionRequest(element2Handlers.keySet());

        // when: BatchManager runs...
        BatchManager sut =
                new BatchManager(
                        lTransaction,
                        transactionListener,
                        stores,
                        transactionResponse,
                        element2Handlers,
                        pDeleteBatchSize);
        sut.run();

        // then:
        // verify contents have been moved appropriately
        assertEquals("First 2 INSERTs have been merged", 2, insert1.getFeatures().size());
        if (pDeleteBatchSize <= 1) {
            assertFalse("First 2 DELETEs must not be merged", delete1.getFilter() instanceof Or);
        } else {
            assertTrue("First 2 DELETEs have been merged", delete1.getFilter() instanceof Or);
            int lOrCount = ((Or) delete1.getFilter()).getChildren().size();
            assertEquals("First 2 DELETEs have been merged", 2, lOrCount);
        }

        // verify invocations of handlers
        // INSERT-1 handler was executed with first insert?
        verify(insert1Handler, times(1)).execute(same(insert1), any(), any(), any(), any());
        // INSERT-2 skipped?
        verify(insert2Handler, times(0)).execute(any(), any(), any(), any(), any());
        // UPDATE-1 was executed?
        verify(update1Handler, times(1)).execute(same(update1), any(), any(), any(), any());
        // INSERT-3 was executed?
        verify(insert3Handler, times(1)).execute(same(insert3), any(), any(), any(), any());
        // DELETE-1 was executed?
        verify(delete1Handler, times(1)).execute(same(delete1), any(), any(), any(), any());
        // DELETE-2 skipped?
        verify(delete2Handler, times(pDeleteBatchSize <= 1 ? 1 : 0))
                .execute(any(), any(), any(), any(), any());
        // DELETE-3 was executed?
        verify(delete3Handler, times(1)).execute(same(delete3), any(), any(), any(), any());
        // REPLACE-1 was executed?
        verify(replace1Handler, times(1)).execute(same(replace1), any(), any(), any(), any());
        // NATIVE-1 was executed?
        verify(native1Handler, times(1)).execute(same(native1), any(), any(), any(), any());

        // verify TransactionRequest was modified appropriately
        List<TransactionElement> lElements = lTransaction.getElements();
        if (pDeleteBatchSize <= 1) {
            assertEquals(8, lElements.size());
        } else {
            assertEquals(7, lElements.size());
        }
    }

    private TransactionRequest transactionRequest(Set<TransactionElement> pElems) {
        TransactionType lTransactionType = Wfs20Factory.eINSTANCE.createTransactionType();
        TransactionRequest lTransactionRequest = TransactionRequest.adapt(lTransactionType);
        lTransactionRequest.setElements(new ArrayList<>(pElems));
        return lTransactionRequest;
    }

    private MapEntry<TransactionElement, TransactionElementHandler> keyValue(
            TransactionElement pElement, TransactionElementHandler pElementHandler) {
        return new MapEntry<TransactionElement, TransactionElementHandler>(
                pElement, pElementHandler);
    }

    @SafeVarargs
    private final <K, V> Map<K, V> asMap(MapEntry<K, V>... pEntries) {
        LinkedHashMap<K, V> lResult = new LinkedHashMap<>();
        for (MapEntry<K, V> mapEntry : pEntries) {
            lResult.put(mapEntry.getKey(), mapEntry.getValue());
        }
        return lResult;
    }

    private Native newNative() {
        NativeType lNativeType = Wfs20Factory.eINSTANCE.createNativeType();
        Native lNative = new Native.WFS20(lNativeType);
        return lNative;
    }

    private Replace newReplace(SimpleFeature pFeature) {
        ReplaceType lUpdateType = Wfs20Factory.eINSTANCE.createReplaceType();
        Replace lReplace = new Replace.WFS20(lUpdateType);
        return lReplace;
    }

    private Delete newDelete(QName pTypeName, Filter pFilter) {
        DeleteType lDeleteType = Wfs20Factory.eINSTANCE.createDeleteType();
        Delete lDelete = new Delete.WFS20(lDeleteType);
        lDelete.setTypeName(pTypeName);
        lDelete.setFilter(pFilter);
        return lDelete;
    }

    private Update newUpdate() {
        UpdateType lUpdateType = Wfs20Factory.eINSTANCE.createUpdateType();
        Update lUpdate = new Update.WFS20(lUpdateType);
        return lUpdate;
    }

    private Insert newInsert(SimpleFeature pFeature) {
        InsertType lInsertType = Wfs20Factory.eINSTANCE.createInsertType();
        Insert lInsert = new Insert.WFS20(lInsertType);
        lInsert.setFeatures(Arrays.asList(pFeature));
        return lInsert;
    }
}

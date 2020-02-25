/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import javax.xml.namespace.QName;
import org.geoserver.catalog.Catalog;
import org.geoserver.data.test.MockData;
import org.geoserver.wfs.request.*;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

public class TransactionCallbackTester implements TransactionCallback {

    public static final String FOLSOM_STREET = "Folsom Street";

    public static TransactionRequest defaultTransformation(
            Catalog catalog, TransactionRequest request) {
        for (TransactionElement element : request.getElements()) {
            if (element instanceof Insert) {
                for (Object f : ((Insert) element).getFeatures()) {
                    SimpleFeature sf = (SimpleFeature) f;
                    if ("Points".equals(sf.getType().getTypeName())) {
                        // check inserts can be modified
                        sf.setAttribute("id", sf.getAttribute("id") + "-modified");
                    }
                }
            } else if (element instanceof Update) {
                Update update = (Update) element;
                List<Property> updateProperties = update.getUpdateProperties();
                Property property = update.createProperty();
                property.setName(new QName(null, "NAME"));
                property.setValue(FOLSOM_STREET);
                updateProperties.add(property);
                update.setUpdateProperties(updateProperties);
            } else if (element instanceof Delete) {
                try {
                    // mass delete more than requested
                    ((Delete) element).setFilter(CQL.toFilter("FID > 102"));
                } catch (CQLException e) {
                    throw new WFSException(e);
                }
            }
        }

        return request;
    }

    public static TransactionRequest replaceWithFixedRoadsInsert(
            Catalog catalog, TransactionRequest request) {
        List<TransactionElement> transactionElements = new ArrayList<>();

        // create an insert
        Insert insert = request.createInsert();
        List<SimpleFeature> features = new ArrayList<>();
        try {
            SimpleFeatureType schema =
                    (SimpleFeatureType)
                            catalog.getFeatureTypeByName(MockData.ROAD_SEGMENTS.getLocalPart())
                                    .getFeatureType();
            Geometry geometry = new WKTReader().read("MULTILINESTRING((0 0, 1 1))");
            SimpleFeature feature =
                    SimpleFeatureBuilder.build(
                            schema, new Object[] {geometry, "107", "New Road"}, null);
            features.add(feature);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        insert.setFeatures(features);
        transactionElements.add(insert);
        request.setElements(transactionElements);

        return request;
    }

    public static TransactionRequest replaceWithFixedRoadsUpdate(
            Catalog catalog, TransactionRequest request) {
        List<TransactionElement> transactionElements = new ArrayList<>();

        // create an update
        Update update = request.createUpdate();
        try {
            update.setTypeName(
                    new QName(
                            MockData.ROAD_SEGMENTS.getNamespaceURI(),
                            MockData.ROAD_SEGMENTS.getLocalPart()));
            update.setFilter(CQL.toFilter("FID = 106"));
            Property property = update.createProperty();
            property.setName(new QName(null, "NAME"));
            property.setValue("Clean Road");
            update.setUpdateProperties(Arrays.asList(property));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        transactionElements.add(update);
        request.setElements(transactionElements);

        return request;
    }

    public static TransactionRequest replaceWithFixedRoadsDelete(
            Catalog catalog, TransactionRequest request) {
        List<TransactionElement> transactionElements = new ArrayList<>();

        // create a delete
        Delete delete = request.createDelete();
        try {
            delete.setTypeName(
                    new QName(
                            MockData.ROAD_SEGMENTS.getNamespaceURI(),
                            MockData.ROAD_SEGMENTS.getLocalPart()));
            delete.setFilter(CQL.toFilter("FID = 106"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        transactionElements.add(delete);
        request.setElements(transactionElements);

        return request;
    }

    TransactionResponse result;
    boolean committed;
    boolean beforeCommitCalled;
    boolean dataStoreChanged;
    TransactionRequest request;
    BiFunction<Catalog, TransactionRequest, TransactionRequest> beforeTransaction =
            TransactionCallbackTester::defaultTransformation;
    Catalog catalog;

    public TransactionCallbackTester(Catalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public TransactionRequest beforeTransaction(TransactionRequest request) throws WFSException {
        this.request = beforeTransaction.apply(this.catalog, request);

        return request;
    }

    @Override
    public void beforeCommit(TransactionRequest request) throws WFSException {
        this.request = request;
        this.beforeCommitCalled = true;
    }

    @Override
    public void afterTransaction(
            TransactionRequest request, TransactionResponse result, boolean committed) {
        this.request = request;
        this.result = result;
        this.committed = committed;
    }

    public void clear() {
        this.result = null;
        this.committed = false;
        this.beforeCommitCalled = false;
        this.dataStoreChanged = false;
        this.beforeTransaction = TransactionCallbackTester::defaultTransformation;
    }

    @Override
    public void dataStoreChange(TransactionEvent event) throws WFSException {
        this.dataStoreChanged = true;
    }
}

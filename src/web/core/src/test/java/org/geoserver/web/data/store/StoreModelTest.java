/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.data.workspace.WorkspaceDetachableModel;
import org.junit.Test;

public class StoreModelTest extends GeoServerWicketTestSupport {

    @Test
    public void testStoreModel() throws Exception {
        DataStoreInfo s = getFeatureTypeInfo(MockData.PRIMITIVEGEOFEATURE).getStore();
        StoreModel<DataStoreInfo> model = new StoreModel<DataStoreInfo>(s);

        model = serializeDeserialize(model);
        assertEquals(s, model.getObject());

        model.detach();
        assertEquals(s, model.getObject());
    }

    @Test
    public void testStoreModelSetNull() throws Exception {
        DataStoreInfo s = getFeatureTypeInfo(MockData.PRIMITIVEGEOFEATURE).getStore();
        StoreModel<DataStoreInfo> model = new StoreModel<DataStoreInfo>(s);

        model = serializeDeserialize(model);
        assertEquals(s, model.getObject());

        model.detach();
        assertEquals(s, model.getObject());

        model.setObject(null);
        assertNull(model.getObject());

        model = serializeDeserialize(model);
        model.detach();
        assertNull(model.getObject());
    }

    @Test
    public void testStoresModel() throws Exception {
        WorkspaceDetachableModel ws =
                new WorkspaceDetachableModel(getCatalog().getWorkspaceByName("sf"));
        StoresModel model = new StoresModel(ws);

        List<StoreInfo> stores = getCatalog().getStoresByWorkspace("ws", StoreInfo.class);
        for (StoreInfo s : stores) {
            assertTrue(model.getObject().contains(s));
        }

        model.detach();
        for (StoreInfo s : stores) {
            assertTrue(model.getObject().contains(s));
        }

        model = serializeDeserialize(model);
        for (StoreInfo s : stores) {
            assertTrue(model.getObject().contains(s));
        }
    }

    <T extends IModel> T serializeDeserialize(T model) throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream objout = new ObjectOutputStream(bout);
        objout.writeObject(model);
        objout.flush();
        objout.close();

        ObjectInputStream objin =
                new ObjectInputStream(new ByteArrayInputStream(bout.toByteArray()));
        return (T) objin.readObject();
    }
}

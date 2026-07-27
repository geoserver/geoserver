/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.vectortiles.wms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.OptionalDouble;
import org.geoserver.ows.Request;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.wms.FeatureInfoRequestParameters;
import org.geoserver.wms.GetFeatureInfoRequest;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMSMapContent;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.Version;
import org.geotools.vectortiles.store.VectorTilesRequestScale;
import org.junit.After;
import org.junit.Test;
import org.locationtech.jts.geom.Envelope;

/** Tests for {@link VectorTilesRequestScaleDenominatorHook} */
public class VectorTilesRequestScaleDenominatorHookTest {

    private VectorTilesRequestScaleDenominatorHook hook = new VectorTilesRequestScaleDenominatorHook();

    @After
    public void clearThreadLocalScale() {
        VectorTilesRequestScale.clear();
    }

    private OptionalDouble requestScale() {
        return VectorTilesRequestScale.get();
    }

    @Test
    public void beforeRenderPublishesMapContentScale() {
        WMSMapContent mapContent = mock(WMSMapContent.class);
        when(mapContent.getScaleDenominator()).thenReturn(25_000d);

        WMSMapContent returned = hook.beforeRender(mapContent);

        assertSame(mapContent, returned);
        assertEquals(OptionalDouble.of(25_000d), requestScale());
    }

    @Test
    public void finishedClearsScale() {
        VectorTilesRequestScale.set(1_000d);

        hook.finished(mock(Request.class));

        assertEquals(OptionalDouble.empty(), requestScale());
    }

    @Test
    public void operationDispatchedIgnoresNonGetFeatureInfoOperations() {
        Operation getMap = operation("GetMap", new GetMapRequest());

        Operation returned = hook.operationDispatched(mock(Request.class), getMap);

        assertSame(getMap, returned);
        assertEquals(OptionalDouble.empty(), requestScale());
    }

    @Test
    public void operationDispatchedPublishesGetFeatureInfoScale() {
        GetFeatureInfoRequest gfi = featureInfoRequest();
        double expected = new FeatureInfoRequestParameters(gfi).getScaleDenominator();

        hook.operationDispatched(mock(Request.class), operation("GetFeatureInfo", gfi));

        assertTrue(requestScale().isPresent());
        assertEquals(expected, requestScale().getAsDouble(), 1e-6);
    }

    private GetFeatureInfoRequest featureInfoRequest() {
        GetMapRequest map = new GetMapRequest();
        map.setBbox(new Envelope(-180, 180, -90, 90));
        map.setCrs(DefaultGeographicCRS.WGS84);
        map.setWidth(1024);
        map.setHeight(512);

        GetFeatureInfoRequest gfi = new GetFeatureInfoRequest();
        gfi.setGetMapRequest(map);
        gfi.setQueryLayers(List.of());
        return gfi;
    }

    private Operation operation(String id, Object requestBean) {
        Service wms = new Service("wms", null, new Version("1.3.0"), List.of(id));
        return new Operation(id, wms, null, new Object[] {requestBean});
    }
}

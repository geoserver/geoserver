/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.web.data.store;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.web.data.store.DefaultDataStoreEditPanel.URLModel;
import org.junit.Test;

/**
 * Testing class for {@link URLModel} use cases.
 *
 * @author Fernando Mino - Geosolutions
 */
public class URLModelTest {

    private static final String URL_VALUE = "https://www.geoserver.org";
    private static final String URL_PARAM = "urlParam";

    /** Test a bug validating the URL protocol disallowing to use 'https://' as valid one. */
    @Test
    public void testHttpsURLSetValue() {
        Map<String, Object> params = new HashMap<String, Object>();
        IModel model = Model.ofMap(params);
        URLModel urlModel = new URLModel(model, URL_PARAM);
        urlModel.setObject(URL_VALUE);
        assertEquals(URL_VALUE, urlModel.getObject());
    }

    /** Tests setting and getting a http:// protocol URL. */
    @Test
    public void testHttpURLSetValue() {
        final String urlValue = "http://www.geoserver.org/";
        Map<String, Object> params = new HashMap<String, Object>();
        IModel model = Model.ofMap(params);
        URLModel urlModel = new URLModel(model, URL_PARAM);
        urlModel.setObject(urlValue);
        assertEquals(urlValue, urlModel.getObject());
    }

    /** Tests setting and getting a file:// protocol URL. */
    @Test
    public void testFileURLSetValue() {
        final String urlValue = "file:///home/fernando/mino.xml";
        Map<String, Object> params = new HashMap<String, Object>();
        IModel model = Model.ofMap(params);
        URLModel urlModel = new URLModel(model, URL_PARAM);
        urlModel.setObject(urlValue);
        assertEquals(urlValue, urlModel.getObject());
    }

    /** Tests setting an URL without protocol and getting it with file:// added. */
    @Test
    public void testNonProtocolUrl() {
        final String urlValue = "/home/geosolutions/file.xml";
        Map<String, Object> params = new HashMap<String, Object>();
        IModel model = Model.ofMap(params);
        URLModel urlModel = new URLModel(model, URL_PARAM);
        urlModel.setObject(urlValue);
        assertEquals("file://" + urlValue, urlModel.getObject());
    }
}

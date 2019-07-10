/*
 *  (c) 2019 Open Source Geospatial Foundation - all rights reserved
 *  This code is licensed under the GPL 2.0 license, available at the root
 *  application directory.
 *
 */

package org.geoserver.web.util;

import static org.junit.Assert.assertEquals;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.junit.Test;
import org.postgresql.jdbc.SslMode;

public class EnumAdapterModelTest {

    @Test
    public void testGetObject() {
        IModel delegate = new Model("DISABLE");
        EnumAdapterModel model = new EnumAdapterModel(delegate, SslMode.class);
        assertEquals(SslMode.DISABLE, model.getObject());
    }
}

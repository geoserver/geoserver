/*
 *  (c) 2019 Open Source Geospatial Foundation - all rights reserved
 *  This code is licensed under the GPL 2.0 license, available at the root
 *  application directory.
 */

package org.geoserver.generatedgeometries.dummy;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

class DummyGGSPanel extends Panel {

    DummyGGSPanel(String id, IModel<?> model) {
        super(id, model);
        setOutputMarkupId(true);
    }
}

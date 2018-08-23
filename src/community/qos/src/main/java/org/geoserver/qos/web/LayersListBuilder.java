/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.web;

import java.io.Serializable;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.geoserver.qos.xml.LimitedAreaRequestConstraints;

public interface LayersListBuilder<T extends LimitedAreaRequestConstraints> extends Serializable {

    WebMarkupContainer build(WebMarkupContainer mainDiv, ModalWindow modalWindow, IModel<T> model);
}

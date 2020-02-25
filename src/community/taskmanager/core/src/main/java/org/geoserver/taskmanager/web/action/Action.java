/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.web.action;

import java.io.Serializable;
import java.util.List;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.geoserver.taskmanager.util.Named;
import org.geoserver.taskmanager.web.ConfigurationPage;

/** @author Niels Charlier */
public interface Action extends Named, Serializable {

    /**
     * Execute this action.
     *
     * @param onPage the configuration page.
     * @param target the target of the ajax request that executed this action.
     * @param valueModel the value of the attribute, for reading and writing.
     * @param dependsOnRawValues raw values of depending attributes.
     */
    void execute(
            ConfigurationPage onPage,
            AjaxRequestTarget target,
            IModel<String> valueModel,
            List<String> dependsOnRawValues);

    /**
     * Check whether this action can be executed with current values. \
     *
     * @param value the value of the attribute.
     * @param dependsOnRawValues raw values of depending attributes.
     * @return whether this action accepts these values.
     */
    default boolean accept(String value, List<String> dependsOnRawValues) {
        return true;
    }
}

/* (c) 2017-2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.beans;

import java.util.List;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.geoserver.taskmanager.web.ConfigurationPage;
import org.geoserver.taskmanager.web.action.Action;
import org.springframework.stereotype.Component;

@Component
public class DummyAction implements Action {

    public static final String NAME = "actionDummy";

    private static final long serialVersionUID = 2055260073253741911L;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void execute(
            ConfigurationPage onPage,
            AjaxRequestTarget target,
            IModel<String> valueModel,
            List<String> dependentValues) {}
}

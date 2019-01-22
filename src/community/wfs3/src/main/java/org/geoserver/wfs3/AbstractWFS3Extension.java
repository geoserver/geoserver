/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3;

import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.util.Map;
import org.geoserver.platform.ServiceException;

public abstract class AbstractWFS3Extension implements WFS3Extension {

    protected void addSchemasAndParameters(OpenAPI api, OpenAPI template) {
        // and add all schemas and parameters
        Components apiComponents = api.getComponents();
        Components tileComponents = template.getComponents();
        Map<String, Schema> tileSchemas = tileComponents.getSchemas();
        apiComponents.getSchemas().putAll(tileSchemas);
        Map<String, Parameter> tileParameters = tileComponents.getParameters();
        apiComponents.getParameters().putAll(tileParameters);
    }

    /**
     * Reads the template to customize (each time, as the object tree is not thread safe nor
     * cloneable not serializable)
     */
    protected OpenAPI readTemplate(String source) {
        try {
            return Yaml.mapper().readValue(source, OpenAPI.class);
        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }
}

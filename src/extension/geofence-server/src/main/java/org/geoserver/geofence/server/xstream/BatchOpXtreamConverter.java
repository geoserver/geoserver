/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.server.xstream;

import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.reflection.AbstractReflectionConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.mapper.Mapper;
import org.apache.commons.lang3.StringUtils;
import org.geoserver.geofence.server.rest.xml.AbstractPayload;
import org.geoserver.geofence.server.rest.xml.BatchOperation;
import org.geoserver.geofence.server.rest.xml.JaxbAdminRule;
import org.geoserver.geofence.server.rest.xml.JaxbRule;

/** Converter for a {@link BatchOperation} instance. */
public class BatchOpXtreamConverter extends AbstractReflectionConverter {

    public BatchOpXtreamConverter(Mapper mapper, ReflectionProvider reflectionProvider) {
        super(mapper, reflectionProvider);
    }

    @Override
    public boolean canConvert(Class type) {
        return BatchOperation.class.isAssignableFrom(type);
    }

    @Override
    protected Object instantiateNewInstance(
            HierarchicalStreamReader reader, UnmarshallingContext context) {
        return new BatchOperation();
    }

    @Override
    public Object doUnmarshal(
            Object result, HierarchicalStreamReader reader, UnmarshallingContext context) {

        BatchOperation operation = (BatchOperation) result;
        String srv = reader.getAttribute("service");
        String type = reader.getAttribute("type");
        String id = reader.getAttribute("id");

        if (!StringUtils.isBlank(srv))
            operation.setService(BatchOperation.ServiceName.valueOf(srv));
        if (!StringUtils.isBlank(type)) operation.setType(BatchOperation.TypeName.valueOf(type));
        if (!StringUtils.isBlank(id)) operation.setId(Long.valueOf(id));
        while (reader.hasMoreChildren()) {
            reader.moveDown();
            String nodeName = reader.getNodeName();
            Object payload = null;
            if (nodeName.equals("Rule")) {
                payload = context.convertAnother(operation, JaxbRule.class);
            } else if (nodeName.equals("AdminRule")) {
                payload = context.convertAnother(operation, JaxbAdminRule.class);
            }
            operation.setPayload((AbstractPayload) payload);
            reader.moveUp();
        }

        return result;
    }
}

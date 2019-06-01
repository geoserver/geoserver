/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.rest;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.replay;

import java.beans.PropertyDescriptor;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import org.geoserver.importer.ImportContext;
import org.geoserver.importer.ImportTask;
import org.geoserver.importer.Importer;
import org.geoserver.importer.rest.converters.ImportJSONReader;
import org.geoserver.importer.rest.converters.ImportJSONWriter;
import org.geoserver.importer.rest.converters.ImportJSONWriter.FlushableJSONBuilder;
import org.geoserver.importer.transform.ImportTransform;
import org.geoserver.rest.RequestInfo;
import org.geotools.data.DataTestCase;
import org.springframework.beans.BeanUtils;
import org.springframework.web.context.request.AbstractRequestAttributes;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/** @author Ian Schneider <ischneider@opengeo.org> */
public abstract class TransformTestSupport extends DataTestCase {

    public TransformTestSupport() {
        super(TransformTestSupport.class.getName());
    }

    public void doJSONTest(ImportTransform transform) throws Exception {
        StringWriter buffer = new StringWriter();

        Importer im = createNiceMock(Importer.class);
        RequestInfo ri = createNiceMock(RequestInfo.class);

        replay(im, ri);

        RequestAttributes oldAttributes = RequestContextHolder.getRequestAttributes();
        RequestContextHolder.setRequestAttributes(new TransformTestSupport.MapRequestAttributes());

        RequestInfo.set(ri);

        ImportJSONWriter jsonio = new ImportJSONWriter(im);
        FlushableJSONBuilder builder = new FlushableJSONBuilder(buffer);

        ImportContext c = new ImportContext(0);
        c.addTask(new ImportTask());

        jsonio.transform(builder, transform, 0, c.task(0), true, 1);

        ImportJSONReader reader = new ImportJSONReader(im);
        ImportTransform transform2 = reader.transform(buffer.toString());
        PropertyDescriptor[] pd = BeanUtils.getPropertyDescriptors(transform.getClass());

        for (int i = 0; i < pd.length; i++) {
            assertEquals(
                    "expected same value of " + pd[i].getName(),
                    pd[i].getReadMethod().invoke(transform),
                    pd[i].getReadMethod().invoke(transform2));
        }
        RequestContextHolder.setRequestAttributes(oldAttributes);
    }

    public static class MapRequestAttributes extends AbstractRequestAttributes {
        Map<String, Object> requestAttributes = new HashMap<>();

        @Override
        public Object getAttribute(String name, int scope) {
            return requestAttributes.get(name);
        }

        @Override
        public void setAttribute(String name, Object value, int scope) {
            requestAttributes.put(name, value);
        }

        @Override
        public void removeAttribute(String name, int scope) {
            requestAttributes.remove(name);
        }

        @Override
        public String[] getAttributeNames(int scope) {
            return requestAttributes.keySet().toArray(new String[requestAttributes.size()]);
        }

        @Override
        protected void updateAccessedSessionAttributes() {}

        @Override
        public void registerDestructionCallback(String name, Runnable callback, int scope) {}

        @Override
        public Object resolveReference(String key) {
            return null;
        }

        @Override
        public String getSessionId() {
            return null;
        }

        @Override
        public Object getSessionMutex() {
            return null;
        }
    }
}

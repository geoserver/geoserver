package org.geoserver.filter.pojo;

import java.util.Iterator;

import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.jxpath.JXPathContextFactory;
import org.geotools.factory.Hints;
import org.geotools.filter.expression.PropertyAccessor;
import org.geotools.filter.expression.PropertyAccessorFactory;
import org.opengis.feature.Property;
import org.opengis.feature.type.PropertyType;
import org.xml.sax.helpers.NamespaceSupport;

import com.google.common.collect.Iterators;

/**
 * {@link PropertyAccessorFactory} to access properties out of regular java beans.
 * <p>
 * Implementation details: the {@link PropertyAccessor} created by this factory uses Apache <a
 * href="http://commons.apache.org/jxpath/">Commons JXPath</a> to evaluate the xpath expressions
 * against the provided Java Bean.
 * </p>
 * Also, this factory explicitly avoids returning a {@code PropertyAccessor} if the object to
 * evaluate is derived from {@link PropertyType} or {@link Property}.
 * 
 * @author groldan
 * 
 */
public class PojoPropertyAccessorFactory implements PropertyAccessorFactory {

    @Override
    public PropertyAccessor createPropertyAccessor(final Class<?> type, final String xpath,
            final Class<?> target, final Hints hints) {

        if (Property.class.isAssignableFrom(type) || PropertyType.class.isAssignableFrom(type)) {
            return null;
        }
        if ("".equals(xpath)) {
            return null;
        }

        NamespaceSupport context = null;
        if (hints != null) {
            Object object = hints.get(PropertyAccessorFactory.NAMESPACE_CONTEXT);
            if (object instanceof NamespaceSupport) {
                context = (NamespaceSupport) object;
            }
        }
        return new PojoPropertyAccessor(context);
    }

    /**
     * We strip off namespace prefix, we need new feature model to do this property
     * <ul>
     * <li>BEFORE: foo:bar
     * <li>AFTER: bar
     * </ul>
     * 
     * @param xpath
     * @return xpath with any XML prefixes removed
     */
    private static String stripPrefix(String xpath) {
        int split = xpath.indexOf(":");
        if (split != -1) {
            return xpath.substring(split + 1);
        }
        return xpath;
    }

    private static String stripPrefixes(String xpath) {
        if (xpath.indexOf('/') == -1) {
            return stripPrefix(xpath);
        }

        String[] steps = xpath.split("/");
        StringBuilder sb = new StringBuilder();
        for (Iterator<String> it = Iterators.forArray(steps); it.hasNext();) {
            sb.append(stripPrefix(it.next()));
            if (it.hasNext()) {
                sb.append('/');
            }
        }
        return sb.toString();
    }

    private static class PojoPropertyAccessor implements PropertyAccessor {

        private static final JXPathContextFactory CONTEXT_FACTORY = JXPathContextFactory
                .newInstance();

        private final NamespaceSupport nscontext;

        public PojoPropertyAccessor(final NamespaceSupport context) {
            this.nscontext = context;
        }

        @Override
        public boolean canHandle(final Object object, final String xpath, final Class<?> target) {
            return (xpath != null) && !"".equals(xpath.trim());
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        @Override
        public Object get(final Object object, final String xpath, final Class target)
                throws IllegalArgumentException {
            Object property = context(object).getValue(stripPrefixes(xpath));
            return property;
        }

        @Override
        public <T> void set(final Object object, final String xpath, final T value,
                final Class<T> target) throws IllegalArgumentException {
            context(object).setValue(xpath, value);
        }

        private JXPathContext context(Object object) {
            JXPathContext context = CONTEXT_FACTORY.newContext(null, object);
            context.setLenient(true);

            if (this.nscontext != null) {
                @SuppressWarnings("unchecked")
                Iterator<String> prefixes = Iterators.forEnumeration(nscontext.getPrefixes());
                String prefix;
                String namespaceURI;
                while (prefixes.hasNext()) {
                    prefix = prefixes.next();
                    namespaceURI = nscontext.getURI(prefix);
                    context.registerNamespace(prefix, namespaceURI);
                }
            }
            return context;
        }
    }
}

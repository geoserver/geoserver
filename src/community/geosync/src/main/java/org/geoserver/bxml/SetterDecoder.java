package org.geoserver.bxml;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.commons.beanutils.BeanUtils;
import org.gvsig.bxml.stream.BxmlStreamReader;
import org.springframework.util.Assert;

/**
 * The Class SetterDecoder execute the decoder given in <code>propertyDecoder</code> and sets the
 * result in property <code>propertyName</code> of <code>target</code>.
 * 
 * @param <T>
 *            the generic type
 * 
 * @author groldan
 */
public class SetterDecoder<T> implements Decoder<T> {

    /** The property decoder. */
    private final Decoder<? extends T> propertyDecoder;

    /** The target. */
    private final Object target;

    /** The property name. */
    private final String propertyName;

    /** The setter. */
    private final Method setter;

    /** The is collection. */
    private boolean isCollection;

    /**
     * Instantiates a new setter decoder.
     * 
     * @param propertyDecoder
     *            the property decoder
     * @param target
     *            the target
     * @param propertyName
     *            the property name
     */
    public SetterDecoder(final Decoder<? extends T> propertyDecoder, final Object target,
            final String propertyName) {
        Assert.notNull(propertyDecoder);
        Assert.notNull(target);
        Assert.notNull(propertyName);

        this.propertyDecoder = propertyDecoder;
        this.target = target;
        this.propertyName = propertyName;
        this.setter = findSetter();
    }

    /**
     * Find setter.
     * 
     * @return the method
     */
    private Method findSetter() {
        for (Method m : target.getClass().getMethods()) {
            if (!Modifier.isPublic(m.getModifiers())) {
                continue;
            }
            String name = m.getName().toLowerCase();
            if (name.equals("set" + propertyName.toLowerCase())) {
                this.isCollection = false;
                return m;
            }

            if (name.equals("get" + propertyName.toLowerCase())) {
                Class<?> returnType = m.getReturnType();
                if (returnType != null && Collection.class.isAssignableFrom(returnType)) {
                    this.isCollection = true;
                    return m;
                }
            }
        }
        throw new IllegalArgumentException("No setter for property " + propertyName
                + " found in class " + target.getClass().getName());
    }

    /**
     * Decode.
     * 
     * @param r
     *            the r
     * @return the t
     * @throws Exception
     *             the exception
     */
    @Override
    public T decode(BxmlStreamReader r) throws Exception {
        T propertyValue = propertyDecoder.decode(r);

        if (isCollection) {
            @SuppressWarnings("unchecked")
            Collection<T> c = (Collection<T>) this.setter.invoke(target, null);
            c.add(propertyValue);
        } else {
            BeanUtils.setProperty(target, propertyName, propertyValue);
        }
        return propertyValue;
    }

    /**
     * Can handle.
     * 
     * @param name
     *            the name
     * @return true, if successful
     */
    @Override
    public boolean canHandle(QName name) {
        return propertyDecoder.canHandle(name);
    }

    /**
     * Gets the targets.
     * 
     * @return the targets
     */
    @Override
    public Set<QName> getTargets() {
        return propertyDecoder.getTargets();
    }

}

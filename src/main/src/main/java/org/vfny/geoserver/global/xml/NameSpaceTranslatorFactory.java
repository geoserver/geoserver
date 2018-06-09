/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.global.xml;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

/**
 * NameSpaceTranslatorFactory purpose.
 *
 * <p>Follows the factory pattern. Creates and stores a list of name space translators.
 *
 * @see NameSpaceTranslator
 * @author dzwiers, Refractions Research, Inc.
 * @author $Author: dmzwiers $ (last modification)
 * @version $Id$
 */
public class NameSpaceTranslatorFactory {
    /** map of namespace names as Strings -> Class representations of NameSpaceTranslators */
    private Map namespaceTranslators;

    /** map of prefixs as String -> Instances of NameSpaceTranslators */
    private Map namespaceTranslatorInstances;

    /** the only instance */
    private static final NameSpaceTranslatorFactory instance = new NameSpaceTranslatorFactory();

    /**
     * NameSpaceTranslatorFactory constructor.
     *
     * <p>Loads some default prefixes into memory when the class is first loaded.
     */
    private NameSpaceTranslatorFactory() {
        namespaceTranslators = new HashMap();
        namespaceTranslatorInstances = new HashMap();

        // TODO replace null for these default namespaces.
        namespaceTranslators.put("http://www.w3.org/2001/XMLSchema", XMLSchemaTranslator.class);
        namespaceTranslators.put("http://www.opengis.net/gml", GMLSchemaTranslator.class);

        addNameSpaceTranslator("xs", "http://www.w3.org/2001/XMLSchema");
        addNameSpaceTranslator("xsd", "http://www.w3.org/2001/XMLSchema");
        addNameSpaceTranslator("gml", "http://www.opengis.net/gml");
    }

    /**
     * getInstance purpose.
     *
     * <p>Completes the singleton pattern of this factory class.
     *
     * @return NameSpaceTranslatorFactory The instance.
     */
    public static NameSpaceTranslatorFactory getInstance() {
        return instance;
    }

    /**
     * addNameSpaceTranslator purpose.
     *
     * <p>Adds a new translator for the namespace specified if a NameSpaceTranslator was registered
     * for that namespace.
     *
     * <p>Some the magic for creating instances using the classloader occurs here (ie. the
     * translators are not loaded lazily)
     *
     * @param prefix The desired namespace prefix
     * @param namespace The desired namespace.
     */
    public void addNameSpaceTranslator(String prefix, String namespace) {
        if ((prefix == null) || (namespace == null)) {
            throw new NullPointerException();
        }

        try {
            Class nstClass = (Class) namespaceTranslators.get(namespace);

            if (nstClass == null) {
                return;
            }

            Constructor nstConstructor =
                    nstClass.getConstructor(
                            new Class[] {
                                String.class,
                            });
            NameSpaceTranslator nst =
                    (NameSpaceTranslator)
                            nstConstructor.newInstance(
                                    new Object[] {
                                        prefix,
                                    });
            namespaceTranslatorInstances.put(prefix, nst);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * getNameSpaceTranslator purpose.
     *
     * <p>Description ...
     *
     * @param prefix the prefix of the translator to get.
     * @return the translator, or null if it was not found
     */
    public NameSpaceTranslator getNameSpaceTranslator(String prefix) {
        return (NameSpaceTranslator) namespaceTranslatorInstances.get(prefix);
    }

    /**
     * registerNameSpaceTranslator purpose.
     *
     * <p>Registers a namespace and it's translator with the factory. good for adding additional
     * namespaces :)
     *
     * @param namespace The namespace.
     * @param nameSpaceTranslator The translator class for this namespace.
     */
    public void registerNameSpaceTranslator(String namespace, Class nameSpaceTranslator) {
        if ((nameSpaceTranslator != null)
                && NameSpaceTranslator.class.isAssignableFrom(nameSpaceTranslator)) {
            namespaceTranslators.put(namespace, nameSpaceTranslator);
        }
    }
}

/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * NameSpaceTranslator purpose.
 *
 * <p>Helps perform translation between element names, definition names and their java types for a
 * particular namespace and namespace prefix.
 *
 * <p>Each name space translator should contain a list of name space elements for their particular
 * prefix. This loading should not be completed lazily to avoid performance lags at run time. When
 * ever posible constants should alos be used for performance purposes.
 *
 * <p>USE: <code>
 * NameSpaceTranslator nst = NameSpaceTranslatorFactor.getInstance().getNameSpaceTranslator("xs");
 * Class cls = nst.getElement("string").getJavaClass();
 * ...
 * Object obj // contains some data, what can it be represented as?
 * String elementName = ((NameSpaceElement)nst.getElements(obj).toArray()[0]).getTypeRefName();
 * </code>
 *
 * @author dzwiers, Refractions Research, Inc.
 * @author $Author: dmzwiers $ (last modification)
 * @version $Id$
 */
public abstract class NameSpaceTranslator {
    /** the prefix for this translator instance */
    private String prefix;

    /**
     * NameSpaceTranslator constructor.
     *
     * <p>Creates an instance of this translator for the given prefix.
     *
     * @param prefix The prefix for which this tranlator will tranlate. A null prefix will affect
     *     the NameSpaceElements returned by the access methods.
     * @see #NameSpaceElement(String)
     */
    public NameSpaceTranslator(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Retrive all elements that can be used with the provided type.
     *
     * <p>Looks for Elements who's Class objects, or the parents of the Class object are compatible
     * with this class object.
     *
     * @param type Class the class to attempt to find related elements for.
     * @return Set a set of associated NameSpaceElements
     */
    public Set getAssociatedTypes(Class type) {
        if (type == null) {
            return null;
        }

        HashSet r = new HashSet();
        Set elems = getElements();
        Iterator i = elems.iterator();

        while (i.hasNext()) {
            NameSpaceElement nse = (NameSpaceElement) i.next();

            if (nse != null) {
                Class cls = nse.getJavaClass();

                if ((cls != null) && cls.isAssignableFrom(type) && !cls.equals(Object.class)) {
                    r.add(nse);
                }
            }
        }

        return r;
    }

    /**
     * Looks for Elements who's name is the same or a super set of this name.
     *
     * <p>(ie. name.indexOf(type)!=-1)
     *
     * @param type String the class to attempt to find related elements for.
     * @return Set a set of associated NameSpaceElements
     * @see String#indexOf(String)
     */
    public Set getAssociatedTypes(String type) {
        if (type == null) {
            return null;
        }

        HashSet r = new HashSet();
        Set elems = getElements();
        Iterator i = elems.iterator();

        while (i.hasNext()) {
            NameSpaceElement nse = (NameSpaceElement) i.next();

            if (nse != null) {
                String name = nse.getTypeRefName();

                if ((name != null) && (name.indexOf(type) != -1)) {
                    r.add(nse);
                }

                name = nse.getTypeDefName();

                if ((name != null) && (name.indexOf(type) != -1)) {
                    r.add(nse);
                }
            }
        }

        return r;
    }

    /**
     * isValidDefinition purpose.
     *
     * <p>checks to see if the definition provided is found in the list of elements for this
     * namespace.
     *
     * @param definition The definition name to check for, may be either definition or
     *     prefix:definition.
     * @return true when found, false otherwise.
     */
    public boolean isValidDefinition(String definition) {
        if ((definition == null) || (definition == "")) {
            return false;
        }

        Set elems = getElements();
        Iterator i = elems.iterator();

        while (i.hasNext()) {
            NameSpaceElement nse = (NameSpaceElement) i.next();

            if (nse == null) {
                continue;
            }

            String def = nse.getTypeDefName();

            if ((def != null) && def.equals(definition)) {
                return true;
            }

            def = nse.getQualifiedTypeDefName();

            if ((def != null) && def.equals(definition)) {
                return true;
            }
        }

        return false;
    }

    /**
     * isValidTypeRef purpose.
     *
     * <p>checks to see if the reference provided is found in the list of elements for this
     * namespace.
     *
     * @param type The reference name to check for, may be either reference or prefix:reference.
     * @return true when found, false otherwise.
     */
    public boolean isValidTypeRef(String type) {
        if ((type == null) || (type == "")) {
            return false;
        }

        Set elems = getElements();
        Iterator i = elems.iterator();

        while (i.hasNext()) {
            NameSpaceElement nse = (NameSpaceElement) i.next();

            if (nse == null) {
                continue;
            }

            String tp = nse.getTypeRefName();

            if ((tp != null) && tp.equals(type)) {
                return true;
            }

            tp = nse.getQualifiedTypeRefName();

            if ((tp != null) && tp.equals(type)) {
                return true;
            }
        }

        return false;
    }

    /**
     * getElements purpose.
     *
     * <p>returns the set of elements.
     *
     * @return Set
     */
    public abstract Set getElements();

    /**
     * getElements purpose.
     *
     * <p>Returns a set of all elements with the exact class specified.
     *
     * @param type Class the class of elements to get
     * @return Set
     */
    public Set getElements(Class type) {
        if (type == null) {
            return null;
        }

        HashSet r = new HashSet();
        Set elems = getElements();
        Iterator i = elems.iterator();

        while (i.hasNext()) {
            NameSpaceElement nse = (NameSpaceElement) i.next();

            if ((nse != null) && type.equals(nse.getJavaClass())) {
                r.add(nse);
            }
        }

        return r;
    }

    /**
     * Gets an element definition by name.
     *
     * @param name The name of the element definition
     * @return NameSpaceElement
     */
    public NameSpaceElement getElement(String name) {
        if (name == null) {
            return null;
        }

        Set elems = getElements();
        Iterator i = elems.iterator();

        while (i.hasNext()) {
            NameSpaceElement nse = (NameSpaceElement) i.next();

            if (nse != null) {
                if (name.equals(nse.getTypeRefName())) {
                    return nse;
                }

                if (name.equals(nse.getTypeDefName())) {
                    return nse;
                }

                if (name.equals(nse.getQualifiedTypeRefName())) {
                    return nse;
                }

                if (name.equals(nse.getQualifiedTypeDefName())) {
                    return nse;
                }
            }
        }

        return null;
    }

    /**
     * Gets the default element for the class type passed in. Note that this is a bit hacky, as it
     * doesn't not depend on a real namespace map, but on careful assignment of the
     * NamespaceElements, so that each class only has one that returns true for isDefault(). Sorry
     * for the hackiness, I need to get a release out.
     */
    public NameSpaceElement getDefaultElement(Class type) {
        Set posibilities = getElements(type);

        // System.out.println("getting default for type: " + type + " = " + posibilities);
        if (posibilities.size() == 0) {
            return null;
        }

        Iterator i = posibilities.iterator();

        while (i.hasNext()) {
            NameSpaceElement nse = (NameSpaceElement) i.next();

            if (nse != null) {
                if (nse.isDefault()) {
                    return nse;
                }
            }
        }

        return null;
    }

    /**
     * Gets an element definition by name.
     *
     * @param name The name of the element definition
     * @return NameSpaceElement
     */
    public NameSpaceElement getElement(Class type, String name) {
        if (type == null) {
            return null;
        }

        Set posibilities = getElements(type);

        if (posibilities.size() == 0) {
            return null;
        }

        if (posibilities.size() == 1) {
            return (NameSpaceElement) posibilities.toArray()[0];
        }

        if (name == null) {
            return (NameSpaceElement) posibilities.toArray()[0];
        }

        Iterator i = posibilities.iterator();

        while (i.hasNext()) {
            NameSpaceElement nse = (NameSpaceElement) i.next();

            if (nse != null) {
                if (name.equals(nse.getTypeRefName())) {
                    return nse;
                }

                if (name.equals(nse.getTypeDefName())) {
                    return nse;
                }

                if (name.equals(nse.getQualifiedTypeRefName())) {
                    return nse;
                }

                if (name.equals(nse.getQualifiedTypeDefName())) {
                    return nse;
                }

                if (nse.isDefault()) {
                    return nse;
                }
            }
        }

        return (NameSpaceElement) posibilities.toArray()[0];
    }

    /**
     * getNameSpace purpose.
     *
     * <p>Returns the current namespace. Should be implemented as a constant.
     *
     * @return String
     */
    public abstract String getNameSpace();

    /**
     * getPrefix purpose.
     *
     * <p>Returns the prefix that this namespace represents.
     *
     * @return String the prefix, null if it does not exist
     */
    public final String getPrefix() {
        return prefix;
    }
}

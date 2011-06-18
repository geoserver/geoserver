/*
 * @(#)ConfigurationStore.java
 *
 * Copyright 2004-2006 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *   1. Redistribution of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *   2. Redistribution in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING
 * ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN MICROSYSTEMS, INC. ("SUN")
 * AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE
 * AS A RESULT OF USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS
 * DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR ANY LOST
 * REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL,
 * INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY
 * OF LIABILITY, ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE,
 * EVEN IF SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that this software is not designed or intended for use in
 * the design, construction, operation or maintenance of any nuclear facility.
 */

package com.sun.xacml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.sun.xacml.attr.AttributeFactory;
import com.sun.xacml.attr.AttributeFactoryProxy;
import com.sun.xacml.attr.AttributeProxy;
import com.sun.xacml.attr.BaseAttributeFactory;
import com.sun.xacml.attr.StandardAttributeFactory;
import com.sun.xacml.combine.BaseCombiningAlgFactory;
import com.sun.xacml.combine.CombiningAlgFactory;
import com.sun.xacml.combine.CombiningAlgFactoryProxy;
import com.sun.xacml.combine.CombiningAlgorithm;
import com.sun.xacml.combine.StandardCombiningAlgFactory;
import com.sun.xacml.cond.BaseFunctionFactory;
import com.sun.xacml.cond.BasicFunctionFactoryProxy;
import com.sun.xacml.cond.Function;
import com.sun.xacml.cond.FunctionFactory;
import com.sun.xacml.cond.FunctionFactoryProxy;
import com.sun.xacml.cond.FunctionProxy;
import com.sun.xacml.cond.StandardFunctionFactory;
import com.sun.xacml.cond.cluster.FunctionCluster;
import com.sun.xacml.finder.AttributeFinder;
import com.sun.xacml.finder.AttributeFinderModule;
import com.sun.xacml.finder.PolicyFinder;
import com.sun.xacml.finder.PolicyFinderModule;
import com.sun.xacml.finder.ResourceFinder;
import com.sun.xacml.finder.ResourceFinderModule;

/**
 * This class supports run-time loading of configuration data. It loads the configurations from an
 * XML file that conforms to the configuration schema. By design this class does not get used
 * automatically, nor does it change the state of the system directly. A programmer must choose to
 * support this mechanism in their program, and then must explicitly use loaded elements. This way,
 * the programmer still has full control over their security model, but also has the convenience of
 * re-using a common configuration mechanism. See
 * http://sunxacml.sourceforge.net/schema/config-0.4.xsd for the valid schema.
 * <p>
 * Note that becuase this doesn't tie directly into the rest of the code, you are still free to
 * design your own run-time configuration mechanisms. This is simply provided as a convenience, and
 * so that all programmers can start from a common point.
 * 
 * @since 1.2
 * @author Seth Proctor
 * 
 *         Adding generic type support by Christian Mueller (geotools)
 */
public class ConfigurationStore {

    /**
     * Property used to specify the configuration file.
     */
    public static final String PDP_CONFIG_PROPERTY = "com.sun.xacml.PDPConfigFile";

    // pdp elements
    private PDPConfig defaultPDPConfig;

    private HashMap<String, PDPConfig> pdpConfigMap;

    // attribute factory elements
    private AttributeFactory defaultAttributeFactory;

    private HashMap<String, AttributeFactory> attributeMap;

    // combining algorithm factory elements
    private CombiningAlgFactory defaultCombiningFactory;

    private HashMap<String, CombiningAlgFactory> combiningMap;

    // function factory elements
    private FunctionFactoryProxy defaultFunctionFactoryProxy;

    private HashMap<String, FunctionFactoryProxy> functionMap;

    // the classloader we'll use for loading classes
    private ClassLoader loader;

    // the logger we'll use for all messages
    private static final Logger logger = Logger.getLogger(ConfigurationStore.class.getName());

    /**
     * Default constructor. This constructor uses the <code>PDP_CONFIG_PROPERTY</code> property to
     * load the configuration. If the property isn't set, if it names a file that can't be accessed,
     * or if the file is invalid, then an exception is thrown.
     * 
     * @throws ParsingException
     *             if anything goes wrong during the parsing of the configuration file, the class
     *             loading, or the factory and pdp setup
     */
    public ConfigurationStore() throws ParsingException {
        String configFile = System.getProperty(PDP_CONFIG_PROPERTY);

        // make sure that the right property was set
        if (configFile == null) {
            logger.severe("A property defining a config file was expected, "
                    + "but none was provided");

            throw new ParsingException("Config property " + PDP_CONFIG_PROPERTY
                    + " needs to be set");
        }

        try {
            setupConfig(new File(configFile));
        } catch (ParsingException pe) {
            logger.log(Level.SEVERE, "Runtime config file couldn't be loaded"
                    + " so no configurations will be available", pe);
            throw pe;
        }
    }

    /**
     * Constructor that explicitly specifies the configuration file to load. This is useful if your
     * security model doesn't allow the use of properties, if you don't want to use a property to
     * specify a configuration file, or if you want to use more then one configuration file. If the
     * file can't be accessed, or if the file is invalid, then an exception is thrown.
     * 
     * @throws ParsingException
     *             if anything goes wrong during the parsing of the configuration file, the class
     *             loading, or the factory and pdp setup
     */
    public ConfigurationStore(File configFile) throws ParsingException {
        try {
            setupConfig(configFile);
        } catch (ParsingException pe) {
            logger.log(Level.SEVERE, "Runtime config file couldn't be loaded"
                    + " so no configurations will be available", pe);
            throw pe;
        }
    }

    /**
     * Private helper function used by both constructors to actually load the configuration data.
     * This is the root of several private methods used to setup all the pdps and factories.
     */
    private void setupConfig(File configFile) throws ParsingException {
        logger.config("Loading runtime configuration");

        // load our classloader
        loader = getClass().getClassLoader();

        // get the root node from the configuration file
        Node root = getRootNode(configFile);

        // initialize all the maps
        pdpConfigMap = new HashMap<String, PDPConfig>();
        attributeMap = new HashMap<String, AttributeFactory>();
        combiningMap = new HashMap<String, CombiningAlgFactory>();
        functionMap = new HashMap<String, FunctionFactoryProxy>();

        // get the default names
        NamedNodeMap attrs = root.getAttributes();
        String defaultPDP = attrs.getNamedItem("defaultPDP").getNodeValue();
        String defaultAF = getDefaultFactory(attrs, "defaultAttributeFactory");
        String defaultCAF = getDefaultFactory(attrs, "defaultCombiningAlgFactory");
        String defaultFF = getDefaultFactory(attrs, "defaultFunctionFactory");

        // loop through all the root-level elements, for each one getting its
        // name and then loading the right kind of element
        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            String childName = child.getNodeName();
            String elementName = null;

            // get the element's name
            if (child.getNodeType() == Node.ELEMENT_NODE)
                elementName = child.getAttributes().getNamedItem("name").getNodeValue();

            // see if this is a pdp or a factory, and load accordingly,
            // putting the new element into the respective map...make sure
            // that we're never loading something with the same name twice
            if (childName.equals("pdp")) {
                if (logger.isLoggable(Level.CONFIG))
                    logger.config("Loading PDP: " + elementName);
                if (pdpConfigMap.containsKey(elementName))
                    throw new ParsingException("more that one pdp with " + "name \"" + elementName
                            + "\"");
                pdpConfigMap.put(elementName, parsePDPConfig(child));
            } else if (childName.equals("attributeFactory")) {
                if (logger.isLoggable(Level.CONFIG))
                    logger.config("Loading AttributeFactory: " + elementName);
                if (attributeMap.containsKey(elementName))
                    throw new ParsingException("more that one " + "attributeFactory with name "
                            + elementName + "\"");
                attributeMap.put(elementName, parseAttributeFactory(child));
            } else if (childName.equals("combiningAlgFactory")) {
                if (logger.isLoggable(Level.CONFIG))
                    logger.config("Loading CombiningAlgFactory: " + elementName);
                if (combiningMap.containsKey(elementName))
                    throw new ParsingException("more that one " + "combiningAlgFactory with "
                            + "name \"" + elementName + "\"");
                combiningMap.put(elementName, parseCombiningAlgFactory(child));
            } else if (childName.equals("functionFactory")) {
                if (logger.isLoggable(Level.CONFIG))
                    logger.config("Loading FunctionFactory: " + elementName);
                if (functionMap.containsKey(elementName))
                    throw new ParsingException("more that one functionFactory" + " with name \""
                            + elementName + "\"");
                functionMap.put(elementName, parseFunctionFactory(child));
            }
        }

        // finally, extract the default elements
        defaultPDPConfig = (PDPConfig) (pdpConfigMap.get(defaultPDP));

        defaultAttributeFactory = (AttributeFactory) (attributeMap.get(defaultAF));
        if (defaultAttributeFactory == null) {
            try {
                defaultAttributeFactory = AttributeFactory.getInstance(defaultAF);
            } catch (Exception e) {
                throw new ParsingException("Unknown AttributeFactory", e);
            }
        }

        defaultCombiningFactory = (CombiningAlgFactory) (combiningMap.get(defaultCAF));
        if (defaultCombiningFactory == null) {
            try {
                defaultCombiningFactory = CombiningAlgFactory.getInstance(defaultCAF);
            } catch (Exception e) {
                throw new ParsingException("Unknown CombininAlgFactory", e);
            }
        }

        defaultFunctionFactoryProxy = (FunctionFactoryProxy) (functionMap.get(defaultFF));
        if (defaultFunctionFactoryProxy == null) {
            try {
                defaultFunctionFactoryProxy = FunctionFactory.getInstance(defaultFF);
            } catch (Exception e) {
                throw new ParsingException("Unknown FunctionFactory", e);
            }
        }
    }

    /**
     * Private helper that gets a default factory identifier, or fills in the default value if no
     * identifier is provided.
     */
    private String getDefaultFactory(NamedNodeMap attrs, String factoryName) {
        Node node = attrs.getNamedItem(factoryName);
        if (node != null)
            return node.getNodeValue();
        else
            return PolicyMetaData.XACML_1_0_IDENTIFIER;
    }

    /**
     * Private helper that parses the file and sets up the DOM tree.
     */
    private Node getRootNode(File configFile) throws ParsingException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();

        dbFactory.setIgnoringComments(true);
        dbFactory.setNamespaceAware(false);
        dbFactory.setValidating(false);

        DocumentBuilder db = null;
        try {
            db = dbFactory.newDocumentBuilder();
        } catch (ParserConfigurationException pce) {
            throw new ParsingException("couldn't get a document builder", pce);
        }

        Document doc = null;
        try {
            doc = db.parse(new FileInputStream(configFile));
        } catch (IOException ioe) {
            throw new ParsingException("failed to load the file ", ioe);
        } catch (SAXException saxe) {
            throw new ParsingException("error parsing the XML tree", saxe);
        } catch (IllegalArgumentException iae) {
            throw new ParsingException("no data to parse", iae);
        }

        Element root = doc.getDocumentElement();

        if (!root.getTagName().equals("config"))
            throw new ParsingException("unknown document type: " + root.getTagName());

        return root;
    }

    /**
     * Private helper that handles the pdp elements.
     */
    private PDPConfig parsePDPConfig(Node root) throws ParsingException {
        ArrayList<AttributeFinderModule> attrModules = new ArrayList<AttributeFinderModule>();
        HashSet<PolicyFinderModule> policyModules = new HashSet<PolicyFinderModule>();
        ArrayList<ResourceFinderModule> rsrcModules = new ArrayList<ResourceFinderModule>();

        // go through all elements of the pdp, loading the specified modules
        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            String name = child.getNodeName();

            if (name.equals("policyFinderModule")) {
                policyModules.add((PolicyFinderModule) loadClass("module", child));
            } else if (name.equals("attributeFinderModule")) {
                attrModules.add((AttributeFinderModule) loadClass("module", child));
            } else if (name.equals("resourceFinderModule")) {
                rsrcModules.add((ResourceFinderModule) loadClass("module", child));
            }
        }

        // after loading the modules, use the collections to setup a
        // PDPConfig based on this pdp element

        AttributeFinder attrFinder = new AttributeFinder();
        attrFinder.setModules(attrModules);

        PolicyFinder policyFinder = new PolicyFinder();
        policyFinder.setModules(policyModules);

        ResourceFinder rsrcFinder = new ResourceFinder();
        rsrcFinder.setModules(rsrcModules);

        return new PDPConfig(attrFinder, policyFinder, rsrcFinder);
    }

    /**
     * Private helper that handles the attributeFactory elements.
     */
    private AttributeFactory parseAttributeFactory(Node root) throws ParsingException {
        AttributeFactory factory = null;

        // check if we're starting with the standard factory setup
        if (useStandard(root, "useStandardDatatypes")) {
            logger.config("Starting with standard Datatypes");

            factory = StandardAttributeFactory.getNewFactory();
        } else {
            factory = new BaseAttributeFactory();
        }

        // now look for all datatypes specified for this factory, adding
        // them as we go
        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);

            if (child.getNodeName().equals("datatype")) {
                // a datatype is a class with an identifier
                String identifier = child.getAttributes().getNamedItem("identifier").getNodeValue();
                AttributeProxy proxy = (AttributeProxy) (loadClass("datatype", child));

                try {
                    factory.addDatatype(identifier, proxy);
                } catch (IllegalArgumentException iae) {
                    throw new ParsingException("duplicate datatype: " + identifier, iae);
                }
            }
        }

        return factory;
    }

    /**
     * Private helper that handles the combiningAlgFactory elements.
     */
    private CombiningAlgFactory parseCombiningAlgFactory(Node root) throws ParsingException {
        CombiningAlgFactory factory = null;

        // check if we're starting with the standard factory setup
        if (useStandard(root, "useStandardAlgorithms")) {
            logger.config("Starting with standard Combining Algorithms");

            factory = StandardCombiningAlgFactory.getNewFactory();
        } else {
            factory = new BaseCombiningAlgFactory();
        }

        // now look for all algorithms specified for this factory, adding
        // them as we go
        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);

            if (child.getNodeName().equals("algorithm")) {
                // an algorithm is a simple class element
                CombiningAlgorithm alg = (CombiningAlgorithm) (loadClass("algorithm", child));
                try {
                    factory.addAlgorithm(alg);
                } catch (IllegalArgumentException iae) {
                    throw new ParsingException("duplicate combining " + "algorithm: "
                            + alg.getIdentifier().toString(), iae);
                }
            }
        }

        return factory;
    }

    /**
     * Private helper that handles the functionFactory elements. This one is a little more complex
     * than the other two factory helper methods, since it consists of three factories (target,
     * condition, and general).
     */
    private FunctionFactoryProxy parseFunctionFactory(Node root) throws ParsingException {
        FunctionFactoryProxy proxy = null;
        FunctionFactory generalFactory = null;
        FunctionFactory conditionFactory = null;
        FunctionFactory targetFactory = null;

        // check if we're starting with the standard factory setup, and
        // make sure that the proxy is pre-configured
        if (useStandard(root, "useStandardFunctions")) {
            logger.config("Starting with standard Functions");

            proxy = StandardFunctionFactory.getNewFactoryProxy();

            targetFactory = proxy.getTargetFactory();
            conditionFactory = proxy.getConditionFactory();
            generalFactory = proxy.getGeneralFactory();
        } else {
            generalFactory = new BaseFunctionFactory();
            conditionFactory = new BaseFunctionFactory(generalFactory);
            targetFactory = new BaseFunctionFactory(conditionFactory);

            proxy = new BasicFunctionFactoryProxy(targetFactory, conditionFactory, generalFactory);
        }

        // go through and load the three sections, putting the loaded
        // functions into the appropriate factory
        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            String name = child.getNodeName();

            if (name.equals("target")) {
                logger.config("Loading [TARGET] functions");
                functionParserHelper(child, targetFactory);
            } else if (name.equals("condition")) {
                logger.config("Loading [CONDITION] functions");
                functionParserHelper(child, conditionFactory);
            } else if (name.equals("general")) {
                logger.config("Loading [GENERAL] functions");
                functionParserHelper(child, generalFactory);
            }
        }

        return proxy;
    }

    /**
     * Private helper used by the function factory code to load a specific target, condition, or
     * general section.
     */
    private void functionParserHelper(Node root, FunctionFactory factory) throws ParsingException {
        // go through all elements in the section
        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            String name = child.getNodeName();

            if (name.equals("function")) {
                // a function section is a simple class element
                Function function = (Function) (loadClass("function", child));
                try {
                    factory.addFunction(function);
                } catch (IllegalArgumentException iae) {
                    throw new ParsingException("duplicate function", iae);
                }
            } else if (name.equals("abstractFunction")) {
                // an abstract function is a class with an identifier
                URI identifier = null;
                try {
                    identifier = new URI(child.getAttributes().getNamedItem("identifier")
                            .getNodeValue());
                } catch (URISyntaxException urise) {
                    throw new ParsingException("invalid function identifier", urise);
                }

                FunctionProxy proxy = (FunctionProxy) (loadClass("abstract function", child));
                try {
                    factory.addAbstractFunction(proxy, identifier);
                } catch (IllegalArgumentException iae) {
                    throw new ParsingException("duplicate abstract function", iae);
                }
            } else if (name.equals("functionCluster")) {
                // a cluster is a class that will give us a collection of
                // functions that need to be added one by one into the factory
                FunctionCluster cluster = (FunctionCluster) (loadClass("function cluster", child));

                for (Function function : cluster.getSupportedFunctions()) {
                    try {
                        factory.addFunction(function);
                    } catch (IllegalArgumentException iae) {
                        throw new ParsingException("duplicate function", iae);
                    }
                }
            }
        }
    }

    /**
     * Private helper that is used by all the code to load an instance of the given class...this
     * assumes that the class is in the classpath, both for simplicity and for stronger security
     */
    private Object loadClass(String prefix, Node root) throws ParsingException {
        // get the name of the class
        String className = root.getAttributes().getNamedItem("class").getNodeValue();

        if (logger.isLoggable(Level.CONFIG))
            logger.config("Loading [ " + prefix + ": " + className + " ]");

        // load the given class using the local classloader
        Class<?> c = null;
        try {
            c = loader.loadClass(className);
        } catch (ClassNotFoundException cnfe) {
            throw new ParsingException("couldn't load class " + className, cnfe);
        }
        Object instance = null;

        // figure out if there are any parameters to the constructor
        if (!root.hasChildNodes()) {
            // we're using a null constructor, so this is easy
            try {
                instance = c.newInstance();
            } catch (InstantiationException ie) {
                throw new ParsingException("couldn't instantiate " + className
                        + " with empty constructor", ie);
            } catch (IllegalAccessException iae) {
                throw new ParsingException("couldn't get access to instance " + "of " + className,
                        iae);
            }
        } else {
            // parse the arguments to the constructor
            List<Object> args = null;
            try {
                args = getArgs(root);
            } catch (IllegalArgumentException iae) {
                throw new ParsingException("illegal class arguments", iae);
            }
            int argLength = args.size();

            // next we need to see if there's a constructor that matches the
            // arguments provided...this has to be done by hand since
            // Class.getConstructor(Class []) doesn't handle sub-classes and
            // generic types (for instance, a constructor taking List won't
            // match a parameter list containing ArrayList)

            // get the list of all available constructors
            Constructor<?>[] cons = c.getConstructors();
            Constructor<?> constructor = null;

            for (int i = 0; i < cons.length; i++) {
                // get the parameters for this constructor
                Class<?>[] params = cons[i].getParameterTypes();
                if (params.length == argLength) {

                    int j = 0;

                    // loop through the parameters and see if each one is
                    // assignable from the coresponding input argument
                    for (Object obj : args) {
                        if (!params[j].isAssignableFrom(obj.getClass()))
                            break;
                        j++;
                    }

                    // if we looked at all the parameters, then this
                    // constructor matches the input
                    if (j == argLength)
                        constructor = cons[i];
                }

                // if we've found a matching constructor then stop looping
                if (constructor != null)
                    break;
            }

            // make sure we found a matching constructor
            if (constructor == null)
                throw new ParsingException("couldn't find a matching " + "constructor");

            // finally, instantiate the class
            try {
                instance = constructor.newInstance(args.toArray());
            } catch (InstantiationException ie) {
                throw new ParsingException("couldn't instantiate " + className, ie);
            } catch (IllegalAccessException iae) {
                throw new ParsingException("couldn't get access to instance " + "of " + className,
                        iae);
            } catch (InvocationTargetException ite) {
                throw new ParsingException("couldn't create " + className, ite);
            }
        }

        return instance;
    }

    /**
     * Private helper that gets the constructor arguments for a given class. Right now this just
     * supports String and List, but it's trivial to add support for other types should that be
     * needed. Right now, it's not clear that there's any need for other types.
     */
    private List<Object> getArgs(Node root) {
        List<Object> args = new ArrayList<Object>();
        NodeList children = root.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            String name = child.getNodeName();

            if (child.getNodeType() == Node.ELEMENT_NODE) {
                if (name.equals("string")) {
                    args.add(child.getFirstChild().getNodeValue());
                } else if (name.equals("list")) {
                    args.add(getArgs(child));
                } else {
                    throw new IllegalArgumentException("unkown arg type: " + name);
                }
            }
        }

        return args;
    }

    /**
     * Private helper used by the three factory routines to see if the given factory should be based
     * on the standard setup
     */
    private boolean useStandard(Node node, String attributeName) {
        NamedNodeMap map = node.getAttributes();
        if (map == null)
            return true;

        Node attrNode = map.getNamedItem(attributeName);
        if (attrNode == null)
            return true;

        return attrNode.getNodeValue().equals("true");
    }

    /**
     * Returns the default PDP configuration. If no default was specified then this throws an
     * exception.
     * 
     * @return the default PDP configuration
     * 
     * @throws UnknownIdentifierException
     *             if there is no default config
     */
    public PDPConfig getDefaultPDPConfig() throws UnknownIdentifierException {
        if (defaultPDPConfig == null)
            throw new UnknownIdentifierException("no default available");

        return defaultPDPConfig;
    }

    /**
     * Returns the PDP configuration with the given name. If no such configuration exists then an
     * exception is thrown.
     * 
     * @return the matching PDP configuation
     * 
     * @throws UnknownIdentifierException
     *             if the name is unknown
     */
    public PDPConfig getPDPConfig(String name) throws UnknownIdentifierException {
        Object object = pdpConfigMap.get(name);

        if (object == null)
            throw new UnknownIdentifierException("unknown pdp: " + name);

        return (PDPConfig) object;
    }

    /**
     * Returns a set of identifiers representing each PDP configuration available.
     * 
     * @return a <code>Set</code> of <code>String</code>s
     */
    public Set<String> getSupportedPDPConfigurations() {
        return Collections.unmodifiableSet(pdpConfigMap.keySet());
    }

    /**
     * Returns the default attribute factory.
     * 
     * @return the default attribute factory
     */
    public AttributeFactory getDefaultAttributeFactory() {
        return defaultAttributeFactory;
    }

    /**
     * Returns the attribute factory with the given name. If no such factory exists then an
     * exception is thrown.
     * 
     * @return the matching attribute factory
     * 
     * @throws UnknownIdentifierException
     *             if the name is unknown
     */
    public AttributeFactory getAttributeFactory(String name) throws UnknownIdentifierException {
        Object object = attributeMap.get(name);

        if (object == null)
            throw new UnknownIdentifierException("unknown factory: " + name);

        return (AttributeFactory) object;
    }

    /**
     * Returns a set of identifiers representing each attribute factory available.
     * 
     * @return a <code>Set</code> of <code>String</code>s
     */
    public Set<String> getSupportedAttributeFactories() {
        return Collections.unmodifiableSet(attributeMap.keySet());
    }

    /**
     * Registers all the supported factories with the given identifiers. If a given identifier is
     * already in use, then that factory is not registered. This method is provided only as a
     * convenience, and any registration that may involve identifier clashes should be done by
     * registering each factory individually.
     */
    public void registerAttributeFactories() {
        for (String id : attributeMap.keySet()) {
            AttributeFactory af = attributeMap.get(id);

            try {
                AttributeFactory.registerFactory(id, new AFProxy(af));
            } catch (IllegalArgumentException iae) {
                logger.log(Level.WARNING, "Couldn't register AttributeFactory:" + id
                        + " (already in use)", iae);
            }
        }
    }

    /**
     * Returns the default combiningAlg factory.
     * 
     * @return the default combiningAlg factory
     */
    public CombiningAlgFactory getDefaultCombiningAlgFactory() {
        return defaultCombiningFactory;
    }

    /**
     * Returns the combiningAlg factory with the given name. If no such factory exists then an
     * exception is thrown.
     * 
     * @return the matching combiningAlg factory
     * 
     * @throws UnknownIdentifierException
     *             if the name is unknown
     */
    public CombiningAlgFactory getCombiningAlgFactory(String name)
            throws UnknownIdentifierException {
        Object object = combiningMap.get(name);

        if (object == null)
            throw new UnknownIdentifierException("unknown factory: " + name);

        return (CombiningAlgFactory) object;
    }

    /**
     * Returns a set of identifiers representing each combiningAlg factory available.
     * 
     * @return a <code>Set</code> of <code>String</code>s
     */
    public Set<String> getSupportedCombiningAlgFactories() {
        return Collections.unmodifiableSet(combiningMap.keySet());
    }

    /**
     * Registers all the supported factories with the given identifiers. If a given identifier is
     * already in use, then that factory is not registered. This method is provided only as a
     * convenience, and any registration that may involve identifier clashes should be done by
     * registering each factory individually.
     */
    public void registerCombiningAlgFactories() {

        for (String id : combiningMap.keySet()) {
            CombiningAlgFactory cf = (CombiningAlgFactory) (combiningMap.get(id));

            try {
                CombiningAlgFactory.registerFactory(id, new CAFProxy(cf));
            } catch (IllegalArgumentException iae) {
                logger.log(Level.WARNING, "Couldn't register " + "CombiningAlgFactory: " + id
                        + " (already in use)", iae);
            }
        }
    }

    /**
     * Returns the default function factory proxy.
     * 
     * @return the default function factory proxy
     */
    public FunctionFactoryProxy getDefaultFunctionFactoryProxy() {
        return defaultFunctionFactoryProxy;
    }

    /**
     * Returns the function factory proxy with the given name. If no such proxy exists then an
     * exception is thrown.
     * 
     * @return the matching function factory proxy
     * 
     * @throws UnknownIdentifierException
     *             if the name is unknown
     */
    public FunctionFactoryProxy getFunctionFactoryProxy(String name)
            throws UnknownIdentifierException {
        Object object = functionMap.get(name);

        if (object == null)
            throw new UnknownIdentifierException("unknown factory: " + name);

        return (FunctionFactoryProxy) object;
    }

    /**
     * Returns a set of identifiers representing each function factory proxy available.
     * 
     * @return a <code>Set</code> of <code>String</code>s
     */
    public Set<String> getSupportedFunctionFactories() {
        return Collections.unmodifiableSet(functionMap.keySet());
    }

    /**
     * Registers all the supported factories with the given identifiers. If a given identifier is
     * already in use, then that factory is not registered. This method is provided only as a
     * convenience, and any registration that may involve identifier clashes should be done by
     * registering each factory individually.
     */
    public void registerFunctionFactories() {

        for (String id : functionMap.keySet()) {
            FunctionFactoryProxy ffp = (FunctionFactoryProxy) (functionMap.get(id));

            try {
                FunctionFactory.registerFactory(id, ffp);
            } catch (IllegalArgumentException iae) {
                logger.log(Level.WARNING, "Couldn't register FunctionFactory: " + id
                        + " (already in use)", iae);
            }
        }
    }

    /**
     * Uses the default configuration to re-set the default factories used by the system (attribute,
     * combining algorithm, and function). If a default is not provided for a given factory, then
     * that factory will not be set as the system's default.
     */
    public void useDefaultFactories() {
        logger.fine("Switching to default factories from configuration");

        // set the default attribute factory, if it exists here
        if (defaultAttributeFactory != null) {
            AttributeFactory.setDefaultFactory(new AFProxy(defaultAttributeFactory));
        }

        // set the default combining algorithm factory, if it exists here
        if (defaultCombiningFactory != null) {
            CombiningAlgFactory.setDefaultFactory(new CAFProxy(defaultCombiningFactory));
        }

        // set the default function factories, if they exists here
        if (defaultFunctionFactoryProxy != null)
            FunctionFactory.setDefaultFactory(defaultFunctionFactoryProxy);
    }

    /**
     *
     */
    class AFProxy implements AttributeFactoryProxy {
        private AttributeFactory factory;

        public AFProxy(AttributeFactory factory) {
            this.factory = factory;
        }

        public AttributeFactory getFactory() {
            return factory;
        }
    }

    /**
     *
     */
    class CAFProxy implements CombiningAlgFactoryProxy {
        private CombiningAlgFactory factory;

        public CAFProxy(CombiningAlgFactory factory) {
            this.factory = factory;
        }

        public CombiningAlgFactory getFactory() {
            return factory;
        }
    }

}

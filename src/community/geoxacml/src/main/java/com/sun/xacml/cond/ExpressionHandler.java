/*
 * ExpressionHandler.java
 *
 * Created by: seth proctor (stp)
 * Created on: Wed Dec 29, 2004	 8:24:30 PM
 * Desc: 
 *
 */

package com.sun.xacml.cond;

import org.w3c.dom.Node;

import com.sun.xacml.ParsingException;
import com.sun.xacml.PolicyMetaData;
import com.sun.xacml.UnknownIdentifierException;
import com.sun.xacml.attr.AttributeDesignator;
import com.sun.xacml.attr.AttributeFactory;
import com.sun.xacml.attr.AttributeSelector;

/**
 * This is a package-private utility class that handles parsing all the possible expression types.
 * It was added becuase in 2.0 multiple classes needed this. Note that this could also be added to
 * Expression and that interface could be made an abstract class, but that would require substantial
 * change.
 * 
 * @since 2.0
 * @author Seth Proctor
 */
class ExpressionHandler {

    /**
     * Parses an expression, recursively handling any sub-elements. This is provided as a utility
     * class, but in practice is used only by <code>Apply</code>, <code>Condition</code>, and
     * <code>VariableDefinition</code>.
     * 
     * @param root
     *            the DOM root of an ExpressionType XML type
     * @param metaData
     *            the meta-data associated with the containing policy
     * @param manager
     *            <code>VariableManager</code> used to connect references and definitions while
     *            parsing
     * 
     * @return an <code>Expression</code> or null if the root node cannot be parsed as a valid
     *         Expression
     */
    public static Expression parseExpression(Node root, PolicyMetaData metaData,
            VariableManager manager) throws ParsingException {
        String name = root.getNodeName();

        if (name.equals("Apply")) {
            return Apply.getInstance(root, metaData, manager);
        } else if (name.equals("AttributeValue")) {
            try {
                return AttributeFactory.getInstance().createValue(root);
            } catch (UnknownIdentifierException uie) {
                throw new ParsingException("Unknown DataType", uie);
            }
        } else if (name.equals("SubjectAttributeDesignator")) {
            return AttributeDesignator.getInstance(root, AttributeDesignator.SUBJECT_TARGET,
                    metaData);
        } else if (name.equals("ResourceAttributeDesignator")) {
            return AttributeDesignator.getInstance(root, AttributeDesignator.RESOURCE_TARGET,
                    metaData);
        } else if (name.equals("ActionAttributeDesignator")) {
            return AttributeDesignator.getInstance(root, AttributeDesignator.ACTION_TARGET,
                    metaData);
        } else if (name.equals("EnvironmentAttributeDesignator")) {
            return AttributeDesignator.getInstance(root, AttributeDesignator.ENVIRONMENT_TARGET,
                    metaData);
        } else if (name.equals("AttributeSelector")) {
            return AttributeSelector.getInstance(root, metaData);
        } else if (name.equals("Function")) {
            return getFunction(root, metaData, FunctionFactory.getGeneralInstance());
        } else if (name.equals("VariableReference")) {
            return VariableReference.getInstance(root, metaData, manager);
        }

        // return null if it was none of these
        return null;
    }

    /**
     * Helper method that tries to get a function instance
     */
    public static Function getFunction(Node root, PolicyMetaData metaData, FunctionFactory factory)
            throws ParsingException {
        Node functionNode = root.getAttributes().getNamedItem("FunctionId");
        String functionName = functionNode.getNodeValue();

        try {
            // try to get an instance of the given function
            return factory.createFunction(functionName);
        } catch (UnknownIdentifierException uie) {
            throw new ParsingException("Unknown FunctionId", uie);
        } catch (FunctionTypeException fte) {
            // try creating as an abstract function
            try {
                FunctionFactory ff = FunctionFactory.getGeneralInstance();
                return ff.createAbstractFunction(functionName, root, metaData.getXPathIdentifier());
            } catch (Exception e) {
                // any exception at this point is a failure
                throw new ParsingException("failed to create abstract function" + " "
                        + functionName, e);
            }
        }
    }

}

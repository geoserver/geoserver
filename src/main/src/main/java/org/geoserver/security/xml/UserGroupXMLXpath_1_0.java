/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.xml;

import static org.geoserver.security.xml.XMLConstants.*;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

/**
 * This class provides precompiled XPath Expressions
 *
 * @author christian
 */
public class UserGroupXMLXpath_1_0 extends UserGroupXMLXpath {

    /** Singleton, the implementation is stateless */
    public static final UserGroupXMLXpath_1_0 Singleton = new UserGroupXMLXpath_1_0();

    /** XML name space context for user/group store */
    protected XPathExpression userListExpression;

    protected XPathExpression userEnabledExpression;
    protected XPathExpression userNameExpression;
    protected XPathExpression userPasswordExpression;
    protected XPathExpression userPropertiesExpression;
    protected XPathExpression propertyNameExpression;
    protected XPathExpression propertyValueExpression;
    protected XPathExpression groupListExpression;
    protected XPathExpression groupNameExpression;
    protected XPathExpression groupEnabledExpression;
    protected XPathExpression groupMemberListExpression;
    protected XPathExpression groupMemberNameExpression;

    /** Constructor is protected, use the static Singleton instance */
    protected UserGroupXMLXpath_1_0() {

        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        xpath.setNamespaceContext(this.urContext);

        // compile(xpath,"/"+E_USERREGISTRY+"["+A_VERSION_UR + "]");

        userListExpression =
                compile(
                        xpath,
                        "/"
                                + NSP_UR
                                + ":"
                                + E_USERREGISTRY_UR
                                + "/"
                                + NSP_UR
                                + ":"
                                + E_USERS_UR
                                + "/"
                                + NSP_UR
                                + ":"
                                + E_USER_UR);
        userEnabledExpression = compileRelativeAttribute(xpath, A_USER_ENABLED_UR, NSP_UR);
        userNameExpression = compileRelativeAttribute(xpath, A_USER_NAME_UR, NSP_UR);
        userPasswordExpression = compileRelativeAttribute(xpath, A_USER_PASSWORD_UR, NSP_UR);

        userPropertiesExpression = compile(xpath, NSP_UR + ":" + E_PROPERTY_UR);
        propertyNameExpression = compileRelativeAttribute(xpath, A_PROPERTY_NAME_UR, NSP_UR);
        propertyValueExpression = compile(xpath, "text()");

        groupListExpression =
                compile(
                        xpath,
                        "/"
                                + NSP_UR
                                + ":"
                                + E_USERREGISTRY_UR
                                + "/"
                                + NSP_UR
                                + ":"
                                + E_GROUPS_UR
                                + "/"
                                + NSP_UR
                                + ":"
                                + E_GROUP_UR);

        groupNameExpression = compileRelativeAttribute(xpath, A_GROUP_NAME_UR, NSP_UR);
        groupEnabledExpression = compileRelativeAttribute(xpath, A_GROUP_ENABLED_UR, NSP_UR);

        groupMemberListExpression = compile(xpath, NSP_UR + ":" + E_MEMBER_UR);
        groupMemberNameExpression = compileRelativeAttribute(xpath, A_MEMBER_NAME_UR, NSP_UR);
    }

    @Override
    public XPathExpression getUserListExpression() {
        return userListExpression;
    }

    @Override
    public XPathExpression getUserEnabledExpression() {
        return userEnabledExpression;
    }

    @Override
    public XPathExpression getUserNameExpression() {
        return userNameExpression;
    }

    @Override
    public XPathExpression getUserPasswordExpression() {
        return userPasswordExpression;
    }

    @Override
    public XPathExpression getUserPropertiesExpression() {
        return userPropertiesExpression;
    }

    @Override
    public XPathExpression getPropertyNameExpression() {
        return propertyNameExpression;
    }

    @Override
    public XPathExpression getPropertyValueExpression() {
        return propertyValueExpression;
    }

    @Override
    public XPathExpression getGroupListExpression() {
        return groupListExpression;
    }

    @Override
    public XPathExpression getGroupNameExpression() {
        return groupNameExpression;
    }

    @Override
    public XPathExpression getGroupEnabledExpression() {
        return groupEnabledExpression;
    }

    @Override
    public XPathExpression getGroupMemberListExpression() {
        return groupMemberListExpression;
    }

    @Override
    public XPathExpression getGroupMemberNameExpression() {
        return groupMemberNameExpression;
    }
}

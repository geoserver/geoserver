/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.config;

import java.beans.PropertyDescriptor;

import junit.framework.TestCase;

import org.geotools.validation.attributes.GazetteerNameValidation;
import org.vfny.geoserver.config.validation.ArgumentConfig;
import org.vfny.geoserver.config.validation.PlugInConfig;
import org.vfny.geoserver.config.validation.TestConfig;

/**
 * ValidationTest purpose.
 * <p>
 * Description of ValidationTest ...
 * </p>
 * 
 * @author dzwiers, Refractions Research, Inc.
 * @author $Author: jive $ (last modification)
 * @version $Id$
 */
public class ValidationTest extends TestCase {

//	public static void main(String[] args) {
	public void test() {
		TestConfig testConfig = new TestConfig();
		PlugInConfig pluginConfig = new PlugInConfig();
		// the plugin to test the bean info for.
		//pluginConfig.setClassName(PolygonBoundaryCoveredByPolygonValidation.class.getName());
		pluginConfig.setClassName(GazetteerNameValidation.class.getName());
		testConfig.setPlugIn(pluginConfig);

//		System.out.println(testConfig.toString());
//		System.out.println("--------------------------------------");
//		for (int i = 0; i < testConfig.getPropertyDescriptors().length; i++) {
//			
//			System.out.println(testConfig.getPropertyDescriptors()[i].getClass().getName());
//			System.out.println(testConfig.getPropertyDescriptors()[i].getDisplayName());
//			System.out.println(testConfig.getPropertyDescriptors()[i].getShortDescription());
//			
//			System.out.println(testConfig.getPropertyDescriptors()[i].attributeNames());
//			System.out.println("--------------------------------------");
//		}
//
//		System.out.println("--------------------------------------");
//		System.out.println("--------------------------------------");
//		System.out.println("--------------------------------------");
		
		
		testConfig = new TestConfig();
		pluginConfig = new PlugInConfig();
		// the plugin to test the bean info for.
		//pluginConfig.setClassName(PolygonBoundaryCoveredByPolygonValidation.class.getName());
		pluginConfig.setClassName(GazetteerNameValidation.class.getName());
		testConfig.setPlugIn(pluginConfig);

		PropertyDescriptor [] pd = pluginConfig.getPropertyDescriptors();
		
//		System.out.println(pluginConfig.toString());
//		System.out.println("--------------------------------------");
//		for (int i = 0; i < pluginConfig.getPropertyDescriptors().length; i++) {
//			
//			System.out.println(pd[i].getClass().getName());
//			System.out.println(ArgumentConfig.getDisplayName(pd[i]));
//			System.out.println(ArgumentConfig.getDescription(pd[i]));
//			
//			System.out.println("--------------------------------------");
//		}
		


		/*System.out.println(pluginConfig.toString());
		System.out.println("--------------------------------------");
		for (int i = 0; i < pluginConfig.getPropertyDescriptors().length; i++) {
			
			System.out.println(pluginConfig.getPropertyDescriptors()[i].getDisplayName());
			System.out.println(pluginConfig.getPropertyDescriptors()[i].getShortDescription());
			
			System.out.println(pluginConfig.getPropertyDescriptors()[i].attributeNames());
			System.out.println("--------------------------------------");
		}*/
	}
}

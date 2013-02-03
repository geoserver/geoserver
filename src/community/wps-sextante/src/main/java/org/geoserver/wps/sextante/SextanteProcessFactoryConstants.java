/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.sextante;

/**
 * Constants used to identify additional information about
 * SEXTANTE parameters
 * @author volaya
 *
 */
public interface SextanteProcessFactoryConstants {


	/**
	 * constants for data objects
	 */
	public static final String PARAMETER_MANDATORY = "PARAMETER_MANDATORY";

	/**
	 * constants for vector layers
	 */
	public static final String SHAPE_TYPE = "SHAPE_TYPE";

	/**
	 * Constants for string parameter
	 */
	public static final String DEFAULT_STRING_VALUE = "DEFAULT_STRING_VALUE";

	/**
	 * constant for numerical parameters
	 */
	public static final String DEFAULT_NUMERICAL_VALUE = "DEFAULT_NUMERICAL_VALUE";
	public static final String MAX_NUMERICAL_VALUE = "MAX_NUMERICAL_VALUE";
	public static final String MIN_NUMERICAL_VALUE = "MIN_NUMERICAL_VALUE";
	public static final String NUMERICAL_VALUE_TYPE = "NUMERICAL_VALUE_TYPE";

	/**
	 * constants for boolean parameters
	 */
	public static final String DEFAULT_BOOLEAN_VALUE = "DEFAULT_NUMERICAL_VALUE";

	public static final String MULTIPLE_INPUT_TYPE = "MULTIPLE_INPUT_TYPE";

	public static final String FIXED_TABLE_NUM_COLS = "FIXED_TABLE_NUM_COLS";
	public static final String FIXED_TABLE_NUM_ROWS = "FIXED_TABLE_NUM_ROWS";
	public static final String FIXED_TABLE_FIXED_NUM_ROWS = "FIXED_TABLE_FIXED_NUM_ROWS";

	public static final String PARENT_PARAMETER_NAME = "PARENT_PARAMETER_NAME";


}

/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.dimension;

import java.util.Date;
import java.util.logging.Logger;

import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionDefaultValueSetting;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.platform.ServiceException;
import org.geotools.feature.type.DateUtil;

public class DimensionDefaultValueStrategyFactoryImpl implements
		DimensionDefaultValueStrategyFactory {
	static final Logger LOGGER = null;

	// Initialized in the applicationContext:
	private DimensionDefaultValueStrategy featureTimeMinimumStrategy;
	private DimensionDefaultValueStrategy featureTimeMaximumStrategy;
	private DimensionDefaultValueStrategy coverageTimeMinimumStrategy;
	private DimensionDefaultValueStrategy coverageTimeMaximumStrategy;

	private DimensionDefaultValueStrategy featureElevationMinimumStrategy;
	private DimensionDefaultValueStrategy featureElevationMaximumStrategy;
	private DimensionDefaultValueStrategy coverageElevationMinimumStrategy;
	private DimensionDefaultValueStrategy coverageElevationMaximumStrategy;

	private DimensionDefaultValueStrategy featureCustomDimensionMinimumStrategy;
	private DimensionDefaultValueStrategy featureCustomDimensionMaximumStrategy;
	private DimensionDefaultValueStrategy coverageCustomDimensionMinimumStrategy;
	private DimensionDefaultValueStrategy coverageCustomDimensionMaximumStrategy;

	/**
	 * Return the default value strategy for the given dimension. If the default
	 * value strategy is not set for this dimension, returns the default
	 * strategy for this resource type.
	 */
	@Override
	public DimensionDefaultValueStrategy getStrategy(ResourceInfo resource,
			String dimensionName, DimensionInfo dimensionInfo) {
		DimensionDefaultValueStrategy retval = getStrategyFromSetting(resource,
				dimensionName, dimensionInfo);
		if (retval != null) {
			return retval;
		}
		System.out.println("No setting for dim "+dimensionName);
		// Else just select the default strategy based on the dimension name
		if (dimensionName.equals(ResourceInfo.TIME)) {
			if (resource instanceof FeatureTypeInfo) {
				retval = new DefaultFeatureNearestValueSelectionStrategy(
						new Date(), "current");
			} else if (resource instanceof CoverageInfo) {
				retval = new DefaultCoverageNearestValueSelectionStrategy(
						new Date(), "current");
			}
		} else if (dimensionName.equals(ResourceInfo.ELEVATION)) {
			if (resource instanceof FeatureTypeInfo) {
				retval = featureElevationMinimumStrategy;
			} else if (resource instanceof CoverageInfo) {
				retval = coverageElevationMinimumStrategy;
			}
		} else if (dimensionName
				.startsWith(ResourceInfo.CUSTOM_DIMENSION_PREFIX)) {
			if (resource instanceof FeatureTypeInfo) {
				retval = featureCustomDimensionMinimumStrategy;
			} else if (resource instanceof CoverageInfo) {
				retval = coverageCustomDimensionMinimumStrategy;
			}
		}
		return retval;
	}

	/**
	 * @return the featureTimeMinimumStrategy
	 */
	public DimensionDefaultValueStrategy getFeatureTimeMinimumStrategy() {
		return featureTimeMinimumStrategy;
	}

	/**
	 * @param featureTimeMinimumStrategy
	 *            the featureTimeMinimumStrategy to set
	 */
	public void setFeatureTimeMinimumStrategy(
			DimensionDefaultValueStrategy featureTimeMinimumStrategy) {
		this.featureTimeMinimumStrategy = featureTimeMinimumStrategy;
	}

	/**
	 * @return the featureTimeMaximumStrategy
	 */
	public DimensionDefaultValueStrategy getFeatureTimeMaximumStrategy() {
		return featureTimeMaximumStrategy;
	}

	/**
	 * @param featureTimeMaximumStrategy
	 *            the featureTimeMaximumStrategy to set
	 */
	public void setFeatureTimeMaximumStrategy(
			DimensionDefaultValueStrategy featureTimeMaximumStrategy) {
		this.featureTimeMaximumStrategy = featureTimeMaximumStrategy;
	}

	/**
	 * @return the coverageTimeMinimumStrategy
	 */
	public DimensionDefaultValueStrategy getCoverageTimeMinimumStrategy() {
		return coverageTimeMinimumStrategy;
	}

	/**
	 * @param coverageTimeMinimumStrategy
	 *            the coverageTimeMinimumStrategy to set
	 */
	public void setCoverageTimeMinimumStrategy(
			DimensionDefaultValueStrategy coverageTimeMinimumStrategy) {
		this.coverageTimeMinimumStrategy = coverageTimeMinimumStrategy;
	}

	/**
	 * @return the coverageTimeMaximumStrategy
	 */
	public DimensionDefaultValueStrategy getCoverageTimeMaximumStrategy() {
		return coverageTimeMaximumStrategy;
	}

	/**
	 * @param coverageTimeMaximumStrategy
	 *            the coverageTimeMaximumStrategy to set
	 */
	public void setCoverageTimeMaximumStrategy(
			DimensionDefaultValueStrategy coverageTimeMaximumStrategy) {
		this.coverageTimeMaximumStrategy = coverageTimeMaximumStrategy;
	}

	/**
	 * @return the featureElevationMiminumStrategy
	 */
	public DimensionDefaultValueStrategy getFeatureElevationMinimumStrategy() {
		return featureElevationMinimumStrategy;
	}

	/**
	 * @param featureElevationMiminumStrategy
	 *            the featureElevationMiminumStrategy to set
	 */
	public void setFeatureElevationMinimumStrategy(
			DimensionDefaultValueStrategy featureElevationMinimumStrategy) {
		this.featureElevationMinimumStrategy = featureElevationMinimumStrategy;
	}

	/**
	 * @return the featureElevationMaximumStrategy
	 */
	public DimensionDefaultValueStrategy getFeatureElevationMaximumStrategy() {
		return featureElevationMaximumStrategy;
	}

	/**
	 * @param featureElevationMaxinumStrategy
	 *            the featureElevationMaxinumStrategy to set
	 */
	public void setFeatureElevationMaximumStrategy(
			DimensionDefaultValueStrategy featureElevationMaximumStrategy) {
		this.featureElevationMaximumStrategy = featureElevationMaximumStrategy;
	}

	/**
	 * @return the coverageElevationMinimumStrategy
	 */
	public DimensionDefaultValueStrategy getCoverageElevationMinimumStrategy() {
		return coverageElevationMinimumStrategy;
	}

	/**
	 * @param coverageElevationMinimumStrategy
	 *            the coverageElevationMinimumStrategy to set
	 */
	public void setCoverageElevationMinimumStrategy(
			DimensionDefaultValueStrategy coverageElevationMinimumStrategy) {
		this.coverageElevationMinimumStrategy = coverageElevationMinimumStrategy;
	}

	/**
	 * @return the coverageElevationMaximumStrategy
	 */
	public DimensionDefaultValueStrategy getCoverageElevationMaximumStrategy() {
		return coverageElevationMaximumStrategy;
	}

	/**
	 * @param coverageElevationMaximumStrategy
	 *            the coverageElevationMaximumStrategy to set
	 */
	public void setCoverageElevationMaximumStrategy(
			DimensionDefaultValueStrategy coverageElevationMaximumStrategy) {
		this.coverageElevationMaximumStrategy = coverageElevationMaximumStrategy;
	}

	/**
	 * @return the featureCustomDimensionMinimumStrategy
	 */
	public DimensionDefaultValueStrategy getFeatureCustomDimensionMinimumStrategy() {
		return featureCustomDimensionMinimumStrategy;
	}

	/**
	 * @param featureCustomDimensionMinimumStrategy
	 *            the featureCustomDimensionMinimumStrategy to set
	 */
	public void setFeatureCustomDimensionMinimumStrategy(
			DimensionDefaultValueStrategy featureCustomDimensionMinimumStrategy) {
		this.featureCustomDimensionMinimumStrategy = featureCustomDimensionMinimumStrategy;
	}

	/**
	 * @return the featureCustomDimensionMaximumStrategy
	 */
	public DimensionDefaultValueStrategy getFeatureCustomDimensionMaximumStrategy() {
		return featureCustomDimensionMaximumStrategy;
	}

	/**
	 * @param featureCustomDimensionMaximumStrategy
	 *            the featureCustomDimensionMaximumStrategy to set
	 */
	public void setFeatureCustomDimensionMaximumStrategy(
			DimensionDefaultValueStrategy featureCustomDimensionMaximumStrategy) {
		this.featureCustomDimensionMaximumStrategy = featureCustomDimensionMaximumStrategy;
	}

	/**
	 * @return the coverageCustomDimensionMinimumStrategy
	 */
	public DimensionDefaultValueStrategy getCoverageCustomDimensionMinimumStrategy() {
		return coverageCustomDimensionMinimumStrategy;
	}

	/**
	 * @param coverageCustomDimensionMinimumStrategy
	 *            the coverageCustomDimensionMinimumStrategy to set
	 */
	public void setCoverageCustomDimensionMinimumStrategy(
			DimensionDefaultValueStrategy coverageCustomDimensionMinimumStrategy) {
		this.coverageCustomDimensionMinimumStrategy = coverageCustomDimensionMinimumStrategy;
	}

	/**
	 * @return the coverageCustomDimensionMaximumStrategy
	 */
	public DimensionDefaultValueStrategy getCoverageCustomDimensionMaximumStrategy() {
		return coverageCustomDimensionMaximumStrategy;
	}

	/**
	 * @param coverageCustomDimensionMaximumStrategy
	 *            the coverageCustomDimensionMaximumStrategy to set
	 */
	public void setCoverageCustomDimensionMaximumStrategy(
			DimensionDefaultValueStrategy coverageCustomDimensionMaximumStrategy) {
		this.coverageCustomDimensionMaximumStrategy = coverageCustomDimensionMaximumStrategy;
	}

	private DimensionDefaultValueStrategy getStrategyFromSetting(
			ResourceInfo resource, String dimensionName,
			DimensionInfo dimensionInfo) {
		DimensionDefaultValueStrategy retval = null;
		DimensionDefaultValueSetting setting = dimensionInfo.getDefaultValue();
		if (setting != null) {
			if (dimensionName.equals(ResourceInfo.TIME)) {
				retval = getDefaultTimeStrategy(resource, setting);
			} else if (dimensionName.equals(ResourceInfo.ELEVATION)) {
				retval = getDefaultElevationStrategy(resource, setting);
			} else if (dimensionName
					.startsWith(ResourceInfo.CUSTOM_DIMENSION_PREFIX)) {
				retval = getDefaultCustomDimensionStrategy(resource, setting);
			}
		}
		return retval;
	}

	private DimensionDefaultValueStrategy getDefaultTimeStrategy(
			ResourceInfo resource, DimensionDefaultValueSetting setting) {
		DimensionDefaultValueStrategy retval = null;
		String referenceValue = null;
		switch (setting.getStrategyType()) {
		case NEAREST: {
			Date refDate;
			String capabilitiesValue = null;
			referenceValue = setting.getReferenceValue();
			if (referenceValue != null) {
				if (referenceValue.equalsIgnoreCase("current")) {
					refDate = new Date();
					capabilitiesValue = "current";
				} else {
					try {
						refDate = new Date(
								DateUtil.parseDateTime(referenceValue));
					} catch (IllegalArgumentException e) {
						throw new ServiceException(
								"Unable to parse time dimension default value reference '"
										+ referenceValue
										+ "' as date, an ISO 8601 datetime format is expected",
								e);
					}
				}
				if (resource instanceof FeatureTypeInfo) {
					retval = new DefaultFeatureNearestValueSelectionStrategy(
							refDate, capabilitiesValue);
				} else if (resource instanceof CoverageInfo) {
					retval = new DefaultCoverageNearestValueSelectionStrategy(
							refDate, capabilitiesValue);
				}
			} else {
				throw new ServiceException(
						"No reference value given for time dimension default value 'nearest' strategy");
			}
			break;
		}
		case MINIMUM: {
			if (resource instanceof FeatureTypeInfo) {
				retval = featureTimeMinimumStrategy;
			} else if (resource instanceof CoverageInfo) {
				retval = coverageTimeMinimumStrategy;
			}
			break;
		}
		case MAXIMUM: {
			if (resource instanceof FeatureTypeInfo) {
				retval = featureTimeMaximumStrategy;
			} else if (resource instanceof CoverageInfo) {
				retval = coverageTimeMaximumStrategy;
			}
			break;
		}
		case FIXED: {
			Date refDate;
			referenceValue = setting.getReferenceValue();
			if (referenceValue != null) {
				try {
					refDate = new Date(DateUtil.parseDateTime(referenceValue));
				} catch (IllegalArgumentException e) {
					throw new ServiceException(
							"Unable to parse time dimension default value reference '"
									+ referenceValue
									+ "' as date, an ISO 8601 datetime format is expected",
							e);
				}
				retval = new FixedValueStrategy(refDate);
			} else {
				throw new ServiceException(
						"No reference value given for time dimension default value 'fixed' strategy");
			}
			break;
		}
		}
		return retval;
	}

	private DimensionDefaultValueStrategy getDefaultElevationStrategy(
			ResourceInfo resource, DimensionDefaultValueSetting setting) {
		DimensionDefaultValueStrategy retval = null;
		String referenceValue = null;
		switch (setting.getStrategyType()) {
		case NEAREST: {
			Number refNumber;
			referenceValue = setting.getReferenceValue();
			if (referenceValue != null) {
				try {
					refNumber = Long.parseLong(referenceValue);
				} catch (NumberFormatException fne) {
					try {
						refNumber = Double.parseDouble(referenceValue);
					} catch (NumberFormatException e) {
						throw new ServiceException(
								"Unable to parse elevation dimension default value reference '"
										+ referenceValue
										+ "' as long or double", e);
					}
				}
				if (resource instanceof FeatureTypeInfo) {
					retval = new DefaultFeatureNearestValueSelectionStrategy(
							refNumber);
				} else if (resource instanceof CoverageInfo) {
					retval = new DefaultCoverageNearestValueSelectionStrategy(
							refNumber);
				}
			} else {
				throw new ServiceException(
						"No reference value given for elevation dimension default value 'nearest' strategy");
			}
			break;
		}
		case MINIMUM: {
			if (resource instanceof FeatureTypeInfo) {
				retval = featureElevationMinimumStrategy;
			} else if (resource instanceof CoverageInfo) {
				retval = coverageElevationMinimumStrategy;
			}
			break;
		}
		case MAXIMUM: {
			if (resource instanceof FeatureTypeInfo) {
				retval = featureElevationMaximumStrategy;
			} else if (resource instanceof CoverageInfo) {
				retval = coverageElevationMaximumStrategy;
			}
			break;
		}
		case FIXED: {
			Number refNumber;
			referenceValue = setting.getReferenceValue();
			if (referenceValue != null) {
				try {
					refNumber = Long.parseLong(referenceValue);
				} catch (NumberFormatException fne) {
					try {
						refNumber = Double.parseDouble(referenceValue);
					} catch (NumberFormatException e) {
						throw new ServiceException(
								"Unable to parse elevation dimension default value reference '"
										+ referenceValue
										+ "' as long or double", e);
					}
				}
			} else {
				throw new ServiceException(
						"No reference value given for elevation dimension default value 'fixed' strategy");
			}
			retval = new FixedValueStrategy(refNumber);
			break;
		}
		}
		return retval;
	}

	private DimensionDefaultValueStrategy getDefaultCustomDimensionStrategy(
			ResourceInfo resource, DimensionDefaultValueSetting setting) {
		DimensionDefaultValueStrategy retval = null;
		String referenceValue = null;
		switch (setting.getStrategyType()) {
		case NEAREST: {
			Object refValue;
			referenceValue = setting.getReferenceValue();
			if (referenceValue != null) {
				try {
					refValue = new Date(DateUtil.parseDateTime(referenceValue));
				} catch (IllegalArgumentException e) {
					try {
						refValue = Long.parseLong(referenceValue);
					} catch (NumberFormatException nfe) {
						try {
							refValue = Double.parseDouble(referenceValue);
						} catch (NumberFormatException nfe2) {
							refValue = referenceValue;
						}
					}
				}
				if (resource instanceof FeatureTypeInfo) {
					retval = new DefaultFeatureNearestValueSelectionStrategy(
							refValue);
				} else if (resource instanceof CoverageInfo) {
					retval = new DefaultCoverageNearestValueSelectionStrategy(
							refValue);
				}
			} else {
				throw new ServiceException(
						"No reference value given for custom dimension default value 'nearest' strategy");
			}
			break;
		}
		case MINIMUM: {
			if (resource instanceof FeatureTypeInfo) {
				retval = featureCustomDimensionMinimumStrategy;
			} else if (resource instanceof CoverageInfo) {
				retval = coverageCustomDimensionMinimumStrategy;
			}
			break;
		}
		case MAXIMUM: {
			if (resource instanceof FeatureTypeInfo) {
				retval = featureCustomDimensionMaximumStrategy;
			} else if (resource instanceof CoverageInfo) {
				retval = coverageCustomDimensionMaximumStrategy;
			}
			break;
		}
		case FIXED: {
			Object refValue;
			referenceValue = setting.getReferenceValue();
			if (referenceValue != null) {
				try {
					refValue = new Date(DateUtil.parseDateTime(referenceValue));
				} catch (IllegalArgumentException e) {
					try {
						refValue = Long.parseLong(referenceValue);
					} catch (NumberFormatException nfe) {
						try {
							refValue = Double.parseDouble(referenceValue);
						} catch (NumberFormatException nfe2) {
							refValue = referenceValue;
						}
					}
				}
			} else {
				throw new ServiceException(
						"No reference value given for custom dimension default value 'fixed' strategy");
			}
			retval = new FixedValueStrategy(refValue);
			break;
		}
		}
		return retval;
	}

}

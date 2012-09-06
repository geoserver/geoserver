/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2012, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.feature;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.AttributeImpl;
import org.geotools.feature.FeatureBuilder;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureFactory;
import org.opengis.feature.Property;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.feature.type.PropertyDescriptor;

/**
 * The complex feature builder allows the construction of features by
 * progressively appending their components and deferring the construction till
 * you're ready.
 * 
 * @author bro879
 * 
 */
public class ComplexFeatureBuilder extends FeatureBuilder<FeatureType, Feature> {
	Map<Name, ArrayList<Property>> values = new HashMap<Name, ArrayList<Property>>();

	public ComplexFeatureBuilder(FeatureType featureType) {
		this(featureType, CommonFactoryFinder.getFeatureFactory(null));
	}

	protected ComplexFeatureBuilder(FeatureType featureType,
			FeatureFactory factory) {
		super(featureType, factory);
	}

	/**
	 * Build and return the feature you've been constructing.
	 * If the id is null it will be assigned from FeatureBuilder.createDefaultFeatureId().
	 */
	@Override
	public Feature buildFeature(String id) {
		// Instantiate if null:
		id = id == null ? FeatureBuilder.createDefaultFeatureId() : id;

		// Validate the values against the featureType; we need to make sure
		// that requirements are honoured:
		for (PropertyDescriptor propertyDescriptor : super.featureType
				.getDescriptors()) {
			Name name = propertyDescriptor.getName();

			// Create a List of Properties for this name if we don't already
			// have one:
			if (!values.containsKey(name)) {
				values.put(name, new ArrayList<Property>());
			}

			// Get the List of Properties:
			List<Property> properties = values.get(name);

			// See if there's a mismatch between the number of properties and
			// minOccurs value:
			int minOccurs = propertyDescriptor.getMinOccurs();
			int numberOfProperties = properties.size();

			if (numberOfProperties < minOccurs) {
				// If the value is nillable anyway then just default it to null:
				if (propertyDescriptor.isNillable()
						&& AttributeDescriptor.class
								.isAssignableFrom(propertyDescriptor.getClass())) {
					do {
						Property nullProperty = new AttributeImpl(
								propertyDescriptor.getType().getBinding()
										.cast(null),
								(AttributeDescriptor) propertyDescriptor, null);

						properties.add(nullProperty);
					} while (++numberOfProperties < minOccurs);
				}
				// NOTE: I was wondering if you could have another if-else here
				// to try to apply default values if they're set..
				// it seems like a good idea but the only problem is that
				// they're only present on the AttributeDescriptors...
				else {
					throw new IllegalStateException(
							String.format(
									"Failed to build feature '%s'; its property '%s' requires at least %s occurrence(s) but number of occurrences was %s.",
									featureType.getName(), name, minOccurs,
									numberOfProperties));
				}
			}
		}

		// Merge the Map<String, ArrayList<Property>> into one collection of
		// properties:
		Collection<Property> properties = new ArrayList<Property>();
		for (Name key : values.keySet()) {
			properties.addAll(values.get(key));
		}

		this.values.clear();
		return factory.createFeature(properties, this.featureType, id);
	}

	/**
	 * Append a property value to the complex feature under construction
	 * and associate it with the name specified.
	 * @param name
	 * 		The name of the property you wish to set.
	 * @param value
	 * 		The value of the property to append.
	 */
	public void append(Name name, Property value) {
		PropertyDescriptor propertyDescriptor = featureType.getDescriptor(name);

		// The 'name' must exist in the type, if not, throw an exception:
		if (propertyDescriptor == null) {
			throw new IllegalArgumentException(
					String.format(
							"The name '%s' is not a valid descriptor name for the type '%s'.",
							name, this.featureType.getName()));
		}

		Class<?> expectedClass = propertyDescriptor.getType().getBinding();
		if (value != null) {
			Class<?> providedClass = value.getType().getBinding();

			// Make sure that the provided class and the expected class match or
			// that the expectedClass is a base class of the providedClass:
			if (!providedClass.equals(expectedClass)
					&& !expectedClass.isAssignableFrom(providedClass)) {
				throw new IllegalArgumentException(
						String.format(
								"The value provided contains an object of '%s' but the method expects an object of '%s'.",
								providedClass, expectedClass));
			}
		} else { // value == null
			if (propertyDescriptor.isNillable()) {
				value = (Property) expectedClass.cast(null);
			} else {
				// NOTE: This could possibly to changed to allow for processing
				// remote xlinks.
				value = (Property) expectedClass.cast(null);
			}
		}

		// At this point the converted value has been set so we must persist it
		// to the object's state:
		ArrayList<Property> valueList;

		if (values.containsKey(name)) {
			valueList = values.get(name);

			// Make sure that the list isn't already at capacity:
			int maxOccurs = propertyDescriptor.getMaxOccurs();
			if (valueList.size() == maxOccurs) {
				throw new IndexOutOfBoundsException(
						String.format(
								"You can't add another object with the name of '%s' because you already have the maximum number (%s) allowed by the property descriptor.",
								name, maxOccurs));
			}
		} else {
			valueList = new ArrayList<Property>();
			values.put(name, valueList);
		}

		valueList.add(value);
	}
}
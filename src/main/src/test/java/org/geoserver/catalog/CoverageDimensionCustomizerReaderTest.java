/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.measure.Quantity;
import javax.measure.quantity.Dimensionless;

import tec.uom.se.unit.BaseUnit;

import org.geoserver.catalog.CoverageDimensionCustomizerReader.WrappedSampleDimension;
import org.geoserver.catalog.impl.CoverageDimensionImpl;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.coverage.Category;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.resources.i18n.Vocabulary;
import org.geotools.resources.i18n.VocabularyKeys;
import org.geotools.util.NumberRange;
import org.junit.Test;
import org.opengis.coverage.ColorInterpretation;
import org.opengis.coverage.SampleDimension;
import org.opengis.coverage.SampleDimensionType;

public class CoverageDimensionCustomizerReaderTest extends GeoServerSystemTestSupport {

    private static final double DELTA = 1E-4;

    /**
     * Test that the null values and range of a wrapped sampleDimension are the same
     * configured on the {@link CoverageDimensionInfo} object used to customize them  
     * @throws IOException
     */
    @Test
    public void testDimensionsWrapping() throws IOException {
        final GridSampleDimension sampleDim = new GridSampleDimension("original", 
                SampleDimensionType.REAL_64BITS, ColorInterpretation.GRAY_INDEX, 
                null, null, new double[]{-9999.0}, -1000d, 1000d, 1d, 0d, null);

        // Setting coverage dimension
        final CoverageDimensionImpl coverageDim = new CoverageDimensionImpl();
        final String wrappedName = "wrapped";
        coverageDim.setName(wrappedName);
        coverageDim.setDimensionType(SampleDimensionType.REAL_64BITS);

        final double newMinimum = -2000d;
        final double newMaximum = 2000d;
        final NumberRange<Double> range = new NumberRange<Double>(Double.class, newMinimum, newMaximum);
        coverageDim.setRange(range);

        final List<Double> nullValues = new ArrayList<Double>();
        final double noData1 = -32768d;
        final double noData2 = -32767d;
        nullValues.add(noData1);
        nullValues.add(noData2);
        coverageDim.setNullValues(nullValues);

        final SampleDimension wrappedDim = WrappedSampleDimension.build(sampleDim, coverageDim);
        double[] noData = wrappedDim.getNoDataValues();

        assertEquals(2, noData.length);
        assertEquals(noData1, noData[0], DELTA);
        assertEquals(noData2, noData[1], DELTA);

        NumberRange wrappedRange = ((WrappedSampleDimension)wrappedDim).getRange();
        assertEquals(newMinimum, wrappedRange.getMinimum(), DELTA);
        assertEquals(newMaximum, wrappedRange.getMaximum(), DELTA);

        assertEquals(wrappedName, wrappedDim.getDescription().toString());
    }
    
    @Test
    public void testWrapCustomizationSurviveCopyConstructor() throws Exception {
        final GridSampleDimension sampleDim = new GridSampleDimension("original", 
                SampleDimensionType.REAL_64BITS, ColorInterpretation.GRAY_INDEX, 
                null, null, new double[]{-9999.0}, -1000d, 1000d, 1d, 0d, null);

        // Setting coverage dimension
        final CoverageDimensionImpl coverageDim = new CoverageDimensionImpl();
        final String wrappedName = "wrapped";
        coverageDim.setName(wrappedName);
        coverageDim.setDimensionType(SampleDimensionType.REAL_64BITS);

        final double newMinimum = -2000d;
        final double newMaximum = 2000d;
        final NumberRange<Double> range = new NumberRange<Double>(Double.class, newMinimum, newMaximum);
        coverageDim.setRange(range);

        final List<Double> nullValues = new ArrayList<Double>();
        final double noData1 = -32768d;
        final double noData2 = -32767d;
        nullValues.add(noData1);
        nullValues.add(noData2);
        coverageDim.setNullValues(nullValues);

        final SampleDimension wrappedDim = WrappedSampleDimension.build(sampleDim, coverageDim);
        double[] noData = wrappedDim.getNoDataValues();

        assertEquals(2, noData.length);
        assertEquals(noData1, noData[0], DELTA);
        assertEquals(noData2, noData[1], DELTA);

        NumberRange wrappedRange = ((WrappedSampleDimension)wrappedDim).getRange();
        assertEquals(newMinimum, wrappedRange.getMinimum(), DELTA);
        assertEquals(newMaximum, wrappedRange.getMaximum(), DELTA);

    }
    
    /**
     * Test that the wrapped nodata categories contains the defined nodata as an int
     * 
     * @throws IOException
     */
    @Test
    public void testIntegerNoDataCategoryWrapping() throws IOException {

        // Setting coverage dimension
        final CoverageDimensionImpl coverageDim = new CoverageDimensionImpl();
        final String wrappedName = "wrapped";
        coverageDim.setName(wrappedName);
        coverageDim.setDimensionType(SampleDimensionType.SIGNED_16BITS);
        coverageDim.setRange(NumberRange.create(0d, 10000d));

        // Definition of the nodata
        final List<Double> nullValues = new ArrayList<Double>();
        final double noData1 = -32768d;
        nullValues.add(noData1);
        coverageDim.setNullValues(nullValues);

        // Quantitative nodata category
        GridSampleDimension sampleDim = new GridSampleDimension("original", new Category[] { new Category(
                Vocabulary.formatInternational(VocabularyKeys.NODATA), new Color[] { new Color(0,
                        0, 0, 0) }, NumberRange.create(-9999, -9999)) }, null);

        // Wrap the dimension
        GridSampleDimension copy = WrappedSampleDimension.build(sampleDim, coverageDim);

        // Extract categories
        List<Category> categories = copy.getCategories();

        // Ensure NoData Category is present
        Category category = categories.get(0);
        assertTrue(category.getName().equals(Category.NODATA.getName()));

        // Check if it contains sampleToGeophisics and the Range contains the first nodata defined
        assertEquals(category.getRange().getMinimum(), noData1, DELTA);
        assertEquals(category.getRange().getMaximum(), noData1, DELTA);
    }

    /**
     * Test that the wrapped nodata categories contains the defined nodata
     * 
     * @throws IOException
     */
    @Test
    public void testNoDataCategoryWrapping() throws IOException {

        // Setting coverage dimension
        final CoverageDimensionImpl coverageDim = new CoverageDimensionImpl();
        final String wrappedName = "wrapped";
        coverageDim.setName(wrappedName);
        coverageDim.setDimensionType(SampleDimensionType.REAL_64BITS);

        // Definition of the nodata
        final List<Double> nullValues = new ArrayList<Double>();
        final double noData1 = -32768d;
        final double noData2 = -32767d;
        nullValues.add(noData1);
        nullValues.add(noData2);
        coverageDim.setNullValues(nullValues);

        // Qualitative nodata category
        GridSampleDimension sampleDim = new GridSampleDimension("original",
                new Category[] { new Category(
                        Vocabulary.formatInternational(VocabularyKeys.NODATA),
                        new Color(0, 0, 0, 0), Double.NaN) }, null);

        // Wrap the dimension
        GridSampleDimension wrappedDim = WrappedSampleDimension.build(sampleDim, coverageDim);
        
        // run the copy constructor
        GridSampleDimension copy = new GridSampleDimension(wrappedDim) {
            // the constructor is visible only to subclasses, hence this hack
        };

        // Extract categories
        List<Category> categories = copy.getCategories();

        // Ensure NoData Category is present
        Category category = categories.get(0);
        assertTrue(category.getName().equals(Category.NODATA.getName()));

        // Check that it does not contain sampleToGeophisics and that the Range contains only NaN
        assertEquals(category.getRange().getMinimum(), Double.NaN, DELTA);
        assertEquals(category.getRange().getMaximum(), Double.NaN, DELTA);

        // Quantitative nodata category
        sampleDim = new GridSampleDimension("original", new Category[] { new Category(
                Vocabulary.formatInternational(VocabularyKeys.NODATA), new Color[] { new Color(0,
                        0, 0, 0) }, NumberRange.create(-9999, -9999)) }, null);

        // Wrap the dimension
        copy = WrappedSampleDimension.build(sampleDim, coverageDim);

        // Extract categories
        categories = copy.getCategories();

        // Ensure NoData Category is present
        category = categories.get(0);
        assertTrue(category.getName().equals(Category.NODATA.getName()));

        // Check if it contains sampleToGeophisics and the Range contains the first nodata defined
        assertEquals(category.getRange().getMinimum(), noData1, DELTA);
        assertEquals(category.getRange().getMaximum(), noData1, DELTA);
    }

    /**
     * Test that if no range is defined, Category values or Default values are used
     * 
     * @throws IOException
     */
    @Test
    public void testNoRange() throws IOException {
        GridSampleDimension sampleDim = new GridSampleDimension("original",
                SampleDimensionType.REAL_64BITS, ColorInterpretation.GRAY_INDEX, null, null,
                new double[] { -9999.0 }, -1000d, 1000d, 1d, 0d, null);

        // Setting coverage dimension
        final CoverageDimensionImpl coverageDim = new CoverageDimensionImpl();
        final String wrappedName = "wrapped";
        coverageDim.setName(wrappedName);
        coverageDim.setDimensionType(SampleDimensionType.REAL_64BITS);
        // Creation of the WrappedSampleDimension
        SampleDimension wrappedDim = WrappedSampleDimension.build(sampleDim, coverageDim);
        // Get the range
        NumberRange<? extends Number> wrappedRange = ((WrappedSampleDimension) wrappedDim)
                .getRange();
        // Ensure the range is not present
        assertNull(wrappedRange);
        // Check if min and max are taken from the categories
        assertEquals(-9999, wrappedDim.getMinimumValue(), DELTA);
        assertEquals(1000, wrappedDim.getMaximumValue(), DELTA);
        // Check that the description is equal to the sample dimension name
        assertEquals(wrappedName, wrappedDim.getDescription().toString());

        // Configure a new GridSampleDimension without categories
        sampleDim = new GridSampleDimension("original", null, new BaseUnit<Dimensionless>("test"));
        // New wrapped sample dimension
        wrappedDim = WrappedSampleDimension.build(sampleDim, coverageDim);
        // Get the range
        wrappedRange = ((WrappedSampleDimension) wrappedDim).getRange();
        // Ensure the range is not present
        assertNull(wrappedRange);
        // Check if min and max are taken from the categories
        assertEquals(Double.NEGATIVE_INFINITY, wrappedDim.getMinimumValue(), DELTA);
        assertEquals(Double.POSITIVE_INFINITY, wrappedDim.getMaximumValue(), DELTA);
    }
}

/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.featureinfo;

import it.geosolutions.imageio.pam.PAMDataset;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;

/** Enriches a GetFeatureInfo response with the fields from a raster attribute table */
class AttributeTableEnricher {

    private final PAMDataset.PAMRasterBand pamRasterBand;

    public AttributeTableEnricher(PAMDataset.PAMRasterBand pamRasterBand) {
        this.pamRasterBand = pamRasterBand;
    }

    /** Returns a predicate that matches the rows in the raster attribute table that match the current pixel value */
    private Predicate<PAMDataset.PAMRasterBand.Row> getRowMatcher(
            PAMDataset.PAMRasterBand.GDALRasterAttributeTable rat, double value) {
        Optional<Integer> minMax = getFieldWithUsage(rat, PAMDataset.PAMRasterBand.FieldUsage.MinMax);
        if (minMax.isPresent())
            return row -> {
                // we have no idea of absolute tolerance, so we use a relative one,
                // based on the "unit in the last place" (spacing between consecutive floating point
                // at the current value magnitude)
                double f = Double.parseDouble(row.getF().get(minMax.get()));
                double magnitude = Math.max(Math.abs(f), Math.abs(value));
                double relativeTolerance = Math.ulp(magnitude) * 10;
                return Math.abs(f - value) <= relativeTolerance;
            };

        Optional<Integer> min = getFieldWithUsage(rat, PAMDataset.PAMRasterBand.FieldUsage.Min);
        Optional<Integer> max = getFieldWithUsage(rat, PAMDataset.PAMRasterBand.FieldUsage.Max);
        if (min.isPresent() && max.isPresent())
            return row -> {
                List<String> fields = row.getF();
                double fMin = Double.parseDouble(fields.get(min.get()));
                double fMax = Double.parseDouble(fields.get(max.get()));
                return fMin <= value && value <= fMax;
            };

        throw new IllegalArgumentException(
                "No field with usage MinMax or Min and Max found, RAT cannot be used to match pixel values.");
    }

    private static Optional<Integer> getFieldWithUsage(
            PAMDataset.PAMRasterBand.GDALRasterAttributeTable rat, PAMDataset.PAMRasterBand.FieldUsage usage) {
        return rat.getFieldDefn().stream()
                .filter(f -> f.getUsage() == usage)
                .map(f -> f.getIndex())
                .findFirst();
    }

    /** Adds the fields from the raster attribute table to the feature type */
    private void addAttributeTableFieldsToFeatureType(
            PAMDataset.PAMRasterBand pamRasterBand, SimpleFeatureTypeBuilder builder) {
        List<PAMDataset.PAMRasterBand.FieldDefn> fields =
                pamRasterBand.getGdalRasterAttributeTable().getFieldDefn();
        for (PAMDataset.PAMRasterBand.FieldDefn field : fields) {
            Class type = String.class;
            switch (field.getType()) {
                case Integer:
                    type = Long.class;
                    break;
                case Real:
                    type = Double.class;
                    break;
            }

            builder.add(field.getName(), type);
        }
    }

    /**
     * Adds the RAT fields to the feature type
     *
     * @param builder
     */
    public void addAttributes(SimpleFeatureTypeBuilder builder) {
        Integer band = pamRasterBand.getBand();
        if (band == null) throw new RuntimeException("Band not found in PAMRasterBand");
        addAttributeTableFieldsToFeatureType(pamRasterBand, builder);
    }

    public void addRowValues(List<Object> values, double[] pixelValues) {
        int band = pamRasterBand.getBand() - 1;
        if (band >= pixelValues.length)
            throw new RuntimeException("Band in PAMRasterBand out of range, band: "
                    + pamRasterBand.getBand()
                    + ", pixelValues.length: "
                    + pixelValues.length);

        double value = pixelValues[band];
        PAMDataset.PAMRasterBand.GDALRasterAttributeTable rat = pamRasterBand.getGdalRasterAttributeTable();
        Predicate<PAMDataset.PAMRasterBand.Row> rowMatcher = getRowMatcher(rat, value);

        for (PAMDataset.PAMRasterBand.Row row : rat.getRow()) {
            if (rowMatcher.test(row)) {
                List<String> fields = row.getF();
                List<PAMDataset.PAMRasterBand.FieldDefn> defs = rat.getFieldDefn();
                for (int i = 0; i < fields.size(); i++) {
                    switch (defs.get(i).getType()) {
                        case Integer:
                            values.add(Long.parseLong(fields.get(i)));
                            break;
                        case Real:
                            values.add(Double.parseDouble(fields.get(i)));
                            break;
                        default:
                            values.add(fields.get(i));
                    }
                }
                // done, no need to continue
                return;
            }
        }

        // if we get here, no match found, add fillers
        for (int i = 0; i < rat.getFieldDefn().size(); i++) {
            values.add(null);
        }
    }

    /**
     * Returns the PAMRasterBand the enricher is working with
     *
     * @return
     */
    PAMDataset.PAMRasterBand getPamRasterBand() {
        return pamRasterBand;
    }
}

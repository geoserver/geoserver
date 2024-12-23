/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rat;

import static it.geosolutions.imageio.pam.PAMDataset.PAMRasterBand.FieldUsage.Alpha;
import static it.geosolutions.imageio.pam.PAMDataset.PAMRasterBand.FieldUsage.Blue;
import static it.geosolutions.imageio.pam.PAMDataset.PAMRasterBand.FieldUsage.BlueMax;
import static it.geosolutions.imageio.pam.PAMDataset.PAMRasterBand.FieldUsage.BlueMin;
import static it.geosolutions.imageio.pam.PAMDataset.PAMRasterBand.FieldUsage.Generic;
import static it.geosolutions.imageio.pam.PAMDataset.PAMRasterBand.FieldUsage.Green;
import static it.geosolutions.imageio.pam.PAMDataset.PAMRasterBand.FieldUsage.GreenMax;
import static it.geosolutions.imageio.pam.PAMDataset.PAMRasterBand.FieldUsage.GreenMin;
import static it.geosolutions.imageio.pam.PAMDataset.PAMRasterBand.FieldUsage.Max;
import static it.geosolutions.imageio.pam.PAMDataset.PAMRasterBand.FieldUsage.Min;
import static it.geosolutions.imageio.pam.PAMDataset.PAMRasterBand.FieldUsage.MinMax;
import static it.geosolutions.imageio.pam.PAMDataset.PAMRasterBand.FieldUsage.Name;
import static it.geosolutions.imageio.pam.PAMDataset.PAMRasterBand.FieldUsage.Red;
import static it.geosolutions.imageio.pam.PAMDataset.PAMRasterBand.FieldUsage.RedMax;
import static it.geosolutions.imageio.pam.PAMDataset.PAMRasterBand.FieldUsage.RedMin;

import it.geosolutions.imageio.pam.PAMDataset.PAMRasterBand.FieldUsage;
import it.geosolutions.imageio.pam.PAMDataset.PAMRasterBand.GDALRasterAttributeTable;
import it.geosolutions.imageio.pam.PAMDataset.PAMRasterBand.Row;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import org.geoserver.wms.featureinfo.RasterLayerIdentifier;
import org.geotools.api.style.ColorMap;
import org.geotools.api.style.Style;
import org.geotools.brewer.styling.builder.ColorMapBuilder;
import org.geotools.brewer.styling.builder.ColorMapEntryBuilder;
import org.geotools.brewer.styling.builder.RasterSymbolizerBuilder;
import org.geotools.brewer.styling.builder.RuleBuilder;
import org.geotools.brewer.styling.builder.StyleBuilder;
import org.geotools.util.ConverterFactory;
import org.geotools.util.Converters;
import org.geotools.util.SuppressFBWarnings;
import org.geotools.util.factory.Hints;

public abstract class RasterAttributeTable {

    private static final Hints SAFE = new Hints(ConverterFactory.SAFE_CONVERSION, true);

    protected final GDALRasterAttributeTable rat;
    protected final ArrayList<Row> rows;
    protected final Map<String, Integer> classifications;

    protected final int bandIdx;

    private static final Set<FieldUsage> COLORS = Set.of(Red, Green, Blue);
    private static final Set<FieldUsage> COLOR_RANGES = Set.of(RedMin, RedMax, GreenMin, GreenMax, BlueMin, BlueMax);
    private final boolean hasSingleColors;
    private final boolean hasRangeColors;
    private final Integer minField;

    public RasterAttributeTable(GDALRasterAttributeTable rat, int bandIdx) {
        this.rat = rat;
        this.bandIdx = bandIdx;
        this.classifications = rat.getFieldDefn().stream()
                .filter(fd -> fd.getUsage() == Name || fd.getUsage() == Generic)
                .collect(Collectors.toMap(fd -> fd.getName(), fd -> fd.getIndex()));
        this.minField = rat.getFieldDefn().stream()
                .filter(fd -> fd.getUsage() == MinMax || fd.getUsage() == Min)
                .map(f -> f.getIndex())
                .findFirst()
                .orElse(null);
        Set<FieldUsage> fieldUsages =
                rat.getFieldDefn().stream().map(f -> f.getUsage()).collect(Collectors.toSet());
        this.hasSingleColors = fieldUsages.containsAll(COLORS);
        this.hasRangeColors = fieldUsages.containsAll(COLOR_RANGES);

        this.rows = new ArrayList<>(rat.getRow());
        // TODO: make null safe?
        Collections.sort(rows, (r1, r2) -> Double.valueOf(r1.getF().get(minField))
                .compareTo(Double.valueOf(r2.getF().get(minField))));
    }

    protected int getClassificationIndex(String classification) {
        Integer value = classifications.get(classification);
        if (value == null) throw new IllegalArgumentException("Unsupported classification: " + classification);
        return value;
    }

    protected Integer getFieldIndex(GDALRasterAttributeTable rat, FieldUsage usage, boolean strict) {
        Integer idx = rat.getFieldDefn().stream()
                .filter(fd -> fd.getUsage() == usage)
                .findFirst()
                .map(f -> f.getIndex())
                .orElse(null);
        if (strict && idx == null) throw new RuntimeException("Could not find field for usage " + usage);
        return idx;
    }

    public Set<String> getClassifications() {
        return classifications.keySet();
    }

    public Style classify(String fieldName) {
        StyleBuilder sb = new StyleBuilder();
        sb.name(fieldName);
        RuleBuilder rule = sb.featureTypeStyle().rule();
        rule.name(fieldName);
        RasterSymbolizerBuilder raster = rule.raster();
        if (bandIdx > 0) raster.channelSelection().gray().channelName(String.valueOf(bandIdx + 1));

        if (classifications.get(fieldName) == null)
            throw new IllegalArgumentException(
                    "Cannot classify on the given field: " + fieldName + ", please use one of: " + classifications);

        ColorMapBuilder colorMapBuilder = raster.colorMap();
        if (hasSingleColors) buildClassificationColorMap(colorMapBuilder, fieldName);
        else if (hasRangeColors) throw new UnsupportedOperationException("Cannot classify on color ranges yet");
        else buildUniqueValuesColorMap(colorMapBuilder, fieldName);

        raster.option(RasterLayerIdentifier.INCLUDE_RAT, "true");

        return sb.build();
    }

    protected abstract void buildUniqueValuesColorMap(ColorMapBuilder colorMapBuilder, String fieldName);

    protected abstract void buildClassificationColorMap(ColorMapBuilder colorMapBuilder, String classification);

    /** Build a decent color ramp for the given number of colors */
    @SuppressFBWarnings("DMI_RANDOM_USED_ONLY_ONCE")
    private static List<Color> randomColors(int numColors) {
        List<Color> ramp = new ArrayList<>(numColors);
        // use a fixed seed to ensure that the colors are stable for the same number of classes
        Random rand = new Random(0);

        // semi-randomly generate colors, but ensure that they are not too similar by picking
        // well separated hues
        for (int i = 0; i < numColors; i++) {
            float hue = 1f / numColors * i;
            // Ensure that the colors are not too pale or too dark
            float saturation = rand.nextFloat() * 0.5f + 0.5f;
            // Ensure that the colors are not too bright or too dull
            float brightness = rand.nextFloat() * 0.5f + 0.5f;

            Color randomColor = Color.getHSBColor(hue, saturation, brightness);
            ramp.add(randomColor);
        }
        // shuffle the colors to avoid having a sequence of similar colors
        Collections.shuffle(ramp, rand);

        return ramp;
    }

    public static class Recode extends RasterAttributeTable {

        private final int valueIdx;
        private final Integer redIdx;
        private final Integer greenIdx;
        private final Integer blueIdx;

        private final Integer alphaIdx;

        public Recode(GDALRasterAttributeTable rat, int bandIdx) {
            super(rat, bandIdx);

            this.valueIdx = getFieldIndex(rat, MinMax, true);
            this.redIdx = getFieldIndex(rat, Red, false);
            this.greenIdx = getFieldIndex(rat, Green, false);
            this.blueIdx = getFieldIndex(rat, Blue, false);
            this.alphaIdx = getFieldIndex(rat, Alpha, false);
        }

        @Override
        protected void buildUniqueValuesColorMap(ColorMapBuilder cmb, String fieldName) {
            int fieldIdx = getClassificationIndex(fieldName);
            cmb.type(ColorMap.TYPE_VALUES);
            Map<String, Color> colorMap = getUniqueValuesColorMap(fieldIdx);
            rows.forEach(row -> rowToColorMapEntry(fieldIdx, cmb, row, colorMap));
        }

        protected void rowToColorMapEntry(int classificationIdx, ColorMapBuilder cm, Row row, Map<String, Color> map) {
            ColorMapEntryBuilder entry = cm.entry();
            List<String> values = row.getF();
            Double quantity = Converters.convert(values.get(valueIdx), Double.class, SAFE);
            String label = Converters.convert(values.get(classificationIdx), String.class);
            Color color = map.get(label);
            entry.quantity(quantity);
            entry.label(label);
            entry.color(color);
        }

        @Override
        protected void buildClassificationColorMap(ColorMapBuilder cm, String classification) {
            cm.type(ColorMap.TYPE_VALUES);
            int classificationIdx = getClassificationIndex(classification);
            rows.forEach(row -> rowToColorMapEntry(classificationIdx, cm, row));
        }

        protected void rowToColorMapEntry(int classificationIdx, ColorMapBuilder cm, Row row) {
            ColorMapEntryBuilder entry = cm.entry();
            List<String> values = row.getF();
            Double quantity = Converters.convert(values.get(valueIdx), Double.class, SAFE);
            String label = Converters.convert(values.get(classificationIdx), String.class);
            // todo: make conversions strict and throw exception?
            int red = Converters.convert(values.get(redIdx), Integer.class, SAFE);
            int green = Converters.convert(values.get(greenIdx), Integer.class, SAFE);
            int blue = Converters.convert(values.get(blueIdx), Integer.class, SAFE);
            Color color = new Color(red, green, blue);
            entry.quantity(quantity);
            entry.label(label);
            entry.color(color);
            if (alphaIdx != null) {
                int alpha = Converters.convert(values.get(alphaIdx), Integer.class, SAFE);
                entry.opacity(alpha / 255.0);
            }
        }
    }

    protected Map<String, Color> getUniqueValuesColorMap(int fieldIdx) {
        List<String> uniqueValues = rows.stream()
                .map(r -> r.getF().get(fieldIdx))
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        Map<String, Color> colorMap = new LinkedHashMap<>();
        List<Color> colors = randomColors(uniqueValues.size());
        for (int i = 0; i < uniqueValues.size(); i++) {
            colorMap.put(uniqueValues.get(i), colors.get(i));
        }
        return colorMap;
    }

    public static class Categorize extends RasterAttributeTable {

        private final int minIdx;

        private final int maxIdx;
        private final Integer redIdx;
        private final Integer greenIdx;
        private final Integer blueIdx;
        private final Integer alphaIdx;

        public Categorize(GDALRasterAttributeTable rat, int bandIdx) {
            super(rat, bandIdx);

            this.minIdx = getFieldIndex(rat, Min, true);
            this.maxIdx = getFieldIndex(rat, Max, true);
            this.redIdx = getFieldIndex(rat, Red, false);
            this.greenIdx = getFieldIndex(rat, Green, false);
            this.blueIdx = getFieldIndex(rat, Blue, false);
            this.alphaIdx = getFieldIndex(rat, Alpha, false);
        }

        @Override
        protected void buildUniqueValuesColorMap(ColorMapBuilder cmb, String fieldName) {
            int fieldIdx = getClassificationIndex(fieldName);
            cmb.type(ColorMap.TYPE_INTERVALS);
            Map<String, Color> colors = getUniqueValuesColorMap(fieldIdx);
            Double lastMax = null;
            for (int i = 0; i < rows.size(); i++) {
                Row row = rows.get(i);
                lastMax = Converters.convert(row.getF().get(maxIdx), Double.class, SAFE);
                rowToColorMapEntry(fieldIdx, cmb, row, colors);
                if (i < rows.size() - 1) {
                    Row nextRow = rows.get(i + 1);
                    Double nextMin = Converters.convert(nextRow.getF().get(minIdx), Double.class, SAFE);

                    if (lastMax < nextMin) {
                        ColorMapEntryBuilder entry = cmb.entry();
                        entry.quantity(lastMax);
                        entry.color(Color.WHITE);
                        entry.opacity(0);
                    }
                }
            }

            if (!lastMax.isInfinite()) {
                ColorMapEntryBuilder entry = cmb.entry();
                entry.quantity(lastMax);
                entry.color(Color.WHITE);
                entry.opacity(0);
            }
        }

        protected void rowToColorMapEntry(int fieldIdx, ColorMapBuilder cm, Row row, Map<String, Color> colors) {
            ColorMapEntryBuilder entry = cm.entry();
            List<String> values = row.getF();
            Double min = Converters.convert(values.get(minIdx), Double.class, SAFE);
            String label = Converters.convert(values.get(fieldIdx), String.class);
            Color color = colors.get(label);
            entry.quantity(min);
            entry.label(label);
            entry.color(color);
            if (alphaIdx != null) {
                int alpha = Converters.convert(values.get(alphaIdx), Integer.class, SAFE);
                entry.opacity(alpha / 255.0);
            }
        }

        @Override
        protected void buildClassificationColorMap(ColorMapBuilder cm, String classification) {
            int classificationIdx = getClassificationIndex(classification);
            cm.type(ColorMap.TYPE_INTERVALS);
            Double lastMax = null;
            for (int i = 0; i < rows.size(); i++) {
                Row row = rows.get(i);
                lastMax = Converters.convert(row.getF().get(maxIdx), Double.class, SAFE);
                rowToColorMapEntry(classificationIdx, cm, row);
                if (i < rows.size() - 1) {
                    Row nextRow = rows.get(i + 1);
                    Double nextMin = Converters.convert(nextRow.getF().get(minIdx), Double.class, SAFE);

                    if (lastMax < nextMin) {
                        ColorMapEntryBuilder entry = cm.entry();
                        entry.quantity(lastMax);
                        entry.color(Color.WHITE);
                        entry.opacity(0);
                    }
                }
            }

            if (!lastMax.isInfinite()) {
                ColorMapEntryBuilder entry = cm.entry();
                entry.quantity(lastMax);
                entry.color(Color.WHITE);
                entry.opacity(0);
            }
        }

        protected void rowToColorMapEntry(int classificationIdx, ColorMapBuilder cm, Row row) {
            ColorMapEntryBuilder entry = cm.entry();
            List<String> values = row.getF();
            Double min = Converters.convert(values.get(minIdx), Double.class, SAFE);
            String label = Converters.convert(values.get(classificationIdx), String.class);
            int red = Converters.convert(values.get(redIdx), Integer.class, SAFE);
            int green = Converters.convert(values.get(greenIdx), Integer.class, SAFE);
            int blue = Converters.convert(values.get(blueIdx), Integer.class, SAFE);
            Color color = new Color(red, green, blue);
            entry.quantity(min);
            entry.label(label);
            entry.color(color);
            if (alphaIdx != null) {
                int alpha = Converters.convert(values.get(alphaIdx), Integer.class, SAFE);
                entry.opacity(alpha / 255.0);
            }
        }
    }
}

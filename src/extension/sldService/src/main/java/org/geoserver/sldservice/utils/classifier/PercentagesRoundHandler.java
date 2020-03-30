/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.sldservice.utils.classifier;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.stream.DoubleStream;
import org.apache.commons.lang3.ArrayUtils;

class PercentagesRoundHandler {

    private int scale;

    PercentagesRoundHandler(Integer scale) {
        this.scale = scale != null ? scale : 1;
    }

    PercentagesRoundHandler() {
        this.scale = 1;
    }

    List<Double> roundPercentages(List<Double> percentages) {
        Double[] array = percentages.toArray(new Double[percentages.size()]);
        double[] rounded = roundPercentages(ArrayUtils.toPrimitive(array));
        return Arrays.asList(ArrayUtils.toObject(rounded));
    }

    double[] roundPercentages(double[] percentages) {
        if (percentages == null) return null;
        double delta = 0.0;
        for (int i = 0; i < percentages.length; i++) {
            double percentage = percentages[i] += delta;
            double rounded = roundDouble(percentage);
            delta = percentage - rounded;
            percentages[i] = rounded;
        }
        if (DoubleStream.of(percentages).sum() != 100.0) {
            double firstRule = percentages[0] + delta;
            percentages[0] = roundDouble(firstRule);
        }

        return percentages;
    }

    double roundDouble(double percentage) {
        BigDecimal bd =
                new BigDecimal(String.valueOf(percentage)).setScale(scale, RoundingMode.HALF_EVEN);
        return bd.doubleValue();
    }
}

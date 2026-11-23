/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.sldservice.utils.classifier.impl;

import static org.junit.Assert.assertEquals;

import java.awt.Color;
import java.util.List;
import org.junit.Test;

public class CustomColorRampTest {

    /** Colors in between land on the given ones: classes 0, 2 and 4 are red, green and blue. */
    @Test
    public void testMoreClassesThanColors() throws Exception {
        List<Color> ramp = ramp(5, "#FF0000", "#00FF00", "#0000FF");
        assertEquals(colors("#FF0000", "#808000", "#00FF00", "#008080", "#0000FF"), ramp);
    }

    /** Both given colors are classes, the first and the last. */
    @Test
    public void testTwoColors() throws Exception {
        List<Color> ramp = ramp(5, "#000000", "#FFFFFF");
        assertEquals(colors("#000000", "#404040", "#808080", "#BFBFBF", "#FFFFFF"), ramp);
    }

    /** Fewer than two classes per color still blends, rather than giving black classes. */
    @Test
    public void testFewClassesPerColor() throws Exception {
        List<Color> ramp = ramp(8, "#000000", "#333333", "#666666", "#999999", "#CCCCCC", "#FFFFFF");
        List<Color> expected =
                colors("#000000", "#242424", "#494949", "#6D6D6D", "#929292", "#B6B6B6", "#DBDBDB", "#FFFFFF");
        assertEquals(expected, ramp);
    }

    @Test
    public void testOneColor() throws Exception {
        assertEquals(colors("#FF0000", "#FF0000", "#FF0000"), ramp(3, "#FF0000"));
    }

    private static List<Color> ramp(int classes, String... colors) throws Exception {
        CustomColorRamp ramp = new CustomColorRamp();
        ramp.setInputColors(colors(colors));
        ramp.setNumClasses(classes);
        return ramp.getRamp();
    }

    private static List<Color> colors(String... colors) {
        return List.of(colors).stream().map(Color::decode).toList();
    }
}

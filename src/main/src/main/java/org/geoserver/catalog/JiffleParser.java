/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import it.geosolutions.jaiext.jiffle.Jiffle;
import it.geosolutions.jaiext.jiffle.JiffleException;
import it.geosolutions.jaiext.jiffle.parser.JiffleParserException;
import it.geosolutions.jaiext.jiffle.parser.node.GetSourceValue;
import it.geosolutions.jaiext.jiffle.runtime.JiffleIndirectRuntime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility class for parsing Jiffle scripts and extracting metadata about input and output variables.
 *
 * <p>Provides methods to analyze Jiffle scripts, identifying input and output variables, and capturing parsing details
 * for further processing.
 */
public class JiffleParser {

    /**
     * Represents the result of parsing a Jiffle script, containing output variable details and parsing metadata.
     *
     * <p>Holds information about the output variable, number of bands, input bands, and any parsing errors encountered
     * during Jiffle script analysis.
     */
    public static class JiffleParsingResult {
        public String outputVar;
        public int numBands;
        public Set<CoverageView.InputCoverageBand> inputBands;
        public String error;
    }

    /**
     * Parses a Jiffle script and extracts metadata about input and output variables.
     *
     * @param outputVariable The name of the output variable in the Jiffle script
     * @param script The Jiffle script to parse
     * @param coverageNames List of coverage names used in the script
     * @return A JiffleParsingResult containing parsed script details
     * @throws IllegalArgumentException if script parsing fails
     */
    public static JiffleParsingResult parse(String outputVariable, String script, List<String> coverageNames)
            throws IllegalArgumentException {
        JiffleParsingResult result = new JiffleParsingResult();

        try {
            // First, let's get the input variables to be used
            Set<GetSourceValue> readPositions = Jiffle.getReadPositions(script, coverageNames);
            Set<CoverageView.InputCoverageBand> inputVariables = new HashSet<>();
            for (GetSourceValue sourceValue : readPositions) {
                inputVariables.add(new CoverageView.InputCoverageBand(
                        sourceValue.getVarName(), sourceValue.getPos().getBand().toString()));
            }

            // Now parse and validate the script
            Jiffle jiffle = new Jiffle();
            jiffle.setScript(script);
            Map<String, Jiffle.ImageRole> imageParams = new HashMap<>();

            for (CoverageView.InputCoverageBand inputBand : inputVariables) {
                imageParams.put(inputBand.getCoverageName(), Jiffle.ImageRole.SOURCE);
            }
            imageParams.put(outputVariable, Jiffle.ImageRole.DEST);
            jiffle.setImageParams(imageParams);
            jiffle.compile();
            JiffleIndirectRuntime runtime =
                    (JiffleIndirectRuntime) jiffle.getRuntimeInstance(Jiffle.RuntimeModel.INDIRECT);
            result.inputBands = inputVariables;
            result.outputVar = outputVariable;
            result.numBands = runtime.getOutputBands();

        } catch (JiffleException | JiffleParserException je) {
            result.error = "Exception occurred while parsing the jiffle script: " + je.getLocalizedMessage();
        }
        return result;
    }
}

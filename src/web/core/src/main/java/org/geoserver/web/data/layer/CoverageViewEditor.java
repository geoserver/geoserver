/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layer;

import static org.geoserver.catalog.CoverageView.BAND_SEPARATOR;
import static org.geoserver.catalog.JiffleFormulaParser.extractBandNames;
import static org.geoserver.catalog.JiffleFormulaParser.extractInputBandsFromExpression;

import it.geosolutions.jaiext.JAIExt;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageView;
import org.geoserver.catalog.CoverageView.CompositionType;
import org.geoserver.catalog.CoverageView.CoverageBand;
import org.geoserver.catalog.CoverageView.EnvelopeCompositionType;
import org.geoserver.catalog.CoverageView.InputCoverageBand;
import org.geoserver.catalog.CoverageView.SelectedResolution;
import org.geoserver.catalog.JiffleFormulaParser;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.JiffleExpressionValidator;

/**
 * A panel for editing a coverage view.
 *
 * <p>It allows to select the input coverages and the output bands.
 */
public class CoverageViewEditor extends FormComponentPanel<List<String>> {

    private static final List<CompositionType> SUPPORTED_MODES = Arrays.stream(CompositionType.values())
            .filter(v -> v != CompositionType.UNSUPPORTED)
            .collect(Collectors.toList());

    IModel<List<String>> coverages;
    IModel<List<CoverageBand>> outputBands;
    List<String> availableCoverages;
    List<CoverageBand> currentOutputBands;
    ListMultipleChoice<String> coveragesChoice;
    IModel<CompositionType> compositionType;
    IModel<EnvelopeCompositionType> envelopeCompositionType;
    IModel<SelectedResolution> selectedResolution;
    IModel<String> resolutionReferenceCoverage;
    ListMultipleChoice<CoverageBand> outputBandsChoice;
    Map<String, Integer> inputCoverageBands = new HashMap<>();
    String storeId;
    WebMarkupContainer bandChoiceContainer;
    WebMarkupContainer jiffleEditorContainer;
    Label inputBandSummary;
    TextArea<String> jiffleFormulaArea;
    IModel<String> jiffleFormulaModel = Model.of("");

    /** Creates a new editor. */
    public CoverageViewEditor(
            String id,
            final IModel<List<String>> inputCoverages,
            final IModel<List<CoverageBand>> bands,
            IModel<EnvelopeCompositionType> envelopeCompositionType,
            IModel<SelectedResolution> selectedResolution,
            IModel<String> resolutionReferenceCoverage,
            IModel<CompositionType> compositionType,
            List<String> availableCoverages,
            String storeId) {
        super(id, inputCoverages);
        this.storeId = storeId;
        this.coverages = inputCoverages;
        this.outputBands = bands;
        this.envelopeCompositionType = envelopeCompositionType;
        this.selectedResolution = selectedResolution;
        this.compositionType = compositionType;
        this.resolutionReferenceCoverage = resolutionReferenceCoverage;
        this.availableCoverages = availableCoverages;

        coveragesChoice = new ListMultipleChoice<>(
                "coveragesChoice", new Model<>(), new ArrayList<>(coverages.getObject()), new ChoiceRenderer<>() {
                    @Override
                    public Object getDisplayValue(String coverage) {
                        return coverage;
                    }
                });
        coveragesChoice.setOutputMarkupId(true);

        new ArrayList<CoverageBand>();
        outputBandsChoice = new ListMultipleChoice<>(
                "outputBandsChoice", new Model<>(), new ArrayList<>(outputBands.getObject()), new ChoiceRenderer<>() {
                    @Override
                    public Object getDisplayValue(CoverageBand vcb) {
                        return vcb.getDefinition();
                    }
                });
        outputBandsChoice.setOutputMarkupId(true);
        currentOutputBands = new ArrayList<>(outputBandsChoice.getChoices());

        DropDownChoice<CompositionType> compositionModeChoice =
                new DropDownChoice<>("compositionMode", SUPPORTED_MODES);
        compositionModeChoice.setModel(compositionType);
        compositionModeChoice.setChoiceRenderer(new EnumChoiceRenderer<>(this));

        compositionModeChoice.setOutputMarkupId(true);
        compositionModeChoice.add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                CompositionType selectedMode = compositionType.getObject();
                bandChoiceContainer.setVisible(selectedMode == CompositionType.BAND_SELECT);
                jiffleEditorContainer.setVisible(selectedMode == CompositionType.JIFFLE);

                if (selectedMode == CompositionType.JIFFLE) {
                    inputBandSummary.modelChanged();
                    target.add(inputBandSummary);
                } else {
                    currentOutputBands.clear();
                    outputBandsChoice.setChoices(currentOutputBands);
                    outputBandsChoice.modelChanged();
                }

                target.add(bandChoiceContainer);
                target.add(jiffleEditorContainer);
            }
        });
        add(compositionModeChoice);

        bandChoiceContainer = new WebMarkupContainer("bandChoiceContainer");
        bandChoiceContainer.setOutputMarkupId(true);
        bandChoiceContainer.add(coveragesChoice);
        bandChoiceContainer.add(outputBandsChoice);
        bandChoiceContainer.add(addBandButton());
        bandChoiceContainer.add(addRemoveButton());
        bandChoiceContainer.add(addRemoveAllButton());

        add(bandChoiceContainer);

        jiffleEditorContainer = new WebMarkupContainer("jiffleEditorContainer");
        inputBandSummary = new Label("inputBandSummary", new LoadableDetachableModel<String>() {
            @Override
            protected String load() {
                try {
                    return generateBandSummary();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        inputBandSummary.setOutputMarkupId(true);
        jiffleEditorContainer.add(inputBandSummary);

        jiffleFormulaArea = new TextArea<>("jiffleFormula", jiffleFormulaModel);
        jiffleFormulaArea.setOutputMarkupId(true);

        IModel<Map<String, Integer>> bandInfoModel = () -> inputCoverageBands;

        jiffleFormulaArea.add(new JiffleExpressionValidator(bandInfoModel));
        jiffleEditorContainer.add(jiffleFormulaArea);
        jiffleEditorContainer.setOutputMarkupId(true);

        add(jiffleEditorContainer);
        bandChoiceContainer.setOutputMarkupPlaceholderTag(true);
        jiffleEditorContainer.setOutputMarkupPlaceholderTag(true);

        CompositionType selected = compositionType.getObject();
        boolean isJiffle = selected == CompositionType.JIFFLE;
        bandChoiceContainer.setVisible(!isJiffle);
        jiffleEditorContainer.setVisible(isJiffle);
        if (isJiffle) {
            List<String> jiffleExpressions = outputBands.getObject().stream()
                    .filter(band -> band.getCompositionType() == CompositionType.JIFFLE)
                    .map(b -> JiffleFormulaParser.extractScriptFromCoverageBand(b))
                    .collect(Collectors.toList());
            if (!jiffleExpressions.isEmpty()) {
                jiffleFormulaModel.setObject(String.join("\n", jiffleExpressions));
            }
        }

        // heterogeneous coverage controls
        WebMarkupContainer heterogeneousControlsContainer = new WebMarkupContainer("heterogeneousControlsContainer");
        heterogeneousControlsContainer.setOutputMarkupId(true);
        add(heterogeneousControlsContainer);
        WebMarkupContainer heterogeneousControls = new WebMarkupContainer("heterogeneousControls");
        heterogeneousControlsContainer.add(heterogeneousControls);
        // need the band-merge from JAI-EXT to work in heterogeneous mode
        heterogeneousControls.setVisible(JAIExt.isJAIExtOperation("BandMerge"));

        DropDownChoice<EnvelopeCompositionType> envelopePolicy =
                new DropDownChoice<>("envelopeCompositionType", Arrays.asList(EnvelopeCompositionType.values()));
        envelopePolicy.setModel(envelopeCompositionType);
        envelopePolicy.setChoiceRenderer(new EnumChoiceRenderer<>(this));
        heterogeneousControls.add(envelopePolicy);

        DropDownChoice<SelectedResolution> resolutionPolicy =
                new DropDownChoice<>("selectedResolution", Arrays.asList(SelectedResolution.values()));
        resolutionPolicy.setModel(selectedResolution);
        resolutionPolicy.setChoiceRenderer(new EnumChoiceRenderer<>(this));
        heterogeneousControls.add(resolutionPolicy);

        add(addRemoveAllButton());
        add(addRemoveButton());
    }

    public void validateAndSave() {
        if (compositionType.getObject() == CompositionType.JIFFLE) {
            saveJiffleOutputBandsFromFormula();
            outputBandsChoice.setChoices(currentOutputBands);
            outputBandsChoice.modelChanged();
        }
    }

    private AjaxButton addBandButton() {
        AjaxButton button = new AjaxButton("addBand") {

            @Override
            public void onSubmit(AjaxRequestTarget target) {
                List<String> selection = (List<String>) coveragesChoice.getModelObject();
                List<CoverageBand> bandsList = new ArrayList<>();
                int i = currentOutputBands.size();
                for (String coverage : selection) {
                    final int bandIndexChar = coverage.indexOf(BAND_SEPARATOR);
                    String coverageName = coverage;
                    String bandIndex = null;
                    if (bandIndexChar != -1) {
                        coverageName = coverage.substring(0, bandIndexChar);
                        bandIndex = coverage.substring(bandIndexChar + 1);
                    }
                    CoverageBand band = new CoverageBand(
                            Collections.singletonList(new InputCoverageBand(coverageName, bandIndex)),
                            coverage,
                            i++,
                            CompositionType.BAND_SELECT,
                            null);
                    bandsList.add(band);
                }
                currentOutputBands.addAll(bandsList);
                outputBandsChoice.setChoices(currentOutputBands);
                outputBandsChoice.modelChanged();
                coveragesChoice.setChoices(availableCoverages);
                coveragesChoice.modelChanged();

                // TODO: Reset choice
                target.add(coveragesChoice);
                target.add(outputBandsChoice);
            }
        };
        return button;
    }

    private AjaxButton addRemoveAllButton() {
        AjaxButton button = new AjaxButton("removeAllBands") {

            @Override
            public void onSubmit(AjaxRequestTarget target) {
                List<CoverageBand> outputBands = (List<CoverageBand>) outputBandsChoice.getModelObject();
                outputBands.clear();
                currentOutputBands.clear();
                outputBandsChoice.setChoices(currentOutputBands);
                outputBandsChoice.modelChanged();

                // TODO: Reset choice
                target.add(outputBandsChoice);
            }
        };
        return button;
    }

    private AjaxButton addRemoveButton() {
        AjaxButton button = new AjaxButton("removeBands") {

            @Override
            public void onSubmit(AjaxRequestTarget target) {

                List<CoverageBand> removedBands =
                        (List<CoverageBand>) outputBandsChoice.getModel().getObject();

                for (Object band : removedBands) {
                    currentOutputBands.remove(band);
                }

                outputBandsChoice.setChoices(currentOutputBands);
                outputBandsChoice.modelChanged();

                // TODO: Reset choice
                target.add(outputBandsChoice);
            }
        };
        return button;
    }

    private void saveJiffleOutputBandsFromFormula() {
        String formulaText = jiffleFormulaModel.getObject();
        if (formulaText == null || formulaText.isBlank()) return;

        String fixedScript = fixMissingSemicolons(formulaText);
        jiffleFormulaModel.setObject(fixedScript); // update the model with fixed script

        // Extract the output variables
        JiffleFormulaParser.JiffleOutputResult parsed = JiffleFormulaParser.extractOutputAssignments(fixedScript);
        if (parsed.outputVar == null || parsed.expressions.isEmpty()) return;

        boolean isMultiBand = parsed.expressions.size() > 1;

        // Extract the input coverages/bands
        Set<String> inputNames = extractBandNames(fixedScript);
        inputNames.remove(parsed.outputVar);

        List<CoverageBand> newBands = new ArrayList<>();
        for (Map.Entry<Integer, String> entry : parsed.expressions.entrySet()) {
            int bandIndex = entry.getKey();
            String outputBand = parsed.outputVar;
            String expr = entry.getValue();
            if (isMultiBand) {
                outputBand += (BAND_SEPARATOR + bandIndex);
            }
            List<InputCoverageBand> inputBands = extractInputBandsFromExpression(expr);
            CoverageBand band = new CoverageBand(inputBands, expr, bandIndex, CompositionType.JIFFLE);
            band.setOutputName(outputBand);
            newBands.add(band);
        }

        // Replace output bands
        currentOutputBands.clear();
        currentOutputBands.addAll(newBands);
        outputBands.setObject(newBands);
    }

    private String generateBandSummary() throws IOException {
        StringBuilder sb = new StringBuilder();
        Catalog catalog = GeoServerApplication.get().getCatalog();
        List<String> bands = new ArrayList<>();
        inputCoverageBands.clear();
        CoverageView.buildCoverageBands(catalog, storeId, CompositionType.JIFFLE, bands, inputCoverageBands);
        sb.append(String.join("\n", bands)).append("\n");
        return sb.toString();
    }

    private static String fixMissingSemicolons(String script) {
        String[] lines = script.split("\\r?\\n");
        StringBuilder fixed = new StringBuilder();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            // If the line looks like an assignment and doesn't end in ";", add it
            if ((trimmed.contains("=") && !trimmed.endsWith(";"))) {
                fixed.append(trimmed).append(";").append("\n");
            } else {
                fixed.append(trimmed).append("\n");
            }
        }

        return fixed.toString();
    }
}

/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
/**
 * This is the JS component for the EnvelopePanel.
 *
 * Its very simple - there is a data entry form (where the user types in the MinX, MaxX, etc...) and
 * a diagram (which shows it in a more user-friendly manner).  The diagram is non-editable.
 *
 * This is very simple process - it copies the information from the user "<input>" to the corresponding place
 * in the diagram.
 *
 * The process for finding the elements is a bit more complex than you might expect because there
 * could be multiple EnvelopePanels on the page (i.e. Editing a Layer with the native and lat/long bounding box).
 *
 * 1. Wicket will put in a unique ID for the control on the page.
 * 2. We find that HTML tag on the page
 * 3. We then search inside for the id="MaxX" elements.  There could be more than one on the page, so we
 *    search "below" the this-component root tag.
 * 4. We search for and input tag with the attribute "valueref=MaxX" "<input ... valueref="MaxX">
 * 5. Once we have found the source (user input) and destination (non-editable in the diagram), we move the value over
 * 6. We also add an input event watch on the user-input "<input>" and do the value transfer whenever the user
 *    types something.
 */


/**
 *
 * This is called by wicket/js when the DOM is ready.
 *
 * This will:
 *
 * 1. call EnvelopePanel_valueChanged that will move the values over
 * 2. setup "input" listers on the user-data-entry "<input>"s that just
 *    calls EnvelopePanel_valueChanged (i.e. moves the data from source (user input) to
 *    destination (non-editable in the diagram)).
 *
 * @param elementId root element ID (from Wicket)
 */
function EnvelopePanel_setup(elementId) {
    //root element for the EnvelopePanel (from wicket)
    var mainElement = $("#"+elementId);
    EnvelopePanel_valueChanged(elementId);

    var input_maxX = mainElement.find("#maxX");
    var input_maxY = mainElement.find("#maxY");
    var input_minX = mainElement.find("#minX");
    var input_minY = mainElement.find("#minY");

    var input_minZ = mainElement.find("#minZ");
    var input_maxZ = mainElement.find("#maxZ");

    input_maxX.on('input',function() {
            EnvelopePanel_valueChanged(elementId);
        }
    );
    input_maxY.on('input',function() {
            EnvelopePanel_valueChanged(elementId);
        }
    );
    input_minX.on('input',function() {
            EnvelopePanel_valueChanged(elementId);
        }
    );
    input_minY.on('input',function() {
            EnvelopePanel_valueChanged(elementId);
        }
    );

    //Z might not be there
    if (input_minZ.length !==0) {
        input_minZ.on('input', function () {
                EnvelopePanel_valueChanged(elementId);
            }
        );
        input_maxZ.on('input', function () {
                EnvelopePanel_valueChanged(elementId);
            }
        );
    }
}

/**
 * This moves source (user input) to destination (non-editable in the diagram).
 *
 * @param elementId
 */
function EnvelopePanel_valueChanged(elementId) {

    //root element for the EnvelopePanel (from wicket)
    var mainElement = $("#"+elementId);

    //find <input> for the user dataentry
    var input_maxX = mainElement.find("#maxX");
    var input_maxY = mainElement.find("#maxY");
    var input_minX = mainElement.find("#minX");
    var input_minY = mainElement.find("#minY");

    var input_minZ = mainElement.find("#minZ");
    var input_maxZ = mainElement.find("#maxZ");


    //these are destinations (non-editable in the diagram)
    var diagramElement = mainElement.find(".diagram");
    var diagram_maxX = diagramElement.find("input[valueref='maxX']");
    var diagram_maxY = diagramElement.find("input[valueref='maxY']");
    var diagram_minX = diagramElement.find("input[valueref='minX']");
    var diagram_minY = diagramElement.find("input[valueref='minY']");

    var diagram_minZ = diagramElement.find("input[valueref='minZ']");
    var diagram_maxZ = diagramElement.find("input[valueref='maxZ']");

    diagram_maxX.val(input_maxX.val());
    diagram_maxY.val(input_maxY.val());
    diagram_minX.val(input_minX.val());
    diagram_minY.val(input_minY.val());

    //Z might not be there
    if (input_minZ.length !==0) {
        diagram_minZ.val(input_minZ.val());
        diagram_maxZ.val(input_maxZ.val());
    }
}
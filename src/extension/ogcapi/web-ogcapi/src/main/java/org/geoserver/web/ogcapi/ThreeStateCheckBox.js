/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Wires a display checkbox to a hidden value field, cycling three states on click:
   null (indeterminate) -> true (checked) -> false (unchecked) -> null. The hidden
   field holds "true"/"false"/"" so the server can tell false from null. */
function gsTriStateInit(displayId, valueId) {
    var display = document.getElementById(displayId);
    var value = document.getElementById(valueId);
    if (!display || !value) return;
    gsTriStateApply(display, value.value);
    if (display.dataset.gsTristateBound) return; // avoid a second listener on re-init
    display.dataset.gsTristateBound = "true";
    display.addEventListener("click", function (e) {
        e.preventDefault(); // cancel the native toggle; the browser restores checked/indeterminate after dispatch
        // apply after the click activation finishes, otherwise the restore would overwrite these values
        setTimeout(function () {
            value.value = gsTriStateNext(value.value);
            gsTriStateApply(display, value.value);
        }, 0);
    });
}

function gsTriStateNext(current) {
    if (current === "") return "true";
    if (current === "true") return "false";
    return "";
}

function gsTriStateApply(display, state) {
    display.indeterminate = state === "";
    display.checked = state === "true";
}

/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Show row identifier as disabled/enabled to reflect conformance being in effect.
   Updates row immediately when its checkbox changes. */
function gsConformanceTableInit(tableId) {
    var table = document.getElementById(tableId);
    if (!table || table.dataset.gsConformanceBound) return; // avoid a second listener on re-init
    table.dataset.gsConformanceBound = "true";
    table.addEventListener("change", function (e) {
        var cell = e.target.closest("[data-in-effect-unset]");
        if (!cell) return;
        gsConformanceRowApply(cell);
    });
}

/* The checkbox state: "true", "false", or "" when unset. */
function gsConformanceState(cell) {
    return cell.querySelector("input[type=hidden]").value;
}

function gsConformanceRowApply(cell) {
    var id = cell.closest("tr").querySelector(".gs-conformance-id");
    if (!id) return;
    var state = gsConformanceState(cell) || "unset";
    id.classList.toggle("gs-conformance-disabled", cell.getAttribute("data-in-effect-" + state) !== "true");
}

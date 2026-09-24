/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Show row identifier as disabled/enabled to reflect conformance being in effect.
   Updates row immediately, then ask server to recompute other rows (across tables if needed). */
function gsConformanceTableInit(tableId, callbackUrl) {
    var table = document.getElementById(tableId);
    if (!table || table.dataset.gsConformanceBound) return; // avoid a second listener on re-init
    table.dataset.gsConformanceBound = "true";
    table.addEventListener("change", function (e) {
        var cell = e.target.closest("[data-conformance-key]");
        if (!cell) return;
        gsConformanceRowApply(cell);
        var states = [];
        document.querySelectorAll("[data-conformance-key]").forEach(function (c) {
            states.push({ name: "s", value: c.getAttribute("data-conformance-key") + "=" + gsConformanceState(c) });
        });
        Wicket.Ajax.post({ u: callbackUrl, ep: states });
    });
}

/* Called back by the server with the outcome of each checkbox state, by row key. */
function gsConformanceTableUpdate(rows) {
    document.querySelectorAll("[data-conformance-key]").forEach(function (cell) {
        var row = rows[cell.getAttribute("data-conformance-key")];
        if (!row) return;
        cell.setAttribute("data-in-effect-unset", row.unset);
        cell.setAttribute("data-in-effect-true", row["true"]);
        cell.setAttribute("data-in-effect-false", row["false"]);
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

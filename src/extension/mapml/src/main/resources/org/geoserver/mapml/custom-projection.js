
addEventListener("load", () => {
    let customProjectionDefinition = document.getElementById("customProjection").value;
    let map = document.querySelector("mapml-viewer");
    map.projection = map.defineCustomProjection(customProjectionDefinition);
});

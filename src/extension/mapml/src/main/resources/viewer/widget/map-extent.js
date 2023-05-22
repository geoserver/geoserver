/*! @maps4html/web-map-custom-element 28-04-2023 */
/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * Copyright (c) 2023 Canada Centrre for Mapping and Earth Observation, Natural
 * Resources Canada
 * Copyright © 2023 World Wide Web Consortium, (Massachusetts Institute of Technology, 
 * European Research Consortium for Informatics and Mathematics, Keio    
 * University, Beihang). All Rights Reserved. This work is distributed under the 
 * W3C® Software License [1] in the hope that it will be useful, but WITHOUT ANY 
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR 
 * A PARTICULAR PURPOSE.
 * [1] http://www.w3.org/Consortium/Legal/copyright-software
 * 
 */
class MapExtent extends HTMLElement{static get observedAttributes(){return["units","checked","label","opacity"]}get units(){return this.getAttribute("units")}set units(t){["OSMTILE","CBMTILE","WGS84","APSTILE"].includes(t)&&this.setAttribute("units",t)}get checked(){return this.hasAttribute("checked")}set checked(t){t?this.setAttribute("checked",""):this.removeAttribute("checked")}get label(){return this.hasAttribute("label")?this.getAttribute("label"):""}set label(t){t&&this.setAttribute("label",t)}get opacity(){return this._opacity}set opacity(t){1<+t||+t<0||this.setAttribute("opacity",t)}attributeChangedCallback(t,e,s){t}constructor(){super()}connectedCallback(){this.querySelector("map-link[rel=query], map-link[rel=features]")&&!this.shadowRoot&&this.attachShadow({mode:"open"});let e="LAYER-"===this.parentNode.nodeName.toUpperCase()?this.parentNode:this.parentNode.host;e._layer?this._layer=e._layer:e.parentNode.addEventListener("createmap",t=>{this._layer=e._layer})}disconnectedCallback(){}}export{MapExtent};
//# sourceMappingURL=map-extent.js.map
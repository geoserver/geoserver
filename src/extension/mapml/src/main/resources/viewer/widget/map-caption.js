/*! @maps4html/web-map-custom-element 06-11-2023 */
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
class MapCaption extends HTMLElement{constructor(){super()}connectedCallback(){if("MAPML-VIEWER"===this.parentElement.nodeName||"MAP"===this.parentElement.nodeName){let t=this.parentElement.querySelector("map-caption").textContent;var e;this.observer=new MutationObserver(()=>{this.parentElement.querySelector("map-caption").textContent!==t&&this.parentElement.setAttribute("aria-label",this.parentElement.querySelector("map-caption").textContent)}),this.observer.observe(this,{characterData:!0,subtree:!0,attributes:!0,childList:!0}),this.parentElement.hasAttribute("aria-label")||(e=this.textContent,this.parentElement.setAttribute("aria-label",e))}}disconnectedCallback(){this.observer.disconnect()}}export{MapCaption};
//# sourceMappingURL=map-caption.js.map
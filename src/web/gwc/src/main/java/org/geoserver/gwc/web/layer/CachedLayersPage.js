
"use strict";

const CachedLayersPage_SetOnChange = () => {
    $('.tile-layers-page-menu-select').on('change', event => {
        const select = event.target;
        window.open(select.options[select.selectedIndex].value);
        select.selectedIndex = 0;
    });
};
$(CachedLayersPage_SetOnChange);

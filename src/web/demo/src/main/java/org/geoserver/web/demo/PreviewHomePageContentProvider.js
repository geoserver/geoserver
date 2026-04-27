"use strict";

const PreviewHomePageContentProvider_SetOnChange = () => {
    $('.preview-home-page-menu-select').on('change', event => {
        const select = event.target;
        window.open(select.options[select.selectedIndex].value);
        select.selectedIndex = 0;
    });
};
$(PreviewHomePageContentProvider_SetOnChange);

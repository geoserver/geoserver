
"use strict";

const MapPreviewPage_SetOnChange = () => {
    $('.map-preview-page-menu-select').on('change', event => {
        const select = event.target;
        const format = select.options[select.selectedIndex].value;
        let url;
        if (select.options[select.selectedIndex].parentNode.label === 'WMS') {
            url = select.getAttribute('wmsLink') + '&format=' + format;
        } else {
            url = select.getAttribute('wfsLink') + '&outputFormat=' + format;
            const maxFeatures = document.getElementById('maxFeatures').value;
            if (parseInt(maxFeatures) > 0) {
                url += '&maxFeatures=' + maxFeatures;
            }
        }
        window.open(url);
        select.selectedIndex = 0;
    });
};
$(MapPreviewPage_SetOnChange);

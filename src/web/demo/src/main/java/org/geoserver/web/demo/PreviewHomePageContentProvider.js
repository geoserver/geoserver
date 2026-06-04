"use strict";

const applyItemDisplay = item => {
    const isHidden =
        item.getAttribute("data-filter-hidden") === "1" ||
        item.getAttribute("data-overflow-hidden") === "1";
    item.style.display = isHidden ? "none" : "";
};

const clearOverflowHiddenItems = section => {
    section.querySelectorAll('[data-overflow-hidden="1"]').forEach(item => {
        item.removeAttribute("data-overflow-hidden");
        applyItemDisplay(item);
    });
};

const updateOverflowToggle = section => {
    clearOverflowHiddenItems(section);

    const content = section.querySelector(".preview-section-content");
    const list = section.querySelector(".preview-link-items");
    const toggle = section.querySelector(".preview-more-toggle");
    if (!content || !list || !toggle) return;

    const isExpanded = section.classList.contains("is-expanded");
    list.style.maxWidth = "";
    const hasOverflow = list.scrollWidth > list.clientWidth + 1;
    toggle.style.display = hasOverflow || isExpanded ? "inline-flex" : "none";
    const icon = toggle.querySelector(".gs-icon");
    if (icon) {
        icon.classList.remove("gs-icon-bullet-arrow-right", "gs-icon-bullet-arrow-down");
        icon.classList.add(
            isExpanded ? "gs-icon-bullet-arrow-down" : "gs-icon-bullet-arrow-right"
        );
    }
    toggle.setAttribute("aria-expanded", isExpanded ? "true" : "false");
    const expandLabel =
        toggle.getAttribute("data-expand-label") || "Show more formats";
    const collapseLabel =
        toggle.getAttribute("data-collapse-label") || "Show fewer formats";
    toggle.setAttribute("aria-label", isExpanded ? collapseLabel : expandLabel);
    toggle.setAttribute("title", isExpanded ? collapseLabel : expandLabel);

    if (!hasOverflow || isExpanded) return;

    list.style.maxWidth = `${content.clientWidth}px`;
    const availableWidth = content.clientWidth;

    const limitRight = list.getBoundingClientRect().left + availableWidth;
    let hideFromHere = false;
    list.querySelectorAll("[data-filter-label]").forEach(item => {
        if (item.getAttribute("data-filter-hidden") === "1") return;
        if (item.getAttribute("data-catalog-link")) return;
        const rect = item.getBoundingClientRect();
        if (!hideFromHere && rect.right > limitRight + 0.5) {
            hideFromHere = true;
        }
        if (hideFromHere) {
            item.setAttribute("data-overflow-hidden", "1");
            applyItemDisplay(item);
        }
    });
};

const updateAllOverflowToggles = root => {
    root.querySelectorAll(".preview-section").forEach(updateOverflowToggle);
};

const PreviewHomePageContentProvider_SetOnChange = () => {
    $(document).on("click", ".preview-more-toggle", event => {
        const section = event.currentTarget.closest(".preview-section");
        if (!section) return;
        section.classList.toggle("is-expanded");
        updateOverflowToggle(section);
    });

    const root = document.querySelector(".gs-panel-PreviewHomePageContentProvider_PreviewPanel");
    if (!root) return;

    const filterInput = root.querySelector(".preview-filter-input");
    if (filterInput) {
        filterInput.addEventListener("input", () => {
            const term = filterInput.value.trim().toLowerCase();
            root.querySelectorAll(".preview-section").forEach(section => {
                const sectionTitle =
                    (section.getAttribute("data-section-title") || "").toLowerCase();
                const sectionTitleMatch =
                    term &&
                    (sectionTitle.startsWith(term) ||
                        sectionTitle
                            .split(/\s+/)
                            .filter(Boolean)
                            .some(word => word.startsWith(term)));
                let visibleLinks = 0;
                section.querySelectorAll("[data-filter-label]").forEach(linkItem => {
                    const rawLabel = linkItem.getAttribute("data-filter-label");
                    const label =
                        (rawLabel != null ? rawLabel : linkItem.textContent || "").trim().toLowerCase();
                    const isVisible =
                        !term || label.includes(term) || sectionTitleMatch;
                    if (isVisible) {
                        linkItem.removeAttribute("data-filter-hidden");
                    } else {
                        linkItem.setAttribute("data-filter-hidden", "1");
                    }
                    applyItemDisplay(linkItem);
                    if (isVisible) visibleLinks++;
                });
                const sectionVisible = !term || sectionTitleMatch || visibleLinks > 0;
                section.style.display = sectionVisible ? "" : "none";
                if (!term) section.classList.remove("is-expanded");
                updateOverflowToggle(section);
            });
        });
    }

    updateAllOverflowToggles(root);
    window.addEventListener("resize", () => updateAllOverflowToggles(root));
};
$(PreviewHomePageContentProvider_SetOnChange);

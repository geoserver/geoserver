# Markdown Syntax

This page contains syntax rules, tips, and tricks for writing GeoServer documentation in Markdown.

## Basic markup

The simplest Markdown elements are:

| **Format** | **Syntax** | **Output** |
|------------|------------|------------|
| Italics    | `*italics*` | *italics* |
| Bold       | `**bold**` | **bold** |
| Monospace  | `` `monospace` `` | `monospace` |

## Lists

Bulleted lists:

- An item
- Another item
- Yet another item

Source code:

```md
- An item
- Another item
- Yet another item
```

Numbered lists:

1. First item
2. Second item
3. Third item

Source code:

```md
1. First item
2. Second item
3. Third item
```

### Nested bullets and outdenting

- Top level
    - Nested level

To return to top level, use 0 indentation again. For example:

- Top level
    - Nested
- Back to top level

## Notes and warnings (admonitions)

GeoServer documentation uses the `admonition` extension in Markdown for notes and warnings.

### Important user guidance

- `!!! note`, `!!! warning`, etc. is valid at top level, and in list nesting when indented to the same depth as surrounding list content.
- For top-level admonitions, use 0-3 spaces before `!!!` and indent block content by 4 spaces.
- For nested admonitions in lists, align `!!!` with the list item block content (e.g. 4 / 8 / 12 spaces depending on nesting).

This means `!!!` can be at 12 spaces and still render when it is nested in a list item at that depth.

Example:

```md
!!! note
    Do not wait for a release to fall out of support before upgrading.
```

This produces a note box.

If you need a note-like callout inside a list item, use inline emphasis instead:

```md
- Remember:
    - **Note:** Do not rely on this in production.
```

## List-packed table

Use a Markdown table instead of rst list-table:

| Shapes | Description |
|--------|-------------|
| Square | Four sides of equal length, 90 degree angles |
| Rectangle | Four sides, 90 degree angles |

## Page labels and anchors

In Markdown, you can use heading-based anchors or explicit named anchors (with HTML) if needed.

## Linking

Do not use "here" as link text. Instead:

- Bad: [here](#linking)
- Good: [Linking](#linking)

External link:

```md
[Text of the link](http://example.com)
```

## Sections

Use `#`, `##`, `###`, etc.: 

```md
# Main section
## Subsection
### Sub-subsection
```

## Notes and warnings (admonitions)

GeoServer documentation uses the `admonition` extension in Markdown for notes and warnings.

### Important user guidance

- `!!! note`, `!!! warning`, etc. is valid at top level, and in list nesting when indented to the same depth as surrounding list content.
- For top-level admonitions, use 0-3 spaces before `!!!` and indent block content by 4 spaces.
- For nested admonitions in lists, align `!!!` with the list item block content (e.g. 4 / 8 / 12 spaces depending on nesting).

Example:

```md
!!! note
    Do not wait for a release to fall out of support before upgrading.
```

This produces a note box.

If you need a note-like callout inside a list item, use inline emphasis instead:

```md
- Remember:
    - **Note:** Do not rely on this in production.
```

## Images

```md
![](pagelogo.png)
*The GeoServer logo as shown on the homepage.*
```

## External files

```md
[An external file](readme.txt)
```

## Code blocks and inline code

Inline code: `` `myCommand` ``

Fenced block:

```md
```bash
command args
```
```

## GUI element references

Use bold text:

- **Main Menu**
- **Start Menu -> Programs -> GeoServer**

## Show Source

All pages have a "Show Source" link in the right-hand table of contents.

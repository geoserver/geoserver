# Spell Checking

This directory contains the [cspell](https://cspell.org/) configuration for the GeoServer documentation.

## Setup

Install cspell globally (requires Node.js >= 22.18):

```bash
npm install -g cspell
```

## Usage

From the repository root:

```bash
cd doc/spelling
cspell
```

### Useful variations

```bash
# Show only the unknown words (no file/line info)
cspell --words-only

# Frequency-sorted list of unknowns (find dictionary candidates)
cspell --words-only --no-progress | sort | uniq -c | sort -rn | head -50

# Show only likely typos (where cspell suggests a fix)
cspell --no-progress | grep " fix:"
```

## Custom dictionary

Domain-specific terms are maintained in `../en/spelling/cspell-geoserver.txt`.

To add a word to the dictionary, append it to that file (one word per line).

## VS Code integration

The [Code Spell Checker](https://marketplace.visualstudio.com/items?itemName=streetsidesoftware.code-spell-checker) extension uses the same `cspell.json` config. Add this to your global or workspace settings:

```json
"cSpell.import": ["<absolute-path-to-repo>/doc/spelling/cspell.json"]
```

Words added via the extension's "Add to dictionary" quick-fix will be written to `cspell-geoserver.txt`.

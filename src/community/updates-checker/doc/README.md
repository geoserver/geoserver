# reStructuredText copy of the user guide page

`main` and `3.0.x` write the user guide in Markdown (mkdocs); `2.28.x` and `2.27.x` still use reStructuredText
(Sphinx). `index.rst` is the 2.x version of
[doc/en/user/community/updates-checker/index.md](../../../../doc/en/user/community/updates-checker/index.md).

**Change both in the same commit.** Then a backport to 2.x copies this file instead of converting the Markdown again.
`../backport-checks.sh` does the copy, together with the screenshots from the Markdown folder.

The two differ only where 2.x differs:

- The menu is **About & Status** only, with the `menu-gs2.png` screenshot.
- The nightly build link uses `|version|.x` instead of `main`.
- The Docker example uses `|release|` and `|version|.x` under `.. only::`, instead of `{{ release }}`.
- Links to other pages use `:ref:` targets (`application_properties`), and the page defines its own
  (`community_updates_checker`, `community_updates_checker_configuring`, `community_updates_checker_suppressing`).

`pandoc -f markdown -t rst` gives a first draft of a new section; tables come out as grid tables, which this page
writes as `list-table`.

The two `UPDATES_CHECKER_*` rows of the application properties table
(`doc/en/user/source/configuration/properties/index.rst`) and the toctree entry in
`doc/en/user/source/community/index.rst` are already on the 2.x branches; change them there by hand if a property is
added.

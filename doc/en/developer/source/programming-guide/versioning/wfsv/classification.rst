.. _versioning_classification:

Versioning WFS - Versioning approaches classification
=====================================================

It's quite clear that the requirements for a Versioning WFS sum up to building a full fledged version control system for features, although we may want to find ways to implement it step by step. So, it may be helpful to compare different approaches at tackling some of the implementation aspects of a version control system.

Difference handling
---------------------

Given two revisions of the same feature, how do you store the difference between them?

* **Copy**: we have two integral copies of the features
* **Diff**: only the differences are stored. There may be multiple ways to do so:
  * keeping a map of xpath/old value for every changed attribute
  * encoding the two revisions in GML, and do a text diff between them
  * keeping a map of xpath -> diff, where the difference of each attribute is encoded as a text difference (less tags around, and less context...), or as a binary difference (serialize the attribute, and then diff the byte arrays?)
* **Adaptive**, try diff, revert to copy if diff does not provide any space benefit.

Copy has the advantage of speed, getting a certain revision is just a matter of selection, whilst patch may use less space, especially if the feature is big, but requires both the selection of patches and their application to the integral data copy we have somewhere (how the integral data copy is managed is discussed in another part of this document).

For a comparison of different diff computation algorithms, see: "An Empirical Study of Delta Algorithms".

Versioning direction and integral copies
----------------------------------------

Do we keep backwards or forward changes?

* **Backward changes**: the last revision is an integral copy, going backwards means applying one or more reverse patches (this is the svn, cvs approach);
* **Forward changes**: we do start from an integral copy (eventually an empty data set), and register normal patches to store the differences.

Were do you have the states that differences refer to in order to build another revision of data?

* **last revision** of each trunk/branch/tag
* **in the root** of the versioning tree

Backward changes ensures fast access to the last revision, forward changes allows to handle multiple branches of data in a more natural way, because revisions can be thought as a tree starting with a single root (so a branch is just recorded as a set of forward changes from the common point where it branched off).

Backwards changes need last revision integral copies, whilst forward changes do need at least an integral copy in the root of the versioning tree.

Forward changes calls for fast to compute differences in order to get decent speed on the common case. ArcSDE uses forward changes coupled with difference by copy approach and integral copy in the root. Svn and csv use backward changes coupled with diffs and integral copy as the last revision.

Branches and tags management
----------------------------

Are these special beasts, or does the data structure seamlessly handle those too?

* no special management in SVN and ArcSDE, the same data structure rules them all;
* as an extra structure on top of the base with CVS (attic and file infos).

In the ArcSDE case, forward changes provide a natural fit to handle whatever trunk "copy" you may think of, branches and tags are really the same. The same goes for SVN.

If would be nice to have the same conceptual ease.

Space management
----------------

How do we manage space and speed up a bogged down versioning server?

* by removing unneeded tags, branches;
* by removing part of the history;
* by removing intermediate revisions between two valuable ones (which means removing the intermediate changes in copy approach, or merging diffs in the diff approach).

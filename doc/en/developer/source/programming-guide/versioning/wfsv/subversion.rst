.. _versioning_implementations_subversion:

Subversion
=========================

Subversion is a well known version control system.

The model visible to the user is well known, notable features in the context of feature versioning are:

* Revisions numbers are related to the whole tree.
* Trunk, tag, and branches are simply conventions, all copies are treated equals through a smart copy mechanism.
* The checkout allows for offline operation;
* Difference computation is efficient and can handle both text and binary files (based on the VDelta algorithm).

Client side view of the world
--------------------------------

It's interesting to note how commands do behave on the client side, and how they do parallel with the WFS.

There are a few similarities in all commands:

* If no revision number is provided, the last revision is assumed.
* Almost all commands support authentication provided through username/password, and these are stored in a special folder so that authentication is required just once (and it's not in the checkout, but in the user home).
* commands can work both on the checkout or directly against the server.

Let's have a look at some interesting commands that we may want to replicate in a versioned WFS:

* **svn cat** provides a way to list the content of an URL at the last revision, or at a specified revision. It can work only against a list of files, but it's relatively similar to GetFeature, and provides no information to build a checkout, just the plain content.
* **svn info** lists informations about a file or directory, specifically, revision the file is at, author, date and revision of the last change occurred before the current revision (you can specify a target revision in svn info, so the last change may be different depending on the chosen target revision). This is interesting:
   * a file is considered to be at the last revision, not at the revision it was last changed. This is consistent with the global revision number, which is really associated with the full tree, branches included.
   * each file has a kind of internal versioning that knows the file has been changed 20 times, but this is not shown to the user, only the global revision number shows up.
* **svn log** provides a history of changes for a specific URL and revision, citing for each change revision, author, date and commit log, eventually it may return a list of all files changed during the commit. The output format can be plain text (human readable) or XML (machine processable), and eventually a limit to the number of changes reported can be specified.
* **svn diff** returns the difference between two revisions of a given path, or even between two revisions at different paths. The difference is encoded as a standard unix diff.
* **svn checkout** checks out the content from the repository.
  It builds a quite specific checkout format, containing informations about the file revisions, full server URL for each file (which identifies the branch the file comes from) and a "base copy" that allows to perform status and diffing operations without being connected to the server.
  The latter is most important because it allows for far more efficient and pleasant usage when the versioning server is not in your local network, or when connection is not available at all.
* **svn commit** commits local changes to the server. It's a command that requires a local checkout, so it's quite unlike the WFS Transaction that does not need it, but at the same time similar, since it sends diffs to the server which are akin to updates.
* **svn revert** reverts local changes, so again works against a local checkout.
  There's no such a thing as a server revert, going back adds to the revision history, unlike SDE or Oracle where a revert wipes out the reverted changes.
  In order to revert a committed change in svn, you have to perform a reverse merge, that is, svn merge -r m:n where m > n, and then commit the change, or perform a direct reverse merge against the server, svn merge -r m:n svn://svn.myserver.org/trunk/path....
* **svn merge** applies changesets from one url to another.
  The changeset to extract is specified by means of an URL and two revision numbers, and it's applied to a specified path. The version working against two remote URL is something compatible with WFS on a conceptual level, much like a combined GetFeature and Transaction operation.
* **svn import**, allows to check in a new subtree of files in the server. This is interesting from a WFS perspective if we think of it as a way to create a new versioned feature type, since Transaction already allows for insertions.

Versioning storage implementations
----------------------------------

The way subversion manages versioning on the server side is probably less known.

Subversion may use two different backends, the FSFS backend, and the Berkeley DB backend. What's most surprising is that not only the two backends are using different storage supports, but they don't behave in the same way either, versioning wise: one works computing forward changes, whilst the other does backward ones.

The informations here are a summary of the subversion design document and a mail discussion I had with subversion developers.

FSFS backend
............

The FSFS backend is a forward change type, that is, each revision of a file stores the delta required to go from a prevision revision to the current one.

To avoid obvious efficiency problem when the revision history of a file becomes long, a skip list approach is used in order to reduce the number of forward deltas that need to be applied, reducing the number of deltas to be applied from O( n ) to O(log n).

Some revisions do not store the delta from a base revision that allows us to perform jumps as big as 2n. Moreover, each time a file on a branch is modified for the first time a full copy of the file for that branch is generated, and it becomes the new base full copy for the file in the branch.

In the following discussion I provide some more detail. When I say file version I do mean the actual n-th version of the file, I do not refer a global version number.

I don't have the exact algorithm, but a way could be to compare each revision against the last power of 2 lower, so if you're committing then 18thversion of a file, you delta against the 16th, which contains the delta against the 8th, and then against the 4th, and finally the 2nd and the 1st (which is a full copy).

+--------------------------------+---+---+---+---+---+---+---+---+---+----+----+----+----+----+----+----+
| File version                   | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 | 12 | 13 | 14 | 15 | 16 |
+================================+===+===+===+===+===+===+===+===+===+====+====+====+====+====+====+====+
| Delta computed against version |   | 1 | 2 | 2 | 4 | 4 | 4 | 4 | 8 | 8  | 8  | 8  | 8  | 8  | 8  | 8  |
+--------------------------------+---+---+---+---+---+---+---+---+---+----+----+----+----+----+----+----+

The first drawback of this approach is that you have to sum all the differences occurred between the last power of 2 and the current revision, so the patch may become (very) big. One way to avoid that would be to insert full copies at selected points in the sequence so that the biggest patch does not store more than 64 or 128 changes.

The second drawback of this approach is that you have to know how many versions of a file are there in order to retrieve the diffs required to rebuild the n-th version of a file. This is a little harder to turn into a database query, but it could be solved by having a reference table that store the powers of two, and have a subquery select all power of two lower than the current file "version".
The BDB backend

The BDB backed stores backwards changes, so the last revision on trunk is always a full copy, whilst on a branch the revision is a full copy if the file has been modified on the branch, otherwise it's just a pointer to the trunk revision the branch originate from.

As with the FSFS backend, the BDB backed uses skip lists in order to rebuild faster old revisions, but since the reference copy is a moving target, also the delta need to be recomputed at each commit.

When you commit a new file, the 4th, 8th, 16th, ... level ancestors are re-deltified against the last revision. So, in the following example, if you request the 3rd version of a file you'll have to apply deltas contained in 3, 19, 23 (in reverse order) to the full copy in 24.

+------------------------+---+---+---+---+---+---+---+---+---+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+
| File version           | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 | 12 | 13 | 14 | 15 | 16 | 17 | 18 | 19 | 20 | 21 | 22 | 23 | 24 |
+========================+===+===+===+===+===+===+===+===+===+====+====+====+====+====+====+====+====+====+====+====+====+====+====+====+
| Delta against version  | 17| 18| 19| 20| 21| 22| 23| 24| 17| 18 | 19 | 20 | 21 | 22 | 23 | 24 | 21 | 22 | 23 | 24 | 22 | 23 | 24 |    |
+------------------------+---+---+---+---+---+---+---+---+---+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+

The positive side of this approach is that the last revision is usually quick to build, on the downside the insert process is complex as it requires recomputing deltas, and gathering a revision other than the last one of trunk goes through the same complex process as FSFS, because not all files on a branch may have been modified (and thus be full copies).

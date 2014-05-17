The last two posts were about testing recovery of the application state of an
akka-persistence_ based application from a `journal </2014/05/testing-recovery.html>`_ or a
`snapshot </2014_05_01_archive.html>`_. This post concludes the
series of posts on testing akka-persistence based application with some thoughts on testing
migration of journals.

Journal Migration
=================

The migration problem has already been addressed in the first `post </2014/04/akka-persistence-and-testing.html>`_
of this series. When the application evolves, the objects that are written to the
journal evolve and thus their serialized representation has to change, too. To ensure that
even if the evolved objects have a new serialized representation the deserialization
is still able to read and process the old representation, custom akka-serializers are applied.
So the idea is to maintain compatibility to existing journals and to restore the changed command-objects
from the old serialized representation. Alternatively one could write a migration-tool that
reads an existing journal and writes the content in the new serialization format. While this
post focuses on the first approach the testing principles can be applied to both.

Snapshot Migration
------------------

Before we get into details, one word about migration of snapshots. In principle
we have the same problems here as in case of journals. As the application evolves, the
state of processors evolve and so does their serialized representation, and one
has to make sure that the evolved application is still able to read existing snapshots.
Like for the journal akka-persistence uses akka-serialization_ when it comes to writing
the snapshot to the storage and that is why it makes sense to employ custom
serializers for maintaining compatibility between versions. However, there is an
important difference between the journal and snapshots. Snapshots can be considered a pure
performance optimization and are not important from a functional point of view for a
successful recovery of application state. That is why it might be a valid alternative
to do without custom serializers and backwards compatibility in case of snapshots.
Incompatible snapshots could simply be deleted in case of an upgrade. Of course this results
in a longer
recovery time for the first restart after an upgrade.

Because of this and because of the fact that maintaining backwards compatibility
for snapshots is a very similar challenge (for implementation and test) as in case of
journals, this post only considers migration of journals.

General Idea
------------

Once again the idea is to reuse as much of the existing tests as possible. Basically
we want to know if the test for recovery from a journal still works even when an
old journal is used. So instead of writing the journal and recover from it in a single test-run,
we rather save the journals produced by the tests when development has reached a state
compatibility has to be maintained to (e.g. right after a release) and make the
test read from this journal when testing recovery instead of reading from a journal produced in the same test.

Let's have a quick look at one of the recover-tests:

.. include:: ../../test/scala/de/blogspot/volkersyadb/akkapersistence/ItemApplicationRecoverSpec.scala
    :start-after: // RECOVER CREATE BEGIN
    :end-before: // RECOVER CREATE END
    :number-lines: 1
    :code: scala

In lines 1-3 the journal is produced and the relevant state is kept locally in ``created``.
For our migration-test we do not need to produce a journal as this has been saved before.
We rather just need
to initialize ``created`` with a value that corresponds to the saved journal.

In lines 4-7 the application is recovered. For our migration-test we need to pass in
the folder that contains the saved journal.

Saving old Journals
-------------------

So the first challenge is to write a tool that saves the journals produced by the
tests to a dedicated place. You may have noticed that the test-code above differs slightly
from the initial-version shown in the post about testing recovery. The ``withApplication``
used before for both creating the journal and testing recovery are replaced by ``startApplication`` (line 1)
and ``restartApplication`` (line 4) respectively. This allows us to inject different actions
in either case.

For our use-case to save the journal to a dedicated place we simply copy the journal
from the temporary folder to the dedicated place after the first application shutdown.
For this we create a trait ``WithItemApplicationWithSaveJournal`` extending the well known
``WithItemApplication`` and overwriting ``startApplication`` like this:

.. include:: ../../test/scala/de/blogspot/volkersyadb/akkapersistence/ItemApplicationFixture.scala
    :start-after: // WITH SAVE JOURNAL APPLICATION BEGIN
    :end-before: // WITH SAVE JOURNAL APPLICATION END
    :number-lines: 1
    :code: scala

When ``startApplication`` (line 2) returns, the application has not only been started, but the test-code (``block``)
has been executed and the application is shutdown. Before it simply returns the result (line 4),
it copies the temporary folder containing the journal (``persistDir``) to a dedicated place (line 3).
In addition to this it writes a text file (*expected.txt*) to this folder with a
string-representation of the result (line 12-13). We will see below how this helps
us to prepare the *relevant state* for verification.

The dedicated place is
``migrationDataDir`` (line 8) which computes to the folder *src/test/saved-journals/<id>/<test-class-name>/<test-name>/*.
The variables in this path are basically provided by a dummy-test that derives from
the original ``ItemApplicationRecoverSpec`` and mixes in the trait ``WithItemApplicationWithSaveJournal`` like follows:

.. include:: ../../test/scala/de/blogspot/volkersyadb/akkapersistence/ItemApplicationMigrationSpec.scala
    :start-after: // SAVE ITEM APPLICATION BEGIN
    :end-before: // SAVE ITEM APPLICATION END
    :number-lines: 1
    :code: scala

*<id>* becomes ``runId`` (line 2), *<test-class-name>* is determined by ``tag`` (line 3) and
*<test-name>* stands for the names of the individual tests in ``ItemApplicationRecoverSpec``. This dummy
test is run by a dedicated scala-application:

.. include:: ../../test/scala/de/blogspot/volkersyadb/akkapersistence/SaveJournalApp.scala
    :start-after: // SAVE JOURNAL APP BEGIN
    :end-before: // SAVE JOURNAL APP END
    :number-lines: 1
    :code: scala

This application simply instantiates the dummy-test with a certain ``runId`` and
runs it using a scala-test test-runner (line 4). It could for example be used after each release
of the application and actually also be included in a release-process. In that case
the ``runId`` could be the version of the application. In our case it produces three folders,
one for each test in ``ItemApplicationRecoverSpec``\, containing the journal
created by the respective test and the file *expected.txt* containing the
string-representation of the value returned by ``startApplication``. So for our three
tests these *expected.txt* files contain the created, updated or deleted item.

The files produced by ``SaveJournalApp`` should be added to the source-control
as resources required to run tests as we cannot easily reproduce those
files once the application evolves.

Running tests against old journals
----------------------------------

The next step is to execute the test of ``ItemApplicationRecoverSpec`` while making
sure that it reads the saved journals when restarting the application. Once again
we basically just need to provide another variation of the ``WithItemApplication``
trait for this:

.. include:: ../../test/scala/de/blogspot/volkersyadb/akkapersistence/ItemApplicationFixture.scala
    :start-after: // WITH MIGRATION APPLICATION BEGIN
    :end-before: // WITH MIGRATION APPLICATION END
    :number-lines: 1
    :code: scala

This overwrites ``startApplication`` and ``restartApplication``. In case of
``startApplication`` it simply returns prepared values without executing the
test-code (``block``) (line 4). These values have to be provided by the test-implementation
in form of the method
``expectedValueFor`` (line 1).

``restartApplication`` executes the ``block`` as usual, however instead
of providing the temporary folder ``persistDir`` it passes the folder with the
previously saved journal ``migrationDataDir`` (line 7).

Having this, a migration test basically
just needs to provide the expected values and the metadata required to find the
correct folder containing the previously saved journal:

.. include:: ../../test/scala/de/blogspot/volkersyadb/akkapersistence/ItemApplicationMigrationSpec.scala
    :start-after: // ITEM APPLICATION MIGRATION SPEC BEGIN
    :end-before: // ITEM APPLICATION MIGRATION SPEC END
    :number-lines: 1
    :code: scala

``runId`` (line 1) and ``tag`` (line 3) represent this metadata for the folder-name. As you
can see, the method ``expectedValueFor`` gets a scala-test
`TestData <http://doc.scalatest.org/2.1.5/index.html#org.scalatest.TestData>`_ instance (line 13) which
allows it to pick the right expected value. For simplicity reasons the implementation here ignores it
and relies on the order in which the tests are executed (line 14). The actual values (line 9-11)
can be derived from the *expected.txt* files that were generated along with the saved journals.

Test in Action
--------------

So just like in case of the snapshot-tests we are able to provide migration tests
while fully reusing the existing test-logic. Now lets see this test in action. After the
journals have been saved by running ``SaveJournalApp``, we modify the domain
model by adding the optional field ``rank: Option[Int] = None`` defaulting to ``None``
to ``Item`` and ``ItemDescription``. Thanks to the play-json_ `macro <http://www.playframework.com/documentation/2.2.x/ScalaJsonInception>`_
-based serialization that we used
for the custom serializers they basically adapt to this change automagically. Of course
we expect that the ``Item``\s recovered from an old journal have the field initialized to
``None`` and as this is also the default we do not even have to change the migration test
and can simply rerun it without any further changes and indeed it runs through just fine.

However if we break compatibility of
the macro-based serializer for example by adding a field ``rank: Int = 0``, the test fails accordingly::

  akka.persistence.RecoveryException:
    Recovery failure by journal (processor id = [/user/ItemActor])
  ...
  Caused by: play.api.libs.json.JsResultException:
    JsResultException(errors:List((/template/rank,
      List(ValidationError(error.path.missing,WrappedArray())))))

So for this kind of change we would have to provide a json-serializer that
initializes missing ``rank``-fields with 0.

Conclusion
----------

There is one thing to keep in mind when it comes to reusing a recovery test for
this kind of migration-test. Once the *original* recovery test evolves, it might
no longer be a suitable basis for the saved journals. Even just adding test-cases is
not a good idea as there is no *old* journal for these new test-cases. So the
migration-test and the original recovery-test might diverge when the development
continues and you have to find a suitable strategy to avoid as much redundancy as possible.

This post concludes the series of posts about akka-persistence and testing. It showed
that it is possible to re-use the same test for testing three different kind of
recovery scenarios: recovery from a journal, a snapshot and an *old* journal
of the previous release.

.. include:: akka-persistence-links.rst
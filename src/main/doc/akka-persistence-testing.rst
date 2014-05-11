In my first scala project I had the pleasure not only to learn scala, akka_ and play_
but also to work with eventsourced_. Eventsourced is an akka-based open-source library
for implementing the concepts of event sourcing. The basic idea is that instead of persisting
the current state of the application  - like you usually do when working with a relational database -,
you rather persist the individual messages that cause the state-changes. More details on
this concept can be found for example in the
`documentation for eventsourced <https://github.com/eligosource/eventsourced/blob/master/README.md>`_,
its successor akka-persistence_ or
an `article <http://martinfowler.com/eaaDev/EventSourcing.html>`_ by Martin Fowler. This blog-post
focuses on a specific (typical) usage scenario and in particular on testing the various
aspects of it that are important for real life projects.

Akka-Persistence and Testing
============================

The example used in this blog-post is supposed to represent a complex Akka-based application (or parts of it) keeping its state in
memory and using akka-persistence to persist this state. The tests are integration style tests accessing the application
from *outside*. To keep things simple the application is just modelled as a simple CRUD service for generic items.
The service basically delegates all operations
to a stateful actor that persists its state through akka-persistence by using it as a write-ahead log (for command-sourcing).
So the service stands for the entire interface of the application (respectively the part under test) and the actor for
the arbitrarily complex logic behind this interface. In this case the interface is based on regular method calls, but the
same ideas apply for a message based interface where actors are addressed directly.

When it comes to testing the integration with akka-persistence, I see 4 important aspects that should be covered:

#. Are custom serializers used for serializing ``PersistentMessages`` into the journal?
#. Does the application recover its state correctly after a restart when replaying the journal?
#. Does the application recover its state correctly after a restart when reading state from a snapshot?
#. Does the application recover its state correctly from an *old* journal of a previous release?

In this blog-post I just cover the first point and will address the other issues in following posts.

The Demo Application
--------------------

Let's first have a closer look at the application. As already stated the actor is
pretty simplistic:

.. include:: ../scala/de/blogspot/volkersyadb/akkapersistence/ItemActor.scala
    :start-after: // ITEM ACTOR BEGIN
    :end-before: // ITEM ACTOR END
    :number-lines: 1
    :code: scala

It extends ``Processor`` so akka-persistence can take care of persisting
messages sent to it. The accepted messages are

* ``CreateItem``
* ``UpdateItem``
* ``DeleteItem``
* ``GetItem``

(The snapshot related messages can be ignored for the moment.)
They return the created, updated, deleted or requested ``Item`` if the operation
was successful and a failure if it was not.
The first three are those that modify the actor's state and thus have to be logged
into the journal by akka-persistence. That is why they are wrapped
in a ``Persistent``, while the ``GetItem`` can be received plainly. The state
is basically a ``Map`` containing all ``Item``\s by their ids (``itemById`` in line 5).

The corresponding domain classes ``Item`` and ``ItemTemplate`` are likewise simplistic:

.. include:: ../scala/de/blogspot/volkersyadb/akkapersistence/Item.scala
    :start-after: // ITEM BEGIN
    :end-before: // ITEM END
    :number-lines: 1
    :code: scala

An ``Item`` contains an id and as representative for a more complex structure a
single field called ``description``. The ``ItemTemplate`` is used for creating
new ``Item``\s when the id is not yet known.

As described above the interface to our application is a service that wraps the
``ItemActor``:

.. include:: ../scala/de/blogspot/volkersyadb/akkapersistence/ItemService.scala
    :start-after: // ITEM SERVICE BEGIN
    :end-before: // ITEM SERVICE END
    :number-lines: 1
    :code: scala

For each of the four commands we have seen above it offers corresponding
methods. In each case the implementation creates the corresponding command, sends
it to the actor (with ask) and returns the result wrapped in a Future to the caller.
As we have already seen on the receiving side, those commands that modify the
application's state are wrapped in a ``Persistent``.

An ``ItemApplication`` rounds off the implementation and takes care of the initialization
and dependency injection as well as the proper configuration of the akka-system.

.. include:: ../scala/de/blogspot/volkersyadb/akkapersistence/ItemApplication.scala
    :start-after: // ITEM APPLICATION BEGIN
    :end-before: // ITEM APPLICATION END
    :number-lines: 1
    :code: scala

The interesting part here is the configuration of the serializers for the akka-system.
It is created in a hard-coded manner through the method ``akkaSerializerConfig``,
but what is it good for?

Customer Serializers
--------------------

When akka-persistence writes the persistent messages received by a ``Processor`` to a
journal it makes use of standard
akka-serialization_.
By default this uses java-serialization_. That means all messages are first
converted to an ``Array[Byte]`` through java-serialization and afterwards this array is
written to the journal. When the journal is replayed, the data is read from disk
as ``Array[Byte]`` and this is converted back to messages again through java-(de)serialization.
If you just go with the default you can quickly run into compatibility problems when
trying to read an existing journal with a new version of the application as plain
java-serialization by default does not ensure compatibility of the serialized data
to evolved classes. To avoid
this kind of problems you can either

* migrate the journal files to the new format before starting the new
  application. However when using java-serialization this can be a challenging
  task by itself, since one application must be able to read an old serialized
  form and write the new serialized form of instances of the *same* class (in
  different versions) or
* cautiously ensure that your messages stay
  downwards compatible to their serialized representation (e.g. by declaring
  ``serialVersionUID``
  explicitly and things like implementing ``readObject, writeObject`` methods)
  or
* you can make
  akka use custom serializers for your messages.

The latter gives you very good
control
over maintaining compatibility of the serialized representation of your message-instances
with the implementation of the corresponding classes. Based on my experience I can
recommend using custom serializers. The serializers should of course use a serial
representation that can easily be kept downwards compatible. JSON is a valid
alternative (even though certainly not one that performs best) as

* it is very flexible when it comes to migrating structures
* it might be used already in a program that offers a REST interface where
  the resources are represented in form of JSON documents

In addition to this it was very convenient to use it for this blog-post as
the `play-json <http://www.playframework.com/documentation/2.2.x/ScalaJson>`_-lib with
`macro-inception <http://www.playframework.com/documentation/2.2.x/ScalaJsonInception>`_
allows to write JSON (de-)serializers of case-classes in basically one line
of code. The corresponding akka-serializer is implemented in the class
``CommandSerializers`` for all messages that derive from ``Command`` (see line 3-4)
which are
all messages to be written into the journal in our case. akka allows to
configure custom serializers through the configuration object passed to the
akka-system. Typically the corresponding configuration is read from a
configuration file like ``application.conf`` or ``reference.conf``. As the
application needs to ensure backwards compatibility of the serialized
representation of the message in the journal I do not consider the custom
serializers for these classes as part of the configuration that could be
modified by an administrator of the application. That is why this
*configuration* is provided in a hard-coded manner in ``ItemApplication``
(line 12-26).

The test
--------

The tricky part about this configuration is, that akka will ignore it silently if it contains errors. Thus we we will not notice any
problems during runtime, if the configuration is wrong. In that case akka will silently use the default
java-serialization, a fact that might remain unnoticed until the first migration
problem appears. Even if the code-based configuration ensures that we
cannot introduce typos in class-names there are still other things that
might go wrong as we will see soon. That is why it is important to test that
the configured
serializers are actually used by akka.

For this we have the following test:

.. include:: ../../test/scala/de/blogspot/volkersyadb/akkapersistence/CommandSerializationSpec.scala
    :start-after: // COMMAND SERIALIZATION TEST BEGIN
    :end-before: // COMMAND SERIALIZATION TEST END
    :number-lines: 1
    :code: scala

At first this test retrieves the ``SerializationExtension`` from the
akka-system of the application. Then it uses it to serialize each of the
possible command-objects and afterwards de-serializes them with the
``CommandSerializer`` that is also expected to be used by the
``SerializationExtension``. If the de-serialized command equals the original
one
we can be sure that the serialization config was correct.

The first test-run discloses that there is indeed an error in the
configuration::

 [WARN] [...] [...] [akka.serialization.Serialization (akka://ItemApplication)]
  Multiple serializers found for class
  de.blogspot.volkersyadb.akkapersistence.ItemActor$CreateItem,
  choosing first: Vector(
   (interface java.io.Serializable,akka.serialization.JavaSerializer@3545fe3b),
   (interface de...ItemActor$Command,de...CommandSerializer@635eed0))

 Unexpected character ('�' (code 65533 / 0xfffd)): expected a valid value
  (number, String, array, object, 'true', 'false' or 'null')

As you can see ``bytes`` does not contain valid JSON and the parser complains
about an unexpected character. However the more interesting output is the
line above that. akka warns about the fact that there are multiple serializers
for the command-object and it chooses an arbitrary one which happens to be
the java-serializer in this case. If there are multiple alternative
serializers for a class the akka documentation states:

    You only need to specify the name of an interface or abstract base class of
    the messages. In case of ambiguity, i.e. the message implements several of
    the configured classes, the most specific configured class will be used,
    i.e. the one of which all other candidates are superclasses. If this
    condition cannot be met, because e.g. java.io.Serializable and
    MyOwnSerializable both apply and neither is a subtype of the other,
    a warning will be issued.

That means our interface (``Command``) must be more specific than
``Serializable``. We can easily achieve this if we make ``Command`` extend
``Serializable``. As soon as we have this the test runs through just fine.

Note that for this test to be relevant, it is important that it uses the same
configuration as is used in production and not a configuration that is
specifically made up for the test. In our case this is ensured by the
``ItemApplicationFixture`` that prepares and cleans the ``ItemApplication`` up
for each test. Let's have a look at the central method:

.. include:: ../../test/scala/de/blogspot/volkersyadb/akkapersistence/ItemApplicationFixture.scala
    :start-after: // WITH ITEM APPLICATION BEGIN
    :end-before: // WITH ITEM APPLICATION END
    :number-lines: 1
    :code: scala

As you can see it uses the *real* ``ItemApplication`` (line 12) that is also used for production.
It does provide test-specific extensions (``ItemApplicationTestExtensions``) and
config (line 2-6) to ``ItemApplication`` but just to ease writing the tests and
to ensure that
the journals and snapshots for the tests are written to a temporary folder
that is cleaned up after the test and that the java-leveldb implementation is
used instead of the native one which is perfectly fine for testing and
typically comes with less problems when running the tests in different
environments.

This concludes the first blog-post about akka-persistence and testing. I will
cover the other important topics about recovery, snapshots and migration in
following posts so stay tuned.

.. _eventsourced: https://github.com/eligosource/eventsourced
.. _akka-persistence: http://doc.akka.io/docs/akka/2.3.0/scala/persistence.html
.. _akka-serialization: http://doc.akka.io/docs/akka/2.3.0/scala/serialization.html
.. _akka: http://akka.io/
.. _play: http://playframework.org/
.. _java-serialization: http://docs.oracle.com/javase/7/docs/platform/serialization/spec/serialTOC.html
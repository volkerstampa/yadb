I have been developing Java with various frameworks like J2EE, spring, hibernate or others for a couple of years now,
but a few months ago I got the chance to switch to scala, akka and play. I really had to learn a lot in the
beginning and of course I am still gaining new insights into these tools and libraries every day.
To share some of the aha effects I experienced during the last months I am starting a series of blog posts
that might be helpful to others who are thinking about a similar switch or are even already in the middle of
such a change.

Using for-comprehensions to build Query-APIs
============================================

When you start working with scala, you will quickly be confronted with its for-comprehensions or for-expressions_.
If you loop for example over two collections, you can do this with a for-expression like this:

.. include:: ../../test/scala/de/blogspot/volkersyadb/forexpression/ForExpressionSpec.scala
    :start-after: // SIMPLE FOR BEGIN
    :end-before: // SIMPLE FOR END
    :number-lines: 1
    :code: scala

This computes the products of all numbers in ``a`` with all numbers in ``b`` resulting in a list
of 9 ``Int``\s. The scala compiler actually translates a for-expression into a cascaded set of calls to
``flatMap, map, withFilter``. So the example above actually becomes:

.. include:: ../../test/scala/de/blogspot/volkersyadb/forexpression/ForExpressionSpec.scala
    :start-after: // FLATMAP BEGIN
    :end-before: // FLATMAP END
    :number-lines: 1
    :code: scala

``withFilter`` comes into play as soon as you add conditions to the for-expression, so:

.. include:: ../../test/scala/de/blogspot/volkersyadb/forexpression/ForExpressionSpec.scala
    :start-after: // FOR WITH IF BEGIN
    :end-before: // FOR WITH IF END
    :number-lines: 1
    :code: scala

becomes:

.. include:: ../../test/scala/de/blogspot/volkersyadb/forexpression/ForExpressionSpec.scala
    :start-after: // WITHFILTER BEGIN
    :end-before: // WITHFILTER END
    :number-lines: 1
    :code: scala

The interesting thing here is that the compiler first translates the for-expression into the cascaded form and
checks afterwards, if the provided expressions actually support the required method calls, e.g. in our case
``as``' and ``bs``' common type (``List[Int]``) has to have all of the three methods:
``flatMap``, ``map`` and ``withFilter``. If you use your type only in a simple for-expression like this:

.. include:: ../../test/scala/de/blogspot/volkersyadb/forexpression/ForExpressionSpec.scala
    :start-after: // MAP ONLY CLASS BEGIN
    :end-before: // MAP ONLY CLASS END
    :number-lines: 1
    :code: scala

you do not need to implement ``flatMap`` or ``withFilter`` at all. You do not even have to be
generic at all. If your class ``Container`` only supports ``Int``\s
and the expression after the yield works with ``Int``\s that will compile as
well. So for the example above ``Container`` could be implemented as:

.. include:: ../../test/scala/de/blogspot/volkersyadb/forexpression/ForExpressionSpec.scala
    :start-after: // INT MAP BEGIN
    :end-before: // INT MAP END
    :number-lines: 1
    :code: scala

So far we basically simplified our custom class and made it just implement what is actually required.
In addition to that one can also change the signature of these methods in a way that may look surprising.
For example ``withFilter`` takes a function as argument that maps whatever is
filtered to ``Boolean``\s.
While the ``Boolean`` seems
to be a very natural fit for the ``if``, there is no requirement at all, that the expression after the
``if`` evaluates to a ``Boolean`` (i.e. that ``withFilter`` takes an argument of
type ``A => Boolean``). Let's check out a little example that basically exchanges the
meanings of ``map`` and ``withFilter``:

.. include:: ../../test/scala/de/blogspot/volkersyadb/forexpression/ForExpressionSpec.scala
    :start-after: // CONFUSE MAP BEGIN
    :end-before: // CONFUSE MAP END
    :number-lines: 1
    :code: scala

The for-expression translates to:

.. include:: ../../test/scala/de/blogspot/volkersyadb/forexpression/ForExpressionSpec.scala
    :start-after: // CONFUSE MAP TRANSLATED BEGIN
    :end-before: // CONFUSE MAP TRANSLATED END
    :number-lines: 1
    :code: scala

Even if it might look like, this does not return a ``Container`` with a list of ``Boolean``\s
(even though the expression after yield evaluates to a ``Boolean``). It rather first adds 2 to each element of the list
(yielding ``List(3,4,5)``), then multiplies each element with 3
(yielding ``List(9,12,15)``) and after
filtering only even elements remain (yielding ``List(12)``).

While this seems to be a pretty useless example, you can definitely utilize this in a more meaningful way. The
container could for example store the elements in a sorted set and answer filter-requests for ranges directly without
actually looping through the whole set:

.. include:: ../../test/scala/de/blogspot/volkersyadb/forexpression/ForExpressionSpec.scala
     :start-after: // RANGE FILTER BEGIN
     :end-before: // RANGE FILTER END
     :number-lines: 1
     :code: scala

The for-expression translates to:

.. include:: ../../test/scala/de/blogspot/volkersyadb/forexpression/ForExpressionSpec.scala
     :start-after: // RANGE FILTER TRANSLATED BEGIN
     :end-before: // RANGE FILTER TRANSLATED END
     :number-lines: 1
     :code: scala

As the function that is passed to ``map`` is basically ignored, any function
can be passed.
		    
In this example the ``Container`` keeps its elements in a ``SortedSet``. The
method ``withFilter``
takes a function that maps the container-elements (``Int``) to a ``RangeFilter``. Instead of looping
through all elements and passing them to the provided function ``filter`` we simply want to retrieve
the ``RangeFilter`` instance that is provided after the ``if`` in the for-comprehension. As the expressions
after the ``if``\s do not depend on ``a``, ``filter`` returns for all
integers the same ``RangeFilter``, so we can pick any integer, e.g. ``0`` (see
line 4). Having the RangeFilter-instance
we can get the from/to values to pass them to ``SortedSet.rangeImpl`` and compute the filtered result
without looping through the set.

With an implementation like this you can provide the user with an elegant and efficient way to query your container with
for-comprehensions.

Another use case for this kind of *surprising* implementation for ``withFilter`` and ``map``
is to use the for-comprehension to build a query-object that is afterwards applied to a dataset.
Slick_ uses
this approach to build SQL-queries in a type-safe manner. I will give a
simple example mirroring the code from the
previous example (with the same definitions of the ``RangeFilter`` class and
its descendants):

.. include:: ../../test/scala/de/blogspot/volkersyadb/forexpression/ForExpressionSpec.scala
     :start-after: // QUERY BUILDER BEGIN
     :end-before: // QUERY BUILDER END
     :number-lines: 1
     :code: scala

In this case the class that is used in the for-comprehension as *source* (``QueryBuilder``)
is not a container any more.
Having no elements to filter ``withFilter`` just collects the passed ``RangeFilter``\s. Output
of the for-comprehension is a ``QueryBuilder``\-instance that can be applied to arbitrary SortedSets to
perform the previously recorded filtering.

As you can see for-comprehensions are even more powerful as you may think in the beginning. They are not only useful
as generic query-language for all kinds of containers, but you have all options to
implement filtering (or mapping) in a more efficient way than simply looping over all container elements. In addition
to this you can even split building the query from executing it and use them as generic query-builder whose output
is applied to arbitrary datasets.

.. _Slick: http://slick.typesafe.com/
.. _for-expressions: http://docs.scala-lang.org/tutorials/tour/sequence-comprehensions.html

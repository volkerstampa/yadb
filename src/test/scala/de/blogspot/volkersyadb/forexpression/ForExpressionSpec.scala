package de.blogspot.volkersyadb.forexpression

import collection.SortedSet
import org.junit.runner.RunWith
import org.scalatest.WordSpec
import org.scalatest.junit.JUnitRunner
import org.scalatest.Matchers
import scala.collection.immutable.TreeSet
import scala.reflect.runtime.universe._

@RunWith(classOf[JUnitRunner])
class ForExpressionSpec extends WordSpec with Matchers {

  "for-expression" must {
    "be same as (flat)map" in {
      // SIMPLE FOR BEGIN
      val as = List(1,2,3)
      val bs = List(4,5,6)
      val cs: List[Int] = for {
        a <- as
        b <- bs
      } yield a*b
      // SIMPLE FOR END

      // FLATMAP BEGIN
      val ds: List[Int] = as.flatMap(a => bs.map(b => a*b))

      ds should be (cs)
      // FLATMAP END
    }
  }
  "for-expression with if" must {
    "be same as (flat)map with filter" in {
      // FOR WITH IF BEGIN
      val as = List(1,2,3)
      val bs = List(4,5,6)
      val cs: List[Int] = for {
        a <- as
        if a % 2 == 1
        b <- bs
        if b % 2 == 0
      } yield a*b
      // FOR WITH IF END

      // WITHFILTER BEGIN
      val ds: List[Int] = as.
        withFilter(a => a % 2 == 1).
        flatMap(a => bs.
          withFilter(b => b % 2 == 0).
          map(b => a*b))

      ds should be (cs)
      // WITHFILTER END

    }
  }
  "simple for-expressions" must {
    "compile even if flatMap, withFilter are not defined" in {
      // MAP ONLY CLASS BEGIN
      case class Container[A](a: A) {
        def map[B](f: A => B): Container[B] = Container(f(a))
      }

      val as = Container(1)
      val b = for(a <- as) yield a+1
      // MAP ONLY CLASS END

      b should be (Container(2))
    }
  }
  "simple for-expressions" must {
    "compile even if map is not generic" in {
      // INT MAP BEGIN
      case class Container(a: Int) {
        def map(f: Int => Int): Container = Container(f(a))
      }
      // INT MAP END
      val mapped = for(t <- Container(1)) yield t+1

      mapped should be (Container(2))
    }
  }
  "withFilter and map" must {
    "work even if used confusingly" in {
      // CONFUSE MAP BEGIN
      case class Container(list: List[Int]) {
        def map(filter: Int => Boolean): Container =
          Container(list.withFilter(filter).map(identity))
        def withFilter(f: Int => Int): Container = Container(list.map(f))
      }

      val as = Container(List(1,2,3))
      val bs = for {
        a <- as
        if a+2
        if a*3
      } yield a % 2 == 0
      // CONFUSE MAP END

      // CONFUSE MAP TRANSLATED BEGIN
      val cs = as.
        withFilter(a => a + 2).
        withFilter(a => a * 3).
        map(a => a % 2 == 0)
      // CONFUSE MAP TRANSLATED END

      // CONFUSE MAP RESULT BEGIN
      bs should be (Container(List(12)))
      cs should be (Container(List(12)))
      // CONFUSE MAP RESULT END
    }
  }
  "withFilter with specific filter-objects" must {
    "filter without looping through the list" in {
      // RANGE FILTER BEGIN
      class Container(sortedElements: SortedSet[Int]) {
        def this(elements: Int*) = this(TreeSet(elements: _*))
        def withFilter(filter: Int => RangeFilter): Container = {
          val rangeFilter = filter(0)
          new Container(sortedElements.rangeImpl(rangeFilter.from, rangeFilter.to))
        }
        def map(id: Int => Int): Container = this
        def toList: List[Int] = sortedElements.toList
      }

      class RangeFilter(val from: Option[Int], val to: Option[Int])
      case class Within(min: Int, max: Int) extends RangeFilter(Some(min), Some(max))
      case class LessOrEqual(max: Int) extends RangeFilter(None, Some(max))
      case class GreaterOrEqual(min: Int) extends RangeFilter(Some(min), None)

      val as = new Container(6, 10, 3, 5, 15)
      val bs = for {
        a <- as
        if Within(5, 10)
        if LessOrEqual(9)
      } yield a
      // RANGE FILTER END

      // RANGE FILTER TRANSLATED BEGIN
      val cs = as.
        withFilter(_ => Within(5,10)).
        withFilter(_ => LessOrEqual(9)).
        map(identity)
      // RANGE FILTER TRANSLATED END

      bs.toList should be (List(5,6))
      cs.toList should be (List(5,6))
    }
  }
  "withFilter when used to build query-object" must {
    "perform a corresponding query" in {
      class RangeFilter(val from: Option[Int], val to: Option[Int])
      case class Within(min: Int, max: Int) extends RangeFilter(Some(min), Some(max))
      case class LessOrEqual(max: Int) extends RangeFilter(None, Some(max))
      case class GreaterOrEqual(min: Int) extends RangeFilter(Some(min), None)

      // QUERY BUILDER BEGIN
      class QueryBuilder(filters: List[RangeFilter]= Nil) {
        def withFilter(filter: Int => RangeFilter): QueryBuilder =
          new QueryBuilder(filters :+ filter(0))
        def map(id: Int => Int): QueryBuilder = this
        def apply(set: SortedSet[Int]): List[Int] = {
          filters.foldLeft(set) { (filteredSet, filter) =>
            filteredSet.rangeImpl(filter.from, filter.to)
          }.toList
        }
      }

      val query = for {
        t <- new QueryBuilder()
        if Within(5, 10)
        if LessOrEqual(9)
      } yield t

      val result = query(TreeSet(6, 10, 3, 5, 15))
      // QUERY BUILDER END

      result.toList should be (List(5,6))
    }
  }
}

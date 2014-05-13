package de.blogspot.volkersyadb.mapandset

import org.junit.runner.RunWith
import org.scalatest.{Matchers, WordSpec}
import org.scalatest.junit.JUnitRunner
import scala.collection.immutable.TreeSet

@RunWith(classOf[JUnitRunner])
class CompareMapSetSpec extends WordSpec with Matchers {
  case class Person(id: Long, firstName: String, lastName: String)
  
  val personOrder: Ordering[Person] = Ordering.by[Person, Long](_.id)
  
  "Map by id is the same as TreeSet sorted by id" in {
    val ids = 1 to 100
    val personMap = ids.map(id => id -> newPerson(id)).toMap
    val personSet = TreeSet(ids.map(newPerson(_)): _*)(personOrder)
    
//    personMap(1) should be (personSet.range(Person(1, "", ""), Person(1, "",
//      "")).head)
  }

  private def newPerson(id: Long) = Person(id, s"First$id", s"Last$id") 
}
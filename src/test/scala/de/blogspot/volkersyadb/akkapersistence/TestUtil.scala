package de.blogspot.volkersyadb.akkapersistence

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.Try
import scala.reflect._
import akka.util.Timeout

trait TestUtil {

  val timeoutDuration = 5.seconds
  implicit val timeout = Timeout(timeoutDuration)

  // TEST HELPER BEGIN
  def resultOf[A](future: Future[A]): A = Await.result(future, timeoutDuration)
  
  def successOf[A](future: Future[Try[A]]): A = successOf(resultOf(future))

  def successOf[A](result: Try[A]): A = result.get
  // TEST HELPER END

  def valueOf[A](future: Future[Option[A]]): A = valueOf(resultOf(future))

  def valueOf[A](result: Option[A]): A =
    result.getOrElse(throw new RuntimeException("No result"))

  def n[A : ClassTag] = classTag[A].runtimeClass.getSimpleName
}

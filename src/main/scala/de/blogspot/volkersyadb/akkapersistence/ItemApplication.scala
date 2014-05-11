package de.blogspot.volkersyadb.akkapersistence

import akka.actor.{Props, ActorSystem}
import com.typesafe.config.{ConfigFactory, Config}
import de.blogspot.volkersyadb.akkapersistence.ItemActor.Command
import akka.serialization.Serializer
import scala.reflect._
import scala.concurrent.duration._
import akka.util.Timeout


// ITEM APPLICATION BEGIN
class ItemApplication(overwriteConfig: Config = ConfigFactory.empty()) {

  val akkaSystemConfig: Config = overwriteConfig.withFallback(
    akkaSerializerConfig[Command, CommandSerializer])

  private val config = akkaSystemConfig.withFallback(ConfigFactory.load())
  val system = ActorSystem(classOf[ItemApplication].getSimpleName, config)
  protected val itemActor =
    system.actorOf(itemActorProps, classOf[ItemActor].getSimpleName)
  val itemService = new ActorService(itemActor)(Timeout(5.seconds))

  protected def itemActorProps: Props = ItemActor.props()

  private def akkaSerializerConfig[M : ClassTag, S <: Serializer : ClassTag]
      : Config = {
    val messageClassName = classTag[M].runtimeClass.getName
    val serializerClassName = classTag[S].runtimeClass.getName
    ConfigFactory.parseString(s"""
        |akka.actor {
        |  serializers {
        |    "$messageClassName" = "$serializerClassName"
        |  }
        |  serialization-bindings {
        |    "$messageClassName" = "$messageClassName"
        |  }
        |}
        |""".stripMargin)
  }

  def shutdown(): Unit = {
    system.shutdown()
    system.awaitTermination()
  }
}
  // ITEM APPLICATION END

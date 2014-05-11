package de.blogspot.volkersyadb.akkapersistence

import org.scalatest.fixture
import akka.serialization.{Serializer, SerializationExtension}
import de.blogspot.volkersyadb.akkapersistence.ItemActor._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner


@RunWith(classOf[JUnitRunner])
class CommandSerializationSpec extends fixture.WordSpec with TestCommons
  with ItemApplicationFixture {

  s"akka ${n[Serializer]}.serialize" when {
    s"invoked with a ${n[Command]}-object" must {
      s"return a byte array that can be de-serialized with ${n[CommandSerializer]}".
      // COMMAND SERIALIZATION TEST BEGIN
          in { application =>
        val serializer = SerializationExtension(application.system)

        Seq(CreateItem(newItemTemplate()),
            UpdateItem(newItem()),
            DeleteItem(newId)).foreach { expected =>

          val bytes = successOf(serializer.serialize(expected))
          val actual = new CommandSerializer()
              .fromBinary(bytes, Some(expected.getClass))

          actual should be (expected)
        }
      }
      // COMMAND SERIALIZATION TEST END
    }
  }
}

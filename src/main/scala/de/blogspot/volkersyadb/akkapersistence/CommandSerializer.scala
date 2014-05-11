package de.blogspot.volkersyadb.akkapersistence

import com.google.common.base.Charsets
import akka.serialization.Serializer
import play.api.libs.json._
import de.blogspot.volkersyadb.akkapersistence.ItemActor._

class CommandSerializer extends Serializer {

  private final val Charset = Charsets.UTF_8
  
  private implicit val itemIdFormatter = Format[ItemId](
      JsPath.read[Long].map(ItemId.apply),
      new Writes[ItemId] { def writes(id: ItemId) = JsNumber(id.id) })

  private implicit val itemTemplateFormatter = Json.format[ItemTemplate]
  private implicit val itemFormatter = Json.format[Item]

  private val createItemFormatter = Json.format[CreateItem]
  private val updateItemFormatter = Json.format[UpdateItem]
  private val deleteItemFormatter = Json.format[DeleteItem]
  
  def formatterByClass(messageClass: Class[_]): Format[Command] = {
    (messageClass match {
      case c if classOf[CreateItem].isAssignableFrom(c) => createItemFormatter
      case c if classOf[UpdateItem].isAssignableFrom(c) => updateItemFormatter
      case c if classOf[DeleteItem].isAssignableFrom(c) => deleteItemFormatter
    }).asInstanceOf[Format[Command]]
  }

  override def fromBinary(bytes: Array[Byte], manifest: Option[Class[_]]) = {
    Json.parse(new String(bytes, Charset))
        .as[Command](formatterByClass(manifest.get))
  }

  override def includeManifest = true

  override def toBinary(command: AnyRef): Array[Byte] = {
    Json.toJson(command.asInstanceOf[Command])(formatterByClass(command.getClass))
        .toString().getBytes(Charset)
  }

  override def identifier = 647383
}

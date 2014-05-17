package de.blogspot.volkersyadb.akkapersistence

import org.scalatest.TestData
import scala.reflect.{ClassTag, classTag}

class ItemApplicationMigrationSpec_0_1
    extends ItemApplicationRecoverSpec with JournalMigrationRecoverSource
    with WithItemApplicationWithMigration[ItemApplicationRecoverSpec] {

  // ITEM APPLICATION MIGRATION SPEC BEGIN
  protected def runId = "0.1"

  protected def tag = classTag[ItemApplicationRecoverSpec]

  protected val Id = ItemId(1)
  protected def description(d: Long) = s"$d - description"

  protected val expectedValues: Iterator[Any] = Iterator(
    Item(Id, description(1)),
    Item(Id, description(3)),
    Item(Id, description(4)))

  override protected def expectedValueFor(test: TestData): Any =
    expectedValues.next()
  // ITEM APPLICATION MIGRATION SPEC END
}

// SAVE ITEM APPLICATION BEGIN
class SaveItemApplicationJournal
    (protected val runId: String)
    (implicit val tag: ClassTag[ItemApplicationRecoverSpec])
    extends ItemApplicationRecoverSpec with JournalMigrationRecoverSource
    with WithItemApplicationWithSaveJournal[ItemApplicationRecoverSpec]
// SAVE ITEM APPLICATION END

trait JournalMigrationRecoverSource {
    this: ItemApplicationRecoverSpec with MigrationUtil[ItemApplicationRecoverSpec] =>
  protected override def recoverSource = s"an $runId journal"
}

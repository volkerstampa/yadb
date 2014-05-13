package de.blogspot.volkersyadb.akkapersistence

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
// RECOVER FROM SNAPSHOT SPEC BEGIN
class ItemApplicationRecoverFromSnapshotSpec extends ItemApplicationRecoverSpec
    with WithItemApplicationWithSnapshot {
// RECOVER FROM SNAPSHOT SPEC END

  s"${n[ItemApplication]} restart and recovery from $recoverSource" when {
    s"performed after an ${n[Item]} has been created" should {
      s"allow to create new items" in { persistDir =>
        // RECOVER ID COUNTER BEGIN
        val existingItem = withApplication(persistDir) { application =>
          application.itemServiceTestExtension.createNewItem()
        }
        withApplication(persistDir) { application =>
          val service = application.itemServiceTestExtension

          val created = service.createNewItem()

          service.findItem(created.id) should be (Some(created))
          service.findItem(existingItem.id) should be (Some(existingItem))
        }
        // RECOVER ID COUNTER END
      }
    }
  }

  override def recoverSource = "snapshot"
}

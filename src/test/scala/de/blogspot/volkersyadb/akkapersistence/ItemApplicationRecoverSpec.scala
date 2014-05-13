package de.blogspot.volkersyadb.akkapersistence

import org.scalatest.fixture
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ItemApplicationRecoverSpec extends fixture.WordSpec with TestCommons
    with TempDirFixture with WithItemApplication {

  s"${n[ItemApplication]} restart and recovery from $recoverSource" when {
    s"performed after an ${n[Item]} has been created" should {
      s"recover this ${n[Item]}" in { persistDir =>
        // RECOVER CREATE BEGIN
        val created = withApplication(persistDir) { application =>
          application.itemServiceTestExtension.createNewItem()
        }
        withApplication(persistDir) { application =>
          application.itemServiceTestExtension.findItem(created.id) should be (
              Some(created))
        }
        // RECOVER CREATE END
      }
    }
    s"performed after an ${n[Item]} has been updated" should {
      s"recover the updated ${n[Item]}" in { persistDir =>
        // RECOVER UPDATE BEGIN
        val updated = withApplication(persistDir) { application =>
          val service = application.itemServiceTestExtension

          service.updateItem(service.createNewItem())
        }
        withApplication(persistDir) { application =>
          application.itemServiceTestExtension.findItem(updated.id) should be (
              Some(updated))
        }
        // RECOVER UPDATE END
      }
    }
    s"performed after an ${n[Item]} has been deleted" should {
      s"recover the ${n[Item]} as deleted" in { persistDir =>
        // RECOVER DELETE BEGIN
        val deleted = withApplication(persistDir) { application =>
          val service = application.itemServiceTestExtension

          service.deleteItem(service.createNewItem().id)
        }
        withApplication(persistDir) { application =>
          application.itemServiceTestExtension.findItem(deleted.id) should be (
              None)
        }
        // RECOVER DELETE END
      }
    }
  }

  def recoverSource = "journal"
}

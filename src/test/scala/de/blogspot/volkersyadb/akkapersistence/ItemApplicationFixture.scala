package de.blogspot.volkersyadb.akkapersistence

import org.scalatest.{Outcome, fixture}
import java.io.File
import scala.util.control.Exception.ultimately
import scala.collection.JavaConverters._
import com.typesafe.config.{Config, ConfigFactory}

trait ItemApplicationFixture extends WithItemApplication with WithTempDir {
    this: fixture .Suite =>

  type FixtureParam = TestApplication

  def withFixture(test: OneArgTest): Outcome = {

    withTempDir(withApplication(_)(
        application => withFixture(test.toNoArgTest(application))))
  }
}

trait WithItemApplication {
  final val JournalDirConfig = "akka.persistence.journal.leveldb.dir"
  final val NativeLevelDbConfig = "akka.persistence.journal.leveldb.native"
  final val SnapshotDirConfig = "akka.persistence.snapshot-store.local.dir"

  type TestApplication = ItemApplication with ItemApplicationTestExtensions
  // WITH ITEM APPLICATION BEGIN
  def withApplication[A](persistDir: File)(block: TestApplication => A) = {
    val tmpDirPersistenceConfig = ConfigFactory.parseMap(
      Map(JournalDirConfig -> new File(persistDir, "journal").getPath,
        NativeLevelDbConfig -> false.toString,
        SnapshotDirConfig -> new File(persistDir, "snapshots").getPath)
        .asJava)
    val application = newItemApplication(tmpDirPersistenceConfig)
    ultimately(application.shutdown())(block(application))
  }

  def newItemApplication(config: Config) =
    new ItemApplication(config) with ItemApplicationTestExtensions
  // WITH ITEM APPLICATION END
}

// WITH SNAPSHOT APPLICATION BEGIN
trait WithItemApplicationWithSnapshot extends WithItemApplication {
  override def newItemApplication(config: Config) =
    new ItemApplication(config) with ItemApplicationWithSnapshot
}
// WITH SNAPSHOT APPLICATION END

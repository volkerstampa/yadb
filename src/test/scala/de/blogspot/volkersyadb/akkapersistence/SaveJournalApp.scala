package de.blogspot.volkersyadb.akkapersistence

import org.scalatest.run

// SAVE JOURNAL APP BEGIN
object SaveJournalApp extends App {
  val runId = args.headOption.getOrElse("generic")

  run(new SaveItemApplicationJournal(runId))
}
// SAVE JOURNAL APP END

package ai.metarank.flow

import ai.metarank.FeatureMapping
import ai.metarank.fstore.Persistence
import ai.metarank.model.Event
import cats.effect.{IO, Ref}
import fs2.Stream

object MetarankFlow {
  case class ProcessResult(events: Long, updates: Long, tookMillis: Long)
  def process(store: Persistence, source: Stream[IO, Event], mapping: FeatureMapping): IO[ProcessResult] = {
    val ct    = ClickthroughImpressionFlow(store, mapping)
    val event = FeatureValueFlow(mapping, store)
    val sink  = FeatureValueSink(store)

    for {
      start         <- IO(System.currentTimeMillis())
      eventCounter  <- Ref.of[IO, Long](0)
      updateCounter <- Ref.of[IO, Long](0)
      _ <- source
        .evalTapChunk(_ => eventCounter.update(_ + 1))
        .through(ai.metarank.flow.PrintProgress.tap)
        .through(ct.process)
        .through(event.process)
        .evalTapChunk(values => updateCounter.update(_ + values.size))
        .through(sink.write)
        .compile
        .drain
      events  <- eventCounter.get
      updates <- updateCounter.get
      end     <- IO(System.currentTimeMillis())
    } yield {
      ProcessResult(events, updates, end - start)
    }
  }
}
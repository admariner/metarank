package ai.metarank.format

import ai.metarank.model.Event.{InteractionEvent, ItemEvent, ItemRelevancy, RankingEvent, UserEvent, eventCodec}
import ai.metarank.model.EventId
import ai.metarank.model.Field.{BooleanField, NumberField, StringField, StringListField}
import ai.metarank.model.Identifier.{ItemId, SessionId, UserId}
import ai.metarank.source.format.SnowplowFormat.{SnowplowJsonFormat, SnowplowTSVFormat}
import better.files.Resource
import cats.data.NonEmptyList
import io.findify.featury.model.Timestamp
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.nio.charset.StandardCharsets

class SnowplowFormatTest extends AnyFlatSpec with Matchers {
  it should "decode items metadata" in {
    val event = Resource.my.getAsString("/snowplow/item.tsv").getBytes(StandardCharsets.UTF_8)
    SnowplowTSVFormat.parse(event) shouldBe Right(
      Some(
        ItemEvent(
          id = EventId("81f46c34-a4bb-469c-8708-f8127cd67d27"),
          item = ItemId("item1"),
          timestamp = Timestamp.date(2020, 9, 6, 11, 24, 27),
          fields = List(
            StringField("title", "You favourite cat"),
            StringListField("color", List("white", "black")),
            BooleanField("is_cute", true)
          )
        )
      )
    )
  }

  it should "decode users metadata" in {
    val event = Resource.my.getAsString("/snowplow/user.tsv").getBytes(StandardCharsets.UTF_8)
    SnowplowTSVFormat.parse(event) shouldBe Right(
      Some(
        UserEvent(
          id = EventId("81f46c34-a4bb-469c-8708-f8127cd67d27"),
          user = UserId("user1"),
          timestamp = Timestamp.date(2020, 9, 6, 11, 24, 27),
          fields = List(
            NumberField("age", 33),
            StringField("gender", "m")
          )
        )
      )
    )
  }

  it should "decode ranking" in {
    val event = Resource.my.getAsString("/snowplow/ranking.tsv").getBytes(StandardCharsets.UTF_8)
    SnowplowTSVFormat.parse(event) shouldBe Right(
      Some(
        RankingEvent(
          id = EventId("81f46c34-a4bb-469c-8708-f8127cd67d27"),
          timestamp = Timestamp.date(2020, 9, 6, 11, 24, 27),
          user = Some(UserId("user1")),
          session = Some(SessionId("session1")),
          fields = List(
            StringField("query", "cat"),
            StringField("source", "search")
          ),
          items = NonEmptyList.of(
            ItemRelevancy(ItemId("item3"), 2.0),
            ItemRelevancy(ItemId("item1"), 1.0),
            ItemRelevancy(ItemId("item2"), 0.5)
          )
        )
      )
    )
  }

  val expectedInteraction = InteractionEvent(
    id = EventId("0f4c0036-04fb-4409-b2c6-7163a59f6b7d"),
    timestamp = Timestamp.date(2020, 9, 6, 11, 24, 27),
    user = Some(UserId("user1")),
    session = Some(SessionId("session1")),
    item = ItemId("item1"),
    ranking = Some(EventId("81f46c34-a4bb-469c-8708-f8127cd67d27")),
    `type` = "purchase",
    fields = List(
      NumberField("count", 1),
      StringField("shipping", "DHL")
    )
  )

  it should "decode interactions in TSV" in {
    val event = Resource.my.getAsString("/snowplow/interaction.tsv").getBytes(StandardCharsets.UTF_8)
    SnowplowTSVFormat.parse(event) shouldBe Right(Some(expectedInteraction))
  }

  it should "decode interactions in JSON" in {
    val event = Resource.my.getAsString("/snowplow/interaction.json").getBytes(StandardCharsets.UTF_8)
    SnowplowJsonFormat.parse(event) shouldBe Right(Some(expectedInteraction))
  }

  it should "decode events without unstruct field" in {
    val event = Resource.my.getAsString("/snowplow/empty.tsv").getBytes(StandardCharsets.UTF_8)
    SnowplowTSVFormat.parse(event) shouldBe Right(None)
  }

  it should "decode events with other schema" in {
    val event = Resource.my.getAsString("/snowplow/other.tsv").getBytes(StandardCharsets.UTF_8)
    SnowplowTSVFormat.parse(event) shouldBe Right(None)
  }
}
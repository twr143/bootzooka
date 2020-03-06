package model
import doobie.util.Read

/**
  * Created by Ilya Volynin on 03.12.2018 at 9:35.
  */

object TrackType extends Enumeration {
  type TrackType = Value
  val Rail = Value(1)
  val Monorail = Value(2)
  val Rubber = Value(3)

  def byId(id: Int): Option[TrackType] = TrackType.values.find(_.id == id)
  def byIdOrThrow(id: Int): TrackType = byId(id).getOrElse(throw new IllegalArgumentException(s"Unknown track type: $id"))

  implicit val rd = Read[Int].map(TrackType(_))
}

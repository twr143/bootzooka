package model
import model.TrackType.TrackType

/**
  * Created by Ilya Volynin on 24.09.2019 at 12:09.
  */
object Model2 {
  case class CityId(id: Int) extends AnyVal
  case class City(id: CityId, name: String, population: Int, area: Float, link: Option[String])

  case class MetroSystemId(id: Int) extends AnyVal
  case class MetroSystem(id: MetroSystemId, cityId: CityId, name: String, dailyRidership: Int)

  case class MetroLineId(id: Int) extends AnyVal
  case class MetroLine(id: MetroLineId, systemId: MetroSystemId, name: String, stationCount: Int, trackType: TrackType)

}

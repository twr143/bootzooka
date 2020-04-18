package template.service
import com.typesafe.scalalogging.StrictLogging
import template.infrastructure.Doobie.ConnectionIO

/**
 * Created by Ilya Volynin on 18.04.2020 at 10:21.
 */
class ServiceService(serviceModel: ServiceModel) extends StrictLogging {
  def deleteUser(login: String): ConnectionIO[Int] =
    for {
      userApiResult <- serviceModel.deleteByLogin(login) //api key will be deleted on cascade
    } yield  userApiResult

}

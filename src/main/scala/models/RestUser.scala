package models

import model.UserOutbound
import play.api.Play
import play.api.data.Form
import play.api.data.Forms._
import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile
import slick.driver.MySQLDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class RestUser(id: Option[Long],
                    sucursalId: Option[Long],
                    firstName: Option[String],
                    lastName: Option[String],
                    mobile: Option[String],
                    username: Option[String],
                    password: Option[String],
                    deleted: Boolean)

case class RestUserOutbound(id: Option[Long],
                            firstName: Option[String],
                            lastName: Option[String],
                            mobile: Option[String],
                            userName: Option[String])

case class RestUserFormData(firstName: String, lastName: String, mobile: String, email: String)

object RestUserForm {

  val form = Form(
    mapping(
      "firstName" -> nonEmptyText,
      "lastName" -> nonEmptyText,
      "mobile" -> nonEmptyText,
      "email" -> email
    )(RestUserFormData.apply)(RestUserFormData.unapply)
  )
}

class RestUserTableDef(tag: Tag) extends Table[RestUser](tag, "rest_user") {

  def id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)

  def sucursalId = column[Option[Long]]("sucursal_id")

  def firstName = column[Option[String]]("first_name")

  def lastName = column[Option[String]]("last_name")

  def mobile = column[Option[String]]("phone_number")

  def userName = column[Option[String]]("user_name")

  def password = column[Option[String]]("password")

  def deleted = column[Boolean]("deleted")

  override def * =
    (
      id,
      sucursalId,
      firstName,
      lastName,
      mobile,
      userName,
      password,
      deleted) <>(RestUser.tupled, RestUser.unapply)
}

object RestUsers {

  val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  val restUsers = TableQuery[RestUserTableDef]

  def add(restUser: RestUser): Future[Option[RestUserOutbound]] = {
    dbConfig.db.run((for {
      newId <- (restUsers returning restUsers.map(_.id)) += restUser
      a <- restUsers.filter(u => u.id === newId && u.deleted === false).map(
        u => (u.id, u.firstName, u.lastName, u.mobile, u.userName)).result.map(
          _.headOption.map {
            case (id, firstName, lastName, mobile, userName) =>
              RestUserOutbound(id, firstName, lastName, mobile, userName)
          }
        )
    } yield a).transactionally)
  }

  def delete(id: Long): Future[Int] = {
    dbConfig.db.run(restUsers.filter(_.id === id).map(u => u.deleted).update(true))
  }

  def listAll: Future[Seq[RestUserOutbound]] = {
    dbConfig.db.run(restUsers.filter(_.deleted === false).map(u =>
      (u.id, u.firstName, u.lastName, u.mobile, u.userName)).result.map(
        _.seq.map {
          case (id, firstName, lastName, mobile, userName) =>
            RestUserOutbound(id, firstName, lastName, mobile, userName)
        }
      )
    )
  }

  def retrieveRestUser(id: Long): Future[Option[RestUserOutbound]] = {
    dbConfig.db.run(restUsers.filter(u => u.id === id && u.deleted === false).map(
      u => (u.id, u.firstName, u.lastName, u.mobile, u.userName)).result.map(
        _.headOption.map {
          case (id, firstName, lastName, mobile, userName) =>
            RestUserOutbound(id, firstName, lastName, mobile, userName)
        }
      ))
  }

  def patchRestUser(restUser: RestUser): Future[Option[RestUserOutbound]] = {

    dbConfig.db.run((for {
      _ <- restUsers.filter(u =>
        u.id === restUser.id && u.deleted === false).map(u =>
        (u.firstName, u.lastName, u.mobile, u.password)).update(
          restUser.firstName, restUser.lastName, restUser.mobile, restUser.password
        )

      a <- restUsers.filter(u => u.id === restUser.id && u.deleted === false).map(
        u => (u.id, u.firstName, u.lastName, u.mobile, u.password)).result.map(
          _.headOption.map {
            case (id, firstName, lastName, mobile, password) =>
              RestUserOutbound(id, firstName, lastName, mobile, password)
          }
        )

    } yield a).transactionally)
  }

}
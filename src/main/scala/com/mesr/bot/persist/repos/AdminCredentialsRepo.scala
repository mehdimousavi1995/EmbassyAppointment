package com.mesr.bot.persist.repos

import java.time.LocalDateTime

import com.mesr.bot.persist.ActorPostgresDriver
import com.mesr.bot.persist.ActorPostgresDriver.api._
import com.mesr.bot.persist.model.AdminCredential
import com.mesr.bot.util.TimeUtils
import slick.dbio.Effect
import slick.lifted.ProvenShape
import slick.sql.FixedSqlAction

final class AdminCredentialsRepo(tag: Tag) extends Table[AdminCredential](tag, "admin_credentials") {

  def chatId: Rep[String] = column[String]("chat_id", O.PrimaryKey)

  def fullName: Rep[String] = column[String]("full_name")

  def createdAt: Rep[LocalDateTime] = column[LocalDateTime]("created_at")

  def deletedAt: Rep[Option[LocalDateTime]] = column[Option[LocalDateTime]]("deleted_at")

  def * : ProvenShape[AdminCredential] = (chatId, fullName, createdAt, deletedAt) <> (AdminCredential.tupled, AdminCredential.unapply)

}

object AdminCredentialsRepo  {
  val adminCredentials: TableQuery[AdminCredentialsRepo] = TableQuery[AdminCredentialsRepo]

  val activeSysAdmin: Query[AdminCredentialsRepo, AdminCredential, Seq] = adminCredentials.filter(_.deletedAt.isEmpty)

  def create(chatId: String, fullName: String): FixedSqlAction[Int, NoStream, Effect.Write] =
    adminCredentials += AdminCredential(chatId, fullName)

  def exists(chatId: String): FixedSqlAction[Boolean, ActorPostgresDriver.api.NoStream, Effect.Read] =
    activeSysAdmin.filter(s ⇒ s.chatId === chatId).exists.result


  def delete(chatId: String): FixedSqlAction[Int, NoStream, Effect.Write] =
    adminCredentials.filter(s ⇒ s.deletedAt.isEmpty && s.chatId === chatId).map(_.deletedAt).update(Some(TimeUtils.now))
}

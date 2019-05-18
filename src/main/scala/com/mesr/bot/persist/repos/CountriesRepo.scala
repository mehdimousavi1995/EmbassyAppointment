package com.mesr.bot.persist.repos

import com.mesr.bot.persist.ActorPostgresDriver.api._
import com.mesr.bot.persist.model.Country
import slick.dbio.Effect
import slick.lifted.ProvenShape
import slick.sql.{FixedSqlAction, FixedSqlStreamingAction}

final class CountriesRepo(tag: Tag) extends Table[Country](tag, "countries") {

  def countryName: Rep[String] = column[String]("country_name", O.PrimaryKey)

  def * : ProvenShape[Country] = countryName <> (Country.apply, Country.unapply)

}

object CountriesRepo  {
  val countries: TableQuery[CountriesRepo] = TableQuery[CountriesRepo]

  def create(country: String): FixedSqlAction[Int, NoStream, Effect.Write] =
    countries += Country(country)

  def delete(country: String): FixedSqlAction[Int, NoStream, Effect.Write] =
    countries.filter(_.countryName === country).delete

  def getAll: FixedSqlStreamingAction[Seq[Country], Country, Effect.Read] = countries.result
}

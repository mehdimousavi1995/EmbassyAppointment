package com.mesr.bot

import akka.actor.ActorSystem
import com.mesr.bot.persist.PostgresDBExtension
import com.mesr.bot.persist.repos.CountriesRepo
import com.mesr.bot.sdk.BotConfig

object Main extends App {
  val config = BotConfig.load()
  implicit val system: ActorSystem = ActorSystem("bot", config)
  val bot = new EmbassyTimeBot(config.getString("bot.token"))
  val postgresDBExtension = PostgresDBExtension(system).db
  implicit val ec = system.dispatcher

//  postgresDBExtension.run(AdminCredentialsRepo.create(85, "گلابی برعکس")).map {
//    result =>
//      system.log.error("result: {}", result)
//  }
  bot.run()
}

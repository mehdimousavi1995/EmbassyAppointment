package com.mesr.bot

import akka.actor.ActorSystem
import com.mesr.bot.persist.PostgresDBExtension
import com.mesr.bot.sdk.BotConfig

object Main extends App {
  val config = BotConfig.load()
  implicit val system: ActorSystem = ActorSystem("bot", config)
  val bot = new EmbassyTimeBot(config.getString("bot.token"))
  PostgresDBExtension(system).db
  bot.run()
}

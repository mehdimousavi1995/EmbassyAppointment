package com.mesr.bot

import akka.actor.ActorSystem
import com.mesr.bot.sdk.BotConfig

object Main extends App {
  val config = BotConfig.load()
  implicit val system: ActorSystem = ActorSystem("bot", config)
  val bot = new ClinicBot(config.getString("bot.token"))
  bot.run()
}

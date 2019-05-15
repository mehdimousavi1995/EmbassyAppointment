package com.mesr.bot.helpers

import akka.actor.ActorSystem
import com.bot4s.telegram.methods.SendMessage
import com.bot4s.telegram.models.{KeyboardButton, Message, ReplyKeyboardMarkup}
import com.mesr.bot.Strings._

trait ClinicHelper extends StateHelper with LevelHelper {
  def startWithMainMenu()(implicit system: ActorSystem, msg: Message): Unit = {
    request(SendMessage(msg.source, helloMessageStr, replyMarkup = Some(ReplyKeyboardMarkup(
      Seq(
        Seq(
          KeyboardButton(VisitTime),
          KeyboardButton(BehSimaServices),
          KeyboardButton(SmsBroadcastEnable),
          KeyboardButton(AboutClinicAnd),
//          KeyboardButton(ClinicCrew),
          KeyboardButton(Support),
          KeyboardButton(ClinicAddress)
        )
      )))))
  }


}

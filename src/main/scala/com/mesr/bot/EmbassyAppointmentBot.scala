package com.mesr.bot

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.bot4s.telegram.api.declarative.Commands
import com.bot4s.telegram.api.{RequestHandler, TelegramBot}
import com.bot4s.telegram.methods._
import com.bot4s.telegram.models.{KeyboardButton, ReplyKeyboardMarkup}
import com.mesr.bot.Strings._
import com.mesr.bot.helpers._
import com.mesr.bot.persist.PostgresDBExtension
import com.mesr.bot.sdk._
import com.mesr.bot.sdk.db.{RedisExtension, RedisExtensionImpl}
import slick.jdbc.PostgresProfile
import slogging.{LogLevel, LoggerConfig, PrintLoggerFactory}

import scala.concurrent.ExecutionContext

class EmbassyAppointmentBot(token: String)(implicit _system: ActorSystem)
  extends TelegramBot
    with BalePolling
    with Commands
    with MessageHandler
    with EmbassyAppointmentHelper
    with RedisKeys
    with UserInformation{

  override val system: ActorSystem = _system
  val ec: ExecutionContext = system.dispatcher
  implicit val redisExt: RedisExtensionImpl = RedisExtension(system)
  implicit val pdb: PostgresProfile.api.Database = PostgresDBExtension(system).db

  implicit val mat: ActorMaterializer = ActorMaterializer()

  LoggerConfig.factory = PrintLoggerFactory()
  LoggerConfig.level = LogLevel.TRACE

  override val client: RequestHandler = new BaleAkkaHttpClient(token, "tapi.bale.ai")

  import UserCurrentState._

  val AdminChatId = 2081746709


  onCommand("/start") { implicit msg =>
    clearUserCache(msg.source.toString)
    startWithMainMenu
  }

  onCommand("/help") { implicit msg =>
    clearUserCache(msg.source.toString)
    startWithMainMenu
  }

  onTextFilter(BookEmbassyAppoinement) { implicit msg =>
    redisExt.set(sKey(msg.source.toString), GettingName).map { _ =>
      request(SendMessage(msg.source, "لطفا نام و نام خانوادگی خود را وارد کنید.", replyMarkup = Some(ReplyKeyboardMarkup(
        Seq(
          Seq(
            KeyboardButton(BackToMainMenu)
          )
        )))))
    }
  }

  onTextFilter(MoreInfoAndContactAdmin) { implicit msg =>
    request(SendMessage(msg.source, "دستیار هوشمند ..." + "\n", replyMarkup = Some(ReplyKeyboardMarkup(
      Seq(
        Seq(
          KeyboardButton(BackToMainMenu)
        )
      )))))
  }

  onTextFilter(BackToMainMenu) { implicit msg =>
    clearUserCache(msg.source.toString)
    startWithMainMenu
  }
  onTextFilter(CancelProcess) { implicit msg =>
    clearUserCache(msg.source.toString)
    startWithMainMenu
  }

  onTextDefaultFilter { implicit msg =>
    redisExt.get(sKey(msg.source.toString)).map {
      case Some(GettingName) => gettingNameHandler
      case Some(GettingPhoneNumber) => gettingPhoneHandler
      case Some(GettingCountryName) => gettingCountryHandler
      case Some(GettingDayOfFlight) => gettingDayOfFlightHandler
      case Some(GettingMonthOfFlight) => gettingMonthOfFlightHandler()
      case Some(GettingYearOfFlight) => gettingYearOfFlightHandler()
      case Some(ApprovingData) => handleApprovingDataHandler()
      case _ => unexpectedSituationHandler
    }
  }


  onPhotoFilter {implicit msg =>
    redisExt.get(sKey(msg.source.toString)).map {
      case Some(GettingPassportScan) => gettingPassportScanHandler
      case _ => unexpectedSituationHandler()
    }
  }

  onReceipt { implicit msg =>
    unexpectedSituationHandler()
  }

}

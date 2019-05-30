package com.mesr.bot

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.bot4s.telegram.api.declarative.Commands
import com.bot4s.telegram.api.{RequestHandler, TelegramBot}
import com.bot4s.telegram.methods._
import com.bot4s.telegram.models.{InlineKeyboardButton, InlineKeyboardMarkup, KeyboardButton, ReplyKeyboardMarkup}
import com.mesr.bot.Strings._
import com.mesr.bot.helpers._
import com.mesr.bot.persist.PostgresDBExtension
import com.mesr.bot.persist.repos.CountriesRepo
import com.mesr.bot.sdk._
import com.mesr.bot.sdk.db.{RedisExtension, RedisExtensionImpl}
import com.mesr.bot.util.ButtonsUtils
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
    with UserInformation {

  override val system: ActorSystem = _system
  val ec: ExecutionContext = system.dispatcher
  implicit val redisExt: RedisExtensionImpl = RedisExtension(system)
  implicit val pdb: PostgresProfile.api.Database = PostgresDBExtension(system).db

  implicit val mat: ActorMaterializer = ActorMaterializer()
  val host: String = system.settings.config.getString("bot.host")

  LoggerConfig.factory = PrintLoggerFactory()
  LoggerConfig.level = LogLevel.TRACE

  override val client: RequestHandler = new BaleAkkaHttpClient(token, host)

  import StringMaker._
  import UserCurrentState._

  onCommand("/start") { implicit msg =>
    clearUserCache(msg.source.toString)
    startWithMainMenu
  }

  onCommand("/help") { implicit msg =>
    clearUserCache(msg.source.toString)
    startWithMainMenu
  }

  onCommand("/cancel") { implicit msg =>
    clearUserCache(msg.source.toString)
    startWithMainMenu()
  }


  onTextFilter(AdminAddOrRemoveCountry) { implicit msg =>
    isAdmin.map { exist =>
      if (exist) {
        pdb.run(CountriesRepo.getAll).map { countries =>
          val text = newText("کشور های اضافه شده به شرح زیر می باشد") +
            countries.map(s => newText("- " + s.country)).foldLeft("")(_ + _) +
            newText("لطفا یکی از گزینه های زیر را انتخاب کنید.")
          request(SendMessage(msg.source, text, replyMarkup = Some(ButtonsUtils.replyMarkupSingleRow(
            Seq(
              KeyboardButton(AdminAddCountry),
              KeyboardButton(AdminRemoveCountry),
              KeyboardButton(BackToMainMenu))))))
        }
      } else youDoNotHavePermissionGarbage
    }
  }


  import StringMaker._

  onTextFilter(AdminAddCountry) { implicit msg =>
    for {
      exist <- isAdmin
      _ = if (exist) {
        redisExt.set(admin_s_key(msg.source.toString), AdminAddingCountry)
        request(SendMessage(msg.source, INSERT_COUNTRY_NAMES))
      } else youDoNotHavePermissionGarbage

    } yield ()
  }

  onTextFilter(AdminRemoveCountry) { implicit msg =>
    for {
      exist <- isAdmin
      countries <- pdb.run(CountriesRepo.getAll)
      buttons = countries.map { c => KeyboardButton(c.country) }
      _ = if (exist) {
        redisExt.set(admin_s_key(msg.source.toString), AdminRemovingCountry)
        request(SendMessage(msg.source, INSERT_COUNTRY_NAMES,
          replyMarkup = Some(
            ReplyKeyboardMarkup(ButtonsUtils.splitList(buttons) ++ Seq(Seq(KeyboardButton(BackToMainMenu)))
            ))))
      } else youDoNotHavePermissionGarbage

    } yield ()
  }


  onTextFilter(BookEmbassyAppointment) { implicit msg =>
    redisExt.set(sKey(msg.source.toString), GettingName).map { _ =>
      request(SendMessage(msg.source, "لطفا نام و نام خانوادگی خود را وارد کنید."))
    }
  }

  onTextFilter(MoreInfoAndContactAdmin) { implicit msg =>
    request(SendMessage(msg.source, BOT_INTRODUCTION,
      replyMarkup = Some(ButtonsUtils.replyMarkupSingleRow(Seq(KeyboardButton(BackToMainMenu))))))
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
    redisExt.get(sKey(msg.source.toString)).zip(redisExt.get(admin_s_key(msg.source.toString))).map {
      case (_, Some(AdminAddingCountry)) => adminAddingCountryHandler
      case (_, Some(AdminRemovingCountry)) => adminRemovingCountryHandler()
      case (Some(GettingName), _) => gettingNameHandler
      case (Some(GettingPhoneNumber), _) => gettingPhoneHandler
      case (Some(GettingCountryName), _) => gettingCountryHandler
      case (Some(GettingDayOfFlight), _) => gettingDayOfFlightHandler
      case (Some(GettingMonthOfFlight), _) => gettingMonthOfFlightHandler()
      case (Some(GettingYearOfFlight), _) => gettingYearOfFlightHandler()
      case (Some(ApprovingData), _) => handleApprovingDataHandler()
      case (Some(GettingPassportScan), _) => gettingPassportScanneErrorHandler()
      case _ => unexpectedSituationHandler
    }
  }


  onPhotoFilter { implicit msg =>
    redisExt.get(sKey(msg.source.toString)).map {
      case Some(GettingPassportScan) => gettingPassportScanHandler
      case _ => unexpectedSituationHandler()
    }
  }

  onReceipt { implicit msg =>
    unexpectedSituationHandler()
  }

}

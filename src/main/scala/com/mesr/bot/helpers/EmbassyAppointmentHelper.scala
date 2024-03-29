package com.mesr.bot.helpers

import akka.actor.ActorSystem
import com.bot4s.telegram.api.TelegramBot
import com.bot4s.telegram.methods.{SendMessage, SendPhoto}
import com.bot4s.telegram.models.{InputFile, KeyboardButton, Message, ReplyKeyboardMarkup}
import com.mesr.bot.Strings._
import com.mesr.bot.UserCurrentState._
import com.mesr.bot.persist.model.RegisteredAppointment
import com.mesr.bot.persist.repos.{AdminCredentialsRepo, CountriesRepo, RegisteredAppointmentRepo}
import com.mesr.bot.util.ButtonsUtils
import com.mesr.bot.{BookAppointmentTicket, EmbassyAppointmentBot}
import com.typesafe.config.ConfigFactory

import scala.concurrent.Future
import scala.util.Try

trait EmbassyAppointmentHelper extends TelegramBot with OperatorExtension {
  this: EmbassyAppointmentBot =>

  def clearUserCache(userId: String): Future[Unit] =
    for {
      _ <- redisExt.delete(uKey(userId))
      _ <- redisExt.delete(sKey(userId))
    } yield ()

  def bold(str: String) = s"*$str*"

  def newLine = "\n"

  def space = " "

  def structApprovalBookEmbassyInfo(emb: BookAppointmentTicket, sendToAdmin: Boolean = false): String = {
    (if (sendToAdmin) {
      "درخواست جدید برای وقت سفارت به شرح زیر:"
    } else {
      "اطلاعات شما به شرح زیر میباشد."
    }) + newLine +
      "نام و نام خانوادگی:" + space + emb.fullName.get + newLine +
      "شماره موبایل:" + space + emb.phoneNumber.get + newLine +
      "وقت سفارت برای کشور:" + space + emb.country.get + newLine +
      "تاریخ سفر:" + space + emb.dayOfFlight.get + space + emb.monthOfFlight.get + space + emb.yearOfFlight.get
  }

  def youDoNotHavePermissionGarbage()(implicit system: ActorSystem, msg: Message): Unit = {
    request(SendMessage(msg.source, "شما دسترسی ادمین نداری",
      replyMarkup = Some(ButtonsUtils.replyMarkupSingleRow(Seq(KeyboardButton(BackToMainMenu))))
    ))
  }

  import StringMaker._

  def isAdmin()(implicit msg: Message) =
    pdb.run(AdminCredentialsRepo.exists(msg.source.toString))

  def adminAddingCountryHandler()(implicit system: ActorSystem, msg: Message): Future[Any] = {
    isAdmin().map { exist =>
      if (exist) {
        val countries = msg.text.get.filter(_ != ' ').split("،").toSet.toList
        for {
          _ <- redisExt.delete(admin_s_key(msg.source.toString))
          _ <- Future.sequence(countries.map { c =>
            pdb.run(CountriesRepo.exists(c)).map { ex =>
              if (ex) Future.successful() else pdb.run(CountriesRepo.create(c))
            }
          })
          _ <- request(SendMessage(msg.source, OPERATION_HAS_BEEN_DON_SUCCESSFULLY,
            replyMarkup = Some(ButtonsUtils.replyMarkupSingleRow(Seq(KeyboardButton(BackToMainMenu))))))
        } yield ()
      } else youDoNotHavePermissionGarbage
    }
  }

  def adminRemovingCountryHandler()(implicit system: ActorSystem, msg: Message): Future[Any] = {
    isAdmin().map { exist =>
      if (exist) {
        val country = msg.text.get.trim
        for {
          _ <- redisExt.delete(admin_s_key(msg.source.toString))
          _ <- pdb.run(CountriesRepo.delete(country))
          _ = request(SendMessage(msg.source, OPERATION_HAS_BEEN_DON_SUCCESSFULLY,
            replyMarkup = Some(ButtonsUtils.replyMarkupSingleRow(Seq(KeyboardButton(BackToMainMenu))))))
        } yield ()
      } else youDoNotHavePermissionGarbage()
    }
  }

  def startWithMainMenu()(implicit system: ActorSystem, msg: Message): Unit = {
    isAdmin.map { exists =>
      val buttons = (if (exists) Seq(
        KeyboardButton(AdminAddOrRemoveCountry)
      ) else Seq.empty) ++ Seq(
        KeyboardButton(BookEmbassyAppointment),
        KeyboardButton(MoreInfoAndContactAdmin)
      )
      request(SendMessage(msg.source, helloMessageStr, replyMarkup = Some(ButtonsUtils.replyMarkupSingleRow(buttons))))
    }

  }

  import StringMaker._

  def gettingNameHandler()(implicit system: ActorSystem, msg: Message): Future[Unit] = {
    system.log.debug("going to state: {}, user: {}", msg.source, GettingPhoneNumber)
    for {
      _ <- redisExt.set(sKey(msg.source.toString), GettingPhoneNumber)
      _ <- setUserState[BookAppointmentTicket](uKey(msg.source.toString), BookAppointmentTicket(msg.text))
      _ <- request(SendMessage(msg.source, ENTER_PHONE_NUMBER))
    } yield ()
  }

  def gettingPhoneHandler()(implicit system: ActorSystem, msg: Message): Future[Unit] = {
    system.log.debug("going to state: {}, user: {}", msg.source, GettingPhoneNumber)
    val phoneValidation = validatePhone(msg.text.get)
    for {
      _ <- if (phoneValidation.isCorrect) {
        for {
          _ <- redisExt.delete(sKey(msg.source.toString))
          countries <- pdb.run(CountriesRepo.getAll)
          _ <- redisExt.set(sKey(msg.source.toString), GettingCountryName)
          embassyData <- getUserState[BookAppointmentTicket](uKey(msg.source.toString)).map(_.get.copy(phoneNumber = Some(phoneValidation.standardPhone)))
          _ <- setUserState[BookAppointmentTicket](uKey(msg.source.toString), embassyData)
          _ = request(SendMessage(msg.source, "لطفا یکی از کشور ها زیر را انتخاب کنید.",
            replyMarkup = Some(ReplyKeyboardMarkup(ButtonsUtils.splitList(countries.map(s => KeyboardButton(s.country))) ++ Seq(Seq(KeyboardButton(BackToMainMenu))), Some(true), Some(true)))
          ))
        } yield ()
      } else {
        request(SendMessage(msg.source, phoneValidation.description))
        Future.successful()
      }
    } yield ()
  }

  def gettingCountryHandler()(implicit system: ActorSystem, msg: Message): Future[Unit] = for {
    countries <- pdb.run(CountriesRepo.getAll).map(_.map(_.country))
    _ <- if (countries.contains(msg.text.get)) {
      for {
        _ <- redisExt.delete(sKey(msg.source.toString))
        _ <- redisExt.set(sKey(msg.source.toString), GettingDayOfFlight)
        embassyData <- getUserState[BookAppointmentTicket](uKey(msg.source.toString)).map(_.get.copy(country = msg.text))
        _ <- setUserState[BookAppointmentTicket](uKey(msg.source.toString), embassyData)
        _ = request(SendMessage(msg.source, ENTER_DAY_OF_FLIGHT,
          replyMarkup = ButtonsUtils.dayOfMonthBtns
        ))
      } yield ()
    } else {
      request(SendMessage(msg.source, "ورودی نامعتبر است. لطفا یکی از کشور های زیر را انتخاب کنید.",
        replyMarkup = Some(ReplyKeyboardMarkup(
          ButtonsUtils.splitList(countries).map { g =>
            g.map(country => KeyboardButton(country))
          } ++ Seq(Seq(KeyboardButton(BackToMainMenu))),
          Some(true),
          Some(true)
        ))))
      Future.successful()
    }

  } yield ()

  def gettingDayOfFlightHandler()(implicit system: ActorSystem, msg: Message): Future[Unit] = {
    val day = makeNumberStandard(msg.text.get)
    if (Try(day.toInt).isSuccess && day.toInt >= 1 && day.toInt <= 31) {
      for {
        _ <- redisExt.delete(sKey(msg.source.toString))
        _ <- redisExt.set(sKey(msg.source.toString), GettingMonthOfFlight)
        embassyData <- getUserState[BookAppointmentTicket](uKey(msg.source.toString)).map(_.get.copy(dayOfFlight = Some(day.toInt)))
        _ <- setUserState[BookAppointmentTicket](uKey(msg.source.toString), embassyData)
        _ = request(SendMessage(msg.source, ENTER_MONTH_OF_FLIGHT, replyMarkup = ButtonsUtils.monthOfYearBtns))
      } yield ()
    } else {
      request(SendMessage(msg.source, ENTER_DAY_OF_FLIGHT_AGAIN,
        replyMarkup = ButtonsUtils.dayOfMonthBtns
      ))
      Future.successful()
    }
  }


  def gettingMonthOfFlightHandler()(implicit system: ActorSystem, msg: Message): Future[Unit] = {
    val filteredText = msg.text.get.filter(s => s != ' ' && s != '.' && Try(s.toString.toInt).isFailure)
    if (englishMonthLists.map(_._2).contains(filteredText)) {
      for {
        _ <- redisExt.delete(sKey(msg.source.toString))
        _ <- redisExt.set(sKey(msg.source.toString), GettingYearOfFlight)
        embassyData <- getUserState[BookAppointmentTicket](uKey(msg.source.toString)).map(_.get.copy(monthOfFlight = Some(filteredText)))
        _ <- setUserState[BookAppointmentTicket](uKey(msg.source.toString), embassyData)
        _ = request(SendMessage(msg.source, ENTER_YEAR_OF_FLIGHT,
          replyMarkup = Some(ButtonsUtils.replyMarkupSingleRow(
            Seq(
              KeyboardButton("2019"),
              KeyboardButton("2020"),
              KeyboardButton(BackToMainMenu)
            ))
          )))
      } yield ()
    } else {
      request(SendMessage(msg.source, ENTER_MONTH_OF_FLIGHT_AGAIN, replyMarkup = ButtonsUtils.monthOfYearBtns))
      Future.successful()
    }

  }


  def gettingYearOfFlightHandler()(implicit system: ActorSystem, msg: Message): Future[Unit] = {
    val yearOfFlight = makeNumberStandard(msg.text.get)
    if (Try(yearOfFlight.toInt).isSuccess && yearOfFlight.toInt >= 2019) {
      for {
        _ <- redisExt.delete(sKey(msg.source.toString))
        _ <- redisExt.set(sKey(msg.source.toString), GettingPassportScan)
        embassyData <- getUserState[BookAppointmentTicket](uKey(msg.source.toString)).map(_.get.copy(yearOfFlight = msg.text.map(_.toInt)))
        _ <- setUserState[BookAppointmentTicket](uKey(msg.source.toString), embassyData)
        _ = request(SendMessage(msg.source, SEND_SCAN_OF_YOUR_PASSPORT,
          replyMarkup = Some(ButtonsUtils.replyMarkupSingleRow(Seq(KeyboardButton(BackToMainMenu))))))
      } yield ()
    } else {
      request(SendMessage(msg.source, ENTER_YEAR_OF_FLIGHT_AGAIN,
        replyMarkup = Some(ButtonsUtils.replyMarkupSingleRow(
          Seq(
            KeyboardButton("2019"),
            KeyboardButton("2020"),
            KeyboardButton(BackToMainMenu)
          ))
        )))
      Future.successful()
    }

  }

  def gettingPassportScanneErrorHandler()(implicit system: ActorSystem, msg: Message): Future[Message] = {
    request(SendMessage(msg.source, INVALID_INPUT_PLEASE_SEND_PASSPORT_SCAN,
      replyMarkup = Some(ButtonsUtils.replyMarkupSingleRow(Seq(KeyboardButton(BackToMainMenu))))
    ))
  }


  def handleApprovingDataHandler()(implicit system: ActorSystem, msg: Message): Future[Unit] = for {
    _ <- redisExt.delete(sKey(msg.source.toString))
    embassyData <- getUserState[BookAppointmentTicket](uKey(msg.source.toString)).map(_.get)
    _ <- pdb.run(RegisteredAppointmentRepo.create(RegisteredAppointment(
      chatId = msg.source.toString,
      fullName = embassyData.fullName.get,
      flightDate = embassyData.dayOfFlight.get + space + embassyData.monthOfFlight.get + space + embassyData.yearOfFlight.get,
      fileId = embassyData.fillId.get
    )))
    _ = request(SendMessage(msg.source, YOUR_REQUEST_HAS_BEEN_REGISTERED_AND_WE_WILL_REACH_YOU_IN_A_FEW_HOURS(embassyData.fullName.get),
      replyMarkup = Some(ButtonsUtils.replyMarkupSingleRow(Seq(KeyboardButton(BackToMainMenu))))))
    admin_chat_ids <- pdb.run(AdminCredentialsRepo.getAllActive)
    _ <- Future.sequence(
      admin_chat_ids.map { admin_chat_id =>
        request(SendPhoto(
          chatId = admin_chat_id,
          photo = InputFile(embassyData.fillId.get),
          caption = Some(structApprovalBookEmbassyInfo(embassyData, sendToAdmin = true)),
          replyMarkup = Some(ButtonsUtils.replyMarkupSingleRow(Seq(KeyboardButton(BackToMainMenu))))
        ))
      }
    )
    _ <- clearUserCache(msg.source.toString)
  } yield ()


  def gettingPassportScanHandler()(implicit system: ActorSystem, msg: Message): Future[Unit] = for {
    _ <- redisExt.delete(sKey(msg.source.toString))
    _ <- redisExt.set(sKey(msg.source.toString), ApprovingData)
    fileId = msg.photo.get.head.fileId
    embassyData <- getUserState[BookAppointmentTicket](uKey(msg.source.toString)).map(_.get.copy(fillId = Some(fileId)))
    _ <- setUserState[BookAppointmentTicket](uKey(msg.source.toString), embassyData)
    _ = request(SendPhoto(
      chatId = msg.source,
      photo = InputFile(embassyData.fillId.get),
      caption = Some(structApprovalBookEmbassyInfo(embassyData, sendToAdmin = true)),
      replyMarkup = Some(ButtonsUtils.replyMarkupSingleRow(Seq(InfoApproval, BackToMainMenu).map(KeyboardButton(_))))))
  } yield ()


  def unexpectedSituationHandler()(implicit system: ActorSystem, msg: Message): Future[Message] = {
    clearUserCache(msg.source.toString)
    request(SendMessage(msg.source, PLEASE_TRY_AGAIN,
      replyMarkup = Some(ButtonsUtils.replyMarkupSingleRow(Seq(KeyboardButton(BackToMainMenu))))))
  }


}

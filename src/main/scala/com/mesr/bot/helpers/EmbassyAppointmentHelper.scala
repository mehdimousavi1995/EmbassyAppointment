package com.mesr.bot.helpers

import akka.actor.ActorSystem
import com.bot4s.telegram.api.TelegramBot
import com.bot4s.telegram.methods.SendMessage
import com.bot4s.telegram.models.{KeyboardButton, Message, ReplyKeyboardMarkup}
import com.mesr.bot.{BookAppointmentTicket, EmbassyAppointmentBot}
import com.mesr.bot.Strings._
import com.mesr.bot.UserCurrentState._
import com.mesr.bot.persist.model.RegisteredAppointment
import com.mesr.bot.persist.repos.{CountriesRepo, RegisteredAppointmentRepo}
import com.mesr.bot.sdk.{CustomKeyboardButton, CustomReplyKeyboardMarkup, SendCustomPhotoMessage}
import com.mesr.bot.sdk.ReplyKeyboardMarkupSerializer.sendHttpRequest

import scala.concurrent.Future

trait EmbassyAppointmentHelper extends TelegramBot {
  this: EmbassyAppointmentBot =>

  def clearUserCache(userId: String): Future[Unit] =
    for {
      _ <- redisExt.delete(uKey(userId))
      _ <- redisExt.delete(sKey(userId))
    } yield ()

  def bold(str: String) = s"*$str*"

  def newLine = "\n"

  def space = " "

  def structApprovalBookEmbassyInfo(emb: BookAppointmentTicket): String = {
    "اطلاعات شما به شرح زیر میباشد." + newLine +
      bold("نام و نام خانوادگی: ") + emb.fullName.get + newLine +
      bold("شماره موبایل: ") + emb.phoneNumber.get + newLine +
      bold("وقت سفارت برای کشور: ") + emb.country.get + newLine +
      bold("تاریخ سفر: ") + emb.dayOfFlight.get + space + emb.monthOfFlight.get + space + emb.yearOfFlight.get
  }


  def startWithMainMenu()(implicit system: ActorSystem, msg: Message): Unit = {
    request(SendMessage(msg.source, helloMessageStr, replyMarkup = Some(ReplyKeyboardMarkup(
      Seq(
        Seq(
          KeyboardButton(BookEmbassyAppoinement),
          KeyboardButton(MoreInfoAndContactAdmin)
        )
      )))))
  }

  def gettingNameHandler()(implicit system: ActorSystem, msg: Message): Future[Unit] = {
    system.log.debug("going to state: {}, user: {}", msg.source, GettingPhoneNumber)
    for {
      _ <- redisExt.set(sKey(msg.source.toString), GettingPhoneNumber)
      _ <- setUserState[BookAppointmentTicket](uKey(msg.source.toString), BookAppointmentTicket(msg.text))
      _ <- request(SendMessage(msg.source, "لطفا شماره موبایل خود را وارد کنید.", replyMarkup = Some(ReplyKeyboardMarkup(
        Seq(
          Seq(
            KeyboardButton(BackToMainMenu)
          )
        )))))
    } yield ()
  }

  def gettingPhoneHandler()(implicit system: ActorSystem, msg: Message): Future[Unit] = {
    system.log.debug("going to state: {}, user: {}", msg.source, GettingPhoneNumber)
    for {
      _ <- redisExt.delete(sKey(msg.source.toString))
      countries <- pdb.run(CountriesRepo.getAll)
      _ <- redisExt.set(sKey(msg.source.toString), GettingCountryName)
      // todo handle safely, validate phonenumber before saving in user state
      embassyData <- getUserState[BookAppointmentTicket](uKey(msg.source.toString)).map(_.get.copy(phoneNumber = msg.text))
      _ <- setUserState[BookAppointmentTicket](uKey(msg.source.toString), embassyData)
      _ = request(SendMessage(msg.source, "لطفا یکی از کشور ها زیر را انتخاب کنید.", replyMarkup = Some(ReplyKeyboardMarkup(
        Seq(
          countries.map(s => KeyboardButton(s.country)) ++
            Seq(
              KeyboardButton(BackToMainMenu)
            )
        )))))
    } yield ()
  }

  def gettingCountryHandler()(implicit system: ActorSystem, msg: Message): Future[Unit] = for {
    _ <- redisExt.delete(sKey(msg.source.toString))
    _ <- redisExt.set(sKey(msg.source.toString), GettingDayOfFlight)
    // todo handle safely, validate phonenumber before saving in user state, validate country
    embassyData <- getUserState[BookAppointmentTicket](uKey(msg.source.toString)).map(_.get.copy(country = msg.text))
    _ <- setUserState[BookAppointmentTicket](uKey(msg.source.toString), embassyData)
    _ = request(SendMessage(msg.source, "لطفا روز میلادی سفر خود را وارد کنید(میلاد). عدد بین ۱ تا ۳۱", replyMarkup = Some(ReplyKeyboardMarkup(
      Seq(
        Seq(
          KeyboardButton(BackToMainMenu)
        )
      )))))
  } yield ()

  def gettingDayOfFlightHandler()(implicit system: ActorSystem, msg: Message): Future[Unit] = for {
    _ <- redisExt.delete(sKey(msg.source.toString))
    _ <- redisExt.set(sKey(msg.source.toString), GettingMonthOfFlight)
    // todo handle safely, validate phonenumber before saving in user state
    embassyData <- getUserState[BookAppointmentTicket](uKey(msg.source.toString)).map(_.get.copy(dayOfFlight = msg.text.map(_.toInt)))
    _ <- setUserState[BookAppointmentTicket](uKey(msg.source.toString), embassyData)
    // todo validation, add example for stupid users
    _ = request(SendMessage(msg.source, "لطفا ماه میلادی سفر خود را انتخاب کنید", replyMarkup = Some(ReplyKeyboardMarkup(
      Seq(
        englishMonthLists.map(KeyboardButton(_)) ++
          Seq(
            KeyboardButton(BackToMainMenu)
          )
      )))))
  } yield ()


  def gettingMonthOfFlightHandler()(implicit system: ActorSystem, msg: Message): Future[Unit] = for {
    _ <- redisExt.delete(sKey(msg.source.toString))
    _ <- redisExt.set(sKey(msg.source.toString), GettingYearOfFlight)
    // todo handle safely, validate phonenumber before saving in user state
    embassyData <- getUserState[BookAppointmentTicket](uKey(msg.source.toString)).map(_.get.copy(monthOfFlight = msg.text))
    _ <- setUserState[BookAppointmentTicket](uKey(msg.source.toString), embassyData)
    // todo validation, add example for stupid users
    _ = request(SendMessage(msg.source, "لطفا سال میلادی سفر خود را وارد کنید", replyMarkup = Some(ReplyKeyboardMarkup(
      Seq(
        Seq(
          KeyboardButton(BackToMainMenu)
        )
      )))))
  } yield ()


  def gettingYearOfFlightHandler()(implicit system: ActorSystem, msg: Message): Future[Unit] = for {
    _ <- redisExt.delete(sKey(msg.source.toString))
    _ <- redisExt.set(sKey(msg.source.toString), GettingPassportScan)
    // todo handle safely, validate phonenumber before saving in user state
    embassyData <- getUserState[BookAppointmentTicket](uKey(msg.source.toString)).map(_.get.copy(yearOfFlight = msg.text.map(_.toInt)))
    _ <- setUserState[BookAppointmentTicket](uKey(msg.source.toString), embassyData)
    // todo validation, add example for stupid users
    _ = request(SendMessage(msg.source, "لطفا اسکن پاسپورت خود را در قالب عکس ارسال کیند.", replyMarkup = Some(ReplyKeyboardMarkup(
      Seq(
        Seq(
          KeyboardButton(BackToMainMenu)
        )
      )))))
  } yield ()


  def handleApprovingDataHandler()(implicit system: ActorSystem, msg: Message): Future[Unit] = for {
    _ <- redisExt.delete(sKey(msg.source.toString))
    embassyData <- getUserState[BookAppointmentTicket](uKey(msg.source.toString)).map(_.get)
    _ <- pdb.run(RegisteredAppointmentRepo.create(RegisteredAppointment(
      chatId = msg.source.toString,
      fullName = embassyData.fullName.get,
      flightDate = embassyData.dayOfFlight.get + space + embassyData.monthOfFlight.get + space + embassyData.yearOfFlight.get,
      fileId = embassyData.fillId.get
    )))
    _ = request(SendMessage(msg.source, "کاربر گرامی درخواست شما با موفقیت ثبت شد. تا ساعاتی دیگر کارشناسان ما برای تایید و انجام درخواست تبت شده، با شما تماس خواهند گرفت.", replyMarkup = Some(ReplyKeyboardMarkup(
      Seq(
        Seq(
          KeyboardButton(BackToMainMenu)
        )
      )))))
    _ <- sendHttpRequest(SendCustomPhotoMessage(
      AdminChatId.toString,
      Some(structApprovalBookEmbassyInfo(embassyData)),
      embassyData.fillId.get,
      Some(CustomReplyKeyboardMarkup(
        Seq(
          Seq(
            CustomKeyboardButton(BackToMainMenu)
          )
        )))
    ))
    _ <- clearUserCache(msg.source.toString)
  } yield ()


  def gettingPassportScanHandler()(implicit system: ActorSystem, msg: Message): Future[Unit] = for {
    _ <- redisExt.delete(sKey(msg.source.toString))
    _ <- redisExt.set(sKey(msg.source.toString), ApprovingData)
    fileId = msg.photo.get.head.fileId
    embassyData <- getUserState[BookAppointmentTicket](uKey(msg.source.toString)).map(_.get.copy(fillId = Some(fileId)))
    _ <- setUserState[BookAppointmentTicket](uKey(msg.source.toString), embassyData)
    _ <- sendHttpRequest(SendCustomPhotoMessage(
      msg.source.toString,
      Some(structApprovalBookEmbassyInfo(embassyData)),
      fileId,
      Some(CustomReplyKeyboardMarkup(
        Seq(
          Seq(
            CustomKeyboardButton(InfoApproval),
            CustomKeyboardButton(CancelProcess)
          )
        )))
    ))
  } yield ()


  def unexpectedSituationHandler()(implicit system:ActorSystem, msg: Message) = {
    clearUserCache(msg.source.toString)
    request(SendMessage(msg.source, "مشکلی به وجود آمده است. لطفا فرآیند را از نو آغاز کنید.", replyMarkup = Some(ReplyKeyboardMarkup(
      Seq(
        Seq(
          KeyboardButton(BackToMainMenu)
        )
      )))))
  }


}

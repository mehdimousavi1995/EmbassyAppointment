package com.mesr.bot

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.bot4s.telegram.api.declarative.Commands
import com.bot4s.telegram.api.{RequestHandler, TelegramBot}
import com.bot4s.telegram.methods._
import com.bot4s.telegram.models.{InputFile, KeyboardButton, ReplyKeyboardMarkup}
import com.mesr.bot.Strings._
import com.mesr.bot.helpers._
import com.mesr.bot.persist.PostgresDBExtension
import com.mesr.bot.persist.repos.CountriesRepo
import com.mesr.bot.sdk.db.RedisExtension
import com.mesr.bot.sdk._
import slogging.{LogLevel, LoggerConfig, PrintLoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

class EmbassyTimeBot(token: String)(implicit _system: ActorSystem)
  extends TelegramBot
    with BalePolling
    with Commands
    with MessageHandler
    with ClinicHelper
    with RedisKeys {

  import UserInformation._

  override val system: ActorSystem = _system
  val ec: ExecutionContext = system.dispatcher
  implicit val redisExt = RedisExtension(system)
  implicit val pdb = PostgresDBExtension(system).db

  implicit val mat: ActorMaterializer = ActorMaterializer()

  LoggerConfig.factory = PrintLoggerFactory()
  LoggerConfig.level = LogLevel.TRACE

  override val client: RequestHandler = new BaleAkkaHttpClient(token, "tapi.bale.ai")

  import UserCurrentState._

  def clearUserCache(userId: String): Future[Unit] =
    for {
      _ <- redisExt.delete(uKey(userId))
      _ <- redisExt.delete(sKey(userId))
    } yield ()

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
      case Some(v) => v match {
        case GettingName =>
          system.log.debug("the stupid name of user is: {}", msg.text)
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


        case GettingPhoneNumber =>
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

        case GettingCountryName =>
          for {
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

        case GettingDayOfFlight =>
          for {
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

        case GettingMonthOfFlight =>
          for {
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

        case GettingYearOfFlight =>
          for {
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


        case ApprovingData =>



        case FeedBackState =>
          system.log.info("the stupid name of user is: {}", msg.text)
          redisExt.delete(sKey(msg.source.toString)).map { _ =>
            request(SendMessage(msg.source, "ممنون از بازخورد شما:)", replyMarkup = Some(ReplyKeyboardMarkup(
              Seq(
                Seq(
                  KeyboardButton(BackToMainMenu)
                )
              )))))
          }
        case _ =>
          clearUserCache(msg.source.toString)
          request(SendMessage(msg.source, "مشکلی به وجود آمده است. لطفا فرآیند را از نو آغاز کنید.", replyMarkup = Some(ReplyKeyboardMarkup(
            Seq(
              Seq(
                KeyboardButton(BackToMainMenu)
              )
            )))))

      }
      case None =>
        clearUserCache(msg.source.toString)
        request(SendMessage(msg.source, "مشکلی به وجود آمده است. لطفا فرآیند را از نو آغاز کنید.", replyMarkup = Some(ReplyKeyboardMarkup(
          Seq(
            Seq(
              KeyboardButton(BackToMainMenu)
            )
          )))))
    }


  }

  def bold(str: String) = s"*$str*"

  def newLine = "\n"

  def space = " "

  def structApprovalBookEmbassyInfo(emb: BookAppointmentTicket): String = {
    "اطلاعات شما به شرح زیر میباشد." + newLine +
      "نام و نام خانوادگی: " + emb.fullName.get + newLine +
      "شماره موبایل: " + emb.phoneNumber.get + newLine +
      "وقت سفارت برای کشور: " + emb.country.get + newLine +
      "تاریخ سفر: " + emb.dayOfFlight.get + space + emb.monthOfFlight.get + space + emb.yearOfFlight.get
  }

  import com.mesr.bot.sdk.ReplyKeyboardMarkupSerializer._

  onPhotoFilter { msg =>
    redisExt.get(sKey(msg.source.toString)).map {
      case Some(value) => value match {
        case GettingPassportScan =>
          for {
            _ <- redisExt.delete(sKey(msg.source.toString))
            _ <- redisExt.set(sKey(msg.source.toString), ApprovingData)
            fileId = msg.photo.get.head.fileId
            embassyData <- getUserState[BookAppointmentTicket](uKey(msg.source.toString)).map(_.get.copy(fillId = Some(fileId)))
            _ <- setUserState[BookAppointmentTicket](uKey(msg.source.toString), embassyData)
            _ <- sendHttpRequest("https://tapi.bale.ai/ad10d06717a15e7771f2e567eb12e7603c6c4144/sendPhoto", SendCustomPhotoMessage(
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
        case _ =>
          clearUserCache(msg.source.toString)
          request(SendMessage(msg.source, "مشکلی به وجود آمده است. لطفا فرآیند را از نو آغاز کنید.", replyMarkup = Some(ReplyKeyboardMarkup(
            Seq(
              Seq(
                KeyboardButton(BackToMainMenu)
              )
            )))))
      }
      case None =>
        clearUserCache(msg.source.toString)
        request(SendMessage(msg.source, "مشکلی به وجود آمده است. لطفا فرآیند را از نو آغاز کنید.", replyMarkup = Some(ReplyKeyboardMarkup(
          Seq(
            Seq(
              KeyboardButton(BackToMainMenu)
            )
          )))))
    }

  }

  onReceipt { implicit msg =>
    //    val payment = msg.successfulPayment.get
    //    val json = parse(payment.invoicePayload)
    //    val jsMap = json.right.toOption.flatMap(_.asObject).map(_.toMap).getOrElse(Map.empty)
    //    if (jsMap.get("status").flatMap(_.asString).getOrElse("FAILURE") == "SUCCESS") {
    //      successfulPayment(payment.totalAmount)
    request(SendMessage(msg.source, "مشکلی به وجود آمده است. لطفا فرآیند را از نو آغاز کنید.", replyMarkup = Some(ReplyKeyboardMarkup(
      Seq(
        Seq(
          KeyboardButton(BackToMainMenu)
        )
      )))))
  }

}

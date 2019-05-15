package com.mesr.bot

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.bot4s.telegram.api.declarative.Commands
import com.bot4s.telegram.api.{RequestHandler, TelegramBot}
import com.bot4s.telegram.methods._
import com.bot4s.telegram.models.{KeyboardButton, ReplyKeyboardMarkup}
import com.mesr.bot.Strings._
import com.mesr.bot.helpers._
import com.mesr.bot.sdk.{BaleAkkaHttpClient, BalePolling, MessageHandler}
import io.circe.generic.semiauto._
import io.circe.parser._
import io.circe.{Decoder, Encoder}
import slogging.{LogLevel, LoggerConfig, PrintLoggerFactory}

import scala.concurrent.ExecutionContext

class ClinicBot(token: String)(implicit _system: ActorSystem)
  extends TelegramBot
    with BalePolling
    with Commands
    with InviteHelper
    with PaymentHelper
    with HintHelper
    with LevelHelper
    with MessageHandler
    with ClinicHelper {
  override val system: ActorSystem = _system
  override val ec: ExecutionContext = executionContext

  implicit val userStateEncoder: Encoder[UserState] = deriveEncoder[UserState]
  implicit val userStateDecoder: Decoder[UserState] = deriveDecoder[UserState]

  implicit val gameStateEncoder: Encoder[GameState] = deriveEncoder[GameState]
  implicit val gameStateDecoder: Decoder[GameState] = deriveDecoder[GameState]

  implicit val requestLevelEncoder: Encoder[RequestLevel] = deriveEncoder[RequestLevel]
  implicit val requestLevelDecoder: Decoder[RequestLevel] = deriveDecoder[RequestLevel]

  override implicit val encoder: Encoder[ClinicState] = deriveEncoder[ClinicState]
  override implicit val decoder: Decoder[ClinicState] = deriveDecoder[ClinicState]

  implicit val mat: ActorMaterializer = ActorMaterializer()

  initializeState

  LoggerConfig.factory = PrintLoggerFactory()
  LoggerConfig.level = LogLevel.TRACE

  override val client: RequestHandler = new BaleAkkaHttpClient(token, "tapi.bale.ai")

  import com.mesr.bot.sdk.UserCurrentState._

  onCommand("/start") { implicit msg =>
    startWithMainMenu
  }

  onCommand("/help") { implicit msg =>
    startWithMainMenu
  }

  onTextFilter(VisitTime) { implicit msg =>
    redisExt.set("state-" + msg.source, GettingName).map { _ =>
      request(SendMessage(msg.source, "لطفا نام خود را وارد کنید.", replyMarkup = Some(ReplyKeyboardMarkup(
        Seq(
          Seq(
            KeyboardButton(BackToMainMenu)
          )
        )))))
    }
  }

  onTextFilter(BehSimaServices) { implicit msg =>
    val services = AllBehSimaServices.map { btn =>
      "- " + btn + "\n"
    }.foldLeft("")(_ + _)

    request(SendMessage(msg.source, "خدمات به سیما شامل موارد زیر می باشد:" + "\n" + services, replyMarkup = Some(ReplyKeyboardMarkup(
      Seq(
        Seq(
          KeyboardButton(BackToMainMenu)
        )
      )))))
  }

  onTextFilter(SmsBroadcastEnable) { implicit msg =>
    request(SendMessage(msg.source, "خدمات پیامکی کلینیک برای شما فعال شد. از این پس هرگونه یادآوری وقت ویزیت، خدمات ويزه و تخفیف های کلینیک از طریق پیام کوتاه برای شما ارسال خواهد شد.", replyMarkup = Some(ReplyKeyboardMarkup(
      Seq(
        Seq(
          KeyboardButton(BackToMainMenu)
        )
      )))))
  }
  onTextFilter(AboutClinicAnd) { implicit msg =>
    request(SendMessage(msg.source, BehsimaInfo, replyMarkup = Some(ReplyKeyboardMarkup(
      Seq(
        Seq(
          KeyboardButton(BackToMainMenu)
        )
      )))))
  }

  onTextFilter(Support) { implicit msg =>
    redisExt.set("state-" + msg.source, FeedBackState).map { _ =>
      request(SendMessage(msg.source, "لطفا هر گونه انتقاد و پیشنهاد را برای ما ارسال کنید.", replyMarkup = Some(ReplyKeyboardMarkup(
        Seq(
          Seq(
            KeyboardButton(BackToMainMenu)
          )
        )))))
    }
  }

  onTextFilter(ClinicAddress) { implicit msg =>

    val add = "تهران – خیابان ولیعصر – بالاتر از توانیر ( بیمارستان دی ) – کوچه شاهین – پلاک 10 – صندوق پستی : 14155-4949 " + "\n\n"
    val tel = "تلفن: 88873000 الی 8 و 88876333 و 88876444 و 88875501 الی 4 " + "\n\n"
    val namabar = "نمابر: 88772569 " + "\n\n"
    val hameRozeh = "همه روزه بجز روز های تعطیل 8 تا 20 - پنج شنبه ها از 8 تا 18"

    redisExt.set("state-" + msg.source, "").map { _ =>
      request(SendMessage(msg.source, add + tel + namabar + hameRozeh, replyMarkup = Some(ReplyKeyboardMarkup(
        Seq(
          Seq(
            KeyboardButton(BackToMainMenu)
          )
        )))))
    }
  }

  onTextFilter(BackToMainMenu) { implicit msg =>
    redisExt.delete("state-" + msg.source.toString)
    startWithMainMenu
  }


  onTextDefaultFilter { implicit msg =>
    redisExt.get("state-" + msg.source).map {
      case Some(v) => v match {
        case GettingName =>
          system.log.info("the stupid name of user is: {}", msg.text)
          redisExt.set("state-" + msg.source, GettingPhoneNumber).map { _ =>
            request(SendMessage(msg.source, "لطفا شماره موبایل خود را وارد کنید.", replyMarkup = Some(ReplyKeyboardMarkup(
              Seq(
                Seq(
                  KeyboardButton(BackToMainMenu)
                )
              )))))
          }

        case GettingPhoneNumber =>
          system.log.info("the stupid name of user is: {}", msg.text)
          redisExt.delete("state-" + msg.source).map { _ =>
            request(SendMessage(msg.source, "وقت ویزیت با موفقیت رزرو شد. وقت در نظر گرفته برای شما روز دوشنبه ۱۲ خرداد ۱۳۹۸ می باشد. لطفا از نیم ساعت قبل در کلینیک حضور داشته باشید.", replyMarkup = Some(ReplyKeyboardMarkup(
              Seq(
                Seq(
                  KeyboardButton(BackToMainMenu)
                )
              )))))
          }

        case FeedBackState =>
          system.log.info("the stupid name of user is: {}", msg.text)
          redisExt.delete("state-" + msg.source).map { _ =>
            request(SendMessage(msg.source, "ممنون از بازخورد شما:)", replyMarkup = Some(ReplyKeyboardMarkup(
              Seq(
                Seq(
                  KeyboardButton(BackToMainMenu)
                )
              )))))
          }
        case _ =>
          request(SendMessage(msg.source, "مشکلی به وجود آمده است. لطفا فرآیند را از نو آغاز کنید.", replyMarkup = Some(ReplyKeyboardMarkup(
            Seq(
              Seq(
                KeyboardButton(BackToMainMenu)
              )
            )))))

      }
      case None =>
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
    request(SendMessage(msg.source, "fuck you"))


  }
}

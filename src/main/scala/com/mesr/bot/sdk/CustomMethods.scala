package com.mesr.bot.sdk

import akka.actor.ActorSystem
import akka.http.javadsl.model.RequestEntity
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import spray.json.{DefaultJsonProtocol, NullOptions, RootJsonFormat}

import scala.concurrent.Future

case class CustomKeyboardButton(
                           text: String,
                           requestContact: Option[Boolean] = None,
                           requestLocation: Option[Boolean] = None
                         )

case class CustomReplyKeyboardMarkup(keyboard: Seq[Seq[CustomKeyboardButton]])

case class SendCustomPhotoMessage(
                             chat_id: String,
                             caption: Option[String],
                             photo: String,
                             reply_markup: Option[CustomReplyKeyboardMarkup] = None
                           )

object ReplyKeyboardMarkupSerializer extends DefaultJsonProtocol with NullOptions {
  implicit val keyboardJsonSerializerJF: RootJsonFormat[CustomKeyboardButton] = jsonFormat3(CustomKeyboardButton)
  implicit val replyKeyboardMarkupSerializerJf: RootJsonFormat[CustomReplyKeyboardMarkup] = jsonFormat1(CustomReplyKeyboardMarkup)
  implicit val sendCustomPhotoMessageSerializerJF: RootJsonFormat[SendCustomPhotoMessage] = jsonFormat4(SendCustomPhotoMessage)
  import spray.json._

  def sendHttpRequest(sendPhoto: SendCustomPhotoMessage, endpoint: String = "https://tapi.bale.ai/ad10d06717a15e7771f2e567eb12e7603c6c4144/sendPhoto")(implicit system: ActorSystem): Future[HttpResponse] = {
    Http().singleRequest(
      HttpRequest(HttpMethods.POST, Uri(endpoint), Nil, HttpEntity(ContentTypes.`application/json`, sendPhoto.toJson.toString))
    )
  }

}
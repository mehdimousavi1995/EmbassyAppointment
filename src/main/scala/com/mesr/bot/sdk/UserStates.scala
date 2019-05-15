package com.mesr.bot.sdk

import com.mesr.bot.sdk.db.RedisExtensionImpl
import io.circe.{Decoder, Encoder}

import scala.concurrent.Future


object UserCurrentState {
  val GettingPhoneNumber = "getting-phone-number"
  val GettingName = "getting-name"
  val FeedBackState = "feed-back-state"
}



trait UserStateInterface


case class UserInformation(
                          fullName: Option[String] = None,
                          phoneNumber: Option[Long] = None
                          )


object UserInformation {

  implicit val userInfoEncoder: Encoder[UserInformation] = Encoder[UserInformation]
  implicit val userInfoDecoder: Decoder[UserInformation] = Decoder[UserInformation]

  def setUserState[T](key: String, userInfo: T)(implicit redisExt: RedisExtensionImpl, encoder: Encoder[T]): Future[Boolean] = {
    redisExt.setObj(key, 6000, userInfo)
  }

  def getUserState[T](key: String)(implicit redisExt: RedisExtensionImpl, decoder: Decoder[T]): Future[Option[T]] = {
    redisExt.getObj[T](key).mapTo[Option[T]]
  }



}
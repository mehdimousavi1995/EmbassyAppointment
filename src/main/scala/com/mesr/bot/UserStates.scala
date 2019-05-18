package com.mesr.bot

import com.mesr.bot.sdk.db.RedisExtensionImpl
import spray.json.{DefaultJsonProtocol, JsonReader, JsonWriter, NullOptions, RootJsonFormat}

import scala.concurrent.Future


object UserCurrentState {
  val GettingName = "getting-name"
  val GettingPhoneNumber = "getting-phone-number"
  val GettingCountryName = "getting-country-name"
  val GettingDayOfFlight = "getting-day-of-flight"
  val GettingMonthOfFlight = "getting-month-of-flight"
  val GettingYearOfFlight = "getting-year-of-flight"
  val GettingPassportScan = "getting-passport-scan"


  val FeedBackState = "feed-back-state"
}


case class BookAppointmentTicket(
                                  fullName: Option[String] = None,
                                  phoneNumber: Option[String] = None,
                                  country: Option[String] = None,
                                  dayOfFlight: Option[Int] = None,
                                  monthOfFlight: Option[String] = None,
                                  yearOfFlight: Option[Int] = None,
                                  passportFileId: Option[String] = None
                                )


object UserInformation extends DefaultJsonProtocol with NullOptions{

  val expirationInSeconds = 6000

  implicit val bookAppointmentTicketSerializerJF: RootJsonFormat[BookAppointmentTicket] = jsonFormat7(BookAppointmentTicket)


  def setUserState[T](key: String, userInfo: T)(implicit redisExt: RedisExtensionImpl, jsonWriter: JsonWriter[T]): Future[Boolean] = {
    redisExt.setObj(key, expirationInSeconds, userInfo)
  }

  def getUserState[T](key: String)(implicit redisExt: RedisExtensionImpl, jsonReader: JsonReader[T]): Future[Option[T]] = {
    redisExt.getObj[T](key).mapTo[Option[T]]
  }

}
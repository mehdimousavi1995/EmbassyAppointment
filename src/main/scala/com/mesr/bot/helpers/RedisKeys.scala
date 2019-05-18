package com.mesr.bot.helpers



trait RedisKeys {

  private val EmbassyAppointment = "embassy-appointment"

  def sKey(userId: String): String = s"state-$EmbassyAppointment-$userId"

  def uKey(userId: String): String = s"user-info-$EmbassyAppointment-$userId"

}

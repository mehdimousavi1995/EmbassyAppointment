package com.mesr.bot.sdk.db

import spray.json._

object ObjectSerializer {

  def serialize[T](obj: T)(implicit jsonWriter: JsonWriter[T]): String =
    obj.toJson.toString()

  def deSerialize[T](str: String)(implicit jsonReader: JsonReader[T]): T =
    str.parseJson.convertTo[T]

}
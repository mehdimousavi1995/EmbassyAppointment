package com.mesr.bot.helpers

import scala.util.Try


trait OperatorExtension extends Converter {

  sealed trait Operator

  case object MCI extends Operator

  case object MTN extends Operator

  case object RIGhTEL extends Operator

  case object TALIA extends Operator

  val mapOpToCode: PartialFunction[Operator, String] = {
    case MCI     ⇒ "0919"
    case MTN     ⇒ "0935"
    case RIGhTEL ⇒ "0920"
    case TALIA   ⇒ "0932"
  }

  val providerList = List(
    ("091", MCI), ("0990", MCI), ("0991", MCI), ("09912", MCI),
    ("0930", MTN), ("0933", MTN), ("0935", MTN), ("0936", MTN),
    ("0937", MTN), ("0938", MTN), ("0939", MTN), ("0901", MTN),
    ("0902", MTN), ("0903", MTN), ("0905", MTN), ("0920", RIGhTEL),
    ("0921", RIGhTEL), ("0922", RIGhTEL), ("0932", TALIA)
  )


  val chargeList = List(10000, 20000, 50000, 100000, 200000)

  val startPhone = List("0098", "098", "98")

  def makePhoneStandard(phoneNum: String): String = {
    val filterPhone = makeNumberStandard(phoneNum.filter(c ⇒ c != '-' && c != '+' && c != ' '))
    val findPrefix = startPhone.filter(p ⇒ filterPhone.startsWith(p))
    if (findPrefix.length == 1)
      "0" + filterPhone.substring(findPrefix.head.length)
    else {
      if (filterPhone.startsWith("0"))
        filterPhone
      else
        "-1111"
    }
  }


  import StringMaker._

  case class Validation(isCorrect: Boolean, description: String, standardPhone: String)

  def validatePhone(num: String): Validation = {
    val phoneNumber = makePhoneStandard(trim(num))
    if (!(phoneNumber == "-1111")) {
      if (!lackOfCharacter(phoneNumber))
        Validation(false, PHONE_NUMBER_CONTAINS_CHARACTER, phoneNumber)
      else if (!hasValidLength(phoneNumber))
        Validation(false, INVALID_PHONE_LENGTH, phoneNumber)
      else if (phoneNumber.length == 0)
        Validation(false, EMPTY_PHONE_NUMBER, phoneNumber)
      else if (!isValid(phoneNumber, providerList))
        Validation(false, INVALID_PHONE_NUMBER, phoneNumber)
      else Validation(true, "VALID", phoneNumber)
    } else Validation(false, INVALID_PHONE_NUMBER, phoneNumber)
  }

  def prettyPhone(standardPhone: String) = standardPhone.substring(0, 4) + "-" + standardPhone.substring(4, 7) + "-" + standardPhone.substring(7, standardPhone.size)

  def validateAmount(amount: Int): Boolean = chargeList.contains(amount)

  def lackOfCharacter(phoneNum: String): Boolean = Try(phoneNum.toLong).isSuccess

  def hasValidLength(phoneNum: String): Boolean = phoneNum.length == 11

  def trim(str: String): String = str.filter(_ > ' ')

  def isValid(phoneNum: String, prList: List[(String, Operator)]): Boolean =
    prList.filter(p ⇒ phoneNum.startsWith(p._1)).size > 0

  def findProvider(phoneNum: String, prList: List[(String, Operator)]): String = prList match {
    case l :: ls ⇒
      if (phoneNum.startsWith(l._1))
        mapOpToCode(l._2)
      else
        findProvider(phoneNum, ls)
    case Nil ⇒ "-1"
  }

  def extractNumberFromString(str: String): Option[Long] = {
    val num = str.split(" ").toList.filter(s ⇒ Try(s.toLong).isSuccess)
    if (num.size == 1)
      Some(num(0).toLong)
    else None
  }
}
package com.mesr.bot.helpers

trait StringMakers {

  def commandBuilder(visibleToUser: String, sendToServer: String): String = s"[$visibleToUser](send:/$sendToServer)"

  def newLine(): String = " \n"

  def newText(text: String): String = text + newLine

  def makeNewLineWithNumber(number: Int, text: String): String = s"$number. $text" + newLine

  def bold(text: String): String = s"*$text*"
}

object StringMaker extends StringMakers with Converter with OperatorExtension {

  val invalid_phone_number = "شماره تلفن *نامعتبر* است."
  val example = "مثال :"
  val example_phone = "0912-345-6789"
  val example_description = "شماره تلفن باید *۱۱ رقمی* و فاقد هرگونه *کاراکتر* باشد."
  val invalid_phone_length = "*تعداد ارقام* شماره موبایل مجاز نمی باشد."
  val empty_phone_number = "لطفا شماره موبایل را وارد کنید."
  val contains_character = "شماره موبایل *نمی تواند حاوی کاراکتر* باشد."
  val expect_start = "مشکلی رخ داده است."
  val TRY_AGAIN = "مشکلی در فرایند خرید رخ داده است. لطفا فرآیند را دوباره شروع کنید."
  val CHOOSE_CHARGE_OWNER = "لطفا دریافت کننده‌ی شارژ را مشخص کنید."
  val USE_BUTTONS = "لطفا از دکمه ها استفاده کنید."
  val enter_yourPhone_number = "لطفا *شماره موبایل* خود را وارد کنید."
  val CHOOSE_OPERATOR = "لطفا اپراتور را انتخاب کنید."
  val MULTIPLE_CONTACT = "مخاطب انتخابی شما *دارای چند شماره موبایل* می‌باشد. لطفا یکی از شماره‌های زیر را انتخاب کنید."
  val SELECT_SUBSCRIBER = "سلام لطفا دریافت کننده شارژ را مشخص کنید."
  val CHOOSE_AMOUNT = "لطفا مبلغ شارژ را مشخص کنید."
  val PIN_OR_DIRECT = "لطفا نوع شارژ را مشخص کنید."
  val THERE_ARE_NO_PRODUCTS = "در حال حاضر محصول مورد نظر شما وجود ندارد."
  val PROBLEM_WITH_PsPROXY = "در ارتباط با بانک مشکلی به وجود آمده است. لطفا دقایقی دیگر فرآیند خود را از طریق *دکمه‌ی شروع* ، مجددا آغاز کنید."
  val WOW_OR_ORDINARY = "لطفا نوع شارژ را انتخاب کنید"
  val DEFAULT_WOW = "شارژ مستقیم شگفت انگیز"
  val WOW_RIGHTELL = "شارژ مستقیم شور انگیز"
  val WOW_MCI = "شارژ مستقیم شگفت انگیز"
  val WOW_MTN = "شارژ مستقیم شگفت انگیز"
  val WOW_TALIA = "شارژ مستقیم شگفت انگیز"
  val ENTER_PHONE_NUMBER_AGAIN = "لطفا *شماره موبایل* را مجددا وارد کنید."

  val INVALID_PHONE_NUMBER: String = newText(invalid_phone_number) + newText(example_description) + newText(example) + newText(bold(example_phone)) + ENTER_PHONE_NUMBER_AGAIN
  val INVALID_PHONE_LENGTH: String = newText(invalid_phone_length) + newText(example_description) + newText(example) + newText(bold(example_phone)) + ENTER_PHONE_NUMBER_AGAIN
  val EMPTY_PHONE_NUMBER: String = newText(empty_phone_number) + newText(example_description) + newText(example) + newText(bold(example_phone)) + newText(ENTER_PHONE_NUMBER_AGAIN)
  val PHONE_NUMBER_CONTAINS_CHARACTER: String = newText(contains_character) + newText(example_description) + newText(example) + bold(example_phone)
  val ENTER_PHONE_NUMBER: String = newText(enter_yourPhone_number) + newLine + newText(example) + bold(example_phone)

  val ENTER_DAY_OF_FLIGHT: String = newText("لطفا *روز سفر* خود را وارد کنید.") + newText("*تاریخ به میلادی*") + newText("ورود مجاز اعدا بین *1 تا 31*")
  val ENTER_DAY_OF_FLIGHT_AGAIN = newText("روز وارد شده *معتبر نمیباشد*") + newText("لطفا روز سفر خود را مجددا وارد کنید") + newText("*تاریخ به میلادی*") + newText("ورود مجاز اعدا بین *1 تا 31*")
  val ENTER_MONTH_OF_FLIGHT = "لطفا ماه میلادی سفر خود را انتخاب کنید"
  val ENTER_MONTH_OF_FLIGHT_AGAIN = newText("ورودی *نامعتبر* است.") + newText("لطفا ماه میلادی را از *گزینه های زیر انتخاب کنید*")
  val ENTER_YEAR_OF_FLIGHT = "لطفا *سال میلادی سفر خود* را انتخاب کنید یا بصورت دستی وارد کنید."
  val ENTER_YEAR_OF_FLIGHT_AGAIN = newText("سال میلادی *معتبر* نیست") + newText("سال میلادی از 2019 شروع میشود.") + "لطفا *سال میلادی سفر خود* را مجددا وارد کنید."
  val SEND_SCAN_OF_YOUR_PASSPORT = "لطفا اسکن پاسپورت خود را در قالب عکس ارسال کیند."

  def YOUR_REQUEST_HAS_BEEN_REGISTERED_AND_WE_WILL_REACH_YOU_IN_A_FEW_HOURS(clientName: String) =
    bold(clientName) + " عزیز درخواست شما با موفقیت ثبت شد. در ساعات آتی کارشناسان ما به منظور تایید و پیگیری درخواست ثبت شده، با شما تماس خواهند گرفت."

  val PLEASE_TRY_AGAIN = "مشکلی به وجود آمده است. لطفا فرآیند را از نو آغاز کنید."

  val INVALID_INPUT_PLEASE_SEND_PASSPORT_SCAN = newText("ورودی نامعتبر است") + newText("لطفا *اسکن عکس پاسپورت* را مجددا ارسال کنید.")


  import spray.json._

  def questionWithNumber(num: Int, question: String) = s"${convertEnglishStringToPersian(num.toString)} - $question"

  def getPhoneFromJsonMessage(rawJson: String): List[String] = {
    val jsValue = rawJson.parseJson
    val jsArray = jsValue
      .asJsObject.fields.getOrElse("data", JsString(""))
      .asJsObject.fields.getOrElse("contact", JsString(""))
      .asJsObject.fields.getOrElse("phones", JsString(""))
    val multiplePhone = jsArray match {
      case array: JsArray =>
        array.elements.toList.map {
          case string: JsString => string.value
          case _ => "-1"
        }.filterNot(p ⇒ p == "-1")
      case _ => List.empty[String]
    }

    multiplePhone.filter(p ⇒ isValid(makePhoneStandard(p), providerList)).distinct
  }

}
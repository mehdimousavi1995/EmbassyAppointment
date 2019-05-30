package com.mesr.bot.util

import com.bot4s.telegram.models.{KeyboardButton, ReplyKeyboardMarkup}
import com.mesr.bot.Strings.BackToMainMenu

import scala.annotation.tailrec

object ButtonsUtils {

  def replyMarkupSingleRow(buttonColumn: Seq[KeyboardButton],
                           resizeKeyboard: Option[Boolean] = Some(true),
                           oneTimeKeyboard: Option[Boolean] = Some(true),
                           selective: Option[Boolean] = None): ReplyKeyboardMarkup =
    ReplyKeyboardMarkup.singleColumn(buttonColumn, resizeKeyboard, oneTimeKeyboard, selective)


  val dayOfMonthBtns = Some(ReplyKeyboardMarkup(
    Seq(
      (1 to 7).map(s => KeyboardButton(s.toString)).reverse,
      (8 to 14).map(s => KeyboardButton(s.toString)).reverse,
      (15 to 21).map(s => KeyboardButton(s.toString)).reverse,
      (22 to 28).map(s => KeyboardButton(s.toString)).reverse,
      (29 to 31).map(s => KeyboardButton(s.toString)).reverse,
      Seq(KeyboardButton(BackToMainMenu))
    ), Some(true), Some(true)))

  import com.mesr.bot.Strings._

  val monthOfYearBtns = Some(ReplyKeyboardMarkup(
    splitList(englishMonthLists).map { buttons =>
      buttons.map { s =>
        KeyboardButton(s._1.toString + ". " + s._2)
      }.reverse
    } ++ Seq(Seq(KeyboardButton(BackToMainMenu))), Some(true)
  ))

  @tailrec
  def splitList[A](list: Seq[A], n: Int = 3, result: List[List[A]] = List.empty): Seq[Seq[A]] = {
    if (list.size >= n)
      splitList(list.slice(n, list.size), n, list.take(n).toList :: result)
    else if (list.nonEmpty)
      (list :: result).reverse
    else result.reverse
  }


}

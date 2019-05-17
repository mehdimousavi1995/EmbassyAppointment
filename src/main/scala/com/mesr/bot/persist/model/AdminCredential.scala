package com.mesr.bot.persist.model

import java.time.LocalDateTime

import com.mesr.bot.util.TimeUtils


@SerialVersionUID(1L)
case class AdminCredential(userId: Int, fullName: String, createdAt: LocalDateTime = TimeUtils.now, deletedAt: Option[LocalDateTime] = None)
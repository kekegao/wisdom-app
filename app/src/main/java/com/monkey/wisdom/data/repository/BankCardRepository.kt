package com.monkey.wisdom.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.monkey.wisdom.core.common.Json
import com.monkey.wisdom.core.util.Formatters
import com.monkey.wisdom.data.model.BankCard

/**
 * 银行卡数据仓库。
 *
 * 说明：后端当前未提供银行卡接口（Web 端同样为纯本地模拟），此处用 SharedPreferences 落地，
 * 行为与 Web 端 localStorage 对齐；后续后端提供接口时只需替换本实现。
 */
interface BankCardRepository {

    /** 银行卡列表（最新添加的在前） */
    fun loadCards(): List<BankCard>

    /** 添加银行卡 */
    fun addCard(bank: String, cardNo: String, cardType: String = DEFAULT_CARD_TYPE, holder: String = DEFAULT_HOLDER): BankCard

    /** 解绑银行卡 */
    fun removeCard(id: Long): List<BankCard>

    companion object {
        const val DEFAULT_CARD_TYPE = "储蓄卡"
        const val DEFAULT_HOLDER = "本人"
    }
}

/**
 * SharedPreferences 实现：以 JSON 字符串整表存储，数据量小、读写简单。
 */
class BankCardRepositoryImpl(context: Context) : BankCardRepository {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    override fun loadCards(): List<BankCard> {
        val raw = prefs.getString(KEY_CARDS, null) ?: return emptyList()
        return runCatching {
            Json.gson.fromJson(raw, Array<BankCard>::class.java)?.toList().orEmpty()
        }.getOrDefault(emptyList())
    }

    override fun addCard(
        bank: String,
        cardNo: String,
        cardType: String,
        holder: String,
    ): BankCard {
        val cards = loadCards()
        val nextId = (cards.maxOfOrNull { it.id } ?: 0L) + 1L
        val card = BankCard(
            id = nextId,
            bank = bank,
            cardType = cardType.ifBlank { BankCardRepository.DEFAULT_CARD_TYPE },
            cardNo = cardNo,
            holder = holder.trim().ifBlank { BankCardRepository.DEFAULT_HOLDER },
            addTime = Formatters.nowText(),
        )
        save(listOf(card) + cards)
        return card
    }

    override fun removeCard(id: Long): List<BankCard> {
        val remaining = loadCards().filterNot { it.id == id }
        save(remaining)
        return remaining
    }

    private fun save(cards: List<BankCard>) {
        prefs.edit().putString(KEY_CARDS, Json.gson.toJson(cards)).apply()
    }

    private companion object {
        const val PREF_NAME = "wisdom_bank_cards"
        const val KEY_CARDS = "key_bank_cards"
    }
}

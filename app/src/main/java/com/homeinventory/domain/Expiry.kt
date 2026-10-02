package com.homeinventory.domain
import java.time.LocalDate
enum class ExpiryStatus { NO_EXPIRY, GOOD, SOON, EXPIRED }
fun expiryStatus(date: String?, today: LocalDate = LocalDate.now()): ExpiryStatus {
 val expiry = date?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return ExpiryStatus.NO_EXPIRY
 return when { expiry.isBefore(today) -> ExpiryStatus.EXPIRED; !expiry.isAfter(today.plusDays(7)) -> ExpiryStatus.SOON; else -> ExpiryStatus.GOOD }
}
fun datesValid(manufactured: String?, expiry: String?): Boolean = try {
 manufactured == null || expiry == null || !LocalDate.parse(expiry).isBefore(LocalDate.parse(manufactured))
} catch (_: Exception) { false }

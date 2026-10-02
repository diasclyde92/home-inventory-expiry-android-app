package com.homeinventory.domain
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class ExpiryTest {
 private val today = LocalDate.of(2026, 10, 1)
 @Test fun missingDateIsNoExpiry() = assertEquals(ExpiryStatus.NO_EXPIRY, expiryStatus(null, today))
 @Test fun pastDateIsExpired() = assertEquals(ExpiryStatus.EXPIRED, expiryStatus("2026-09-30", today))
 @Test fun todayAndSevenDaysAreSoon() { assertEquals(ExpiryStatus.SOON, expiryStatus("2026-10-01",today)); assertEquals(ExpiryStatus.SOON,expiryStatus("2026-10-08",today)) }
 @Test fun laterDateIsGood() = assertEquals(ExpiryStatus.GOOD, expiryStatus("2026-10-09",today))
 @Test fun validatesManufactureBeforeExpiry(){assertTrue(datesValid("2026-01-01","2026-01-01"));assertFalse(datesValid("2026-02-01","2026-01-01"))}
}

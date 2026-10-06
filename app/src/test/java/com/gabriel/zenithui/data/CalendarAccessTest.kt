package com.gabriel.zenithui.data

import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarAccessTest {

    @Test
    fun `once allowed, nothing else matters`() {
        for (declined in listOf(true, false)) for (refused in listOf(true, false)) for (again in listOf(true, false)) {
            assertEquals(CalendarAccess.Granted, CalendarAccess.of(granted = true, declined, refused, again))
        }
    }

    @Test
    fun `not allowed and never asked, it is offered with its reasons`() {
        assertEquals(CalendarAccess.Offer, CalendarAccess.of(granted = false, declined = false, refused = false, canAskAgain = false))
        assertEquals(CalendarAccess.Offer, CalendarAccess.of(granted = false, declined = false, refused = false, canAskAgain = true))
    }

    @Test
    fun `refused once, it can still be offered`() {
        assertEquals(CalendarAccess.Offer, CalendarAccess.of(granted = false, declined = false, refused = true, canAskAgain = true))
    }

    @Test
    fun `refused for good, only the phone's settings can allow it`() {
        assertEquals(CalendarAccess.Blocked, CalendarAccess.of(granted = false, declined = false, refused = true, canAskAgain = false))
        // Even after "Not now": the button was pressed again and Android would not ask.
        assertEquals(CalendarAccess.Blocked, CalendarAccess.of(granted = false, declined = true, refused = true, canAskAgain = false))
    }

    @Test
    fun `not now is remembered, and nobody is asked again until they ask`() {
        assertEquals(CalendarAccess.Declined, CalendarAccess.of(granted = false, declined = true, refused = false, canAskAgain = false))
        assertEquals(CalendarAccess.Declined, CalendarAccess.of(granted = false, declined = true, refused = true, canAskAgain = true))
    }
}

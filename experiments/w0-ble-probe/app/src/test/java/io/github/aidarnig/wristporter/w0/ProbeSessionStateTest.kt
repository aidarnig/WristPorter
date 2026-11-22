package io.github.aidarnig.wristporter.w0

import org.junit.Assert.*
import org.junit.Test

class ProbeSessionStateTest {
    @Test fun reconnectDoesNotReuseStaleServiceOrEventState() {
        val state = ProbeSessionState()
        state.newConnection()
        state.connected()
        state.servicesDiscovered(ancs = true, ams = true)
        state.subscription("ANCS", 0)
        state.subscription("AMS command", 0)
        state.subscription("AMS entity", 0)
        state.amsRegistration(0)
        state.amsRegistration(0)
        state.ancsEvent()
        state.amsEvent()

        assertTrue(state.summary().contains("ANCS present=true sub=0 events=1"))
        assertTrue(state.summary().contains("AMS registrations=2/2 events=1"))

        state.disconnected()
        state.newConnection()

        assertTrue(state.summary().contains("Session #2 link=connecting discoveries=0"))
        assertTrue(state.summary().contains("ANCS present=? sub=? events=0"))
        assertTrue(state.summary().contains("AMS present=? commandSub=? entitySub=?"))
        assertTrue(state.summary().contains("AMS registrations=0/0 events=0"))
    }

    @Test fun rediscoveryClearsSubscriptionsUntilAcknowledgedAgain() {
        val state = ProbeSessionState()
        state.newConnection()
        state.connected()
        state.servicesDiscovered(ancs = true, ams = true)
        state.subscription("AMS entity", 0)
        state.amsRegistration(0)
        state.amsEvent()

        state.servicesDiscovered(ancs = true, ams = false)

        assertTrue(state.summary().contains("discoveries=2"))
        assertTrue(state.summary().contains("AMS present=false commandSub=? entitySub=?"))
        assertTrue(state.summary().contains("AMS registrations=0/0 events=0"))
    }

    @Test fun failedRegistrationIsVisibleWithoutPayload() {
        val state = ProbeSessionState()
        state.newConnection()
        state.servicesDiscovered(ancs = false, ams = true)
        state.subscription("AMS entity", 5)
        state.amsRegistration(5)

        assertTrue(state.summary().contains("AMS present=true commandSub=? entitySub=5"))
        assertTrue(state.summary().contains("AMS registrations=0/1 events=0"))
    }
}

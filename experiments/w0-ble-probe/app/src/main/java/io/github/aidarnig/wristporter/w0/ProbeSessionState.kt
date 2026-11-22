package io.github.aidarnig.wristporter.w0

/**
 * One-screen privacy-safe status for the foreground W0 experiment.
 * Only protocol status codes, service presence and event counts are retained.
 */
internal class ProbeSessionState {
    var session = 0
        private set
    private var link = "idle"
    private var discoveries = 0
    private var ancsPresent: Boolean? = null
    private var amsPresent: Boolean? = null
    private var ancsSubscription: Int? = null
    private var amsCommandSubscription: Int? = null
    private var amsEntitySubscription: Int? = null
    private var amsRegistrationSuccesses = 0
    private var amsRegistrationAttempts = 0
    private var ancsEvents = 0
    private var amsEvents = 0

    fun newConnection() {
        session++
        link = "connecting"
        discoveries = 0
        clearServices()
    }

    fun connected() { link = "connected" }
    fun disconnected() { link = "disconnected" }

    fun servicesDiscovered(ancs: Boolean, ams: Boolean) {
        discoveries++
        clearServices()
        ancsPresent = ancs
        amsPresent = ams
    }

    fun subscription(kind: String, status: Int) {
        when (kind) {
            "ANCS" -> ancsSubscription = status
            "AMS command" -> amsCommandSubscription = status
            "AMS entity" -> amsEntitySubscription = status
        }
    }

    fun amsRegistration(status: Int) {
        amsRegistrationAttempts++
        if (status == 0) amsRegistrationSuccesses++
    }

    fun ancsEvent() { ancsEvents++ }
    fun amsEvent() { amsEvents++ }

    fun summary(): String =
        "Session #" + session + " link=" + link + " discoveries=" + discoveries + "\n" +
        "ANCS present=" + shown(ancsPresent) + " sub=" + shown(ancsSubscription) + " events=" + ancsEvents + "\n" +
        "AMS present=" + shown(amsPresent) + " commandSub=" + shown(amsCommandSubscription) +
        " entitySub=" + shown(amsEntitySubscription) + "\n" +
        "AMS registrations=" + amsRegistrationSuccesses + "/" + amsRegistrationAttempts +
        " events=" + amsEvents

    private fun clearServices() {
        ancsPresent = null
        amsPresent = null
        ancsSubscription = null
        amsCommandSubscription = null
        amsEntitySubscription = null
        amsRegistrationSuccesses = 0
        amsRegistrationAttempts = 0
        ancsEvents = 0
        amsEvents = 0
    }

    private fun shown(value: Any?): String = value?.toString() ?: "?"
}

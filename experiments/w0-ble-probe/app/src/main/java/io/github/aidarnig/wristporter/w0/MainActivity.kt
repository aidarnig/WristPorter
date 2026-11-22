package io.github.aidarnig.wristporter.w0

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.view.View
import android.view.ViewGroup
import java.util.ArrayDeque
import java.util.UUID

/**
 * Temporary, foreground-only hardware experiment for Issue #13.
 * Does not run a service, scan in background, log personal content or reconnect.
 * iPhone must be paired using explicit OS UI before GATT can be probed.
 */
class MainActivity : Activity() {
    private val ancsService = UUID.fromString("7905f431-b5ce-4e99-a40f-4b1e122d00d0")
    private val amsService = UUID.fromString("89d3502b-0f36-433a-8ef4-c502ad55f8dc")
    private val notificationSource = UUID.fromString("9fbf120d-6301-42d9-8c58-25e699a21dbd")
    private val remoteCommand = UUID.fromString("9b3c81d8-57b1-4a8a-b8df-0e56f7ca51c2")
    private val entityUpdate = UUID.fromString("2f7cabce-808d-411f-9a0c-bb92ba96c102")
    private val cccd = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    private val gattService = uuid16(0x1801)
    private val serviceChangedCharacteristic = uuid16(0x2a05)

    private data class Op(val characteristic: BluetoothGattCharacteristic, val subscribe: Boolean, val value: ByteArray)
    private val operationQueue = ArrayDeque<Op>()
    private var inFlight: Op? = null
    private var adapter: BluetoothAdapter? = null
    private var gatt: BluetoothGatt? = null
    private lateinit var status: TextView
    private lateinit var health: TextView
    private val diagnostics = ProbeSessionState()
    private var connected = false
    private var discoveryInProgress = false
    private var serviceRefreshPending = false
    private var serviceChangeRefreshUsed = false
    private val lines = ArrayDeque<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = getSystemService(BluetoothManager::class.java)?.adapter
        // Percentage insets and bottom scroll space keep the log readable on a round dial.
        val insets = RoundViewport.insets(
            resources.displayMetrics.widthPixels,
            resources.displayMetrics.density,
            resources.configuration.isScreenRound
        )
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(insets.side, insets.top, insets.side, 0)
        }
        status = TextView(this).apply { textSize = 12f }
        fun control(text: String, action: () -> Unit) {
            layout.addView(Button(this).apply {
                this.text = text
                setAllCaps(false)
                textSize = 12f
                maxLines = 2
                setOnClickListener { action() }
            })
        }
        control("1. Permissions") { requestAccess() }
        control("2. Discoverable (120s)") { requestDiscoverable() }
        control("3. Choose bonded phone") { showBondedDevices() }
        control("4. Disconnect") { disconnect("user request") }
        control("5. Refresh GATT") { requestServiceRefresh("manual") }
        health = TextView(this).apply {
            textSize = 11f
            setPadding(0, dp(10), 0, dp(8))
        }
        layout.addView(status)
        // The current session summary stays after the log, where it is easy to find.
        layout.addView(health)
        // Extra scrollable blank space lets the final lines move up from behind
        // the circular lower bezel into the center of the watch face.
        layout.addView(View(this), LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, insets.bottomScrollSlack
        ))
        setContentView(ScrollView(this).apply {
            addView(layout)
            isFillViewport = false
            isVerticalScrollBarEnabled = true
        })
        record("W0 Round Probe v0.0.3 (diagnostic only)")
        record("W0 experimental only; no phone data saved")
        record("Watch Android=${Build.VERSION.RELEASE} API=${Build.VERSION.SDK_INT}")
        record("Watch firmware=${Build.DISPLAY}")
        record("On iPhone: Settings > Bluetooth; confirm matching pairing code and allow notification sharing")
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()

    private fun hasAccess(): Boolean = Build.VERSION.SDK_INT < 31 ||
        (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
         checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED)

    private fun requestAccess() {
        if (Build.VERSION.SDK_INT >= 31 && !hasAccess()) {
            requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_ADVERTISE), 10)
        } else record("Nearby-device permission available")
    }

    override fun onRequestPermissionsResult(code: Int, permissions: Array<out String>, grants: IntArray) {
        super.onRequestPermissionsResult(code, permissions, grants)
        if (code == 10) record(if (hasAccess()) "Permissions granted" else "Permissions denied; try again via system settings")
    }

    private fun requestDiscoverable() {
        if (!hasAccess()) { record("Grant permissions first"); return }
        try {
            startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120)
            }, 11)
            record("Waiting for system discoverability consent (max 120s)")
        } catch (e: SecurityException) {
            record("Discoverability permission blocked")
        } catch (e: Exception) {
            record("Discoverability unavailable on this firmware")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 11) record(if (resultCode > 0) "Discoverability accepted for ${resultCode}s" else "Discoverability rejected")
    }

    private fun showBondedDevices() {
        if (!hasAccess()) { record("Grant permissions first"); return }
        val devices: List<BluetoothDevice> = try { adapter?.bondedDevices?.toList().orEmpty() } catch (_: SecurityException) { emptyList() }
        if (devices.isEmpty()) { record("No bonded devices visible; pair through iPhone Bluetooth settings first"); return }
        // Device names appear on the watch only. Never log names, addresses or identifiers.
        val labels = devices.mapIndexed { index, device ->
            "${index + 1}. ${try { device.name ?: "Unnamed device" } catch (_: SecurityException) { "Unnamed device" }}"
        }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Select YOUR iPhone only")
            .setItems(labels) { _, which ->
                AlertDialog.Builder(this)
                    .setTitle("Connect to selected device?")
                    .setMessage("Verify the selected entry is your own iPhone. No automatic device selection.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Connect") { _, _ -> connect(devices[which]) }
                    .show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun connect(device: BluetoothDevice) {
        if (!hasAccess()) { record("Permission missing"); return }
        disconnect("replace connection")
        // Do not mix previous-session GATT errors with the new connection.
        lines.clear()
        diagnostics.newConnection()
        connected = false
        discoveryInProgress = false
        serviceRefreshPending = false
        serviceChangeRefreshUsed = false
        try {
            gatt = device.connectGatt(this, false, callbacks, BluetoothDevice.TRANSPORT_LE)
            record("GATT connection requested (selected bonded device; LE transport)")
            if (gatt == null) record("GATT connection request returned null")
        } catch (_: SecurityException) {
            record("GATT connection denied by OS permission")
        } catch (_: Exception) {
            record("GATT request failed")
        }
    }

    private val callbacks = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(connection: BluetoothGatt, statusCode: Int, newState: Int) {
            runOnUiThread {
                if (gatt !== connection) return@runOnUiThread
                record("Link state=${if (newState == BluetoothProfile.STATE_CONNECTED) "connected" else "disconnected"}, GATT status=$statusCode")
                if (statusCode == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                    connected = true
                    diagnostics.connected()
                    requestServiceRefresh("new GATT link")
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    disconnect("remote disconnected; no auto-retry")
                }
            }
        }

        override fun onServiceChanged(connection: BluetoothGatt) {
            runOnUiThread {
                if (gatt !== connection) return@runOnUiThread
                serviceChanged("Android onServiceChanged")
            }
        }

        override fun onServicesDiscovered(connection: BluetoothGatt, statusCode: Int) {
            runOnUiThread {
                if (gatt !== connection) return@runOnUiThread
                discoveryInProgress = false
                if (statusCode != BluetoothGatt.GATT_SUCCESS) {
                    record("Service discovery failed status=$statusCode")
                    requestDeferredRefreshIfReady()
                    return@runOnUiThread
                }
                val ids = connection.services.map { it.uuid }.toSet()
                diagnostics.servicesDiscovered(ancsService in ids, amsService in ids)
                record("ANCS present=" + (ancsService in ids) + "; AMS present=" + (amsService in ids))
                record("Optional GATT: Battery=" + (uuid16(0x180f) in ids) +
                       ", DeviceInfo=" + (uuid16(0x180a) in ids) + ", Time=" + (uuid16(0x1805) in ids))
                val changed = connection.getService(gattService)?.getCharacteristic(serviceChangedCharacteristic)
                if (changed != null) subscribe(changed, indication = true)
                else record("GATT Service Changed characteristic not exposed")
                subscribe(connection.getService(ancsService)?.getCharacteristic(notificationSource))
                subscribe(connection.getService(amsService)?.getCharacteristic(remoteCommand))
                subscribe(connection.getService(amsService)?.getCharacteristic(entityUpdate))
                // AMS registration writes follow a successful Entity Update CCCD response.
                // Re-register after each discovery because iOS may republish AMS.
                runNext()
            }
        }

        override fun onDescriptorWrite(connection: BluetoothGatt, descriptor: BluetoothGattDescriptor, statusCode: Int) {
            runOnUiThread {
                if (gatt !== connection) return@runOnUiThread
                val characteristic = descriptor.characteristic
                if (inFlight?.subscribe != true || inFlight?.characteristic?.uuid != characteristic.uuid) return@runOnUiThread
                record("Subscribed " + serviceLabel(characteristic.uuid) + " status=$statusCode")
                when (characteristic.uuid) {
                    notificationSource -> diagnostics.subscription("ANCS", statusCode)
                    remoteCommand -> diagnostics.subscription("AMS command", statusCode)
                    entityUpdate -> {
                        diagnostics.subscription("AMS entity", statusCode)
                        if (statusCode == BluetoothGatt.GATT_SUCCESS) {
                            operationQueue.addLast(Op(characteristic, false, byteArrayOf(0, 0, 1))) // Player name and playback status
                            operationQueue.addLast(Op(characteristic, false, byteArrayOf(2, 2))) // Track title; never display the value
                        }
                    }
                }
                inFlight = null
                runNext()
            }
        }

        override fun onCharacteristicWrite(connection: BluetoothGatt, characteristic: BluetoothGattCharacteristic, statusCode: Int) {
            runOnUiThread {
                if (gatt !== connection) return@runOnUiThread
                if (inFlight?.subscribe != false || inFlight?.characteristic?.uuid != characteristic.uuid) return@runOnUiThread
                record("AMS metadata subscription request status=$statusCode")
                diagnostics.amsRegistration(statusCode)
                inFlight = null
                runNext()
            }
        }

        override fun onCharacteristicChanged(connection: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            received(connection, characteristic.uuid, value)
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(connection: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            if (Build.VERSION.SDK_INT < 33) received(connection, characteristic.uuid, characteristic.value?.copyOf() ?: byteArrayOf())
        }
    }

    private fun received(connection: BluetoothGatt, characteristic: UUID, data: ByteArray) {
        runOnUiThread {
            if (gatt !== connection) return@runOnUiThread
            if (characteristic == serviceChangedCharacteristic) {
                serviceChanged("GATT indication")
                return@runOnUiThread
            }
            val eventSummary = when (characteristic) {
                notificationSource -> {
                    diagnostics.ancsEvent()
                    ProbeSummaries.ancs(data)
                }
                remoteCommand -> ProbeSummaries.amsCommands(data)
                entityUpdate -> {
                    diagnostics.amsEvent()
                    ProbeSummaries.amsEntity(data)
                }
                else -> return@runOnUiThread
            }
            record(eventSummary) // No raw bytes, UID, text or remote identity.
        }
    }

    private fun subscribe(ch: BluetoothGattCharacteristic?, indication: Boolean = false) {
        if (ch != null) {
            val value = if (indication) BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                        else BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            operationQueue.addLast(Op(ch, true, value))
        } else record("One protocol characteristic not exposed")
    }

    private fun runNext() {
        val connection = gatt ?: return
        if (inFlight != null) return
        while (operationQueue.isNotEmpty()) {
            val op = operationQueue.removeFirst()
            val started = try {
                if (op.subscribe) {
                    val descriptor = op.characteristic.getDescriptor(cccd)
                    if (descriptor == null || !connection.setCharacteristicNotification(op.characteristic, true)) false
                    else writeDescriptor(connection, descriptor, op.value)
                } else writeCharacteristic(connection, op.characteristic, op.value)
            } catch (_: SecurityException) { false } catch (_: IllegalArgumentException) { false }
            if (started) { inFlight = op; return }
            record("GATT operation could not start (" + serviceLabel(op.characteristic.uuid) + ")")
        }
        requestDeferredRefreshIfReady()
    }

    private fun serviceChanged(source: String) {
        record("GATT services changed: $source")
        if (!serviceChangeRefreshUsed) {
            serviceChangeRefreshUsed = true // One automatic refresh maximum per selected GATT connection.
            requestServiceRefresh("service change")
        } else record("Additional change: use manual Refresh GATT services")
    }

    private fun requestServiceRefresh(reason: String) {
        val connection = gatt
        if (connection == null || !connected) {
            record("GATT refresh unavailable: select bonded phone first")
            return
        }
        if (discoveryInProgress || inFlight != null || operationQueue.isNotEmpty()) {
            serviceRefreshPending = true
            record("GATT refresh queued ($reason)")
            return
        }
        serviceRefreshPending = false
        discoveryInProgress = true
        record("GATT service discovery requested ($reason)")
        try {
            if (!connection.discoverServices()) {
                discoveryInProgress = false
                record("GATT service discovery request refused")
            }
        } catch (_: SecurityException) {
            discoveryInProgress = false
            record("GATT service discovery permission denied")
        } catch (_: IllegalStateException) {
            discoveryInProgress = false
            record("GATT service discovery unavailable")
        }
    }

    private fun requestDeferredRefreshIfReady() {
        if (serviceRefreshPending && !discoveryInProgress && inFlight == null && operationQueue.isEmpty()) {
            requestServiceRefresh("deferred service change")
        }
    }

    @Suppress("DEPRECATION")
    private fun writeDescriptor(connection: BluetoothGatt, descriptor: BluetoothGattDescriptor, value: ByteArray): Boolean {
        return if (Build.VERSION.SDK_INT >= 33) connection.writeDescriptor(descriptor, value) == BluetoothGatt.GATT_SUCCESS
        else { descriptor.value = value; connection.writeDescriptor(descriptor) }
    }

    @Suppress("DEPRECATION")
    private fun writeCharacteristic(connection: BluetoothGatt, ch: BluetoothGattCharacteristic, value: ByteArray): Boolean {
        return if (Build.VERSION.SDK_INT >= 33) connection.writeCharacteristic(ch, value, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT) == BluetoothGatt.GATT_SUCCESS
        else { ch.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT; ch.value = value; connection.writeCharacteristic(ch) }
    }

    private fun disconnect(reason: String) {
        operationQueue.clear()
        inFlight = null
        val old = gatt
        gatt = null
        connected = false
        discoveryInProgress = false
        serviceRefreshPending = false
        serviceChangeRefreshUsed = false
        diagnostics.disconnected()
        try { old?.disconnect() } catch (_: Exception) { /* permission may have been revoked */ }
        try { old?.close() } catch (_: Exception) { /* ignore */ }
        if (old != null) record("Disconnected: $reason")
    }

    private fun serviceLabel(id: UUID): String = when (id) {
        notificationSource -> "ANCS Notification Source"
        remoteCommand -> "AMS Remote Command"
        entityUpdate -> "AMS Entity Update"
        serviceChangedCharacteristic -> "GATT Service Changed"
        else -> "unknown characteristic"
    }

    private fun uuid16(value: Int): UUID = UUID.fromString("%08x-0000-1000-8000-00805f9b34fb".format(value))

    private fun record(message: String) {
        lines.addLast(message)
        while (lines.size > 60) lines.removeFirst()
        status.text = lines.joinToString("\n")
        if (::health.isInitialized) health.text = "v0.0.3 round | Latest session\n" + diagnostics.summary()
    }

    override fun onDestroy() {
        disconnect("activity stopped")
        super.onDestroy()
    }
}

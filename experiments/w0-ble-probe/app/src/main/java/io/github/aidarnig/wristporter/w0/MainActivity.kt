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

    private data class Op(val characteristic: BluetoothGattCharacteristic, val subscribe: Boolean, val value: ByteArray)
    private val operationQueue = ArrayDeque<Op>()
    private var inFlight: Op? = null
    private var adapter: BluetoothAdapter? = null
    private var gatt: BluetoothGatt? = null
    private lateinit var status: TextView
    private val lines = ArrayDeque<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = getSystemService(BluetoothManager::class.java)?.adapter
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 8, 12, 8)
        }
        status = TextView(this).apply { textSize = 12f }
        fun control(text: String, action: () -> Unit) {
            layout.addView(Button(this).apply {
                this.text = text
                setOnClickListener { action() }
            })
        }
        control("1. Permissions") { requestAccess() }
        control("2. Discoverable (120s)") { requestDiscoverable() }
        control("3. Choose bonded phone") { showBondedDevices() }
        control("4. Disconnect") { disconnect("user request") }
        layout.addView(status)
        setContentView(ScrollView(this).apply { addView(layout) })
        record("W0 experimental only; no phone data saved")
        record("Watch Android=${Build.VERSION.RELEASE} API=${Build.VERSION.SDK_INT}")
        record("Watch firmware=${Build.DISPLAY}")
        record("On iPhone: Settings > Bluetooth; confirm matching pairing code and allow notification sharing")
    }

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
                    try {
                        if (!connection.discoverServices()) record("Service discovery start refused")
                    } catch (_: SecurityException) { record("Service discovery permission denied") }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    disconnect("remote disconnected; no auto-retry")
                }
            }
        }

        override fun onServicesDiscovered(connection: BluetoothGatt, statusCode: Int) {
            runOnUiThread {
                if (gatt !== connection) return@runOnUiThread
                if (statusCode != BluetoothGatt.GATT_SUCCESS) {
                    record("Service discovery failed status=$statusCode"); return@runOnUiThread
                }
                val ids = connection.services.map { it.uuid }.toSet()
                record("ANCS present=${ancsService in ids}; AMS present=${amsService in ids}")
                record("Optional GATT: Battery=${uuid16(0x180f) in ids}, DeviceInfo=${uuid16(0x180a) in ids}, Time=${uuid16(0x1805) in ids}")
                val ancs = connection.getService(ancsService)
                val ams = connection.getService(amsService)
                subscribe(ancs?.getCharacteristic(notificationSource))
                subscribe(ams?.getCharacteristic(remoteCommand))
                subscribe(ams?.getCharacteristic(entityUpdate))
                // Subscribe to Entity Update first, then register monitoring of Player and Track.
                ams?.getCharacteristic(entityUpdate)?.let { ch ->
                    operationQueue.addLast(Op(ch, false, byteArrayOf(0, 0, 1))) // Player name + playback status
                    operationQueue.addLast(Op(ch, false, byteArrayOf(2, 2)))    // Track title: event only, never displayed
                }
                runNext()
            }
        }

        override fun onDescriptorWrite(connection: BluetoothGatt, descriptor: BluetoothGattDescriptor, statusCode: Int) {
            runOnUiThread {
                if (gatt !== connection) return@runOnUiThread
                if (inFlight?.subscribe != true || inFlight?.characteristic?.uuid != descriptor.characteristic.uuid) return@runOnUiThread
                record("Subscribed ${serviceLabel(descriptor.characteristic.uuid)} status=$statusCode")
                inFlight = null
                runNext()
            }
        }

        override fun onCharacteristicWrite(connection: BluetoothGatt, characteristic: BluetoothGattCharacteristic, statusCode: Int) {
            runOnUiThread {
                if (gatt !== connection) return@runOnUiThread
                if (inFlight?.subscribe != false || inFlight?.characteristic?.uuid != characteristic.uuid) return@runOnUiThread
                record("AMS metadata subscription request status=$statusCode")
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
            val summary = when (characteristic) {
                notificationSource -> ProbeSummaries.ancs(data)
                remoteCommand -> ProbeSummaries.amsCommands(data)
                entityUpdate -> ProbeSummaries.amsEntity(data)
                else -> return@runOnUiThread
            }
            record(summary) // No raw bytes, UID, text or remote identity.
        }
    }

    private fun subscribe(ch: BluetoothGattCharacteristic?) {
        if (ch != null) operationQueue.addLast(Op(ch, true, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE))
        else record("One optional protocol characteristic not exposed")
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
            record("GATT operation could not start (${serviceLabel(op.characteristic.uuid)})")
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
        try { old?.disconnect() } catch (_: Exception) { /* permission may have been revoked */ }
        try { old?.close() } catch (_: Exception) { /* ignore */ }
        if (old != null) record("Disconnected: $reason")
    }

    private fun serviceLabel(id: UUID): String = when (id) {
        notificationSource -> "ANCS Notification Source"
        remoteCommand -> "AMS Remote Command"
        entityUpdate -> "AMS Entity Update"
        else -> "unknown characteristic"
    }

    private fun uuid16(value: Int): UUID = UUID.fromString("%08x-0000-1000-8000-00805f9b34fb".format(value))

    private fun record(message: String) {
        lines.addLast(message)
        while (lines.size > 60) lines.removeFirst()
        status.text = lines.joinToString("\n")
    }

    override fun onDestroy() {
        disconnect("activity stopped")
        super.onDestroy()
    }
}

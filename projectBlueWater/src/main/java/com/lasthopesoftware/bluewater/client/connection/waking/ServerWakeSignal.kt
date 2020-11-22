package com.lasthopesoftware.bluewater.client.connection.waking

import com.lasthopesoftware.bluewater.client.connection.SendPackets
import com.lasthopesoftware.bluewater.shared.promises.PromisePolicies
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.projectblue.shared.promises.PromiseDelay.delay
import org.joda.time.Duration

class ServerWakeSignal(private val packetSender: SendPackets) : PokeServer {
	override fun promiseWakeSignal(machineAddress: MachineAddress, timesToSendSignal: Int, durationBetween: Duration): Promise<Unit> {
		val macBytes = getMacBytes(machineAddress.macAddress)
		val bytes = ByteArray(6 + 16 * macBytes.size)
		for (i in 0..5) {
			bytes[i] = 0xff.toByte()
		}

		for (i in 6 until bytes.size step macBytes.size) {
			System.arraycopy(macBytes, 0, bytes, i, macBytes.size)
        }

		return PromisePolicies.repeat({
			packetSender.promiseSentPackets(machineAddress.host, wakePort, bytes)
				.eventually { delay<Unit>(durationBetween) }
		}, timesToSendSignal)
	}

	companion object {
		private const val wakePort = 9

		private fun getMacBytes(macStr: String): ByteArray {
			val hex = macStr.split(":", "-")
			require(hex.size == 6) { "Invalid MAC address." }
			try {
				return hex.map { it.toInt(16).toByte() }.toByteArray()
			} catch (e: NumberFormatException) {
				throw IllegalArgumentException("Invalid hex digit in MAC address.")
			}
		}
	}
}

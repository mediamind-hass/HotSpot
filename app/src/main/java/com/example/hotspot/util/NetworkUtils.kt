package com.example.hotspot.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log

object NetworkUtils {

    private const val TAG = "NetworkUtils"

    /**
     * Forza il processo dell'app ad utilizzare l'interfaccia Wi-Fi per il traffico di rete.
     * Questo risolve il problema su Samsung (One UI / Android 14) dove i Dati Mobili (4G/5G)
     * scavalcano il Wi-Fi locale quando la rete MikroTik non ha ancora la connessione Internet convalidata.
     */
    fun forceWifiNetwork(context: Context) {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            // Verifica della rete attiva corrente
            val activeNetwork = cm.activeNetwork
            if (activeNetwork != null) {
                val caps = cm.getNetworkCapabilities(activeNetwork)
                if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    cm.bindProcessToNetwork(activeNetwork)
                    Log.d(TAG, "Processo vincolato alla rete Wi-Fi attiva: $activeNetwork")
                }
            }

            // Registra una richiesta per catturare l'interfaccia Wi-Fi
            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build()

            cm.requestNetwork(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    try {
                        cm.bindProcessToNetwork(network)
                        Log.d(TAG, "Processo vincolato via callback alla rete Wi-Fi: $network")
                    } catch (e: Exception) {
                        Log.e(TAG, "Errore nel binding Wi-Fi", e)
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Errore nella forzatura rete Wi-Fi", e)
        }
    }
}

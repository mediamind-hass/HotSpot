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
     * Risolve il problema su Samsung One UI (inclusa la One UI 9 / Android 15/16) in cui i Dati Mobili
     * o la verifica di connettività scavalcano il Wi-Fi locale quando non ha ancora accesso a Internet.
     */
    fun forceWifiNetwork(context: Context) {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            // 1. Scansione di tutte le reti per trovare immediatamente l'interfaccia Wi-Fi
            @Suppress("DEPRECATION")
            val networks = cm.allNetworks
            for (network in networks) {
                val caps = cm.getNetworkCapabilities(network) ?: continue
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    cm.bindProcessToNetwork(network)
                    Log.d(TAG, "Processo vincolato direttamente alla rete Wi-Fi: $network")
                    break
                }
            }

            // 2. Richiesta specifica per il Wi-Fi RIMUOVENDO l'obbligo di NET_CAPABILITY_INTERNET.
            // Questo è FONDAMENTALE su One UI 6/7/9 per consentire connessioni a reti locali senza Internet.
            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            cm.requestNetwork(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    try {
                        cm.bindProcessToNetwork(network)
                        Log.d(TAG, "Processo vincolato via callback alla rete Wi-Fi (Senza Internet): $network")
                    } catch (e: Exception) {
                        Log.e(TAG, "Errore nel binding Wi-Fi via callback", e)
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Errore nella forzatura rete Wi-Fi", e)
        }
    }
}

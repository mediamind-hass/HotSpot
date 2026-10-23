package com.example.hotspot.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log

object NetworkUtils {

    private const val TAG = "NetworkUtils"

    var currentWifiNetwork: Network? = null
        private set

    /**
     * Tenta il binding del processo e cattura la rete Wi-Fi attiva per openConnection/socketFactory.
     */
    fun forceWifiNetwork(context: Context) {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            @Suppress("DEPRECATION")
            val networks = cm.allNetworks
            for (network in networks) {
                val caps = cm.getNetworkCapabilities(network) ?: continue
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    currentWifiNetwork = network
                    try {
                        cm.bindProcessToNetwork(network)
                    } catch (_: Exception) {}
                    Log.d(TAG, "Rete Wi-Fi catturata e vincolata: $network")
                    return
                }
            }

            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            cm.requestNetwork(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    try {
                        currentWifiNetwork = network
                        cm.bindProcessToNetwork(network)
                        Log.d(TAG, "Rete Wi-Fi catturata via callback: $network")
                    } catch (e: Exception) {
                        Log.e(TAG, "Errore nel binding Wi-Fi", e)
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Errore nella forzatura rete Wi-Fi", e)
        }
    }

    /**
     * Ripristina il routing di rete predefinito di Android.
     */
    fun unbindNetwork(context: Context) {
        try {
            currentWifiNetwork = null
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            cm.bindProcessToNetwork(null)
            Log.d(TAG, "Ripristinato il routing di rete predefinito (unbind)")
        } catch (e: Exception) {
            Log.e(TAG, "Errore durante unbind rete", e)
        }
    }
}

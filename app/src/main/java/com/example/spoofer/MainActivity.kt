package com.example.spoofer


import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.activity.ComponentActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import com.example.spoofer.services.MyVpnService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.*
import java.net.URL

class MainActivity : ComponentActivity() {
    // Variables pour nos éléments d'interface
    private lateinit var statusTextView: TextView
    private lateinit var connectButton: Button
    private lateinit var textApp: TextView
    private lateinit var  ipText: TextView

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            startVpnService()
        } else {
            statusTextView.text = "Permission refusée"
        }
    }


    // Variable pour suivre l'état de connexion
    private var isConnected = true

    //Scope pour les coroutines (tâches asynchrones)
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Liaison avec les éléments du layout XML
        statusTextView = findViewById(R.id.statusTextView)
        connectButton = findViewById(R.id.connectButton)
        textApp = findViewById(R.id.titleTextView)
        ipText = findViewById(R.id.ipTextView)

        // Configuration du bouton
        connectButton.setOnClickListener {
            toggleVpnConnection()
        }

        //Récupérer l'adresse ip au démarrage
        fetchPublicIp()
    }

    public fun fetchPublicIp(){
        scope.launch {
            try {
                ipText.text = "Chargement de l'adresse ip publique..."

                val ip = withContext(Dispatchers.IO){
                    URL("https://api.ipify.org").readText()
                }

                ipText.text = "ip actuelle: $ip"
            }
            catch (e: Exception){
                ipText.text = "ip actuelle: Erreur de connexion"
                e.printStackTrace()
            }
        }
    }

    //TODO Régler le problème du bouton car il ne change pas correctement l'état de l'application et de la connection vpn (Vue + connection)

    // Fonction pour démarrer le service VPN
    private fun startVpnService() {
        val intent = Intent(this, MyVpnService::class.java)
        startService(intent)

        // Mettre à jour l'UI
        isConnected = true
        statusTextView.text = "Statut : Connecté ✓"
        statusTextView.setTextColor(getColor(android.R.color.holo_green_dark))
        connectButton.text = "Se déconnecter"
        connectButton.setBackgroundColor(getColor(android.R.color.holo_red_light))
        textApp.setTextColor(getColor(android.R.color.holo_green_dark))
    }

    private fun stopVpnService(){
        val intent = Intent(this, MyVpnService::class.java)
        stopService(intent)

        // Mise à jour de l'ui
        isConnected = false
        statusTextView.text = "Statut: Déconnecté"
        statusTextView.setTextColor(getColor(android.R.color.holo_red_dark))
        connectButton.text = "Se connecter"
        connectButton.setBackgroundColor(getColor(android.R.color.holo_green_light))
        textApp.setTextColor(getColor(android.R.color.holo_red_light))
    }

    override fun onDestroy() {
        super.onDestroy()
        // Annuler les tâches en cours quand l'activité est détruite
        scope.cancel()
    }

    /**
     * Fonction qui change l'état de connexion
     * Pour l'instant, elle change juste l'affichage
     * Plus tard, elle démarrera le vrai service VPN
     */
    @SuppressLint("SetTextI18n")
    private fun toggleVpnConnection() {
        isConnected = !isConnected

        if (isConnected) {
            // État connecté
            val response = VpnService.prepare(this)
            if (response != null) {
                vpnPermissionLauncher.launch(response)
            } else {
                // Déconnecter
                stopVpnService()
            }
        }else{
            isConnected = false
            statusTextView.text = "Statut: Déconnecté"
            statusTextView.setTextColor(getColor(android.R.color.holo_red_dark))
            connectButton.text = "Se connecter"
            connectButton.setBackgroundColor(getColor(android.R.color.holo_green_light))
            textApp.setTextColor(getColor(android.R.color.holo_red_light))
        }
    }
}
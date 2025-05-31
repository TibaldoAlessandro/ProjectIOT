# Controllo veicolo IoT
Applicazione Android sviluppata in Kotlin che permette la gestione della propria automobile da remoto.

## Funzionalità principali
- Monitoraggio posizione auto: tramite GPS l'utente può conoscere la posizione della propria automobile su una mappa interattiva.
- Dashboard stato veicolo: controllo dello stato delle porte e presenza passeggeri nel mezzo.
- Gestione antifurto: attivazione e disattivazione antifurto da remoto.
- Notifiche di distanza: attivazione di avvisi in caso di allontanamento dall'auto con antifurto disattivato.

## Screenshot
![Screenshot_20250530_225119](https://github.com/user-attachments/assets/7c7fff46-31ca-40f2-8277-bc0d46676487)

## Installazione
1. Clona il repo:

   ```bash
   git clone https://github.com/TibaldoAlessandro/ProjectIOT.git
   ```

2. Aggiorna l'indirizzo IP nel file MqttService.kt:

   ```kotlin
     private const val BROKER_URL = "tcp://X.X.X.X:1883" // Inserisci l'indirizzo IP del MQTT Broker (guardare IP della VM con Debian)
   ```

3. Esegui il codice.

## Autori
Basso Giovanni e Tibaldo Alessandro

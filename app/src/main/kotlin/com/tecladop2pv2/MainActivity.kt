package com.tecladop2pv2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PairingScreen(modifier = Modifier)
                }
            }
        }
    }
}

@Composable
private fun PairingScreen(modifier: Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Desconectado",
            color = Color(0xFFB3261E),
            style = MaterialTheme.typography.titleLarge
        )

        Text("O estado também é anunciado por leitores de tela; a cor não é o único indicador.")

        Text(
            "Pareamento por código e QR ainda não está habilitado porque não há servidor de sinalização HTTPS/WSS configurado. Nenhuma conexão é simulada e nenhum texto sai deste dispositivo."
        )

        OutlinedButton(
            onClick = {},
            enabled = false,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Gerar código / QR indisponível")
        }

        Text(
            text = "Para habilitar: implante um backend de sinalização temporária autenticado, configure HTTPS/WSS e TURN com credenciais efêmeras. Nunca coloque segredos no APK.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

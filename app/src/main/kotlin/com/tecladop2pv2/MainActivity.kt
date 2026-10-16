package com.tecladop2pv2

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Fundo = Color(0xFF121212)
private val FundoTopo = Color(0xFF222222)
private val TextoPrincipal = Color(0xFFF5F5F5)
private val TextoSecundario = Color(0xFFAAAAAA)
private val Azul = Color(0xFF9DBDFF)

class MainActivity : ComponentActivity() {

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {
        MaterialTheme(
            colorScheme = darkColorScheme(
                background = Fundo,
                surface = Fundo,
                primary = Azul,
                onBackground = TextoPrincipal,
                onSurface = TextoPrincipal
            )
        ) {
            TelaConfiguracoes(
                abrirSeletorTeclado = {
                    val intent = Intent("android.settings.INPUT_METHOD_SETTINGS")
                    startActivity(intent)
                }
            )
        }
    }
}

}

private data class OpcaoConfiguracao(
val titulo: String,
val descricao: String = "",
val icone: String
)

@Composable
private fun TelaConfiguracoes(
abrirSeletorTeclado: () -> Unit
) {
var telaSelecionada by remember { mutableStateOf<String?>(null) }
var mostrarPareamento by remember { mutableStateOf(false) }

val opcoes = listOf(
    OpcaoConfiguracao("Conta", "Configurações do usuário", "●"),
    OpcaoConfiguracao("Idiomas", "Idioma e layout do teclado", "◎"),
    OpcaoConfiguracao("Temas", "Cores e aparência", "◉"),
    OpcaoConfiguracao("Digitação", "Maiúsculas e comportamento das teclas", "T"),
    OpcaoConfiguracao("Emoji", "Configurações de emojis", "☺"),
    OpcaoConfiguracao("Entrada avançada", "Recursos adicionais", "♩"),
    OpcaoConfiguracao("Layout e teclas", "Personalizar as teclas", "▦"),
    OpcaoConfiguracao("Som e vibração", "Feedback ao pressionar teclas", "♫"),
    OpcaoConfiguracao("Privacidade", "Informações sobre seus dados", "◇"),
    OpcaoConfiguracao("Ajuda e Feedback", "Ajuda sobre o aplicativo", "?"),
    OpcaoConfiguracao("Sobre o Teclado P2P V2", "Versão e informações", "ⓘ")
)

if (mostrarPareamento) {
    TelaPareamento(
        voltar = { mostrarPareamento = false }
    )
    return
}

if (telaSelecionada != null) {
    TelaOpcao(
        titulo = telaSelecionada!!,
        voltar = { telaSelecionada = null }
    )
    return
}

Box(
    modifier = Modifier
        .fillMaxSize()
        .background(Fundo)
) {
    Column(modifier = Modifier.fillMaxSize()) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(FundoTopo)
                .height(76.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = { mostrarPareamento = true },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.defaultMinSize(minWidth = 1.dp, minHeight = 1.dp)
            ) {
                Text(
                    text = "QR",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Azul
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Teclado P2P V2",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = TextoPrincipal
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 90.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp)
                ) {
                    Text(
                        text = "Configurações do teclado",
                        fontSize = 14.sp,
                        color = TextoSecundario
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Personalize sua experiência de digitação.",
                        fontSize = 17.sp,
                        color = TextoPrincipal
                    )
                }
            }

            items(opcoes) { opcao ->
                LinhaConfiguracao(
                    opcao = opcao,
                    aoClicar = { telaSelecionada = opcao.titulo }
                )
            }
        }
    }

    FloatingActionButton(
        onClick = abrirSeletorTeclado,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(20.dp),
        shape = RoundedCornerShape(20.dp),
        containerColor = Azul,
        contentColor = Color(0xFF101820)
    ) {
        Text(
            text = "⌨",
            fontSize = 28.sp
        )
    }
}

}

@Composable
private fun TelaPareamento(
voltar: () -> Unit
) {
Column(
modifier = Modifier
.fillMaxSize()
.background(Fundo)
) {
Row(
modifier = Modifier
.fillMaxWidth()
.background(FundoTopo)
.height(64.dp)
.clickable(onClick = voltar)
.padding(horizontal = 20.dp),
verticalAlignment = Alignment.CenterVertically
) {
Text(
text = "‹",
fontSize = 32.sp,
color = TextoPrincipal
)

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = "Pareamento P2P",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = TextoPrincipal
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Conectar dispositivo",
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold,
            color = TextoPrincipal,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Use o QR Code para iniciar o pareamento com outro dispositivo.",
            fontSize = 14.sp,
            color = TextoSecundario,
            textAlign = TextAlign.Center,
            lineHeight = 21.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Espaço reservado para o QR Code real.
        Box(
            modifier = Modifier
                .size(220.dp)
                .background(Color.White, RoundedCornerShape(16.dp))
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "QR",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF202020)
                )

                Text(
                    text = "Aguardando sessão",
                    fontSize = 12.sp,
                    color = Color(0xFF555555),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Nenhuma sessão de pareamento foi criada.",
            fontSize = 14.sp,
            color = TextoSecundario,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "O QR Code será gerado quando o serviço de sinalização estiver configurado.",
            fontSize = 12.sp,
            color = TextoSecundario,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = voltar,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Voltar",
                color = Azul
            )
        }
    }
}

}

@Composable
private fun LinhaConfiguracao(
opcao: OpcaoConfiguracao,
aoClicar: () -> Unit
) {
Row(
modifier = Modifier
.fillMaxWidth()
.clickable(onClick = aoClicar)
.heightIn(min = 76.dp)
.padding(horizontal = 24.dp, vertical = 12.dp),
verticalAlignment = Alignment.CenterVertically
) {
Box(
modifier = Modifier.size(42.dp),
contentAlignment = Alignment.Center
) {
Text(
text = opcao.icone,
fontSize = 28.sp,
color = TextoPrincipal
)
}

    Spacer(modifier = Modifier.width(24.dp))

    Column(modifier = Modifier.weight(1f)) {
        Text(
            text = opcao.titulo,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = TextoPrincipal
        )

        if (opcao.descricao.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = opcao.descricao,
                fontSize = 12.sp,
                color = TextoSecundario
            )
        }
    }
}

}

@Composable
private fun TelaOpcao(
titulo: String,
voltar: () -> Unit
) {
Column(
modifier = Modifier
.fillMaxSize()
.background(Fundo)
) {
Row(
modifier = Modifier
.fillMaxWidth()
.background(FundoTopo)
.height(64.dp)
.clickable(onClick = voltar)
.padding(horizontal = 20.dp),
verticalAlignment = Alignment.CenterVertically
) {
Text(
text = "‹",
fontSize = 32.sp,
color = TextoPrincipal
)

        Spacer(modifier = Modifier.width(20.dp))

        Text(
            text = titulo,
            fontSize = 18.sp,
            color = TextoPrincipal
        )
    }

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = titulo,
        modifier = Modifier.padding(horizontal = 24.dp),
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        color = TextoPrincipal
    )

    Spacer(modifier = Modifier.height(12.dp))

    Text(
        text = when (titulo) {
            "Idiomas" ->
                "As opções de idioma e layout serão implementadas aqui."
            "Temas" ->
                "A personalização de cores e aparência será implementada aqui."
            "Digitação" ->
                "As preferências de digitação serão implementadas aqui."
            "Emoji" ->
                "As opções de emoji serão implementadas aqui."
            "Entrada avançada" ->
                "Os recursos adicionais serão implementados aqui."
            "Layout e teclas" ->
                "A personalização do layout será implementada aqui."
            "Som e vibração" ->
                "As opções de som e vibração serão implementadas aqui."
            "Privacidade" ->
                "Aqui ficarão as informações de privacidade do teclado."
            "Conta" ->
                "As opções de conta serão implementadas aqui."
            "Ajuda e Feedback" ->
                "As informações de ajuda serão adicionadas aqui."
            "Sobre o Teclado P2P V2" ->
                "Teclado P2P V2\nVersão 1.0"
            else ->
                "Configurações disponíveis em breve."
        },
        modifier = Modifier.padding(horizontal = 24.dp),
        fontSize = 15.sp,
        color = TextoSecundario,
        lineHeight = 24.sp
    )
}

}

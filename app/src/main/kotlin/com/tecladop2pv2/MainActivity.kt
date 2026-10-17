package com.tecladop2pv2

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.BinaryBitmap
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.tecladop2pv2.signaling.FirebaseSignalingClient
import com.tecladop2pv2.signaling.PairingSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

private val Fundo = Color(0xFF121212)
private val FundoTopo = Color(0xFF222222)
private val TextoPrincipal = Color(0xFFF5F5F5)
private val TextoSecundario = Color(0xFFAAAAAA)
private val Azul = Color(0xFF9DBDFF)
private val Verde = Color(0xFF81C784)

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
                        startActivity(
                            Intent("android.settings.INPUT_METHOD_SETTINGS")
                        )
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
    var mostrarLeitor by remember { mutableStateOf(false) }

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

    when {
        mostrarLeitor -> {
            TelaLerQrCode(
                voltar = { mostrarLeitor = false }
            )
            return
        }

        mostrarPareamento -> {
            TelaPareamento(
                voltar = { mostrarPareamento = false },
                abrirLeitor = { mostrarLeitor = true }
            )
            return
        }

        telaSelecionada != null -> {
            TelaOpcao(
                titulo = telaSelecionada!!,
                voltar = { telaSelecionada = null }
            )
            return
        }
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
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        "QR",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Azul
                    )
                }

                TextButton(
                    onClick = { mostrarLeitor = true }
                ) {
                    Text(
                        "CONECTAR",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Azul
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    "Teclado P2P V2",
                    fontSize = 18.sp,
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
                            "Configurações do teclado",
                            fontSize = 14.sp,
                            color = TextoSecundario
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Personalize sua experiência de digitação.",
                            fontSize = 17.sp,
                            color = TextoPrincipal
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { mostrarLeitor = true },
                            colors = CardDefaults.cardColors(
                                containerColor = FundoTopo
                            ),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("▦", fontSize = 34.sp, color = Azul)
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Conectar dispositivo",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextoPrincipal
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Leia um QR Code ou digite o código de pareamento.",
                                        fontSize = 12.sp,
                                        color = TextoSecundario
                                    )
                                }
                                Text("›", fontSize = 30.sp, color = Azul)
                            }
                        }
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
            Text("⌨", fontSize = 28.sp)
        }
    }
}

@Composable
private fun TelaPareamento(
    voltar: () -> Unit,
    abrirLeitor: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val cliente = remember { FirebaseSignalingClient() }

    var sessao by remember { mutableStateOf<PairingSession?>(null) }
    var carregando by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }

    val qrBitmap = remember(sessao?.oneTimeCode, sessao?.sessionId) {
        sessao?.let {
            runCatching {
                gerarQrCode(
                    "teclado-p2p://pair?code=${it.oneTimeCode}&session=${it.sessionId}"
                )
            }.getOrNull()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Fundo)
    ) {
        Cabecalho("Pareamento P2P", voltar)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Compartilhar conexão",
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                color = TextoPrincipal,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Crie uma sessão e mostre o QR Code ao outro dispositivo.",
                fontSize = 14.sp,
                color = TextoSecundario,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(22.dp))

            if (qrBitmap != null) {
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = "QR Code de pareamento",
                    modifier = Modifier
                        .size(240.dp)
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .padding(8.dp),
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    "Código: ${sessao?.oneTimeCode}",
                    color = Azul,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                sessao?.expiresAtMillis?.let { expiracao ->
                    Text(
                        "Expira em ${DateFormat.getTimeInstance().format(Date(expiracao))}",
                        color = TextoSecundario,
                        fontSize = 12.sp
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .background(Color.White, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (carregando) "Criando sessão..." else "QR Code não gerado",
                        color = Color.DarkGray,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            erro?.let {
                Text(
                    it,
                    color = Color(0xFFFF8A80),
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp
                )
            }

            Button(
                enabled = !carregando,
                onClick = {
                    scope.launch {
                        carregando = true
                        erro = null
                        sessao = null

                        try {
                            sessao = cliente.createSession()
                        } catch (e: Exception) {
                            erro = "Falha ao criar sessão: ${e.message ?: "verifique o Firebase"}"
                        } finally {
                            carregando = false
                        }
                    }
                }
            ) {
                Text(if (carregando) "Criando..." else "Criar QR Code")
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(onClick = abrirLeitor) {
                Text("Ler QR Code de outro dispositivo", color = Azul)
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = voltar) {
                Text("Voltar", color = Azul)
            }
        }
    }
}

@Composable
private fun TelaLerQrCode(
    voltar: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var codigo by remember { mutableStateOf("") }
    var mensagem by remember { mutableStateOf("Aponte a câmera para o QR Code do outro dispositivo.") }
    var erro by remember { mutableStateOf(false) }
    var processando by remember { mutableStateOf(false) }
    var mostrarCamera by remember { mutableStateOf(false) }

    val permissaoCamera = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedida ->
        if (concedida) {
            mostrarCamera = true
        } else {
            erro = true
            mensagem = "Permissão da câmera negada. Permita o acesso nas configurações do Android."
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Fundo)
    ) {
        Cabecalho("Conectar dispositivo", voltar)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Leia o QR Code",
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                color = TextoPrincipal
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Use a câmera para ler o código exibido no outro celular.",
                fontSize = 14.sp,
                color = TextoSecundario,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = FundoTopo)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (mostrarCamera) {
                        LeitorCamera(
                            aoLer = { conteudo ->
                                mostrarCamera = false
                                val dados = interpretarQrPareamento(conteudo)

                                if (dados != null) {
                                    codigo = dados.first
                                    mensagem = "QR Code reconhecido. Código: ${dados.first}"
                                    erro = false
                                } else {
                                    mensagem = "Este QR Code não é válido para o Teclado P2P V2."
                                    erro = true
                                }
                            },
                            aoFalhar = { detalhe ->
                                mensagem = detalhe
                                erro = true
                            }
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("▦", fontSize = 68.sp, color = Azul)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Área de leitura do QR Code",
                                color = TextoSecundario,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val permitido = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED

                    if (permitido) {
                        mostrarCamera = true
                        erro = false
                    } else {
                        permissaoCamera.launch(Manifest.permission.CAMERA)
                    }
                }
            ) {
                Text(if (mostrarCamera) "Câmera aberta" else "Abrir câmera e ler QR Code")
            }

            Spacer(modifier = Modifier.height(22.dp))

            HorizontalDivider(color = Color(0xFF383838))

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                "Ou digite o código de pareamento",
                color = TextoPrincipal,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = codigo,
                onValueChange = { novo ->
                    codigo = novo.filter(Char::isDigit).take(6)
                    erro = false
                    mensagem = "Digite o código de seis dígitos exibido no outro celular."
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Código de 6 dígitos") },
                placeholder = { Text("123456") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextoPrincipal,
                    unfocusedTextColor = TextoPrincipal,
                    focusedBorderColor = Azul,
                    focusedLabelColor = Azul,
                    cursorColor = Azul
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                mensagem,
                color = if (erro) Color(0xFFFF8A80) else TextoSecundario,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = codigo.length == 6 && !processando,
                onClick = {
                    /*
                     * A sessão pode ser reconhecida aqui, mas a conexão P2P
                     * completa exige implementar e integrar a sinalização WebRTC.
                     */
                    scope.launch {
                        processando = true
                        erro = false
                        mensagem = "Código reconhecido. A entrada na sessão ainda precisa ser integrada ao WebRTC."
                        processando = false
                    }
                }
            ) {
                Text(if (processando) "Conectando..." else "Conectar")
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = voltar) {
                Text("Voltar", color = Azul)
            }
        }
    }
}

private fun interpretarQrPareamento(conteudo: String): Pair<String, String>? {
    return runCatching {
        val uri = android.net.Uri.parse(conteudo)

        if (uri.scheme != "teclado-p2p" || uri.host != "pair") {
            return null
        }

        val code = uri.getQueryParameter("code") ?: return null
        val session = uri.getQueryParameter("session") ?: return null

        if (!code.matches(Regex("\\d{6}"))) return null
        if (!session.matches(Regex("[a-fA-F0-9-]{36}"))) return null

        code to session
    }.getOrNull()
}

@Composable
private fun LeitorCamera(
    aoLer: (String) -> Unit,
    aoFalhar: (String) -> Unit
) {
    val context = LocalContext.current

    androidx.compose.ui.viewinterop.AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            android.widget.FrameLayout(ctx).apply {
                val preview = android.widget.TextView(ctx).apply {
                    text = "A leitura contínua pela câmera será conectada na próxima etapa."
                    setTextColor(android.graphics.Color.WHITE)
                    textSize = 14f
                    gravity = android.view.Gravity.CENTER
                    setPadding(24, 24, 24, 24)
                }
                addView(
                    preview,
                    android.widget.FrameLayout.LayoutParams(
                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                    )
                )
            }
        },
        update = {}
    )
}

@Composable
private fun Cabecalho(
    titulo: String,
    voltar: () -> Unit
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
        Text("‹", fontSize = 32.sp, color = TextoPrincipal)
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            titulo,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = TextoPrincipal
        )
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
                opcao.icone,
                fontSize = 28.sp,
                color = TextoPrincipal
            )
        }

        Spacer(modifier = Modifier.width(24.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                opcao.titulo,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = TextoPrincipal
            )

            if (opcao.descricao.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    opcao.descricao,
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
        Cabecalho(titulo, voltar)

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            titulo,
            modifier = Modifier.padding(horizontal = 24.dp),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextoPrincipal
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = when (titulo) {
                "Idiomas" -> "As opções de idioma e layout serão implementadas aqui."
                "Temas" -> "A personalização de cores e aparência será implementada aqui."
                "Digitação" -> "As preferências de digitação serão implementadas aqui."
                "Emoji" -> "As opções de emoji serão implementadas aqui."
                "Entrada avançada" -> "Os recursos adicionais serão implementados aqui."
                "Layout e teclas" -> "A personalização do layout será implementada aqui."
                "Som e vibração" -> "As opções de som e vibração serão implementadas aqui."
                "Privacidade" -> "Aqui ficarão as informações de privacidade do teclado."
                "Conta" -> "As opções de conta serão implementadas aqui."
                "Ajuda e Feedback" -> "As informações de ajuda serão adicionadas aqui."
                "Sobre o Teclado P2P V2" -> "Teclado P2P V2\nVersão 1.0"
                else -> "Configurações disponíveis em breve."
            },
            modifier = Modifier.padding(horizontal = 24.dp),
            fontSize = 15.sp,
            color = TextoSecundario,
            lineHeight = 24.sp
        )
    }
}           textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            erro?.let {
                Text(
                    it,
                    color = Color(0xFFFF8A80),
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Button(
                enabled = !carregando,
                onClick = {
                    scope.launch {
                        carregando = true
                        erro = null
                        sessao = null

                        try {
                            sessao = cliente.createSession()
                        } catch (e: Exception) {
                            erro = when (e.message) {
                                "SESSION_NOT_FOUND" ->
                                    "Sessão não encontrada."
                                "FIREBASE_AUTH_REQUIRED" ->
                                    "Autenticação Firebase necessária."
                                else ->
                                    "Falha ao criar sessão: ${e.message ?: "verifique a configuração Firebase"}"
                            }
                        } finally {
                            carregando = false
                        }
                    }
                }
            ) {
                Text(if (carregando) "Criando..." else "Criar sessão e QR Code")
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(onClick = voltar) {
                Text("Voltar", color = Azul)
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
                opcao.icone,
                fontSize = 28.sp,
                color = TextoPrincipal
            )
        }

        Spacer(modifier = Modifier.width(24.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                opcao.titulo,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = TextoPrincipal
            )

            if (opcao.descricao.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    opcao.descricao,
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
            Text("‹", fontSize = 32.sp, color = TextoPrincipal)
            Spacer(modifier = Modifier.width(20.dp))
            Text(titulo, fontSize = 18.sp, color = TextoPrincipal)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            titulo,
            modifier = Modifier.padding(horizontal = 24.dp),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextoPrincipal
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = when (titulo) {
                "Idiomas" -> "As opções de idioma e layout serão implementadas aqui."
                "Temas" -> "A personalização de cores e aparência será implementada aqui."
                "Digitação" -> "As preferências de digitação serão implementadas aqui."
                "Emoji" -> "As opções de emoji serão implementadas aqui."
                "Entrada avançada" -> "Os recursos adicionais serão implementados aqui."
                "Layout e teclas" -> "A personalização do layout será implementada aqui."
                "Som e vibração" -> "As opções de som e vibração serão implementadas aqui."
                "Privacidade" -> "Aqui ficarão as informações de privacidade do teclado."
                "Conta" -> "As opções de conta serão implementadas aqui."
                "Ajuda e Feedback" -> "As informações de ajuda serão adicionadas aqui."
                "Sobre o Teclado P2P V2" -> "Teclado P2P V2\nVersão 1.0"
                else -> "Configurações disponíveis em breve."
            },
            modifier = Modifier.padding(horizontal = 24.dp),
            fontSize = 15.sp,
            color = TextoSecundario,
            lineHeight = 24.sp
        )
    }
}

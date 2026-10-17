
package com.tecladop2pv2

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.tecladop2pv2.signaling.FirebaseSignalingClient
import com.tecladop2pv2.signaling.PairingSession
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

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
                        startActivity(Intent("android.settings.INPUT_METHOD_SETTINGS"))
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

private data class RegistroHistorico(
    val texto: String,
    val tipo: String,
    val aplicativo: String,
    val data: String
)

private fun carregarHistorico(context: Context): List<RegistroHistorico> {
    val preferencias = context.getSharedPreferences(
        "teclado_p2p_historico",
        Context.MODE_PRIVATE
    )

    return try {
        val array = JSONArray(preferencias.getString("registros", "[]"))
        (0 until array.length()).mapNotNull { i ->
            val item = array.optJSONObject(i) ?: return@mapNotNull null
            RegistroHistorico(
                texto = item.optString("texto"),
                tipo = item.optString("tipo", "mensagem"),
                aplicativo = item.optString("aplicativo", "Aplicativo desconhecido"),
                data = item.optString("data")
            )
        }.reversed()
    } catch (_: Exception) {
        emptyList()
    }
}

@Composable
private fun TelaConfiguracoes(abrirSeletorTeclado: () -> Unit) {
    var telaSelecionada by remember { mutableStateOf<String?>(null) }
    var mostrarPareamento by remember { mutableStateOf(false) }
    var mostrarLeitor by remember { mutableStateOf(false) }
    var mostrarHistorico by remember { mutableStateOf(false) }

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
        mostrarHistorico -> {
            TelaHistorico(voltar = { mostrarHistorico = false })
            return
        }
        mostrarLeitor -> {
            TelaLerQrCode(voltar = { mostrarLeitor = false })
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
        modifier = Modifier.fillMaxSize().background(Fundo)
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
                TextButton(onClick = { mostrarPareamento = true }) {
                    Text("QR", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Azul)
                }
                TextButton(onClick = { mostrarLeitor = true }) {
                    Text("CONECTAR", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Azul)
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
                modifier = Modifier.fillMaxSize().padding(bottom = 90.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp)
                    ) {
                        Text("Configurações do teclado", fontSize = 14.sp, color = TextoSecundario)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Personalize sua experiência de digitação.",
                            fontSize = 17.sp,
                            color = TextoPrincipal
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { mostrarLeitor = true },
                            colors = CardDefaults.cardColors(containerColor = FundoTopo),
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

                        Spacer(modifier = Modifier.height(12.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { mostrarHistorico = true },
                            colors = CardDefaults.cardColors(containerColor = FundoTopo),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("◷", fontSize = 34.sp, color = Azul)
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Histórico de digitação",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextoPrincipal
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Consulte os registros locais e o estado do histórico online.",
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
                    LinhaConfiguracao(opcao = opcao) {
                        telaSelecionada = opcao.titulo
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = abrirSeletorTeclado,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            shape = RoundedCornerShape(20.dp),
            containerColor = Azul,
            contentColor = Color(0xFF101820)
        ) {
            Text("⌨", fontSize = 28.sp)
        }
    }
}

@Composable
private fun TelaHistorico(voltar: () -> Unit) {
    val context = LocalContext.current
    var aba by remember { mutableStateOf(0) }
    var registros by remember { mutableStateOf(carregarHistorico(context)) }
    var filtro by remember { mutableStateOf("") }
    var somenteMensagens by remember { mutableStateOf(false) }
    var mostrarConfirmacao by remember { mutableStateOf(false) }

    val preferencias = remember(context) {
        context.getSharedPreferences("teclado_p2p_historico", Context.MODE_PRIVATE)
    }
    var registroAtivo by remember {
        mutableStateOf(preferencias.getBoolean("registro_ativado", false))
    }
    var modoBruto by remember {
        mutableStateOf(preferencias.getBoolean("modo_bruto", false))
    }

    val filtrados = registros.filter { registro ->
        (filtro.isBlank() ||
            registro.texto.contains(filtro, ignoreCase = true) ||
            registro.aplicativo.contains(filtro, ignoreCase = true)) &&
            (!somenteMensagens || registro.tipo == "mensagem")
    }

    if (mostrarConfirmacao) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacao = false },
            title = { Text("Apagar histórico local?") },
            text = { Text("Esta ação apagará todos os registros guardados neste celular.") },
            confirmButton = {
                TextButton(onClick = {
                    preferencias.edit().putString("registros", "[]").apply()
                    registros = emptyList()
                    mostrarConfirmacao = false
                }) {
                    Text("Apagar", color = Color(0xFFFF8A80))
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmacao = false }) {
                    Text("Cancelar")
                }
            },
            containerColor = FundoTopo,
            titleContentColor = TextoPrincipal,
            textContentColor = TextoSecundario
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Fundo)
    ) {
        Cabecalho("Histórico", voltar)

        TabRow(
            selectedTabIndex = aba,
            containerColor = FundoTopo,
            contentColor = Azul
        ) {
            Tab(
                selected = aba == 0,
                onClick = { aba = 0 },
                text = { Text("LOCAL") }
            )
            Tab(
                selected = aba == 1,
                onClick = { aba = 1 },
                text = { Text("ONLINE") }
            )
        }

        if (aba == 0) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = FundoTopo),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                        Text(
                            "Registro local",
                            fontWeight = FontWeight.Bold,
                            color = TextoPrincipal
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Ativar histórico", color = TextoPrincipal, fontSize = 14.sp)
                                Text(
                                    "Permite salvar registros neste aparelho.",
                                    color = TextoSecundario,
                                    fontSize = 12.sp
                                )
                            }
                            Switch(
                                checked = registroAtivo,
                                onCheckedChange = {
                                    registroAtivo = it
                                    preferencias.edit().putBoolean("registro_ativado", it).apply()
                                }
                            )
                        }

                        HorizontalDivider(color = Color(0xFF383838))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Modo bruto", color = TextoPrincipal, fontSize = 14.sp)
                                Text(
                                    "Registra eventos de teclas em campos comuns.",
                                    color = TextoSecundario,
                                    fontSize = 12.sp
                                )
                            }
                            Switch(
                                checked = modoBruto,
                                enabled = registroAtivo,
                                onCheckedChange = {
                                    modoBruto = it
                                    preferencias.edit().putBoolean("modo_bruto", it).apply()
                                }
                            )
                        }

                        Text(
                            "Não ative o registro em dispositivos compartilhados. Campos identificados como sensíveis são excluídos, mas aplicativos podem não informar corretamente o tipo do campo.",
                            color = TextoSecundario,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = filtro,
                    onValueChange = { filtro = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Buscar texto ou aplicativo") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextoPrincipal,
                        unfocusedTextColor = TextoPrincipal,
                        focusedBorderColor = Azul,
                        unfocusedBorderColor = TextoSecundario,
                        focusedLabelColor = Azul,
                        cursorColor = Azul
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = somenteMensagens,
                        onCheckedChange = { somenteMensagens = it },
                        colors = CheckboxDefaults.colors(checkedColor = Azul)
                    )
                    Text("Somente mensagens concluídas", color = TextoSecundario, fontSize = 12.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = { mostrarConfirmacao = true }) {
                        Text("Apagar tudo", color = Color(0xFFFF8A80), fontSize = 12.sp)
                    }
                }

                Text(
                    "${filtrados.size} registro(s)",
                    color = TextoSecundario,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (filtrados.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (registros.isEmpty())
                                "Nenhum registro local ainda.\nAtive o histórico para começar."
                            else
                                "Nenhum registro corresponde à busca.",
                            color = TextoSecundario,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filtrados) { registro ->
                            CartaoRegistro(registro)
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("⇄", fontSize = 54.sp, color = Azul)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Histórico online indisponível",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextoPrincipal,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "Você precisa se conectar a um dispositivo para ter acesso ao histórico online. A sincronização entre celulares ainda não está ativa.",
                    color = TextoSecundario,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = {
                        // A conexão deve ser iniciada pela tela principal.
                        voltar()
                    }
                ) {
                    Text("Voltar e conectar dispositivo")
                }
            }
        }
    }
}

@Composable
private fun CartaoRegistro(registro: RegistroHistorico) {
    val context = LocalContext.current
    val nomeAplicativo = remember(registro.aplicativo) {
        try {
            val info = context.packageManager.getApplicationInfo(registro.aplicativo, 0)
            context.packageManager.getApplicationLabel(info).toString()
        } catch (_: Exception) {
            registro.aplicativo
        }
    }

    val icone = remember(registro.aplicativo) {
        try {
            context.packageManager.getApplicationIcon(registro.aplicativo)
        } catch (_: Exception) {
            null
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = FundoTopo)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            if (icone != null) {
                androidx.compose.foundation.Image(
                    bitmap = android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888)
                        .asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(1.dp)
                )
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.size(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { android.widget.ImageView(it).apply { setImageDrawable(icone) } },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                Box(
                    modifier = Modifier.size(36.dp).background(Fundo, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("▣", color = Azul, fontSize = 20.sp)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    nomeAplicativo,
                    color = Azul,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    registro.texto,
                    color = TextoPrincipal,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "${registro.data} • ${if (registro.tipo == "tecla") "Tecla" else "Mensagem"}",
                    color = TextoSecundario,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun TelaPareamento(voltar: () -> Unit, abrirLeitor: () -> Unit) {
    val scope = rememberCoroutineScope()
    val cliente = remember { FirebaseSignalingClient() }

    var sessao by remember { mutableStateOf<PairingSession?>(null) }
    var carregando by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }

    val qrBitmap = remember(sessao?.oneTimeCode, sessao?.sessionId) {
        sessao?.let {
            runCatching {
                gerarQrCode("teclado-p2p://pair?code=${it.oneTimeCode}&session=${it.sessionId}")
            }.getOrNull()
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Fundo)) {
        Cabecalho("Pareamento P2P", voltar)

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp),
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
                    modifier = Modifier.size(240.dp).background(Color.White, RoundedCornerShape(12.dp)).padding(8.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("Código: ${sessao?.oneTimeCode}", color = Azul, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                sessao?.expiresAtMillis?.let {
                    Text("Expira em ${DateFormat.getTimeInstance().format(Date(it))}", color = TextoSecundario, fontSize = 12.sp)
                }
            } else {
                Box(
                    modifier = Modifier.size(240.dp).background(Color.White, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (carregando) "Criando sessão..." else "QR Code não gerado", color = Color.DarkGray)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            erro?.let {
                Text(it, color = Color(0xFFFF8A80), textAlign = TextAlign.Center, fontSize = 13.sp)
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
                            erro = if (e.message == "FIREBASE_AUTH_REQUIRED")
                                "Autenticação Firebase necessária."
                            else "Falha ao criar sessão: ${e.message ?: "verifique o Firebase"}"
                        } finally {
                            carregando = false
                        }
                    }
                }
            ) {
                Text(if (carregando) "Criando..." else "Criar sessão e QR Code")
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = abrirLeitor) {
                Text("Ler QR Code de outro dispositivo", color = Azul)
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = voltar) { Text("Voltar", color = Azul) }
        }
    }
}

@Composable
private fun TelaLerQrCode(voltar: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cliente = remember { FirebaseSignalingClient() }

    var codigo by remember { mutableStateOf("") }
    var mensagem by remember {
        mutableStateOf("Aponte a câmera para o QR Code ou digite o código de seis dígitos.")
    }
    var erro by remember { mutableStateOf(false) }
    var cameraAtiva by remember { mutableStateOf(false) }
    var processando by remember { mutableStateOf(false) }
    var sessaoEncontrada by remember { mutableStateOf<PairingSession?>(null) }

    val permissaoCamera = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedida ->
        cameraAtiva = concedida
        erro = !concedida
        mensagem = if (concedida) "Aponte a câmera para o QR Code."
        else "Permissão da câmera negada. Digite o código manualmente ou permita o acesso nas configurações."
    }

    Column(modifier = Modifier.fillMaxSize().background(Fundo)) {
        Cabecalho("Conectar dispositivo", voltar)

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Conectar dispositivo", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = TextoPrincipal)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Leia o QR Code ou informe o código mostrado no outro dispositivo.",
                fontSize = 14.sp,
                color = TextoSecundario,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(18.dp))

            if (cameraAtiva && codigo.length != 6) {
                LeitorCamera(
                    modifier = Modifier.fillMaxWidth().height(240.dp),
                    aoLerCodigo = { valor ->
                        val codigoLido = extrairCodigoPareamento(valor)
                        if (codigoLido != null && codigo.isEmpty()) {
                            codigo = codigoLido
                            cameraAtiva = false
                            erro = false
                            mensagem = "QR Code reconhecido. Confira o código e toque em Conectar."
                        }
                    },
                    aoFalhar = { detalhe ->
                        erro = true
                        mensagem = detalhe
                    }
                )
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth().height(130.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = FundoTopo)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("▦", fontSize = 42.sp, color = Azul)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            if (codigo.length == 6) "Código capturado" else "Leitor de QR Code",
                            color = TextoPrincipal,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (cameraAtiva) {
                        cameraAtiva = false
                    } else {
                        val permitido = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                            PackageManager.PERMISSION_GRANTED
                        if (permitido) {
                            cameraAtiva = true
                            erro = false
                            mensagem = "Aponte a câmera para o QR Code."
                        } else {
                            permissaoCamera.launch(Manifest.permission.CAMERA)
                        }
                    }
                }
            ) {
                Text(if (cameraAtiva) "Fechar câmera" else "Ler QR Code com a câmera", color = Azul)
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFF383838))
            Spacer(modifier = Modifier.height(14.dp))
            Text("Ou digite o código de pareamento", color = TextoPrincipal, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = codigo,
                onValueChange = { novo ->
                    codigo = novo.filter(Char::isDigit).take(6)
                    erro = false
                    sessaoEncontrada = null
                    mensagem = "Informe os seis dígitos do código."
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
                    unfocusedBorderColor = TextoSecundario,
                    focusedLabelColor = Azul,
                    cursorColor = Azul
                )
            )

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                mensagem,
                color = if (erro) Color(0xFFFF8A80) else TextoSecundario,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )

            sessaoEncontrada?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Sessão localizada: ${it.sessionId}", color = Azul, fontSize = 12.sp, textAlign = TextAlign.Center)
            }

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = codigo.length == 6 && !processando,
                onClick = {
                    scope.launch {
                        processando = true
                        erro = false
                        sessaoEncontrada = null
                        mensagem = "Procurando sessão..."
                        try {
                            val sessao = cliente.joinSession(codigo)
                            sessaoEncontrada = sessao
                            mensagem = "Sessão localizada. A negociação WebRTC ainda precisa ser integrada."
                        } catch (e: Exception) {
                            erro = true
                            mensagem = when (e.message) {
                                "SESSION_NOT_FOUND" -> "Sessão não encontrada ou código expirado."
                                "FIREBASE_AUTH_REQUIRED" -> "Autenticação Firebase necessária."
                                else -> "Não foi possível localizar a sessão: ${e.message ?: "erro desconhecido"}"
                            }
                        } finally {
                            processando = false
                        }
                    }
                }
            ) {
                Text(if (processando) "Procurando..." else "Conectar")
            }

            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = voltar) { Text("Voltar", color = Azul) }
        }
    }
}

private fun extrairCodigoPareamento(valor: String): String? {
    val texto = valor.trim()
    if (texto.matches(Regex("^\\d{6}$"))) return texto

    return runCatching {
        val uri = android.net.Uri.parse(texto)
        val codigoUri = uri.getQueryParameter("code") ?: uri.getQueryParameter("codigo")
        codigoUri?.filter(Char::isDigit)?.takeIf { it.length == 6 }
    }.getOrNull()
}

@Composable
private fun LeitorCamera(
    modifier: Modifier = Modifier,
    aoLerCodigo: (String) -> Unit,
    aoFalhar: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        BarcodeScanning.getClient(options)
    }

    var codigoJaLido by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            scanner.close()
            executor.shutdown()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            previewView.scaleType = PreviewView.ScaleType.FILL_CENTER
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    analysis.setAnalyzer(executor) { imageProxy ->
                        if (codigoJaLido) {
                            imageProxy.close()
                        } else {
                            val mediaImage = imageProxy.image
                            if (mediaImage == null) {
                                imageProxy.close()
                            } else {
                                val image = InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.imageInfo.rotationDegrees
                                )
                                scanner.process(image)
                                    .addOnSuccessListener { barcodes ->
                                        val valor = barcodes.firstOrNull {
                                            !it.rawValue.isNullOrBlank()
                                        }?.rawValue

                                        if (valor != null && !codigoJaLido) {
                                            val codigo = extrairCodigoPareamento(valor)
                                            if (codigo != null) {
                                                codigoJaLido = true
                                                aoLerCodigo(valor)
                                            } else {
                                                aoFalhar("O QR Code não contém um código de pareamento válido.")
                                            }
                                        }
                                    }
                                    .addOnFailureListener {
                                        aoFalhar("Não foi possível ler o QR Code. Tente novamente.")
                                    }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                    }
                            }
                        }
                    }

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis
                    )
                } catch (e: Exception) {
                    aoFalhar("Não foi possível iniciar a câmera: ${e.message ?: "erro desconhecido"}")
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        }
    )
}

@Composable
private fun Cabecalho(titulo: String, voltar: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(FundoTopo).height(64.dp)
            .clickable(onClick = voltar).padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("‹", fontSize = 32.sp, color = TextoPrincipal)
        Spacer(modifier = Modifier.width(16.dp))
        Text(titulo, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = TextoPrincipal)
    }
}

@Composable
private fun LinhaConfiguracao(opcao: OpcaoConfiguracao, aoClicar: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = aoClicar)
            .heightIn(min = 76.dp).padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(42.dp), contentAlignment = Alignment.Center) {
            Text(opcao.icone, fontSize = 28.sp, color = TextoPrincipal)
        }
        Spacer(modifier = Modifier.width(24.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(opcao.titulo, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextoPrincipal)
            if (opcao.descricao.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(opcao.descricao, fontSize = 12.sp, color = TextoSecundario)
            }
        }
    }
}

@Composable
private fun TelaOpcao(titulo: String, voltar: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(Fundo)) {
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
}

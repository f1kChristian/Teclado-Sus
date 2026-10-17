package com.tecladop2pv2.feature.keyboard

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.PopupWindow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class TecladoInputMethodService : InputMethodService() {

    private val fundo = Color.rgb(35, 36, 40)
    private val corTecla = Color.rgb(65, 67, 73)
    private val corEspecial = Color.rgb(85, 91, 105)
    private val corPopup = Color.rgb(45, 47, 54)
    private val corSelecionado = Color.rgb(35, 130, 220)

    private var maiusculas = false
    private var simbolos = false

    private val textoBruto = StringBuilder()

    private var popupAtual: PopupWindow? = null
    private var botoesAlternativas = mutableListOf<Button>()
    private var indiceAlternativaSelecionada = 0
    private var alternativasVisiveis = listOf<String>()

    private val handler = Handler(Looper.getMainLooper())

    private val tempoPressaoLonga =
        ViewConfiguration.getLongPressTimeout().toLong()

    private val limiteMovimento =
        ViewConfiguration.get(this).scaledTouchSlop

    private var ultimoTextoNaoVazio = ""
    private var envioJaRegistrado = false

    private val verificarLimpezaCampo = Runnable {
        val ic = currentInputConnection ?: return@Runnable

        val antes = ic.getTextBeforeCursor(4000, 0)
            ?.toString().orEmpty()

        val depois = ic.getTextAfterCursor(4000, 0)
            ?.toString().orEmpty()

        val textoAtual = antes + depois

        if (
            textoAtual.isBlank() &&
            ultimoTextoNaoVazio.isNotBlank() &&
            !envioJaRegistrado
        ) {
            registrarMensagem(ultimoTextoNaoVazio)
            envioJaRegistrado = true
            ultimoTextoNaoVazio = ""
        }
    }

    private val preferencias by lazy {
        getSharedPreferences(
            "teclado_p2p_historico",
            Context.MODE_PRIVATE
        )
    }

    override fun onCreateInputView(): View = criarTeclado()

    override fun onStartInput(
        attribute: EditorInfo?,
        restarting: Boolean
    ) {
        super.onStartInput(attribute, restarting)

        fecharBufferBruto()
        fecharPopupAlternativas()

        handler.removeCallbacks(verificarLimpezaCampo)
        ultimoTextoNaoVazio = ""
        envioJaRegistrado = false
    }

    override fun onFinishInput() {
        handler.removeCallbacks(verificarLimpezaCampo)
        fecharBufferBruto()
        fecharPopupAlternativas()

        ultimoTextoNaoVazio = ""
        envioJaRegistrado = false

        super.onFinishInput()
    }

    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int
    ) {
        super.onUpdateSelection(
            oldSelStart,
            oldSelEnd,
            newSelStart,
            newSelEnd,
            candidatesStart,
            candidatesEnd
        )

        val ic = currentInputConnection ?: return

        val antes = ic.getTextBeforeCursor(4000, 0)
            ?.toString().orEmpty()

        val depois = ic.getTextAfterCursor(4000, 0)
            ?.toString().orEmpty()

        val textoAtual = antes + depois

        if (textoAtual.isNotBlank()) {
            ultimoTextoNaoVazio = textoAtual
            envioJaRegistrado = false
            handler.removeCallbacks(verificarLimpezaCampo)
        } else if (
            ultimoTextoNaoVazio.isNotBlank() &&
            !envioJaRegistrado
        ) {
            handler.removeCallbacks(verificarLimpezaCampo)
            handler.postDelayed(verificarLimpezaCampo, 500)
        }
    }

    private fun campoSensivel(): Boolean {
        val tipo = currentInputEditorInfo?.inputType ?: return true
        val classe = tipo and InputType.TYPE_MASK_CLASS
        val variacao = tipo and InputType.TYPE_MASK_VARIATION

        if (
            classe == InputType.TYPE_CLASS_NUMBER &&
            variacao == InputType.TYPE_NUMBER_VARIATION_PASSWORD
        ) {
            return true
        }

        if (
            classe == InputType.TYPE_CLASS_TEXT &&
            variacao in setOf(
                InputType.TYPE_TEXT_VARIATION_PASSWORD,
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
            )
        ) {
            return true
        }

        return false
    }

    private fun registroAtivo(): Boolean =
        preferencias.getBoolean("registro_ativado", false)

    private fun modoBruto(): Boolean =
        preferencias.getBoolean("modo_bruto", false)

    private fun salvarRegistro(texto: String, tipo: String) {
        if (!registroAtivo() || campoSensivel() || texto.isBlank()) {
            return
        }

        val pacote = currentInputEditorInfo?.packageName
            ?: "Aplicativo desconhecido"

        val data = SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss",
            Locale.getDefault()
        ).format(Date())

        val lista = try {
            JSONArray(preferencias.getString("registros", "[]"))
        } catch (_: Exception) {
            JSONArray()
        }

        lista.put(
            JSONObject().apply {
                put("texto", texto)
                put("tipo", tipo)
                put("aplicativo", pacote)
                put("data", data)
            }
        )

        val limite = JSONArray()
        val inicio = (lista.length() - 500).coerceAtLeast(0)

        for (i in inicio until lista.length()) {
            limite.put(lista.getJSONObject(i))
        }

        preferencias.edit()
            .putString("registros", limite.toString())
            .apply()
    }

    private fun adicionarAoBuffer(caractere: String) {
        if (registroAtivo() && modoBruto() && !campoSensivel()) {
            textoBruto.append(caractere)
        }
    }

    private fun fecharBufferBruto() {
        if (textoBruto.isNotEmpty()) {
            salvarRegistro(textoBruto.toString(), "tecla")
            textoBruto.clear()
        }
    }

    private fun registrarMensagem(texto: String) {
        if (texto.isBlank() || campoSensivel() || !registroAtivo()) {
            return
        }

        if (modoBruto()) {
            fecharBufferBruto()
        }

        salvarRegistro(texto.trim(), "mensagem")
    }

    private fun criarTeclado(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), dp(5), dp(4), dp(5))
            setBackgroundColor(fundo)

            if (simbolos) {
                adicionarLinha(
                    this,
                    listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
                )
                adicionarLinha(
                    this,
                    listOf("@", "#", "$", "%", "&", "-", "+", "(", ")", "/")
                )
                adicionarLinha(
                    this,
                    listOf("=", "*", "\"", "'", ":", ";", "!", "?", "⌫")
                )
                adicionarLinha(
                    this,
                    listOf("ABC", ",", "🌐", "Espaço", ".", "↵")
                )
            } else {
                adicionarLinha(
                    this,
                    listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
                )
                adicionarLinha(
                    this,
                    listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
                )
                adicionarLinha(
                    this,
                    listOf("⇧", "z", "x", "c", "v", "b", "n", "m", "⌫")
                )
                adicionarLinha(
                    this,
                    listOf("?123", ",", "🌐", "Espaço", ".", "↵")
                )
            }
        }
    }

    private fun adicionarLinha(
        teclado: LinearLayout,
        teclas: List<String>
    ) {
        val linha = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        teclas.forEach { texto ->
            val rotulo = when {
                texto == "Espaço" -> ""
                texto.length == 1 &&
                    texto[0].isLetter() &&
                    maiusculas -> texto.uppercase()
                else -> texto
            }

            val botao = Button(this).apply {
                text = rotulo
                textSize = if (texto == "Espaço") 14f else 18f
                isAllCaps = false
                setTextColor(Color.WHITE)
                typeface = Typeface.DEFAULT

                background = criarFundo(
                    if (
                        texto in listOf(
                            "⇧", "⌫", "?123", "ABC", "↵", "🌐"
                        )
                    ) corEspecial else corTecla
                )

                minWidth = 0
                minimumWidth = 0
                minHeight = dp(44)
                minimumHeight = dp(44)
                setPadding(dp(2), 0, dp(2), 0)

                contentDescription = when (texto) {
                    "Espaço" -> "Barra de espaço"
                    "⌫" -> "Apagar"
                    "↵" -> "Enviar ou Enter"
                    "⇧" -> "Maiúsculas"
                    "?123" -> "Números e símbolos"
                    "ABC" -> "Voltar para letras"
                    "🌐" -> "Trocar idioma"
                    else -> texto
                }
            }

            if (alternativasPara(texto).isNotEmpty()) {
                configurarPressaoLonga(botao, texto)
            } else {
                botao.setOnClickListener {
                    tratarTecla(texto)
                }
            }

            val peso = when (texto) {
                "Espaço" -> 4f
                "⇧", "⌫", "?123", "ABC", "↵" -> 1.5f
                "🌐" -> 1.2f
                else -> 1f
            }

            linha.addView(
                botao,
                LinearLayout.LayoutParams(0, dp(45), peso).apply {
                    setMargins(dp(2), dp(3), dp(2), dp(3))
                }
            )
        }

        teclado.addView(
            linha,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
    }

    /*
     * O toque é controlado pela própria tecla.
     * Isso evita que o popup roube o gesto durante o arrasto.
     */
    private fun configurarPressaoLonga(
        botao: Button,
        tecla: String
    ) {
        var pressionouLongo = false
        var movimentoCancelouLongPress = false
        var inicioX = 0f
        var inicioY = 0f

        val abrirPopup = Runnable {
            if (!movimentoCancelouLongPress && botao.isPressed) {
                pressionouLongo = true
                mostrarCaracteresAlternativos(botao, tecla)
            }
        }

        botao.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    fecharPopupAlternativas()

                    pressionouLongo = false
                    movimentoCancelouLongPress = false
                    inicioX = event.x
                    inicioY = event.y

                    view.isPressed = true
                    handler.postDelayed(abrirPopup, tempoPressaoLonga)
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    if (!pressionouLongo) {
                        val dx = abs(event.x - inicioX)
                        val dy = abs(event.y - inicioY)

                        if (
                            dx > limiteMovimento ||
                            dy > limiteMovimento
                        ) {
                            movimentoCancelouLongPress = true
                            handler.removeCallbacks(abrirPopup)
                        }
                    } else {
                        selecionarAlternativaPeloDedo(
                            event.rawX,
                            event.rawY
                        )
                    }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    handler.removeCallbacks(abrirPopup)
                    view.isPressed = false

                    if (pressionouLongo) {
                        confirmarAlternativaSelecionada()
                    } else if (!movimentoCancelouLongPress) {
                        tratarTecla(tecla)
                    }

                    true
                }

                MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacks(abrirPopup)
                    view.isPressed = false
                    fecharPopupAlternativas()
                    true
                }

                else -> true
            }
        }
    }

    private fun alternativasPara(tecla: String): List<String> {
        return when (tecla.lowercase()) {
            "a" -> listOf("a", "á", "à", "â", "ã", "ä", "å")
            "c" -> listOf("c", "ç", "ć", "č")
            "e" -> listOf("e", "é", "è", "ê", "ë")
            "i" -> listOf("i", "í", "ì", "î", "ï")
            "n" -> listOf("n", "ñ", "ń")
            "o" -> listOf("o", "ó", "ò", "ô", "õ", "ö", "ø")
            "u" -> listOf("u", "ú", "ù", "û", "ü")
            "y" -> listOf("y", "ý", "ÿ")
            "s" -> listOf("s", "ß", "ś", "š")
            "z" -> listOf("z", "ž", "ź", "ż")

            "," -> listOf(",", ";", "،", "、")
            "." -> listOf(".", "…", "·", "。")
            "!" -> listOf("!", "¡", "‼")
            "?" -> listOf("?", "¿", "⁇")
            ":" -> listOf(":", "：", "÷")
            ";" -> listOf(";", "؛")
            "'" -> listOf("'", "’", "‘", "´", "`")
            "\"" -> listOf("\"", "“", "”", "„")
            "-" -> listOf("-", "–", "—", "−")
            "(" -> listOf("(", "[", "{", "（")
            ")" -> listOf(")", "]", "}", "）")
            "/" -> listOf("/", "\\", "|")
            "=" -> listOf("=", "≠", "≈", "≤", "≥")
            "+" -> listOf("+", "±")
            "*" -> listOf("*", "×", "÷")
            "$" -> listOf("$", "€", "£", "¥", "¢")
            "%" -> listOf("%", "‰")
            "&" -> listOf("&", "§")
            "@" -> listOf("@", "©", "®", "™")
            "#" -> listOf("#", "№")
            "1" -> listOf("1", "¹", "½")
            "2" -> listOf("2", "²", "⅔")
            "3" -> listOf("3", "³", "¾")
            "0" -> listOf("0", "°")

            else -> emptyList()
        }
    }

    private fun mostrarCaracteresAlternativos(
        origem: View,
        tecla: String
    ) {
        fecharPopupAlternativas()

        val alternativas = alternativasPara(tecla)
        if (alternativas.isEmpty()) return

        alternativasVisiveis = alternativas
        indiceAlternativaSelecionada = 0
        botoesAlternativas = mutableListOf()

        val scroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            isVerticalScrollBarEnabled = false
            clipToPadding = false
            setPadding(dp(3), dp(3), dp(3), dp(3))
            setBackgroundColor(corPopup)
        }

        val linha = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(2), dp(2), dp(2), dp(2))
        }

        alternativas.forEachIndexed { indice, alternativa ->
            val caractere = if (
                maiusculas && alternativa.any { it.isLetter() }
            ) {
                alternativa.uppercase()
            } else {
                alternativa
            }

            val botaoAlternativa = Button(this).apply {
                text = caractere
                textSize = 19f
                isAllCaps = false
                setTextColor(Color.WHITE)
                background = criarFundo(
                    if (indice == 0) corSelecionado else corEspecial
                )

                minWidth = dp(44)
                minimumWidth = dp(44)
                minHeight = dp(48)
                minimumHeight = dp(48)
                setPadding(dp(3), 0, dp(3), 0)

                // O gesto é tratado pela tecla original, não pelo popup.
                isClickable = false
                isFocusable = false
            }

            botoesAlternativas.add(botaoAlternativa)

            linha.addView(
                botaoAlternativa,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    dp(48)
                ).apply {
                    setMargins(dp(2), 0, dp(2), 0)
                }
            )
        }

        scroll.addView(
            linha,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        val larguraMaxima =
            (resources.displayMetrics.widthPixels - dp(16))
                .coerceAtLeast(dp(120))

        val larguraConteudo =
            alternativas.size * dp(48) + dp(16)

        val larguraPopup = minOf(larguraMaxima, larguraConteudo)
        val alturaPopup = dp(60)

        val popup = PopupWindow(
            scroll,
            larguraPopup,
            alturaPopup,
            false
        ).apply {
            isFocusable = false
            isTouchable = false
            isOutsideTouchable = false
            elevation = dp(8).toFloat()
            setBackgroundDrawable(criarFundo(corPopup))
        }

        popupAtual = popup

        /*
         * O popup fica ancorado na tecla pressionada.
         * O deslocamento negativo posiciona-o acima dela.
         */
        popup.showAsDropDown(
            origem,
            0,
            -origem.height - alturaPopup - dp(4),
            Gravity.CENTER_HORIZONTAL
        )

        atualizarDestaqueAlternativa()
    }

    /*
     * Quando o dedo entra na faixa vertical do popup, destaca a
     * alternativa horizontalmente mais próxima da posição do dedo.
     */
    private fun selecionarAlternativaPeloDedo(
        rawX: Float,
        rawY: Float
    ) {
        val popup = popupAtual ?: return
        if (!popup.isShowing || botoesAlternativas.isEmpty()) return

        val localizacaoPopup = IntArray(2)
        popup.contentView.getLocationOnScreen(localizacaoPopup)

        val topo = localizacaoPopup[1]
        val base = topo + popup.contentView.height

        // Não altera a seleção enquanto o dedo ainda estiver fora da faixa.
        if (rawY < topo || rawY > base) return

        var melhorIndice = -1
        var menorDistancia = Float.MAX_VALUE

        botoesAlternativas.forEachIndexed { indice, botao ->
            val localizacao = IntArray(2)
            botao.getLocationOnScreen(localizacao)

            val centroX = localizacao[0] + botao.width / 2f
            val distancia = abs(rawX - centroX)

            if (distancia < menorDistancia) {
                menorDistancia = distancia
                melhorIndice = indice
            }
        }

        if (
            melhorIndice >= 0 &&
            melhorIndice != indiceAlternativaSelecionada
        ) {
            indiceAlternativaSelecionada = melhorIndice
            atualizarDestaqueAlternativa()
        }
    }

    private fun atualizarDestaqueAlternativa() {
        botoesAlternativas.forEachIndexed { indice, botao ->
            botao.background = criarFundo(
                if (indice == indiceAlternativaSelecionada) {
                    corSelecionado
                } else {
                    corEspecial
                }
            )

            botao.alpha =
                if (indice == indiceAlternativaSelecionada) 1f else 0.85f
        }
    }

    private fun confirmarAlternativaSelecionada() {
        val indice = indiceAlternativaSelecionada
        val alternativa = alternativasVisiveis.getOrNull(indice)

        fecharPopupAlternativas()

        if (alternativa != null && !campoSensivel()) {
            val caractere = if (
                maiusculas && alternativa.any { it.isLetter() }
            ) {
                alternativa.uppercase()
            } else {
                alternativa
            }

            currentInputConnection?.commitText(caractere, 1)
            adicionarAoBuffer(caractere)
            desativarMaiusculas()
        }
    }

    private fun fecharPopupAlternativas() {
        popupAtual?.dismiss()
        popupAtual = null
        botoesAlternativas.clear()
        alternativasVisiveis = emptyList()
        indiceAlternativaSelecionada = 0
    }

    private fun tratarTecla(tecla: String) {
        val ic = currentInputConnection ?: return
        val sensivel = campoSensivel()

        when (tecla) {
            "⌫" -> {
                ic.deleteSurroundingText(1, 0)

                if (modoBruto() && textoBruto.isNotEmpty()) {
                    textoBruto.deleteCharAt(textoBruto.length - 1)
                }
            }

            "Espaço" -> {
                ic.commitText(" ", 1)
                adicionarAoBuffer(" ")
                desativarMaiusculas()
            }

            "↵" -> {
                val textoAtual = (
                    ic.getTextBeforeCursor(4000, 0)?.toString().orEmpty() +
                    ic.getTextAfterCursor(4000, 0)?.toString().orEmpty()
                )

                if (!sensivel && textoAtual.isNotBlank()) {
                    registrarMensagem(textoAtual)
                    envioJaRegistrado = true
                    ultimoTextoNaoVazio = ""
                    handler.removeCallbacks(verificarLimpezaCampo)
                }

                val acao = currentInputEditorInfo?.imeOptions
                    ?.and(EditorInfo.IME_MASK_ACTION)
                    ?: EditorInfo.IME_ACTION_NONE

                if (
                    acao != EditorInfo.IME_ACTION_NONE &&
                    acao != EditorInfo.IME_ACTION_UNSPECIFIED
                ) {
                    ic.performEditorAction(acao)
                } else {
                    ic.commitText("\n", 1)
                }
            }

            "⇧" -> {
                maiusculas = !maiusculas
                atualizarTeclado()
            }

            "?123" -> {
                simbolos = true
                atualizarTeclado()
            }

            "ABC" -> {
                simbolos = false
                atualizarTeclado()
            }

            "🌐" -> {
                // Troca de idioma ainda não implementada.
            }

            else -> {
                val caractere = if (
                    maiusculas &&
                    tecla.length == 1 &&
                    tecla[0].isLetter()
                ) {
                    tecla.uppercase()
                } else {
                    tecla
                }

                if (!sensivel) {
                    adicionarAoBuffer(caractere)
                }

                ic.commitText(caractere, 1)
                desativarMaiusculas()
            }
        }
    }

    private fun desativarMaiusculas() {
        if (maiusculas) {
            maiusculas = false
            atualizarTeclado()
        }
    }

    private fun atualizarTeclado() {
        fecharPopupAlternativas()
        setInputView(criarTeclado())
    }

    private fun criarFundo(cor: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(cor)
            cornerRadius = dp(6).toFloat()
        }
    }

    private fun dp(valor: Int): Int =
        (valor * resources.displayMetrics.density).toInt()
}

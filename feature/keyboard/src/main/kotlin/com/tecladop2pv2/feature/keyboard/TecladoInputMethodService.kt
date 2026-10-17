
package com.tecladop2pv2.feature.keyboard

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.PopupWindow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TecladoInputMethodService : InputMethodService() {

    private val fundo = Color.rgb(35, 36, 40)
    private val corTecla = Color.rgb(65, 67, 73)
    private val corEspecial = Color.rgb(85, 91, 105)
    private val corPopup = Color.rgb(45, 47, 54)

    private var maiusculas = false
    private var simbolos = false
    private var textoBruto = StringBuilder()
    private var textoAntesDaEdicao = ""
    private var popupAtual: PopupWindow? = null

    private val preferencias by lazy {
        getSharedPreferences("teclado_p2p_historico", Context.MODE_PRIVATE)
    }

    override fun onCreateInputView(): View = criarTeclado()

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        fecharBufferBruto()
        textoAntesDaEdicao = ""
    }

    override fun onFinishInput() {
        fecharBufferBruto()
        popupAtual?.dismiss()
        popupAtual = null
        super.onFinishInput()
    }

    private fun campoSensivel(): Boolean {
        val tipo = currentInputEditorInfo?.inputType ?: return true
        val classe = tipo and InputType.TYPE_MASK_CLASS
        val variacao = tipo and InputType.TYPE_MASK_VARIATION

        if (classe == InputType.TYPE_CLASS_NUMBER &&
            variacao == InputType.TYPE_NUMBER_VARIATION_PASSWORD
        ) return true

        if (classe == InputType.TYPE_CLASS_TEXT &&
            variacao in setOf(
                InputType.TYPE_TEXT_VARIATION_PASSWORD,
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
            )
        ) return true

        return false
    }

    private fun registroAtivo(): Boolean =
        preferencias.getBoolean("registro_ativado", false)

    private fun modoBruto(): Boolean =
        preferencias.getBoolean("modo_bruto", false)

    private fun salvarRegistro(texto: String, tipo: String) {
        if (!registroAtivo() || campoSensivel() || texto.isBlank()) return

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

    private fun criarTeclado(): View {
        val teclado = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), dp(5), dp(4), dp(5))
            setBackgroundColor(fundo)
        }

        if (simbolos) {
            adicionarLinha(teclado, listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"))
            adicionarLinha(teclado, listOf("@", "#", "$", "%", "&", "-", "+", "(", ")", "/"))
            adicionarLinha(teclado, listOf("=", "*", "\"", "'", ":", ";", "!", "?", "⌫"))
            adicionarLinha(teclado, listOf("ABC", ",", "🌐", "Espaço", ".", "↵"))
        } else {
            adicionarLinha(teclado, listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"))
            adicionarLinha(teclado, listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"))
            adicionarLinha(teclado, listOf("⇧", "z", "x", "c", "v", "b", "n", "m", "⌫"))
            adicionarLinha(teclado, listOf("?123", ",", "🌐", "Espaço", ".", "↵"))
        }

        return teclado
    }

    private fun adicionarLinha(teclado: LinearLayout, teclas: List<String>) {
        val linha = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        teclas.forEach { texto ->
            val botao = Button(this).apply {
                text = when {
                    texto == "Espaço" -> ""
                    texto.length == 1 && texto[0].isLetter() && maiusculas ->
                        texto.uppercase()
                    else -> texto
                }

                textSize = if (texto == "Espaço") 14f else 18f
                isAllCaps = false
                setTextColor(Color.WHITE)
                typeface = Typeface.DEFAULT
                background = criarFundo(
                    if (texto in listOf("⇧", "⌫", "?123", "ABC", "↵", "🌐"))
                        corEspecial else corTecla
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

                setOnClickListener { tratarTecla(texto) }

                if (texto.length == 1 && texto[0].isLetter()) {
                    setOnLongClickListener {
                        mostrarCaracteresAlternativos(this, texto)
                        true
                    }
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

    private fun mostrarCaracteresAlternativos(origem: View, tecla: String) {
        popupAtual?.dismiss()

        val alternativas = when (tecla.lowercase()) {
            "a" -> listOf("á", "à", "â", "ã", "ä", "å", "æ")
            "c" -> listOf("ç", "ć", "č")
            "e" -> listOf("é", "è", "ê", "ë")
            "i" -> listOf("í", "ì", "î", "ï")
            "n" -> listOf("ñ", "ń")
            "o" -> listOf("ó", "ò", "ô", "õ", "ö", "ø", "œ")
            "u" -> listOf("ú", "ù", "û", "ü")
            "y" -> listOf("ý", "ÿ")
            "s" -> listOf("ß", "ś", "š")
            "z" -> listOf("ž", "ź", "ż")
            else -> emptyList()
        }

        if (alternativas.isEmpty()) return

        val linha = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(4), dp(4), dp(4), dp(4))
            setBackgroundColor(corPopup)
        }

        alternativas.forEach { alternativa ->
            val caractere = if (maiusculas) alternativa.uppercase() else alternativa

            val botao = Button(this).apply {
                text = caractere
                textSize = 18f
                isAllCaps = false
                setTextColor(Color.WHITE)
                background = criarFundo(corEspecial)
                minWidth = dp(42)
                minimumWidth = dp(42)
                minHeight = dp(48)
                minimumHeight = dp(48)
                setPadding(dp(3), 0, dp(3), 0)

                setOnClickListener {
                    if (campoSensivel()) {
                        popupAtual?.dismiss()
                        return@setOnClickListener
                    }

                    currentInputConnection?.commitText(caractere, 1)
                    adicionarAoBuffer(caractere)

                    if (registroAtivo() && !modoBruto()) {
                        // O texto completo será salvo quando a mensagem for enviada.
                    }

                    popupAtual?.dismiss()
                    popupAtual = null
                    desativarMaiusculas()
                }
            }

            linha.addView(
                botao,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    dp(48)
                ).apply {
                    setMargins(dp(2), 0, dp(2), 0)
                }
            )
        }

        val popup = PopupWindow(
            linha,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            dp(58),
            true
        ).apply {
            isOutsideTouchable = true
            elevation = dp(8).toFloat()
        }

        popupAtual = popup
        popup.showAsDropDown(origem, 0, -origem.height - dp(62), Gravity.CENTER)
    }

    private fun tratarTecla(tecla: String) {
        val ic = currentInputConnection ?: return
        val sensivel = campoSensivel()

        when (tecla) {
            "⌫" -> {
                // Apagar não é registrado como evento nem adicionado ao buffer.
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
                val textoAtual = ic.getTextBeforeCursor(4000, 0)?.toString().orEmpty()

                if (!sensivel && registroAtivo()) {
                    if (modoBruto()) {
                        // Uma entrada bruta por mensagem digitada.
                        fecharBufferBruto()
                    }

                    // Salva a mensagem completa quando o usuário envia.
                    if (textoAtual.isNotBlank()) {
                        salvarRegistro(textoAtual.trim(), "mensagem")
                    }
                }

                val acao = currentInputEditorInfo?.imeOptions
                    ?.and(EditorInfo.IME_MASK_ACTION)
                    ?: EditorInfo.IME_ACTION_NONE

                if (acao != EditorInfo.IME_ACTION_NONE &&
                    acao != EditorInfo.IME_ACTION_UNSPECIFIED
                ) {
                    ic.performEditorAction(acao)
                } else {
                    ic.commitText("\n", 1)
                }

                textoAntesDaEdicao = ""
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
                // A troca de idioma será implementada depois.
            }

            else -> {
                val caractere = if (maiusculas) tecla.uppercase() else tecla

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


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
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TecladoInputMethodService : InputMethodService() {

    private val fundo = Color.rgb(35, 36, 40)
    private val corTecla = Color.rgb(65, 67, 73)
    private val corEspecial = Color.rgb(85, 91, 105)

    private var maiusculas = false
    private var simbolos = false

    private val preferencias by lazy {
        getSharedPreferences("teclado_p2p_historico", Context.MODE_PRIVATE)
    }

    override fun onCreateInputView(): View = criarTeclado()

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
    }

    private fun campoSensivel(): Boolean {
        val tipo = currentInputEditorInfo?.inputType ?: return true
        val classe = tipo and InputType.TYPE_MASK_CLASS
        val variacao = tipo and InputType.TYPE_MASK_VARIATION

        return classe == InputType.TYPE_CLASS_NUMBER &&
            variacao == InputType.TYPE_NUMBER_VARIATION_PASSWORD ||
            classe == InputType.TYPE_CLASS_TEXT &&
            variacao in setOf(
                InputType.TYPE_TEXT_VARIATION_PASSWORD,
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
            ) ||
            classe == InputType.TYPE_CLASS_TEXT &&
            (tipo and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS) != 0
    }

    private fun registroAtivo(): Boolean =
        preferencias.getBoolean("registro_ativado", false)

    private fun modoBruto(): Boolean =
        preferencias.getBoolean("modo_bruto", false)

    private fun registrar(texto: String, tipo: String) {
        if (!registroAtivo() || campoSensivel() || texto.isEmpty()) return

        val pacote = currentInputEditorInfo?.packageName ?: "Aplicativo desconhecido"
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

        // Limita o histórico local aos 500 registros mais recentes.
        val limite = JSONArray()
        val inicio = (lista.length() - 500).coerceAtLeast(0)
        for (i in inicio until lista.length()) {
            limite.put(lista.getJSONObject(i))
        }

        preferencias.edit()
            .putString("registros", limite.toString())
            .apply()
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
                    "↵" -> "Enter"
                    "⇧" -> "Maiúsculas"
                    "?123" -> "Números e símbolos"
                    "ABC" -> "Voltar para letras"
                    "🌐" -> "Trocar idioma"
                    else -> texto
                }

                setOnClickListener { tratarTecla(texto) }
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

    private fun tratarTecla(tecla: String) {
        val ic = currentInputConnection ?: return
        val sensivel = campoSensivel()

        when (tecla) {
            "⌫" -> {
                if (modoBruto() && !sensivel) registrar("[APAGAR]", "tecla")
                ic.deleteSurroundingText(1, 0)
            }

            "Espaço" -> {
                if (modoBruto() && !sensivel) registrar("[ESPAÇO]", "tecla")
                ic.commitText(" ", 1)
                desativarMaiusculas()
            }

            "↵" -> {
                val antes = ic.getTextBeforeCursor(4000, 0)?.toString().orEmpty()
                if (!sensivel && antes.isNotBlank()) {
                    if (registroAtivo() && !modoBruto()) {
                        registrar(antes.trim(), "mensagem")
                    }
                    if (modoBruto()) registrar("[ENTER]", "tecla")
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
                if (modoBruto() && !sensivel) registrar(caractere, "tecla")
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

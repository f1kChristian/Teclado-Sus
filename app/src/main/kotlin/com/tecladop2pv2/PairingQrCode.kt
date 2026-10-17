
package com.tecladop2pv2

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

fun gerarQrCode(
    conteudo: String,
    tamanho: Int = 600
): Bitmap {
    require(conteudo.isNotBlank()) {
        "O conteúdo do QR Code não pode estar vazio."
    }

    require(tamanho > 0) {
        "O tamanho deve ser maior que zero."
    }

    val configuracoes = mapOf(
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
        EncodeHintType.MARGIN to 2
    )

    val matriz = QRCodeWriter().encode(
        conteudo,
        BarcodeFormat.QR_CODE,
        tamanho,
        tamanho,
        configuracoes
    )

    val pixels = IntArray(tamanho * tamanho)

    for (y in 0 until tamanho) {
        for (x in 0 until tamanho) {
            pixels[y * tamanho + x] =
                if (matriz[x, y]) Color.BLACK else Color.WHITE
        }
    }

    return Bitmap.createBitmap(
        pixels,
        tamanho,
        tamanho,
        Bitmap.Config.ARGB_8888
    )
}

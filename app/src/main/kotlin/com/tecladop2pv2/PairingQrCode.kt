package com.tecladop2pv2

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

fun gerarQrCode(conteudo: String, tamanho: Int = 600): Bitmap {
require(conteudo.isNotBlank()) { "O conteúdo do QR Code não pode estar vazio." }

val hints = mapOf(
    EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
    EncodeHintType.MARGIN to 2
)

val matriz = QRCodeWriter().encode(
    conteudo,
    BarcodeFormat.QR_CODE,
    tamanho,
    tamanho,
    hints
)

val pixels = IntArray(tamanho * tamanho)

for (y in 0 until tamanho) {
    for (x in 0 until tamanho) {
        pixels[y * tamanho + x] =
            if (matriz[x, y]) android.graphics.Color.BLACK
            else android.graphics.Color.WHITE
    }
}

return Bitmap.createBitmap(
    pixels,
    tamanho,
    tamanho,
    Bitmap.Config.ARGB_8888
)

}

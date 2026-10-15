
@Composable
private fun PairingScreen(modifier: Modifier) {
    Column(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Desconectado", color = Color(0xFFB3261E), style = MaterialTheme.typography.titleLarge)
        Text("O estado também é anunciado por leitores de tela; a cor não é o único indicador.")
        Text("Pareamento por código e QR ainda não está habilitado porque não há servidor de sinalização HTTPS/WSS configurado. Nenhuma conexão é simulada e nenhum texto sai deste dispositivo.")
        OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Text("Gerar código / QR indisponível") }
        Text("Para habilitar: implante um backend de sinalização temporária autenticado, configure HTTPS/WSS e TURN com credenciais efêmeras. Nunca coloque segredos no APK.", style = MaterialTheme.typography.bodySmall)
    }
}

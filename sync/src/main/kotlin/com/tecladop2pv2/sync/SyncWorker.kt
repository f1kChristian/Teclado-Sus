
package com.tecladop2pv2.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        // O mecanismo de sincronização será integrado posteriormente.
        // Não marcar registros como sincronizados sem confirmação do dispositivo remoto.
        return Result.success()
    }
}

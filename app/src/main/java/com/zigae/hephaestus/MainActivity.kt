package com.zigae.hephaestus

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.zigae.hephaestus.service.DetectionService
import com.zigae.hephaestus.ui.HephApp as HephUi

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        // 설치 직후에는 부팅 리시버가 동작하지 않으므로 첫 실행에서 서비스를 띄운다.
        DetectionService.start(this, "activity")
        val graph = HephApp.graph(this)
        setContent { HephUi(graph) }
    }
}

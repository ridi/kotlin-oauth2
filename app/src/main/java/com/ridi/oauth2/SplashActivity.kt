package com.ridi.oauth2

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import java.util.ArrayList

class SplashActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val permissionListener = object : PermissionListener {
            override fun onPermissionGranted() {
                val intent = Intent(this@SplashActivity, MainActivity::class.java)
                startActivity(intent)
                finish()
            }

            override fun onPermissionDenied(deniedPermissions: ArrayList<String>) {
                Toast.makeText(this@SplashActivity, "권한을 설정하셔야 이용하실 수 있습니다.", Toast.LENGTH_SHORT)
                    .show()
                finish()
            }
        }

        TedPermission.with(this@SplashActivity)
            .setPermissionListener(permissionListener)
            .setDeniedMessage("권한을 설정하지 않으시면 서비스를 이용하실 수 없습니다")
            .setPermissions(Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .check()
    }
}

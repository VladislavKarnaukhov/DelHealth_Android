package ru.delhealth.app.health

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ru.delhealth.app.BuildConfig

/** Открывает политику конфиденциальности сайта: на неё ссылается окно разрешений Health Connect. */
class PermissionsRationaleActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.SITE_URL + "privacy")))
        finish()
    }
}

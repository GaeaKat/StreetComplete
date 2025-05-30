package de.westnordost.streetcomplete.screens

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import de.westnordost.streetcomplete.data.preferences.Preferences
import de.westnordost.streetcomplete.util.getSelectedLocale
import de.westnordost.streetcomplete.util.getSystemLocales
import de.westnordost.streetcomplete.util.ktx.addedToFront
import org.koin.android.ext.android.inject
import java.util.Locale

open class BaseActivity : AppCompatActivity {
    constructor() : super()
    constructor(@LayoutRes contentLayoutId: Int) : super(contentLayoutId)

    private val prefs: Preferences by inject()

    private var locale: Locale? = null

    override fun attachBaseContext(base: Context) {
        val locale = getSelectedLocale(prefs)
        this.locale = locale

        var newBase = base

        if (locale != null) {
            val locales = getSystemLocales().addedToFront(locale)
            LocaleList.setDefault(locales)
            newBase = base.createConfigurationContext(Configuration().also { it.setLocales(locales) })
        }

        super.attachBaseContext(newBase)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        // this is called for example on screen rotate. It (sometimes?) overwrites the previously
        // set locale with the system default locale.
        val locale = locale
        if (locale != null) {
            val locales = getSystemLocales().addedToFront(locale)
            newConfig.setLocales(locales)
        }
        super.onConfigurationChanged(newConfig)
    }

    override fun onRestart() {
        super.onRestart()
        // force restart if the locale changed while the activity was in background
        if (locale != getSelectedLocale(prefs)) {
            ActivityCompat.recreate(this)
        }
    }
}

# Методы моста вызываются из JavaScript по имени — R8 не должен их переименовывать.
-keepclassmembers class ru.delhealth.app.NativeBridge {
    @android.webkit.JavascriptInterface <methods>;
}

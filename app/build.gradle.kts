plugins { id("com.android.application") }
android {
  namespace = "com.ninjagaming.privatevpn"
  compileSdk = 35
  defaultConfig {
    applicationId = "com.ninjagaming.privatevpn"
    minSdk = 26
    targetSdk = 35
    versionCode = 3
    versionName = "0.3-beta"
  }
  buildFeatures { aidl = true }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
}

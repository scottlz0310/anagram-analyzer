package com.anagram.analyzer

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp(Application::class)
class AnagramApplication : Hilt_AnagramApplication()

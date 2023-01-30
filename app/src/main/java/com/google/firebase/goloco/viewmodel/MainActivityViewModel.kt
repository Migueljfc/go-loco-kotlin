package com.google.firebase.goloco.viewmodel

import androidx.lifecycle.ViewModel

import com.google.firebase.goloco.Filters


/**
 * ViewModel for [com.google.firebase.goloco.MainActivity].
 */

class MainActivityViewModel : ViewModel() {

    var isSigningIn: Boolean = false
    var filters: Filters = Filters.default
}

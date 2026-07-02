package com.lomito.seguro.ui.auth

import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavDirections
import com.lomito.seguro.R

public class RegisterFragmentDirections private constructor() {
  public companion object {
    public fun actionRegisterToHome(): NavDirections =
        ActionOnlyNavDirections(R.id.action_register_to_home)
  }
}

package com.lomito.seguro.ui.auth

import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavDirections
import com.lomito.seguro.R

public class LoginFragmentDirections private constructor() {
  public companion object {
    public fun actionLoginToHome(): NavDirections =
        ActionOnlyNavDirections(R.id.action_login_to_home)

    public fun actionLoginToRegister(): NavDirections =
        ActionOnlyNavDirections(R.id.action_login_to_register)
  }
}

package com.lomito.seguro

import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavDirections

public class NavGraphDirections private constructor() {
  public companion object {
    public fun actionGlobalLogout(): NavDirections =
        ActionOnlyNavDirections(R.id.action_global_logout)
  }
}

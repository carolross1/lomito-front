package com.lomito.seguro.ui.simulator

import androidx.navigation.NavDirections
import com.lomito.seguro.NavGraphDirections

public class SimulatorFragmentDirections private constructor() {
  public companion object {
    public fun actionGlobalLogout(): NavDirections = NavGraphDirections.actionGlobalLogout()
  }
}

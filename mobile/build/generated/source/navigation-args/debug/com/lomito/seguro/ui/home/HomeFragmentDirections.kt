package com.lomito.seguro.ui.home

import android.os.Bundle
import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavDirections
import com.lomito.seguro.NavGraphDirections
import com.lomito.seguro.R
import kotlin.Int
import kotlin.String

public class HomeFragmentDirections private constructor() {
  private data class ActionHomeToMascotaDetail(
    public val mascotaId: String,
  ) : NavDirections {
    public override val actionId: Int = R.id.action_home_to_mascota_detail

    public override val arguments: Bundle
      get() {
        val result = Bundle()
        result.putString("mascotaId", this.mascotaId)
        return result
      }
  }

  private data class ActionHomeToCrearMascota(
    public val mascotaId: String? = null,
  ) : NavDirections {
    public override val actionId: Int = R.id.action_home_to_crear_mascota

    public override val arguments: Bundle
      get() {
        val result = Bundle()
        result.putString("mascotaId", this.mascotaId)
        return result
      }
  }

  public companion object {
    public fun actionHomeToLogin(): NavDirections =
        ActionOnlyNavDirections(R.id.action_home_to_login)

    public fun actionHomeToMascotaDetail(mascotaId: String): NavDirections =
        ActionHomeToMascotaDetail(mascotaId)

    public fun actionHomeToCrearMascota(mascotaId: String? = null): NavDirections =
        ActionHomeToCrearMascota(mascotaId)

    public fun actionHomeToAlertas(): NavDirections =
        ActionOnlyNavDirections(R.id.action_home_to_alertas)

    public fun actionHomeToRefugios(): NavDirections =
        ActionOnlyNavDirections(R.id.action_home_to_refugios)

    public fun actionHomeToSimulator(): NavDirections =
        ActionOnlyNavDirections(R.id.action_home_to_simulator)

    public fun actionHomeToMural(): NavDirections =
        ActionOnlyNavDirections(R.id.action_home_to_mural)

    public fun actionGlobalLogout(): NavDirections = NavGraphDirections.actionGlobalLogout()
  }
}

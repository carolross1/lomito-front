package com.lomito.seguro.ui.mascota

import android.os.Bundle
import androidx.navigation.NavDirections
import com.lomito.seguro.NavGraphDirections
import com.lomito.seguro.R
import kotlin.Int
import kotlin.String

public class MascotaDetailFragmentDirections private constructor() {
  private data class ActionMascotaDetailToEditar(
    public val mascotaId: String? = null,
  ) : NavDirections {
    public override val actionId: Int = R.id.action_mascota_detail_to_editar

    public override val arguments: Bundle
      get() {
        val result = Bundle()
        result.putString("mascotaId", this.mascotaId)
        return result
      }
  }

  public companion object {
    public fun actionMascotaDetailToEditar(mascotaId: String? = null): NavDirections =
        ActionMascotaDetailToEditar(mascotaId)

    public fun actionGlobalLogout(): NavDirections = NavGraphDirections.actionGlobalLogout()
  }
}

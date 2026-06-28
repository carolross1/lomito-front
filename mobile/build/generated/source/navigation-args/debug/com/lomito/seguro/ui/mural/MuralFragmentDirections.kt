package com.lomito.seguro.ui.mural

import android.os.Bundle
import androidx.navigation.NavDirections
import com.lomito.seguro.R
import kotlin.Int
import kotlin.String

public class MuralFragmentDirections private constructor() {
  private data class ActionMuralToMascotaDetail(
    public val mascotaId: String,
  ) : NavDirections {
    public override val actionId: Int = R.id.action_mural_to_mascota_detail

    public override val arguments: Bundle
      get() {
        val result = Bundle()
        result.putString("mascotaId", this.mascotaId)
        return result
      }
  }

  public companion object {
    public fun actionMuralToMascotaDetail(mascotaId: String): NavDirections =
        ActionMuralToMascotaDetail(mascotaId)
  }
}

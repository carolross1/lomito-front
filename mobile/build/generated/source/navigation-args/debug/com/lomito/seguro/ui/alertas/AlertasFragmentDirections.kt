package com.lomito.seguro.ui.alertas

import android.os.Bundle
import androidx.navigation.NavDirections
import com.lomito.seguro.R
import kotlin.Int
import kotlin.String

public class AlertasFragmentDirections private constructor() {
  private data class ActionAlertasToMascotaDetail(
    public val mascotaId: String,
  ) : NavDirections {
    public override val actionId: Int = R.id.action_alertas_to_mascota_detail

    public override val arguments: Bundle
      get() {
        val result = Bundle()
        result.putString("mascotaId", this.mascotaId)
        return result
      }
  }

  public companion object {
    public fun actionAlertasToMascotaDetail(mascotaId: String): NavDirections =
        ActionAlertasToMascotaDetail(mascotaId)
  }
}

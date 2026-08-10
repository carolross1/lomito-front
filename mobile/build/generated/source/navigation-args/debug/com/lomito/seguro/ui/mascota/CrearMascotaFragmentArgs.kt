package com.lomito.seguro.ui.mascota

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavArgs
import kotlin.String
import kotlin.jvm.JvmStatic

public data class CrearMascotaFragmentArgs(
  public val mascotaId: String? = null,
) : NavArgs {
  public fun toBundle(): Bundle {
    val result = Bundle()
    result.putString("mascotaId", this.mascotaId)
    return result
  }

  public fun toSavedStateHandle(): SavedStateHandle {
    val result = SavedStateHandle()
    result.set("mascotaId", this.mascotaId)
    return result
  }

  public companion object {
    @JvmStatic
    public fun fromBundle(bundle: Bundle): CrearMascotaFragmentArgs {
      bundle.setClassLoader(CrearMascotaFragmentArgs::class.java.classLoader)
      val __mascotaId : String?
      if (bundle.containsKey("mascotaId")) {
        __mascotaId = bundle.getString("mascotaId")
      } else {
        __mascotaId = null
      }
      return CrearMascotaFragmentArgs(__mascotaId)
    }

    @JvmStatic
    public fun fromSavedStateHandle(savedStateHandle: SavedStateHandle): CrearMascotaFragmentArgs {
      val __mascotaId : String?
      if (savedStateHandle.contains("mascotaId")) {
        __mascotaId = savedStateHandle["mascotaId"]
      } else {
        __mascotaId = null
      }
      return CrearMascotaFragmentArgs(__mascotaId)
    }
  }
}

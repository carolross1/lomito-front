package com.lomito.seguro.ui.mascota

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavArgs
import java.lang.IllegalArgumentException
import kotlin.String
import kotlin.jvm.JvmStatic

public data class MascotaDetailFragmentArgs(
  public val mascotaId: String,
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
    public fun fromBundle(bundle: Bundle): MascotaDetailFragmentArgs {
      bundle.setClassLoader(MascotaDetailFragmentArgs::class.java.classLoader)
      val __mascotaId : String?
      if (bundle.containsKey("mascotaId")) {
        __mascotaId = bundle.getString("mascotaId")
        if (__mascotaId == null) {
          throw IllegalArgumentException("Argument \"mascotaId\" is marked as non-null but was passed a null value.")
        }
      } else {
        throw IllegalArgumentException("Required argument \"mascotaId\" is missing and does not have an android:defaultValue")
      }
      return MascotaDetailFragmentArgs(__mascotaId)
    }

    @JvmStatic
    public fun fromSavedStateHandle(savedStateHandle: SavedStateHandle): MascotaDetailFragmentArgs {
      val __mascotaId : String?
      if (savedStateHandle.contains("mascotaId")) {
        __mascotaId = savedStateHandle["mascotaId"]
        if (__mascotaId == null) {
          throw IllegalArgumentException("Argument \"mascotaId\" is marked as non-null but was passed a null value")
        }
      } else {
        throw IllegalArgumentException("Required argument \"mascotaId\" is missing and does not have an android:defaultValue")
      }
      return MascotaDetailFragmentArgs(__mascotaId)
    }
  }
}

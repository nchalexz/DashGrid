package net.prototux.aare.obdapp

import net.prototux.aare.R

/**
 * A car the Cockpit can be told about up front: what the adapter can't say — the engine's fuel
 * and size, the tank, the mass, the rev counter's range. Picking one fills those settings in;
 * they stay ordinary settings afterwards, so a number that's wrong for the car can be corrected
 * without leaving the profile.
 *
 * The profile does not change what's read: the values still come from the standard OBD2 PIDs the
 * car reports. It's the numbers the app needs around them (fuel use maths, the range, the dyno,
 * where the rev counter's red zone is).
 */
class CarProfile(
    /** Stored in the settings: never change it once released. */
    val id: String,
    val label: Int,
    val engine: ObdAppSettings.Engine,
    val fuel: ObdAppSettings.Fuel,
    val displacementCc: Int,
    val tankLiters: Int,
    val massKg: Int,
    val rpmMax: Int,
    val redline: Int,
)

object CarProfiles {
    /** The settings as they were before profiles, and the fallback for an unknown id. */
    val GENERIC = CarProfile(
        id = "",
        label = R.string.obd_profile_generic,
        engine = ObdAppSettings.Engine.AUTO,
        fuel = ObdAppSettings.Fuel.PETROL,
        displacementCc = 1600,
        tankLiters = 50,
        massKg = 1300,
        rpmMax = 7000,
        redline = 6500,
    )

    /**
     * VW Golf 7, 2.0 TDI 110 kW (CUNA, EA288) with the 7-speed wet DSG (DQ381). The rev counter
     * on a TDI ends around 5000, and the engine is done pulling by ~4500; about 1500 kg with the
     * driver is the dyno's mass; the tank is 50 L.
     */
    val GOLF7_CUNA_DQ381 = CarProfile(
        id = "golf7-cuna-dq381",
        label = R.string.obd_profile_golf7_cuna,
        engine = ObdAppSettings.Engine.TURBO,
        fuel = ObdAppSettings.Fuel.DIESEL,
        displacementCc = 1968,
        tankLiters = 50,
        massKg = 1500,
        rpmMax = 5000,
        redline = 4600,
    )

    val ALL = listOf(GENERIC, GOLF7_CUNA_DQ381)

    fun byId(id: String?): CarProfile = ALL.firstOrNull { it.id == id } ?: GENERIC
}

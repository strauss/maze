package de.dreamcube.mazegame.common.control

data class ReducedServerInformationDto(
    val id: Int,
    val maxClients: Int,
    val activeClients: Int,
    val speed: Int,
    val width: Int,
    val height: Int,
    val spectatorName: String? = null
) {
    override fun toString(): String = "$id ($width x $height)"
}
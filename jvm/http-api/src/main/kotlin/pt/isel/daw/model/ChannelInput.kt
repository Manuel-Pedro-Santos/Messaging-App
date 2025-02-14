package pt.isel.daw.model

import pt.isel.daw.Controls
import pt.isel.daw.SelectionType

data class ChannelInput(
    val name: String,
    val type: SelectionType,
    val controls: Controls = Controls.PRIVATE,
)

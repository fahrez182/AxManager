package frb.axeron.manager.features.quicksettings

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.widget.Toast
import frb.axeron.manager.R

class AxeronTileService : android.service.quicksettings.TileService() {

    override fun onStartListening() {
        super.onStartListening()
        qsTile.apply {
            label = "AxTest"
            icon = Icon.createWithResource(this@AxeronTileService, R.drawable.ic_axeron)
            state = Tile.STATE_INACTIVE
            updateTile()
        }


    }

    override fun onClick() {
        super.onClick()
        Toast.makeText(this, "Clicked", Toast.LENGTH_SHORT).show()
    }


}
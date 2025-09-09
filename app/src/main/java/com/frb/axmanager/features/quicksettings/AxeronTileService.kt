package com.frb.axmanager.features.quicksettings

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast

class AxeronTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        qsTile.apply {
            label = "AxTest"
            icon = Icon.createWithResource(this@AxeronTileService, com.frb.engine.R.drawable.ic_axeron)
            state = Tile.STATE_INACTIVE
            updateTile()
        }


    }

    override fun onClick() {
        super.onClick()
        Toast.makeText(this, "Clicked", Toast.LENGTH_SHORT).show()
    }


}
package com.cronorep

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService

/** Receives the look settings from the phone app, also when the watch app is closed. */
class LookListenerService : WearableListenerService() {
    override fun onDataChanged(events: DataEventBuffer) {
        for (event in events) {
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == LookSync.PATH) {
                Look.update(this, LookSync.fromDataMap(DataMapItem.fromDataItem(event.dataItem).dataMap))
            }
        }
    }
}

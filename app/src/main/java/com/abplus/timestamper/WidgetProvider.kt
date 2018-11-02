package com.abplus.timestamper

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews

class WidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context?, appWidgetManager: AppWidgetManager?, appWidgetIds: IntArray?) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        if (context != null && appWidgetManager != null) {
            RemoteViews(context.packageName, R.layout.view_widget).let {
                it.setOnClickPendingIntent(R.id.image_view, launch(context))
                appWidgetManager.updateAppWidget(appWidgetIds, it)
            }
        }
    }

    private fun launch(context: Context): PendingIntent {
        val uri = Uri.parse(context.getString(R.string.intent_scheme) + "://foo")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

}
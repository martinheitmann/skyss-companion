package com.app.skyss_companion.workers

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.app.skyss_companion.R
import com.app.skyss_companion.prefs.AppSharedPrefs
import com.app.skyss_companion.repository.StopGroupRepository
import com.app.skyss_companion.widget.FavoriteWidgetService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class WidgetUpdateWorker @AssistedInject constructor(
    @Assisted val appContext: Context,
    @Assisted val workerParams: WorkerParameters,
    private val stopGroupRepository: StopGroupRepository,
    private val sharedPrefs: AppSharedPrefs
) : CoroutineWorker(appContext, workerParams) {
    val TAG = "WidgetUpdateWorker"

    override suspend fun doWork(): Result {
        try {
            Log.d(TAG, "UpdateService started")
            val manager = AppWidgetManager.getInstance(appContext)
            val stopIdentifier = inputData.getString("STOP_IDENTIFIER")
            val appWidgetId = inputData.getInt("APP_WIDGET_ID", -1)
            var stopGroupName: String? = null

            Log.d(TAG, "stopIdentifier passed to worker: $stopIdentifier")
            Log.d(TAG, "appWidgetId passed to worker: $appWidgetId")

            if(stopIdentifier != null){
                val widgetStopGroup = stopGroupRepository.findStopGroup(stopIdentifier)
                Log.d(TAG, "StopGroup fetched: $widgetStopGroup")
                if (widgetStopGroup != null) {
                    stopGroupName = widgetStopGroup.description
                }
            }

            val intent = Intent(appContext, FavoriteWidgetService::class.java).apply {
                // Add the app widget ID to the intent extras.
                putExtra("STOP_IDENTIFIER", stopIdentifier ?: "")
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            // Instantiate the RemoteViews object for the app widget layout.
            val rv = RemoteViews(appContext.packageName, R.layout.widget_view_favorited_stopgroup).apply {
                setRemoteAdapter(R.id.widget_stopgroup_listview, intent)
                setEmptyView(R.id.widget_stopgroup_listview, R.id.widget_stopgroup_tv_empty)
                if(stopGroupName != null){
                    setTextViewText(R.id.widget_stopgroup_tv_title, stopGroupName)
                }
            }

            manager.updateAppWidget(appWidgetId, rv)
            return Result.success()
        } catch (e: Throwable){
            Log.d(TAG, e.stackTraceToString())
            return Result.failure()
        }
    }

}
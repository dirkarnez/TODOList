package com.alex.todolist

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.RemoteViews
import android.widget.Toast
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


/**
 * Implementation of App Widget functionality.
 */
class NewAppWidget : AppWidgetProvider() {
    // Create some member variables for the ExecutorService
    // and for the Handler that will update the UI from the main thread
    var mExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    var mHandler: Handler = Handler(Looper.getMainLooper())

    // Create an interface to respond with the result after processing
    interface OnProcessedListener {
        fun onProcessed(result: String?)
    }

    private fun processInBg(context: Context, finished: Boolean) {
        val listener: OnProcessedListener = object : OnProcessedListener {
            override fun onProcessed(result: String?) {
                // Use the handler so we're not trying to update the UI from the bg thread
                mHandler.post { // Update the UI here
                    Toast.makeText(
                        context,
                        result,
                        Toast.LENGTH_SHORT
                    ).show()


                    val views = RemoteViews(context.packageName, R.layout.new_app_widget)
                    views.setTextViewText(R.id.btnToggleBluetooth, result)


                    var appWidgetManager = AppWidgetManager.getInstance(context)
                    appWidgetManager.updateAppWidget(ComponentName(
                        context.packageName,
                        NewAppWidget::class.java.name
                    ), views)
                    // ...

                    // If we're done with the ExecutorService, shut it down.
                    // (If you want to re-use the ExecutorService,
                    // make sure to shut it down whenever everything's completed
                    // and you don't need it any more.)
                    if (finished) {
                        mExecutor.shutdown()
                    }
                }
            }
        }
        val backgroundRunnable =
            Runnable { // Perform your background operation(s) and set the result(s)

                // Create a very simple REST adapter which points the GitHub API.
                val retrofit = Retrofit.Builder()
                    .baseUrl(GitHubService.API_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                // Create an instance of our GitHub API interface.
                val github = retrofit.create(GitHubService.GitHub::class.java)

                // Create a call instance for looking up Retrofit contributors.
                val call = github.contributors("square", "retrofit")

                // Fetch and print a list of the contributors to the library.
                val contributors = call.execute().body()!!

                // Use the interface to pass along the result
                listener.onProcessed(contributors.stream().map { a -> a.login }.reduce ("") { sum, element -> sum + element })
            }
        mExecutor.execute(backgroundRunnable)
    }
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)

        if (intent != null && AppWidgetManager.ACTION_APPWIDGET_UPDATE == intent.action) {
            if (context != null) {
                processInBg(context, true)
            }
        }
    }
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    val appWidgetIds = appWidgetManager.getAppWidgetIds(
        ComponentName(context, NewAppWidget::class.java)
    )

    val intent = Intent(context, NewAppWidget::class.java)
    intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)


    //val widgetText = context.getString(R.string.appwidget_text)
    // Construct the RemoteViews object
    val views = RemoteViews(context.packageName, R.layout.new_app_widget)
    //views.setTextViewText(R.id.appwidget_text, widgetText)
    views.setOnClickPendingIntent(R.id.btnToggleBluetooth, PendingIntent.getBroadcast(context, 0, intent, 0))
    // Instruct the widget manager to update the widget
    appWidgetManager.updateAppWidget(appWidgetId, views)
}
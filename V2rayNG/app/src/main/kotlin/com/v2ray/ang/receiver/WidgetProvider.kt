package com.v2ray.ang.receiver

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import com.v2ray.ang.R
import com.v2ray.ang.AppConfig
import com.v2ray.ang.service.V2RayVpnService
import com.v2ray.ang.util.AngConfigManager
import com.v2ray.ang.util.MessageUtil
import com.v2ray.ang.util.Utils
import org.jetbrains.anko.toast

class WidgetProvider : AppWidgetProvider() {
    /**
     * 每次窗口小部件被更新都调用一次该方法
     */
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        val remoteViews = RemoteViews(context.packageName, R.layout.widget_switch)
        val intent = Intent(AppConfig.BROADCAST_ACTION_WIDGET_CLICK)
        val pendingIntent = PendingIntent.getBroadcast(context, R.id.layout_switch, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        remoteViews.setOnClickPendingIntent(R.id.layout_switch, pendingIntent)

        for (appWidgetId in appWidgetIds) {
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
        }
    }

    /**
     * 接收窗口小部件点击时发送的广播
     */
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (AppConfig.BROADCAST_ACTION_WIDGET_CLICK == intent.action) {
            if (Utils.isServiceRun(context, "com.v2ray.ang.service.V2RayVpnService")) {
                MessageUtil.sendMsg2Service(context, AppConfig.MSG_STATE_STOP, "")
                context.toast(R.string.toast_services_stop)
            } else {
                context.toast(R.string.toast_services_start)
                if (AngConfigManager.genStoreV2rayConfig()) {
                    V2RayVpnService.startV2Ray(context)
                }
            }
        }
    }

    /**
     * 每删除一次窗口小部件就调用一次
     */
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
    }

    /**
     * 当最后一个该窗口小部件删除时调用该方法
     */
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
    }

    /**
     * 当该窗口小部件第一次添加到桌面时调用该方法
     */
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
    }

    /**
     * 当小部件大小改变时
     */
    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }

    /**
     * 当小部件从备份恢复时调用该方法
     */
    override fun onRestored(context: Context, oldWidgetIds: IntArray, newWidgetIds: IntArray) {
        super.onRestored(context, oldWidgetIds, newWidgetIds)
    }
}

package zlc.season.rxdownload3.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.graphics.Color
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.O
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationCompat.Builder
import zlc.season.rxdownload3.R
import zlc.season.rxdownload3.core.*
import zlc.season.rxdownload3.core.DownloadConfig.context
import zlc.season.rxdownload3.extension.ApkInstallExtension


class NotificationFactoryImpl : NotificationFactory {
    private val channelId = "RxDownload"
    private val channelName = "RxDownload"

    private val map = mutableMapOf<RealMission, Builder>()

    override fun build(context: Context, mission: RealMission, status: Status): Notification? {
        createChannelForOreo(context, channelId, channelName)

        val builder = get(mission, context)

        return when (status) {
            is Suspend -> suspend(builder)
            is Waiting -> waiting(builder, context)
            is Downloading -> downloading(builder, status, context)
            is Failed -> failed(builder, context)
            is Succeed -> succeed(builder, context)
            is ApkInstallExtension.Installing -> installing(builder)
            is ApkInstallExtension.Installed -> installed(builder)
            is Deleted -> deleted(context, mission)
            else -> {
                Notification()
            }
        }
    }

    private fun deleted(context: Context, mission: RealMission): Notification? {
        val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(mission.hashCode())
        return null
    }

    private fun installed(builder: Builder): Notification {
        builder.setContentText("安装完成")
        dismissProgress(builder)
        return builder.build()
    }

    private fun installing(builder: Builder): Notification {
        builder.setContentText("安装中")
        dismissProgress(builder)
        return builder.build()
    }

    private fun suspend(builder: Builder): Notification {
        builder.setContentText("已暂停")
        dismissProgress(builder)
        return builder.build()
    }

    private fun succeed(builder: Builder, context: Context): Notification {
        builder.setContentText(context.resources.getString(R.string.download_success))
        builder.priority = NotificationCompat.PRIORITY_MAX
        dismissProgress(builder)
        return builder.build()
    }

    private fun downloading(builder: Builder, status: Status, context: Context): Notification {
        builder.setContentText(context.resources.getString(R.string.downloading) )
        if (status.chunkFlag) {
            builder.setProgress(0, 0, true)
        } else {
            builder.setProgress(status.totalSize.toInt(), status.downloadSize.toInt(), false)
        }
        return builder.build()
    }

    private fun failed(builder: Builder, context: Context): Notification {
        builder.setContentText(context.resources.getString(R.string.failed))
        dismissProgress(builder)
        return builder.build()
    }

    private fun waiting(builder: Builder, context: Context): Notification {
        builder.setContentText(context.resources.getString(R.string.pending))
        builder.setProgress(0, 0, true)
        return builder.build()
    }


    private fun dismissProgress(builder: Builder) {
        builder.setProgress(0, 0, false)
    }

    private fun get(mission: RealMission, context: Context): Builder {
        var builder = map[mission]
        if (builder == null) {
            builder = createNotificationBuilder(mission, context)
            map.put(mission, builder)
        }

        return builder.setContentTitle(mission.actual.saveName)
    }

    private fun createNotificationBuilder(mission: RealMission, context: Context): Builder {
        return Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_download)
                .setContentTitle(mission.actual.saveName)
    }

    private fun createChannelForOreo(context: Context, channelId: String, channelName: String) {
        if (SDK_INT >= O) {
            val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            channel.enableLights(true)
            channel.setShowBadge(true)
            channel.lightColor = Color.GREEN
            channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC

            notificationManager.createNotificationChannel(channel)
        }
    }
}
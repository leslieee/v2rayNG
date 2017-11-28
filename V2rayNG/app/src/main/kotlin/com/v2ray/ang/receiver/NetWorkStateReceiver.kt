package com.v2ray.ang.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.widget.Toast
import com.v2ray.ang.AppConfig
import com.v2ray.ang.util.MessageUtil


class NetWorkStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        var isConnected = false

        try {
            //检测API是不是小于23，因为到了API23之后getNetworkInfo(int networkType)方法被弃用
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {

                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val activeNetwork = cm.activeNetworkInfo
                if (activeNetwork != null) { // connected to the internet
                    if (activeNetwork.type == ConnectivityManager.TYPE_WIFI) {
                        // connected to wifi
                        isConnected = true
                        //Toast.makeText(context, activeNetwork.typeName, Toast.LENGTH_SHORT).show()
                    } else if (activeNetwork.type == ConnectivityManager.TYPE_MOBILE) {
                        // connected to the mobile provider's data plan
                        isConnected = true
                        //Toast.makeText(context, activeNetwork.typeName, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // not connected to the internet
                }
                //API大于23时使用下面的方式进行网络监听
            } else {
                //获得ConnectivityManager对象
                val connMgr = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                //获取所有网络连接的信息
                val networks = connMgr.allNetworks
                if (networks != null) {
                    //通过循环将网络信息逐个取出来
                    loop@ for (i in networks.indices) {
                        //获取ConnectivityManager对象对应的NetworkInfo对象
                        val networkInfo = connMgr.getNetworkInfo(networks[i])
                        if (networkInfo != null && networkInfo.isConnected) {
                            if (networkInfo.type == ConnectivityManager.TYPE_WIFI) {
                                // connected to wifi
                                isConnected = true
                                //Toast.makeText(context, networkInfo.typeName, Toast.LENGTH_SHORT).show()
                                break@loop
                            } else if (networkInfo.type == ConnectivityManager.TYPE_MOBILE) {
                                // connected to mobile
                                isConnected = true
                                //Toast.makeText(context, networkInfo.typeName, Toast.LENGTH_SHORT).show()
                                break@loop
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (isConnected) {
            sendMsg2Service(context)
        }
    }

    private fun sendMsg2Service(context: Context) {
        //Toast.makeText(context, "Restart v2ray", Toast.LENGTH_SHORT).show()
        MessageUtil.sendMsg2Service(context, AppConfig.MSG_STATE_RESTART_SOFT, "")
    }
}
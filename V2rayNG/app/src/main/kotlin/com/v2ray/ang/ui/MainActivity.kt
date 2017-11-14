package com.v2ray.ang.ui

import android.Manifest
import android.content.*
import android.net.VpnService
import android.os.*
import android.support.v7.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuItem
import com.tbruyelle.rxpermissions.RxPermissions
import com.v2ray.ang.R
import com.v2ray.ang.service.V2RayVpnService
import com.v2ray.ang.util.AngConfigManager
import com.v2ray.ang.util.Utils
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.imageResource
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.toast
import android.os.Bundle
import android.view.KeyEvent
import com.v2ray.ang.AppConfig
import org.jetbrains.anko.startActivityForResult
import java.lang.ref.SoftReference
import android.view.KeyEvent.KEYCODE_BACK


class MainActivity : BaseActivity() {
    companion object {
        private const val REQUEST_CODE_VPN_PREPARE = 0
        private const val REQUEST_SCAN = 1
    }

    var fabChecked = false
        set(value) {
            field = value
            adapter.changeable = !value
            if (value) {
                fab.imageResource = R.drawable.ic_fab_check
            } else {
                fab.imageResource = R.drawable.ic_fab_uncheck
            }
        }

    private val adapter by lazy { MainRecyclerAdapter(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fab.setOnClickListener {
            if (fabChecked) {
                sendMsg(AppConfig.MSG_STATE_STOP, "")
            } else {
                val intent = VpnService.prepare(this)
                if (intent == null)
                    startV2Ray()
                else
                    startActivityForResult(intent, REQUEST_CODE_VPN_PREPARE)
            }
        }

        recycler_view.layoutManager = LinearLayoutManager(this)
        recycler_view.adapter = adapter
    }

    fun startV2Ray() {
        toast(R.string.toast_services_start)
        if (AngConfigManager.genStoreV2rayConfig()) {
            V2RayVpnService.startV2Ray(this)
        }
    }

    override fun onStart() {
        super.onStart()
        fabChecked = false

//        val intent = Intent(this.applicationContext, V2RayVpnService::class.java)
//        intent.`package` = AppConfig.ANG_PACKAGE
//        bindService(intent, mConnection, BIND_AUTO_CREATE)

        mMsgReceive = ReceiveMessageHandler(this@MainActivity)
        registerReceiver(mMsgReceive, IntentFilter(AppConfig.BROADCAST_ACTION_ACTIVITY))
        sendMsg(AppConfig.MSG_REGISTER_CLIENT, "")
    }

    override fun onStop() {
        super.onStop()
//        unbindService(mConnection)
        unregisterReceiver(mMsgReceive)
        mMsgReceive = null
    }

    public override fun onResume() {
        super.onResume()
        adapter.updateConfigList()
    }

    public override fun onPause() {
        super.onPause()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_VPN_PREPARE ->
                startV2Ray()
            REQUEST_SCAN ->
                importConfig(data?.getStringExtra("SCAN_RESULT"))
//            IntentIntegrator.REQUEST_CODE -> {
//                if (resultCode == RESULT_CANCELED) {
//                } else {
//                    val scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
//                    importConfig(scanResult.contents)
//                }
//            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.import_qrcode -> {
            importQRcode()
            true
        }
        R.id.import_clipboard -> {
            importClipboard()
            true
        }
        R.id.import_manually -> {
            startActivity<ServerActivity>("position" to -1, "isRunning" to fabChecked)
            adapter.updateConfigList()
            true
        }
        R.id.settings -> {
            startActivity<SettingsActivity>("isRunning" to fabChecked)
            true
        }
        else -> super.onOptionsItemSelected(item)
    }


    override fun onBackPressed() {
        super.onBackPressed()
    }

    /**
     * import config from qrcode
     */
    fun importQRcode(): Boolean {
        try {
            startActivityForResult(Intent("com.google.zxing.client.android.SCAN")
                    .addCategory(Intent.CATEGORY_DEFAULT)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP), REQUEST_SCAN)
        } catch (e: Exception) {
            RxPermissions.getInstance(this)
                    .request(Manifest.permission.CAMERA)
                    .subscribe {
                        if (it)
                            startActivityForResult<ScannerActivity>(REQUEST_SCAN)
                        else
                            toast(R.string.toast_permission_denied)
                    }
        }
//        val integrator = IntentIntegrator(this)
//        integrator.initiateScan(IntentIntegrator.ALL_CODE_TYPES)
        return true
    }

    /**
     * import config from clipboard
     */
    fun importClipboard(): Boolean {
        try {
            val clipboard = Utils.getClipboard(this)
            importConfig(clipboard)
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return true
    }

    fun importConfig(server: String?) {
        if (server == null) {
            return
        }
        val resId = AngConfigManager.importConfig(server)
        if (resId > 0) {
            toast(resId)
        } else {
            toast(R.string.toast_success)
            adapter.updateConfigList()
        }
    }

//    val mConnection = object : ServiceConnection {
//        override fun onServiceDisconnected(name: ComponentName?) {
//        }
//
//        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
//            sendMsg(AppConfig.MSG_REGISTER_CLIENT, "")
//        }
//    }

    private var mMsgReceive: BroadcastReceiver? = null

    private class ReceiveMessageHandler(activity: MainActivity) : BroadcastReceiver() {
        internal var mReference: SoftReference<MainActivity> = SoftReference(activity)
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val activity = mReference.get()
            when (intent?.getIntExtra("key", 0)) {
                AppConfig.MSG_STATE_RUNNING -> {
                    activity?.fabChecked = true
                }
                AppConfig.MSG_STATE_NOT_RUNNING -> {
                    activity?.fabChecked = false
                }
                AppConfig.MSG_STATE_START_SUCCESS -> {
                    activity?.toast(R.string.toast_services_success)
                    activity?.fabChecked = true
                }
                AppConfig.MSG_STATE_START_FAILURE -> {
                    activity?.toast(R.string.toast_services_failure)
                    activity?.fabChecked = false
                }
                AppConfig.MSG_STATE_STOP_SUCCESS -> {
                    activity?.fabChecked = false
                }
            }
        }
    }

    fun sendMsg(what: Int, content: String) {
        try {
            val intent = Intent()
            intent.action = AppConfig.BROADCAST_ACTION_SERVICE
            intent.`package` = AppConfig.ANG_PACKAGE
            intent.putExtra("key", what)
            sendBroadcast(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(false)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
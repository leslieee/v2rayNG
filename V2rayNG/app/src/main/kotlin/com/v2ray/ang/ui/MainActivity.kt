package com.v2ray.ang.ui

import android.Manifest
import android.content.*
import android.net.Uri
import android.net.VpnService
import android.support.v7.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuItem
import com.tbruyelle.rxpermissions.RxPermissions
import com.v2ray.ang.R
import com.v2ray.ang.util.AngConfigManager
import com.v2ray.ang.util.Utils
import kotlinx.android.synthetic.main.activity_main.*
import android.os.Bundle
import android.text.TextUtils
import android.view.KeyEvent
import com.v2ray.ang.AppConfig
import com.v2ray.ang.util.MessageUtil
import com.v2ray.ang.util.V2rayConfigUtil
import org.jetbrains.anko.*
import java.lang.ref.SoftReference
import java.net.URL
import android.content.IntentFilter
import android.support.v7.app.AlertDialog
import android.view.View
import android.widget.Toast
import android.widget.TextView
import com.beust.klaxon.Parser
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException
import com.google.gson.Gson
// import com.squareup.haha.perflib.Main
import kotlinx.android.synthetic.main.alertdialog_login.view.*
import me.dozen.dpreference.DPreference

class MainActivity : BaseActivity() {
    companion object {
        private const val REQUEST_CODE_VPN_PREPARE = 0
        private const val REQUEST_SCAN = 1
        private const val REQUEST_FILE_CHOOSER = 2
        private const val REQUEST_SCAN_URL = 3
    }

    var okHttpClient: OkHttpClient? = null
    val loginAlert by lazy { AlertDialog.Builder(this@MainActivity).create() }

    var fabChecked = false
        set(value) {
            field = value
            adapter.changeable = !value
            if (value) {
                fab.imageResource = R.drawable.ic_start_connected
            } else {
                fab.imageResource = R.drawable.ic_start_idle
            }
        }

    private val adapter by lazy { MainRecyclerAdapter(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fab.setOnClickListener {
            if (fabChecked) {
                Utils.stopVService(this)
            } else {
                val intent = VpnService.prepare(this)
                if (intent == null) {
                    startV2Ray()
                } else {
                    // 没有vpn权限的话弹出授权界面
                    startActivityForResult(intent, REQUEST_CODE_VPN_PREPARE)
                }
            }
        }

        webviewbutton.setOnClickListener {
            startActivity<WebviewActivity>()
        }

        recycler_view.layoutManager = LinearLayoutManager(this)
        recycler_view.adapter = adapter

        // 1.拿到OkHttpClient对象
        okHttpClient = OkHttpClient().newBuilder().build()
        // 这里想办法判断登录状态(是否已保存过用户名密码)

        if (DPreference(this, packageName + "_preferences").getPrefBoolean("is_login", false)) {
            // 有的话直接doGet()
            // doGet()
        } else {
            // 没有话弹输入框 输入完成后调doGet()
            loginToGetConfiguration()
        }

    }

    fun loginToGetConfiguration() {
        val inflater = getLayoutInflater()
        val dialoglayout = inflater.inflate(R.layout.alertdialog_login, null)
        val subscribe = DPreference(this, packageName + "_preferences").getPrefString("subscribe", "")
        if (subscribe != "") {
            dialoglayout.subscribe.setText(subscribe, TextView.BufferType.EDITABLE)
        }
        dialoglayout.loginButton.setOnClickListener {
            // 先将用户名密码保存
            val subscribe = dialoglayout.subscribe.text.toString()
            if (subscribe == "") {
                Toast.makeText(this@MainActivity, "订阅地址不能为空", Toast.LENGTH_SHORT).show()
            } else {
                DPreference(this, packageName + "_preferences").setPrefString("subscribe", subscribe)

                doGet()
            }
        }
        loginAlert.setView(dialoglayout)
        loginAlert.setCanceledOnTouchOutside(false)
        loginAlert.show()
    }

    fun doGet() {
        // 创建request对象
        var builder = Request.Builder();
        // 获取本地保存的用户名密码
        val subscribe = DPreference(this, packageName + "_preferences").getPrefString("subscribe", "")

        // 准备开个接口, 不需要登录, 上传用户名密码然后拿到服务器配置信息
        var request = builder.get().url(subscribe).build();
        execute(request);
    }

    private fun execute(request: Request) {
        //封装成一个请求的任务
        val call = okHttpClient?.newCall(request)
        //同步的请求 Response response = call.execute();
        //  执行请求(异步的请求)
        call?.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // LogUtil.i("请求失败:" + e.toString())
                // println("请求失败:" + e.toString())
                runOnUiThread{
                    Toast.makeText(this@MainActivity, "更新服务器失败: " + e.toString(), Toast.LENGTH_LONG).show()
                }
           }

            override fun onResponse(call: Call, response: Response) {
                // LogUtil.i("请求成功:" + response.body().string())
                // println("请求成功:" + response.body().string())
                // 拿到用户配置 1. 存入本地 2. 生产列表 3. 设为活动服务器 4. 进入连接状态
                val str = response.body().string()
                // if (str.contains("ret") || str.contains("msg")) {
                //    runOnUiThread {
                //        Toast.makeText(this@MainActivity, "用户名或者密码错误", Toast.LENGTH_SHORT).show()
                //    }
                // } else {
                    runOnUiThread {
                        loginAlert.dismiss()
                    }
                    // 没有错误的情况下先保留登录状态
                    DPreference(this@MainActivity, packageName + "_preferences").setPrefBoolean("is_login", true)

                    val ss = Gson().fromJson(str, Array<String>::class.java)
                    // 插入之前把之前的清理掉
                    AngConfigManager.configs.vmess.clear()
                    for (s in ss)
                        importConfigNoToast(s)
                    // 最后来更新
                    runOnUiThread {
                        // 更新界面信息
                        Toast.makeText(this@MainActivity, "更新服务器成功", Toast.LENGTH_SHORT).show()
                        adapter.updateConfigList()
                        // 模拟点击fab
                        if (!fabChecked) {
                            fab.performClick()
                        }
                    }
                // }
            }
        })
    }

    fun parse(name: String) : Any? {
        val cls = Parser::class.java
        return cls.getResourceAsStream(name)?.let { inputStream ->
            return Parser().parse(inputStream)
        }
    }

    fun startV2Ray() {
        toast(R.string.toast_services_start)
        Utils.startVService(this)
    }

    override fun onStart() {
        super.onStart()
        // fabChecked = false

//        val intent = Intent(this.applicationContext, V2RayVpnService::class.java)
//        intent.`package` = AppConfig.ANG_PACKAGE
//        bindService(intent, mConnection, BIND_AUTO_CREATE)

        mMsgReceive = ReceiveMessageHandler(this@MainActivity)
        registerReceiver(mMsgReceive, IntentFilter(AppConfig.BROADCAST_ACTION_ACTIVITY))
        MessageUtil.sendMsg2Service(this, AppConfig.MSG_REGISTER_CLIENT, "")
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
        val isRunning = Utils.isServiceRun(this, "com.v2ray.ang.service.V2RayVpnService")
        if (!isRunning) {
            fabChecked = false;
            webviewbutton.visibility = View.INVISIBLE
        }
    }

    public override fun onPause() {
        super.onPause()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_VPN_PREPARE ->
                if (resultCode == RESULT_OK) {
                    startV2Ray()
                }
            REQUEST_SCAN ->
                if (resultCode == RESULT_OK) {
                    importConfig(data?.getStringExtra("SCAN_RESULT"))
                }
            REQUEST_FILE_CHOOSER -> {
                if (resultCode == RESULT_OK) {
                    val uri = data!!.data
                    readContentFromUri(uri)
                }
            }
            REQUEST_SCAN_URL ->
                if (resultCode == RESULT_OK) {
                    importConfigCustomUrl(data?.getStringExtra("SCAN_RESULT"))
                }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.import_qrcode -> {
            importQRcode(REQUEST_SCAN)
            true
        }
        R.id.import_login -> {
            loginToGetConfiguration()
            true
        }
        // R.id.import_clipboard -> {
        //    importClipboard()
        //    true
        // }
        R.id.import_manually -> {
            startActivity<ServerActivity>("position" to -1, "isRunning" to fabChecked)
            adapter.updateConfigList()
            true
        }
        // R.id.import_config_custom_local -> {
        //    importConfigCustomLocal()
        //    true
        // }
        // R.id.import_config_custom_url -> {
        //    importConfigCustomUrlClipboard()
        //    true
        // }
        // R.id.import_config_custom_url_scan -> {
        //    importQRcode(REQUEST_SCAN_URL)
        //    true
        // }
        R.id.settings -> {
            startActivity<SettingsActivity>("isRunning" to fabChecked)
            true
        }
        // R.id.logcat -> {
        //    startActivity<LogcatActivity>()
        //    true
        // }
        else -> super.onOptionsItemSelected(item)
    }


    /**
     * import config from qrcode
     */
    fun importQRcode(requestCode: Int): Boolean {
        try {
            startActivityForResult(Intent("com.google.zxing.client.android.SCAN")
                    .addCategory(Intent.CATEGORY_DEFAULT)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP), requestCode)
        } catch (e: Exception) {
            RxPermissions.getInstance(this)
                    .request(Manifest.permission.CAMERA)
                    .subscribe {
                        if (it)
                            startActivityForResult<ScannerActivity>(requestCode)
                        else
                            toast(R.string.toast_permission_denied)
                    }
        }
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

    fun importConfigNoToast(server: String?) {
        if (server == null) {
            return
        }
        val resId = AngConfigManager.importConfig(server)
        runOnUiThread {
            if (resId > 0) {
                toast(resId)
            } else {
                // toast(R.string.toast_success)
                // adapter.updateConfigList()
            }
        }
    }

    /**
     * import config from local config file
     */
    fun importConfigCustomLocal(): Boolean {
        try {
            showFileChooser()
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return true
    }

    fun importConfigCustomUrlClipboard(): Boolean {
        try {
            val url = Utils.getClipboard(this)
            if (TextUtils.isEmpty(url)) {
                toast(R.string.toast_none_data_clipboard)
                return false
            }
            return importConfigCustomUrl(url)
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * import config from url
     */
    fun importConfigCustomUrl(url: String?): Boolean {
        try {
            if (!Utils.isValidUrl(url)) {
                toast(R.string.toast_invalid_url)
                return false
            }
            doAsync {
                val configText = URL(url).readText()
                uiThread {
                    importCustomizeConfig(configText)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return true
    }

    /**
     * show file chooser
     */
    private fun showFileChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)

        try {
            startActivityForResult(
                    Intent.createChooser(intent, getString(R.string.title_file_chooser)),
                    REQUEST_FILE_CHOOSER)
        } catch (ex: android.content.ActivityNotFoundException) {
            toast(R.string.toast_require_file_manager)
        }
    }

    /**
     * read content from uri
     */
    private fun readContentFromUri(uri: Uri) {
        RxPermissions.getInstance(this)
                .request(Manifest.permission.READ_EXTERNAL_STORAGE)
                .subscribe {
                    if (it) {
                        try {
                            val inputStream = contentResolver.openInputStream(uri)
                            val configText = inputStream.bufferedReader().readText()
                            importCustomizeConfig(configText)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } else
                        toast(R.string.toast_permission_denied)
                }
    }

    /**
     * import customize config
     */
    fun importCustomizeConfig(server: String?) {
        if (server == null) {
            return
        }
        if (!V2rayConfigUtil.isValidConfig(server)) {
            toast(R.string.toast_config_file_invalid)
            return
        }
        val resId = AngConfigManager.importCustomizeConfig(server)
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
                    activity?.webviewbutton?.visibility = View.VISIBLE
                }
                AppConfig.MSG_STATE_NOT_RUNNING -> {
                    activity?.fabChecked = false
                    activity?.webviewbutton?.visibility = View.INVISIBLE
                }
                AppConfig.MSG_STATE_START_SUCCESS -> {
                    activity?.toast(R.string.toast_services_success)
                    activity?.fabChecked = true
                    activity?.webviewbutton?.visibility = View.VISIBLE
                    // 这里弹一个浏览器
                    // activity?.startActivity<WebviewActivity>()
                }
                AppConfig.MSG_STATE_START_FAILURE -> {
                    activity?.toast(R.string.toast_services_failure)
                    activity?.fabChecked = false
                    activity?.webviewbutton?.visibility = View.INVISIBLE
                }
                AppConfig.MSG_STATE_STOP_SUCCESS -> {
                    activity?.fabChecked = false
                    activity?.webviewbutton?.visibility = View.INVISIBLE
                }
            }
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
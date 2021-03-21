package com.libdumper

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.system.Os.chmod
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.*
import java.util.*

/*
    Credit :
    Lib By kp7742 : https://github.com/kp7742
*/
class MainActivity : AppCompatActivity() {
    private var mBit = 0
    private var isRoot: Boolean = false
    private lateinit var nProg: ProgressBar
    private lateinit var pkg: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        reqStorageLD()
        copyFolder("armeabi-v7a")
        copyFolder("arm64-v8a")
        nProg = findViewById(R.id.Progress)
        pkg = findViewById(R.id.pkg)
        findViewById<RadioGroup>(R.id.Bit).setOnCheckedChangeListener { _, checkedId ->
            run {
                when (checkedId) {
                    R.id.b32 -> mBit = 32
                    R.id.b64 -> mBit = 64
                    else -> Toast.makeText(this, "failed by id", Toast.LENGTH_SHORT).show()
                }
            }
        }
        findViewById<RadioGroup>(R.id.isRoot).setOnCheckedChangeListener { _, checkedId ->
            run {
                when (checkedId) {
                    R.id.root -> isRoot = true
                    R.id.nonroot -> isRoot = false
                    else -> Toast.makeText(this, "failed by id", Toast.LENGTH_SHORT).show()
                }
            }
        }
        findViewById<Button>(R.id.dumpUE4).setOnClickListener {
            if (mBit == 32 || mBit == 64) {
                nProg.visibility = View.VISIBLE
                runNative("ue4dumper", mBit)
            } else {
                Toast.makeText(this@MainActivity, "Please Select Arch", Toast.LENGTH_SHORT)
                    .show()
            }
        }
        findViewById<Button>(R.id.dumpil2cpp).setOnClickListener {
            when (mBit) {
                32 -> {
                    nProg.visibility = View.VISIBLE
                    runNative("il2cppdumper", mBit)
                }
                64 -> {
                    Toast.makeText(
                        this@MainActivity,
                        "Il2cppdump is not Support For arm64",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else -> Toast.makeText(
                    this@MainActivity,
                    "Please Select Arch",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun reqStorageLD() {
        val permission = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                10
            )
        }
    }

    private fun runNative(str: String, megaBit: Int) {
        var path = "${filesDir.path}/"
        if (megaBit == 32) {
            path += "armeabi-v7a/$str"
        } else if (megaBit == 64) {
            path += "arm64-v8a/$str"
        }
        chmod(path, 511)//set to 00777 perms

        Thread {
            Runtime.getRuntime()
                .exec(
                    arrayOf(
                        if (isRoot) "su -c $path" else path,
                        "--package",
                        pkg.text.toString(),
                        "--lib"
                    )
                )
                .waitFor()
            synchronized(this) {
                runOnUiThread {
                    Toast.makeText(this, "Dump success", Toast.LENGTH_SHORT).show()
                    nProg.visibility = View.GONE
                }
            }
        }.start()
    }

    private fun copyFolder(name: String) {
        val files: Array<String>? = assets.list(name)
        for (filename in files!!) {
            var `in`: InputStream?
            var out: OutputStream?
            val folder = File("${filesDir.path}/$name")
            var success = true
            if (!folder.exists()) {
                success = folder.mkdir()
            }
            if (success) {
                try {
                    `in` = assets.open("$name/$filename")
                    out = FileOutputStream("${filesDir.path}/$name/$filename")
                    copyFile(`in`, out)
                    `in`.close()
                    out.flush()
                    out.close()
                } catch (e: IOException) {
                    Log.e("ERROR", "Failed to copy asset file: $filename", e)
                }
            } else {
                Log.d("TAG", " Do something else on failure")
            }
        }
    }

    // Method used by copyAssets() on purpose to copy a file.
    @Throws(IOException::class)
    private fun copyFile(`in`: InputStream, out: OutputStream?) {
        val buffer = ByteArray(`in`.available())
        var read: Int
        while (`in`.read(buffer).also { read = it } != -1) {
            out!!.write(buffer, 0, read)
        }
    }
}

package com.libdumper

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.*
import java.lang.StringBuilder
import java.util.*
import kotlin.properties.Delegates

/*
    Credit :
    Lib By kp7742 : https://github.com/kp7742
*/
class MainActivity : AppCompatActivity() {
    private var mBit by Delegates.notNull<Int>()
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
        val mex: RadioGroup = findViewById(R.id.Bit)
        mex.setOnCheckedChangeListener { _, checkedId ->
            run {
                if (checkedId == 1) {
                    mBit = 32
                } else if (checkedId == 2) {
                    mBit = 64
                }
            }
        }
        val dumpUE4: Button = findViewById(R.id.dumpUE4)
        dumpUE4.setOnClickListener {
            if (mBit == 32 || mBit == 64) {
                nProg.visibility = View.VISIBLE
                runNative("ue4dumper", mBit)
            } else {
                Toast.makeText(this@MainActivity, "Please Select The Arch", Toast.LENGTH_SHORT)
                    .show()
            }
        }
        val dumpIL2CPP: Button = findViewById(R.id.dumpil2cpp)
        dumpIL2CPP.setOnClickListener {
            if (mBit == 64) {
                Toast.makeText(this@MainActivity, "Not Support For arm64", Toast.LENGTH_SHORT)
                    .show()
            } else {
                if (mBit == 32) {
                    nProg.visibility = View.VISIBLE
                    runNative("il2cppdumper", mBit)
                } else {
                    Toast.makeText(this@MainActivity, "Please Select The Arch", Toast.LENGTH_SHORT)
                        .show()
                }
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
        val path = StringBuilder()
        path.append("${filesDir.path}/")
        if (megaBit == 32) {
            path.append("armeabi-v7a/$str")
        } else if (megaBit == 64) {
            path.append("arm64-v8a/$str")
        }
        try {
            Runtime.getRuntime().exec(arrayOf("chmod", "777", path.toString()))
            Runtime.getRuntime()
                .exec(
                    arrayOf(
                        path.toString(),
                        "--package",
                        pkg.text.toString(),
                        "--lib"
                    )
                )
                .waitFor()
            Toast.makeText(this, "Done", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Please Re-Try Again\n${e.message}", Toast.LENGTH_SHORT).show()
        }
        nProg.visibility = View.GONE
    }

    private fun copyFolder(name: String) {
        val assetManager = assets
        var files: Array<String>? = null
        try {
            files = assetManager.list(name)
        } catch (e: IOException) {
            Log.e("ERROR", "Failed to get asset file list.", e)
        }
        for (filename in files!!) {
            var `in`: InputStream? = null
            var out: OutputStream? = null
            val folder = File("${filesDir.path}/$name")
            var success = true
            if (!folder.exists()) {
                success = folder.mkdir()
            }
            if (success) {
                try {
                    `in` = assetManager.open("$name/$filename")
                    out = FileOutputStream("${filesDir.path}/$name/$filename")
                    copyFile(`in`, out)
                    `in`.close()
                    out.flush()
                    out.close()
                } catch (e: IOException) {
                    Log.e("ERROR", "Failed to copy asset file: $filename", e)
                } finally {
                    `in`!!.close()
                    out!!.flush()
                    out.close()
                }
            } else {
                Log.d("TAG", " Do something else on failure")
            }
        }
    }

    // Method used by copyAssets() on purpose to copy a file.
    @Throws(IOException::class)
    private fun copyFile(`in`: InputStream, out: OutputStream?) {
        val buffer = ByteArray(1024)
        var read: Int
        while (`in`.read(buffer).also { read = it } != -1) {
            out!!.write(buffer, 0, read)
        }
    }
}

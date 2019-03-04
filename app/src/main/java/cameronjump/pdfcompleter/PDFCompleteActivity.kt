package cameronjump.pdfcompleter

import android.Manifest
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import kotlinx.android.synthetic.main.activity_pdf.*
import java.io.File


class PDFCompleteActivity : AppCompatActivity(), PDFListAdapter.PDFCompleteInterface {

    private val TAG = "PDFActivityDebug"

    private lateinit var root: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf)

        val fileName = intent.getStringExtra("fileName")

        if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this as MainActivity,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
        }
        else {
            //Add ListView params
            val viewAdapter = PDFListAdapter(fileName, this)
            list_view.adapter = viewAdapter
        }
    }

    override fun completePDF(path: String) {
    }




}

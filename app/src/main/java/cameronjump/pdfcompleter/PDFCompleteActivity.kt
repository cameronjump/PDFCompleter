package cameronjump.pdfcompleter

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File


class PDFCompleteActivity : AppCompatActivity(), PDFListAdapter.PDFCompleteInterface {

    private val TAG = "MainActivityDebug"

    private lateinit var root: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Add ListView params
        val viewAdapter = PDFListAdapter("2-Qualified-Scientist.pdf", this)
        list_view.adapter = viewAdapter
    }

    override fun completePDF(path: String) {
    }




}

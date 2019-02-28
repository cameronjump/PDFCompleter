package cameronjump.pdfcompleter

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDField
import com.tom_roush.pdfbox.util.PDFBoxResourceLoader
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import android.graphics.Bitmap
import com.tom_roush.pdfbox.rendering.PDFRenderer
import java.io.FileOutputStream


class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivityDebug"

    private lateinit var root: File
    private lateinit var assetManager: AssetManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setup()

        //Setup document
        val document = PDDocument.load(assetManager.open("1-Checklist-for-Adult-Sponsor.pdf"))

        val docCatalog = document.documentCatalog
        val acroForm = docCatalog.acroForm

        val renderer = PDFRenderer(document)
        // Render the image to an RGB Bitmap
        val pageImage = renderer.renderImage(0, 1f, Bitmap.Config.RGB_565)

        // Save the render result to an image
        val path = root.absolutePath + "/render.jpg"
        val renderFile = File(path)
        val fileOut = FileOutputStream(renderFile)
        pageImage.compress(Bitmap.CompressFormat.JPEG, 100, fileOut)
        fileOut.close()

        //Adapter list initialization
        val list = mutableListOf<PDFCompleteAdapter.ViewItem>()

        //Add PDF preview
        val preview = PDFCompleteAdapter.ViewItem(name="PDF Preview",
                type=PDFCompleteAdapter.ViewType.IMAGE,
                url=root.absolutePath+"/render.jpg")
        list.add(preview)

        // Add PDF forms
        for (field in acroForm.fields) {
            list.add(parseViewItem(field))
        }
        Log.d(TAG, list.toString())

        //Add RecyclerView params
        val viewManager = LinearLayoutManager(this)
        val viewAdapter = PDFCompleteAdapter(list, this)
        recycler_view.layoutManager = viewManager
        recycler_view.adapter = viewAdapter
    }

    private fun parseViewItem(field: PDField): PDFCompleteAdapter.ViewItem {
        if(field.partialName.toString().contains("Signature")) {
            return PDFCompleteAdapter.ViewItem(name=field.partialName,
                    type=PDFCompleteAdapter.ViewType.SIGNATURE)
        }
        return PDFCompleteAdapter.ViewItem(name=field.partialName,
                type=PDFCompleteAdapter.ViewType.valueOf(field.fieldType))
    }

    private fun setup() {
        // Enable Android-style asset loading (highly recommended)
        PDFBoxResourceLoader.init(applicationContext)
        // Find the root of the external storage.
        root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        assetManager = assets

        // Need to ask for write permissions on SDK 23 and up, this is ignored on older versions
        if (ContextCompat.checkSelfPermission(this@MainActivity,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this@MainActivity,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
        }
    }




}

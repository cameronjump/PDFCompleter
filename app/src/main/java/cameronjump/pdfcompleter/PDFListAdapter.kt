package cameronjump.pdfcompleter

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Bitmap.createBitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.ContextCompat.getColor
import android.support.v7.app.AlertDialog
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.Toast
import com.mukesh.DrawingView
import com.squareup.picasso.Picasso
import com.tom_roush.pdfbox.cos.COSArray
import com.tom_roush.pdfbox.cos.COSDictionary
import com.tom_roush.pdfbox.cos.COSName
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDField
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDSignatureField
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDTextField
import com.tom_roush.pdfbox.util.PDFBoxResourceLoader
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.draw_layout.view.*
import kotlinx.android.synthetic.main.image_holder.view.*
import kotlinx.android.synthetic.main.signature_field.view.*
import kotlinx.android.synthetic.main.submit_field.view.*
import kotlinx.android.synthetic.main.text_field.view.*
import kotlinx.android.synthetic.main.toggle_field.view.*
import java.io.File
import java.io.FileOutputStream

class PDFListAdapter(private val fileName: String, private val context: Context) : BaseAdapter() {

    private val TAG = "PDFListAdapter"

    private val viewItems = mutableListOf<ViewItem>()

    private val textKeyValues = HashMap<String, String>()
    private val buttonKeyValues = HashMap<String, Boolean>()
    private val sigKeyValues = HashMap<String, String>()

    private val root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
    private val assetManager = context.assets
    private val renderPath = root.absolutePath +"/render.jpg"
    private val documentPath = root.absolutePath + "/filled-"+fileName

    init {
        // Enable Android-style asset loading (highly recommended)
        PDFBoxResourceLoader.init(context.applicationContext)
        // Find the root of the external storage.

        // Need to ask for write permissions on SDK 23 and up, this is ignored on older versions
        if (ContextCompat.checkSelfPermission(context,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(context as PDFCompleteActivity,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
        }

        //Setup document
        val document = PDDocument.load(assetManager.open(fileName))

        val docCatalog = document.documentCatalog
        val acroForm = docCatalog.acroForm

        document.save(File(documentPath))

        setRenderBitmap()

        //Add PDF preview
        val preview = PDFListAdapter.ViewItem(name="PDF Preview",
                type=PDFListAdapter.ViewType.PDFPREVIEW)
        viewItems.add(preview)

        // Add PDF forms
        for (field in acroForm.fields) {
            viewItems.add(parseViewItem(field))
        }
        Log.d(TAG, viewItems.toString())

        // Add Submit button
        val submit = PDFListAdapter.ViewItem(name="Submit",
                type=PDFListAdapter.ViewType.SUBMITBUTTON)
        viewItems.add(submit)

        document.close()
    }

    override fun getCount(): Int {
        return viewItems.size
    }

    override fun getItem(position: Int): ViewItem {
        return viewItems[position]
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val field = viewItems[position]
        var view = when(field.type.type) {
            ViewType.PDFPREVIEW.type ->
                LayoutInflater.from(context).inflate(R.layout.image_holder, parent, false)
            ViewType.Tx.type ->
                LayoutInflater.from(context).inflate(R.layout.text_field, parent, false)
            ViewType.Btn.type ->
                LayoutInflater.from(context).inflate(R.layout.toggle_field, parent, false)
            ViewType.Sig.type ->
                LayoutInflater.from(context).inflate(R.layout.signature_field, parent, false)
            ViewType.SUBMITBUTTON.type ->
                LayoutInflater.from(context).inflate(R.layout.submit_field, parent, false)
            else ->
                LayoutInflater.from(context).inflate(R.layout.text_field, parent, false)
        }

        when(field.type.type) {
            ViewType.PDFPREVIEW.type -> Picasso.get().load(File(renderPath)).into(view.pdf_view)
            ViewType.Tx.type -> {
                view.title.text = field.name
                if(textKeyValues.containsKey(field.name)) {
                    view.text_input.text.append(textKeyValues[field.name])
                }
                view.text_input.setOnFocusChangeListener { v, hasFocus ->
                    (context as PDFCompleteActivity).list_view.setSelection(position)
                }
                view.text_input.addTextChangedListener(object: TextWatcher {
                    override fun afterTextChanged(s: Editable?) {
                        textKeyValues[field.name] = view.text_input.text.toString()
                    }
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                })}
            ViewType.Btn.type -> {
                view.switch_input.text = field.name
                if(buttonKeyValues.containsKey(field.name)) {
                    view.switch_input.setChecked(buttonKeyValues[field.name] as Boolean)
                }
                view.switch_input.setOnCheckedChangeListener(object: CompoundButton.OnCheckedChangeListener {
                    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
                        buttonKeyValues[field.name] = isChecked
                    }
                })}
            ViewType.Sig.type -> {
                view.signature_title.text = field.name
                if(sigKeyValues.containsKey(field.name)) {
                    view.sign_button.text = "Signature Received"
                    view.sign_button.setBackgroundColor(getColor(context, R.color.colorPrimary))
                }
                view.sign_button.setOnClickListener {
                    val builder = AlertDialog.Builder(context)
                    builder.setTitle("Sign")
                    val drawingView = LayoutInflater.from(context).inflate(R.layout.draw_layout, parent, false)
                    builder.setView(drawingView)
                    builder.setNeutralButton("Cancel",null)
                    builder.setNegativeButton("Reset", null)
                    builder.setPositiveButton("Sign") { dialog, which ->
                        drawingView.drawing_view.saveImage(root.absolutePath, "signature-"+field.name, Bitmap.CompressFormat.PNG, 100)
                        sigKeyValues[field.name] = root.absolutePath + "/signature-"+field.name + ".png"

                    }
                    val dialog: AlertDialog = builder.create()

                    // Display the alert dialog on app interface
                    dialog.show()

                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
                        drawingView.drawing_view.clear()
                    }
                }
            }
            ViewType.SUBMITBUTTON.type ->
                view.button.setOnClickListener {
                    submit()
                }
        }

        return view
    }

    private fun submit() {
        for (i in IntRange(0, count-1)) {
            val item = getItem(i)
            when(item.type.type) {
                ViewType.Tx.type ->
                    if (!textKeyValues.contains(item.name) || textKeyValues[item.name].isNullOrBlank()) {
                        Toast.makeText(context, item.name +" field must be completed", Toast.LENGTH_SHORT).show()
                        return
                    }
                ViewType.Btn.type -> null
                ViewType.Sig.type ->
                    if (!sigKeyValues.contains(item.name) || sigKeyValues[item.name].isNullOrBlank()) {
                        Toast.makeText(context, item.name +" field must be completed", Toast.LENGTH_SHORT).show()
                        return
                    }
            }
        }

        //Setup document
        val document = PDDocument.load(assetManager.open(fileName))
        val docCatalog = document.documentCatalog
        val acroForm = docCatalog.acroForm

        for (fieldName in textKeyValues.keys) {
            val field = acroForm.getField(fieldName) as PDTextField
            field.value = textKeyValues[fieldName]
        }

        val contentStream = PDPageContentStream(document, document.getPage(0), true, false)

        for (fieldName in sigKeyValues.keys) {
            val field = acroForm.getField(fieldName) as PDSignatureField
            val rectangle = getFieldArea(field)
            val inputStream = File(sigKeyValues[fieldName]).inputStream()
            val alphaImage = BitmapFactory.decodeStream(inputStream)
            val scaledImage = Bitmap.createScaledBitmap(alphaImage, 120, 40, false)
            Log.d(TAG, rectangle.width.toString() +" "+rectangle.height.toString())
            inputStream.close()
            val pdImage = LosslessFactory.createFromImage(document, scaledImage)
            contentStream.drawImage(pdImage, rectangle.lowerLeftX, rectangle.lowerLeftY)
        }

        contentStream.close()
        document.save(documentPath)
        document.close()

        Log.d(TAG, "Wrote " + documentPath)
        //signatureDocument.close()

        //setRenderBitmap()

        (context as PDFCompleteActivity).completePDF(documentPath)
        //getView(0, null, null).pdf_view.setImageBitmap(bitmap)
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    private fun parseViewItem(field: PDField): PDFListAdapter.ViewItem {
        return when (PDFListAdapter.ViewType.valueOf(field.fieldType)) {
            ViewType.Sig -> {
                val rectangle = getFieldArea(field)
                PDFListAdapter.ViewItem(name=field.fullyQualifiedName,
                        type=PDFListAdapter.ViewType.Sig, rectangle=rectangle)
            }
            else -> PDFListAdapter.ViewItem(name=field.fullyQualifiedName,
                    type=PDFListAdapter.ViewType.valueOf(field.fieldType))
        }

    }

    private fun setRenderBitmap() {
        val fileDescriptor = ParcelFileDescriptor.open(File(documentPath), ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(fileDescriptor)
        val page = renderer.openPage(0)

        val bitmap = createBitmap(page.width, page.height, Bitmap.Config.ARGB_4444)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

        val file = File(renderPath)
        val fOut = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
        fOut.flush()
        fOut.close()
        page.close()
        renderer.close()
    }

    private fun getFieldArea(field: PDField) : PDRectangle {
      val fieldDict = field.cosObject
      val fieldAreaArray = fieldDict.getDictionaryObject(COSName.RECT) as COSArray
      return PDRectangle(fieldAreaArray)
    }

    interface PDFCompleteInterface {

        fun completePDF(path: String)
    }

    data class ViewItem(val name:String, val type:ViewType, var value: String = "", var rectangle: PDRectangle = PDRectangle())

    enum class ViewType(val type: Int) {
        PDFPREVIEW(0),
        Tx(1),
        Btn(2),
        Sig(3),
        SUBMITBUTTON(4)
    }
}
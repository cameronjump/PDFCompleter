package cameronjump.pdfcompleter

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity: Activity() {

    private val TAG = "ActivityMainDebug"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        file_select_button.setOnClickListener {
            val intent = Intent()
                    .setType("*/*")
                    .setAction(Intent.ACTION_GET_CONTENT)

            startActivityForResult(Intent.createChooser(intent, "Select a file"), 111)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 111 && resultCode == RESULT_OK) {
            if(data?.data != null) {
                val selectedFile = data?.data as Uri //The uri with the location of the file
                Log.d(TAG, selectedFile.path)
                //TODO Fix file select
                val intent = Intent(this, PDFCompleteActivity::class.java)
                intent.putExtra("fileName", "2-Qualified-Scientist.pdf")
                startActivity(intent)

            }
        }
    }
}
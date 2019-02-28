package cameronjump.pdfcompleter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.image_holder.view.*
import kotlinx.android.synthetic.main.text_field.view.*
import java.io.File


class PDFCompleteAdapter(private val list: List<ViewItem>, private val context: Context) : RecyclerView.Adapter<PDFCompleteAdapter.PDFViewHolder>() {

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: PDFViewHolder, position: Int) {
        val field = list[position]

        if (field.type == ViewType.IMAGE) {
            val file = File(field.url)
            Picasso.get().load(file).into(holder.itemView.pdf_image_view)
        }
        else if (field.type == ViewType.Tx) {
            holder.itemView.title.text = field.name
        }
        else {
            holder.itemView.title.text = field.name
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PDFViewHolder {
        if(viewType == ViewType.IMAGE.type) {
            return PDFViewHolder(LayoutInflater.from(context).inflate(R.layout.image_holder, parent, false))
        }
        else if (viewType == ViewType.Tx.type) {
            return PDFViewHolder(LayoutInflater.from(context).inflate(R.layout.text_field, parent, false))
        }
        else {
            return PDFViewHolder(LayoutInflater.from(context).inflate(R.layout.text_field, parent, false))
        }
    }

    override fun getItemViewType(position: Int): Int {
        return list[position].type.type
    }

    class PDFViewHolder (view: View) : RecyclerView.ViewHolder(view)

    data class ViewItem(val name:String, val type:ViewType, var url: String = "")

    enum class ViewType(val type: Int) {
        IMAGE(0),
        Tx(1),
        Btn(2),
        SIGNATURE(3)
    }
}
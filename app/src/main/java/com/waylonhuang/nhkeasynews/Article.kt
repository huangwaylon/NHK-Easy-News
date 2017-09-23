package com.waylonhuang.nhkeasynews

import android.support.v4.app.Fragment
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.viewholders.FlexibleViewHolder

/**
 * Created by Waylon on 9/22/2017.
 */
class Article(val time: String, val id: String, val title: String, val url: String, val mp3: String, val image: String, private val fragment: Fragment) : AbstractFlexibleItem<Article.MyViewHolder>() {

    override fun toString(): String {
        return this.id + ": " + this.title
    }

    override fun equals(other: Any?): Boolean {
        if (other is Article) {
            return this.url == other.url
        }
        return false
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun getLayoutRes(): Int {
        return R.layout.layout_item_article
    }

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<*>): MyViewHolder {
        return MyViewHolder(view, adapter)
    }

    override fun bindViewHolder(adapter: FlexibleAdapter<*>, holder: MyViewHolder, position: Int, payloads: List<*>) {
        holder.article_title.text = title
        holder.article_date.text = time

        GlideApp.with(fragment)
                .load(image)
                .fitCenter()
                .into(holder.article_img)
    }

    class MyViewHolder(view: View, adapter: FlexibleAdapter<*>) : FlexibleViewHolder(view, adapter) {
        var article_img: ImageView
        var article_title: TextView
        var article_date: TextView

        init {
            article_img = view.findViewById<ImageView>(R.id.article_img)
            article_title = view.findViewById<TextView>(R.id.article_title)
            article_date = view.findViewById<TextView>(R.id.article_date)
        }
    }
}

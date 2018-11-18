package com.waylonhuang.nhkeasynews

import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.JsonReader
import android.util.JsonToken
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import eu.davidea.fastscroller.FastScroller
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.common.FlexibleItemDecoration
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class ArticlesFragment : Fragment() {
    private var mListener: OnFragmentInteractionListener? = null
    private val adapter: ArticleFragmentFlexibleAdapter = ArticleFragmentFlexibleAdapter(ArrayList(), null)
    private var swipeRefreshLayout: SwipeRefreshLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        (activity as AppCompatActivity).supportActionBar!!.title = "NHK Easy News"

        // Inflate the layout for this fragment
        val view = inflater!!.inflate(R.layout.fragment_articles, container, false)
        val recyclerView = view.findViewById<RecyclerView>(R.id.rv_article)
        val fastScroller = view.findViewById<FastScroller>(R.id.fs_article)

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
        recyclerView.itemAnimator = DefaultItemAnimator()
        recyclerView.addItemDecoration(FlexibleItemDecoration(context).withDefaultDivider())

        if (savedInstanceState != null) {
            // rv.setScrollY(savedInstanceState.getInt(SCROLL_POSITION));
        }

        adapter.setFastScroller(fastScroller)
        adapter.setLongPressDragEnabled(true)
                .setDisplayHeadersAtStartUp(true)
                .setStickyHeaders(true)
        adapter.addListener(FlexibleAdapter.OnItemClickListener { position ->
            mListener!!.onArticleSelected(adapter.getItem(position)!!.id)
            true
        })

        swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.sr_article)
        swipeRefreshLayout!!.setOnRefreshListener {
            val asyncTask = ArticleAsyncTask()
            asyncTask.execute()
        }

        return view
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val asyncTask = ArticleAsyncTask()
        asyncTask.execute()
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            mListener = context
        } else {
            throw RuntimeException(context!!.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    interface OnFragmentInteractionListener {
        fun onArticleSelected(articleId: String)
    }

    @Throws(IOException::class)
    fun readJsonStream(inputStream: InputStream?): List<Article> {
        val reader = JsonReader(InputStreamReader(inputStream!!, "UTF-8"))
        try {
            return readArticles(reader)
        } finally {
            reader.close()
        }
    }

    @Throws(IOException::class)
    fun readArticles(reader: JsonReader): List<Article> {
        val articles = ArrayList<Article>()

        reader.beginArray()
        reader.beginObject()

        while (reader.hasNext()) {
            reader.nextName()
            reader.beginArray()

            while (reader.hasNext()) {
                articles.add(readArticle(reader))
            }

            reader.endArray()
        }
        reader.endObject()
        reader.endArray()
        return articles
    }

    @Throws(IOException::class)
    fun readArticle(reader: JsonReader): Article {
        var time: String? = null
        var id: String? = null
        var title: String? = null
        var mp3: String? = null
        var image: String? = null
        var url: String? = null

        val inDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val outDateFormat = SimpleDateFormat("M月 d日 h:mm a", Locale.getDefault())

        reader.beginObject()
        while (reader.hasNext()) {
            val name = reader.nextName()
            if (name == "news_prearranged_time") {
                val dateStr = reader.nextString()
                try {
                    time = outDateFormat.format(inDateFormat.parse(dateStr))
                } catch (e: ParseException) {
                    e.printStackTrace()
                }

            } else if (name == "news_id" && reader.peek() != JsonToken.NULL) {
                id = reader.nextString()
                url = "http://www3.nhk.or.jp/news/easy/$id/$id.html"
                mp3 = "http://www3.nhk.or.jp/news/easy/$id/$id.mp3"
            } else if (name == "title") {
                title = reader.nextString()
            } else if (name == "news_web_image_uri") {
                image = reader.nextString()
                if (image!!.isEmpty() || image[0] != 'h') {
                    println(image)
                    image = "http://www3.nhk.or.jp/news/easy/$id/$id.jpg"
                    println(image)
                }
            } else {
                reader.skipValue()
            }
        }
        reader.endObject()
        return Article(time!!, id!!, title!!, url!!, mp3!!, image!!, this)
    }

    private inner class ArticleAsyncTask : AsyncTask<String, String, List<Article>>() {
        override fun onPreExecute() {
            super.onPreExecute()
        }

        override fun doInBackground(vararg params: String): List<Article>? {
            var input: InputStream? = null
            var connection: HttpURLConnection? = null
            try {
                val url = URL("http://www3.nhk.or.jp/news/easy/news-list.json")
                connection = url.openConnection() as HttpURLConnection
                connection.connect()

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    Log.e("ArticlesFragment", "Server returned HTTP " + connection.responseCode + " " + connection.responseMessage)
                    return null
                }

                input = connection.inputStream
                return readJsonStream(input)
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            } finally {
                try {
                    if (input != null) {
                        input.close()
                    }
                } catch (ignored: IOException) {
                    ignored.printStackTrace()
                }

                if (connection != null) {
                    connection.disconnect()
                }
            }
        }

        override fun onPostExecute(articles: List<Article>?) {
            if (articles == null) {
                Toast.makeText(activity, "Couldn't update news, check network connection", Toast.LENGTH_SHORT).show()
                adapter.updateDataSet(ArrayList<Article>())
            } else {
                Toast.makeText(activity, "News updated", Toast.LENGTH_SHORT).show()
                adapter.updateDataSet(articles)
            }
            swipeRefreshLayout!!.isRefreshing = false
        }
    }


    private inner class ArticleFragmentFlexibleAdapter(items: List<Article>, listeners: Any?) : FlexibleAdapter<Article>(items, listeners, true)
    companion object {
        fun newInstance(): ArticlesFragment {
            val fragment = ArticlesFragment()
            val args = Bundle()
            fragment.arguments = args
            return fragment
        }
    }
}
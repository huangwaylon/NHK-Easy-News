package com.waylonhuang.nhkeasynews

import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.*
import android.widget.TextView
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.ui.SimpleExoPlayerView
import org.jsoup.Jsoup
import org.jsoup.helper.StringUtil
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.jsoup.select.NodeTraversor
import org.jsoup.select.NodeVisitor
import java.io.IOException
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.util.Util
import android.content.Intent
import android.support.v7.app.AlertDialog
import android.widget.SeekBar


class ArticleDetailFragment : Fragment() {
    private var furiganaView: FuriganaView? = null
    private var textView: TextView? = null

    private var playerView: SimpleExoPlayerView? = null
    private var player: SimpleExoPlayer? = null

    private var playbackPosition: Long = 0
    private var currentWindow: Int = 0
    private var playWhenReady = true

    private var articleText: String = ""

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater!!.inflate(R.layout.fragment_article_detail, container, false)

        // Needed to inflate fragment's own options menu.
        setHasOptionsMenu(true)

        furiganaView = view.findViewById<FuriganaView>(R.id.furigana_view)
        textView = view.findViewById<TextView>(R.id.text_view)

        playerView = view.findViewById<SimpleExoPlayerView>(R.id.video_view);

        return view
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ArticleDetailAsyncTask().execute(arguments.getString("ID"))
    }

    private fun initializePlayer(mp3Url: String) {
        player = ExoPlayerFactory.newSimpleInstance(
                DefaultRenderersFactory(activity),
                DefaultTrackSelector(),
                DefaultLoadControl())

        playerView!!.setPlayer(player)
        // Non positive value so that the controls are shown indefinitely.
        playerView!!.setControllerShowTimeoutMs(-1);

        player!!.setPlayWhenReady(playWhenReady)
        player!!.seekTo(currentWindow, playbackPosition)

        val uri = Uri.parse(mp3Url)
        val mediaSource = buildMediaSource(uri)
        player!!.prepare(mediaSource, true, false)
    }

    private fun releasePlayer() {
        if (player != null) {
            playbackPosition = player!!.getCurrentPosition();
            currentWindow = player!!.getCurrentWindowIndex();
            playWhenReady = player!!.getPlayWhenReady();
            player!!.release();
            player = null;
        }
    }

    private fun buildMediaSource(uri: Uri): MediaSource {
        return ExtractorMediaSource(uri,
                DefaultHttpDataSourceFactory("ua"),
                DefaultExtractorsFactory(), null, null)
    }

    private inner class ArticleDetailAsyncTask : AsyncTask<String, String, String>() {
        override fun doInBackground(vararg params: String): String {
            try {
                val url = "http://www3.nhk.or.jp/news/easy/${params[0]}/${params[0]}.html"
                val doc = Jsoup.connect(url).get()
                val newsTitle = doc.select("div#newstitle").first()

                var result = ""
                for (e in newsTitle.children()) {
                    result += text(e)
                }

                val newsArticle = doc.select("div#newsarticle").first()
                for (e in newsArticle.children()) {
                    result += text(e)
                }
                return result
            } catch (e: IOException) {
                e.printStackTrace()
            }

            return ""
        }

        override fun onPostExecute(result: String) {
            articleText = result
            updateFuriganaView()
        }
    }

    private fun updateFuriganaView() {
        val tp = textView!!.paint
        val mark_s = 0 // highlight 厚い in text (characters 11-13)
        val mark_e = 0
        furiganaView!!.text_set(tp, articleText, mark_s, mark_e)
    }

    fun text(element: Element): String {
        val accum = StringBuilder()
        NodeTraversor(object : NodeVisitor {
            override fun head(node: Node, depth: Int) {
                if (node is TextNode) {
                    appendNormalisedText(accum, node)

                    if (node.parentNode() is Element) {
                        if ((node.parentNode() as Element).tagName().equals("rt")) {
                            accum.append("}")
                        }
                    }

                } else if (node is Element) {
                    if (node.tagName().equals("ruby")) {
                        accum.append("{")
                    } else if (node.tagName().equals("rt")) {
                        accum.append(";")
                    }

                    if (accum.length > 0 && (node.isBlock() || node.tagName().equals("br")) && !lastCharIsWhitespace(accum))
                        accum.append(" ")
                }
            }

            override fun tail(node: Node, depth: Int) {}
        }).traverse(element)
        return accum.toString().trim { it <= ' ' }
    }

    companion object {
        fun newInstance(url: String): ArticleDetailFragment {
            val fragment = ArticleDetailFragment()
            val args = Bundle()
            args.putString("ID", url)
            fragment.arguments = args
            return fragment
        }

        private fun appendNormalisedText(accum: StringBuilder, textNode: TextNode) {
            val text = textNode.getWholeText()

            if (preserveWhitespace(textNode.parentNode()))
                accum.append(text)
            else
                StringUtil.appendNormalisedWhitespace(accum, text, lastCharIsWhitespace(accum))
        }

        internal fun lastCharIsWhitespace(sb: StringBuilder): Boolean {
            return sb.length != 0 && sb[sb.length - 1] == ' '
        }

        internal fun preserveWhitespace(node: Node?): Boolean {
            // looks only at this element and one level up, to prevent recursion & needless stack searches
            if (node != null && node is Element) {
                val element = node as Element?
                return element!!.tag().preserveWhitespace() || element!!.parent() != null && element!!.parent().tag().preserveWhitespace()
            }
            return false
        }
    }

    override fun onStart() {
        super.onStart()
        if (Util.SDK_INT > 23) {
            val id = arguments.getString("ID")
            val mp3Url = "http://www3.nhk.or.jp/news/easy/$id/$id.mp3"
            initializePlayer(mp3Url)
        }
    }

    override fun onResume() {
        super.onResume()
        if ((Util.SDK_INT <= 23 || player == null)) {
            val id = arguments.getString("ID")
            val mp3Url = "http://www3.nhk.or.jp/news/easy/$id/$id.mp3"
            initializePlayer(mp3Url)
        }
    }

    override fun onPause() {
        super.onPause()
        if (Util.SDK_INT <= 23) {
            releasePlayer()
        }
    }

    override fun onStop() {
        super.onStop()
        if (Util.SDK_INT > 23) {
            releasePlayer()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        menu!!.clear()
        activity.menuInflater.inflate(R.menu.menu_detail, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_font -> {
                showDialog(1,
                        56,
                        textView!!.textSize.toInt() - 16,
                        "Select Font Size",
                        "${textView!!.textSize}",
                        { progress: Int ->
                            val fontVal = progress + 16.0f
                            textView!!.textSize = fontVal
                            updateFuriganaView()

                            // Return the font value.
                            "$fontVal"
                        })
            }
            R.id.action_speed -> {
                showDialog(1,
                        29,
                        player!!.playbackParameters.speed.toInt() * 10 - 1,
                        "Select Audio Playback Speed",
                        "${player!!.playbackParameters.speed}x",
                        { progress: Int ->
                            val speedVal = (progress + 1) / 10.0f
                            player!!.playbackParameters = PlaybackParameters(speedVal, 1.0f)

                            // Return the font value.
                            "${speedVal}x"
                        })
            }
            R.id.action_website -> {
                val articleId = arguments.getString("ID")
                val articleUrl = "http://www3.nhk.or.jp/news/easy/$articleId/$articleId.html"
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(articleUrl)
                startActivity(intent)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showDialog(incrementVal: Int, maxVal: Int, progressVal: Int, titleStr: String, initVal: String, routine: (Int) -> String) {
        val builder = AlertDialog.Builder(activity)

        val dialogView = layoutInflater.inflate(R.layout.alert_dialog_fragment_detail, null);
        builder.setView(dialogView);

        val dialogTextView = dialogView.findViewById<TextView>(R.id.alert_dialog_tv)
        dialogTextView.text = initVal

        val seekBar = dialogView.findViewById<SeekBar>(R.id.alert_dialog_sb)
        seekBar.keyProgressIncrement = incrementVal
        seekBar.max = maxVal
        seekBar.progress = progressVal
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar1: SeekBar?, progress: Int, fromUser: Boolean) {
                val result = routine(progress)

                // Set the text of the text view associated with the seek bar.
                dialogTextView.text = result
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
            }
        })

        builder.setTitle(titleStr)
        builder.setPositiveButton("OK", null)
        builder.show()
    }
}

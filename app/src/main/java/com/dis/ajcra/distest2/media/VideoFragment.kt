package com.dis.ajcra.distest2.media

import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferType
import com.dis.ajcra.distest2.R
import com.dis.ajcra.distest2.login.CognitoManager
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.SimpleExoPlayerView
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import java.io.File
import java.net.URI


class VideoFragment : Fragment() {
    private var key: String? = null
    private lateinit var cfm: CloudFileManager
    private lateinit var videoView: SimpleExoPlayerView
    private lateinit var player: ExoPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            key = arguments.getString(ARG_KEY)
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.fragment_video, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        var bandwidthMeter = DefaultBandwidthMeter()
        var trackSelectionFactory = AdaptiveTrackSelection.Factory(bandwidthMeter)
        var trackSelector = DefaultTrackSelector(trackSelectionFactory)
        this.videoView = view!!.findViewById(R.id.video_vidview)
        this.videoView.player = ExoPlayerFactory.newSimpleInstance(this.context, trackSelector)
        this.player = videoView.player
        var cognitoManager = CognitoManager.GetInstance(this.context.applicationContext)
        cfm = CloudFileManager(cognitoManager, this.context.applicationContext)
        if (key != null) {
            download()
        }
    }

    fun linkToUpload(): Boolean {
        var observer = cfm.getObservers(TransferType.UPLOAD)[key.toString()]
        if (observer != null) {
            if (observer.addListener(object: CloudFileListener() {
                override fun onError(id: Int, ex: Exception?) {

                }

                override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {

                }

                override fun onStateChanged(id: Int, state: TransferState?) {

                }

                override fun onComplete(id: Int, file: File) {

                }
            })) {
                var uri = observer.file.toURI()
                playURI(uri)
                return true
            }
        }
        return false
    }

    fun playURI(uri: URI) {
        Log.d("STATE", "Playing video: " + uri.toString())
        var bandwidthMeter = DefaultBandwidthMeter()
        var dataSourceFactory = DefaultDataSourceFactory(this@VideoFragment.context,
                Util.getUserAgent(this@VideoFragment.context, "DisTest2"), bandwidthMeter)

        var androidUri = Uri.parse(uri.toString())
        var videoSource = ExtractorMediaSource.Factory(dataSourceFactory).createMediaSource(androidUri)
        player.prepare(videoSource)
    }

    fun download() {
        async(UI) {
            var getURIJob = async {
                cfm.genPresignedURI(key!!.toString(), 60)
            }
            var uri = getURIJob.await()
            playURI(uri)
        }
    }

    companion object {
        private val ARG_KEY = "key"

        fun newInstance(key: String): VideoFragment {
            val fragment = VideoFragment()
            val args = Bundle()
            args.putString(ARG_KEY, key)
            fragment.arguments = args
            return fragment
        }
    }
}// Required empty public constructor

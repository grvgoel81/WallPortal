package tk.zedlabs.wallportal.ui.fragment

import android.app.WallpaperManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tk.zedlabs.wallportal.R
import tk.zedlabs.wallportal.persistence.BookmarkImage
import tk.zedlabs.wallportal.ui.util.LoadImage
import tk.zedlabs.wallportal.util.getUriForId
import tk.zedlabs.wallportal.util.shortToast
import tk.zedlabs.wallportal.viewmodel.BookmarkViewModel

@ExperimentalMaterialApi
@AndroidEntryPoint
class DetailFragment : Fragment() {

    val bookMarkViewModel: BookmarkViewModel by viewModels()
    private val args: DetailFragmentArgs by navArgs()
    lateinit var tb: Toolbar

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // --this can be removed when fragment is a full composable
        tb = requireActivity().findViewById(R.id.toolbar)
        tb.visibility = View.GONE
    }

    //val t = produceState(initialValue = , producer = )
    override fun onDestroy() {
        super.onDestroy()
        tb.visibility = View.VISIBLE
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        bookMarkViewModel.checkBookmark(args.listItem.imageName)
        return ComposeView(requireContext()).apply {
            setContent {
                DetailsContent(Modifier)
            }
        }
    }

    @Composable
    fun DetailsContent(modifier: Modifier) {
        BottomSheetScaffold(
            sheetContent = {
                ImageInformationAndOptions()
            },
            sheetBackgroundColor = colorResource(R.color.pastelPrimary),
            sheetElevation = 20.dp,
            sheetShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        ) {
            //image goes here
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(Color.Gray)
            ) {
                LoadImage(url = args.listItem.imageUrlFull!!)
            }
        }
    }

    @Composable
    fun ImageInformationAndOptions() {
        val isBookmark by bookMarkViewModel.isBookmark.observeAsState()
        val details by bookMarkViewModel.imageDetails.observeAsState()

        //options icons row --downloads --setWallpaper --bookmark --externalLink
        Column(
            modifier = Modifier.padding(20.dp, 10.dp, 20.dp, 20.dp)
        ) {
            Divider(
                modifier = Modifier
                    .width(80.dp)
                    .height(5.dp)
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(3.dp))
                    .alpha(0.3f),
                color = Color.LightGray
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 30.dp, end = 30.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Icon(
                    imageVector = Icons.Outlined.Download,
                    contentDescription = "",
                    tint = Color.White,
                    modifier = Modifier
                        .size(50.dp)
                        .clickable { download(args.listItem) },
                )
                Spacer(modifier = Modifier.width(10.dp))
                Icon(
                    imageVector = Icons.Outlined.Panorama,
                    contentDescription = "",
                    tint = Color.White,
                    modifier = Modifier
                        .size(50.dp)
                        .clickable {
                            setWallpaper(
                                args.listItem,
                                requireContext().getUriForId(args.listItem.imageName)
                            )
                        },
                )
                Spacer(modifier = Modifier.width(10.dp))
                Icon(
                    imageVector = Icons.Outlined.BookmarkAdd,
                    contentDescription = "",
                    tint = Color.White,
                    modifier = Modifier
                        .size(50.dp)
                        .clickable {
                            if (!isBookmark!!) {
                                bookMarkViewModel.setBookmark(args.listItem)
                                Toast
                                    .makeText(requireContext(), "Added!", Toast.LENGTH_SHORT)
                                    .show()
                            } else {
                                //todo: already bookmarked, prompt to remove bookmark
                            }
                        },
                )
                Spacer(modifier = Modifier.width(10.dp))
                Icon(
                    imageVector = Icons.Outlined.OpenInNew,
                    contentDescription = "",
                    tint = Color.White,
                    modifier = Modifier
                        .size(50.dp)
                        .clickable {
                            /* todo open wallhaven link in browser */
                            navigateOriginalRes(args.listItem)
                        },
                )
                Spacer(modifier = Modifier.width(10.dp))
            }
            // --uploader --resolution --views --category
            Spacer(modifier = Modifier.height(20.dp))
            Text(text = details?.uploader?.username ?: "", color = Color.White)
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = details?.resolution ?: "", color = Color.White)
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = (details?.views ?: "").toString(), color = Color.White)
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = details?.category ?: "", color = Color.White)
        }
    }

    private fun setWallpaper(item: BookmarkImage, uri: Uri) {

        Glide.with(this)
            .asBitmap()
            .load(item.imageUrlFull)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap>?
                ) {
                    CoroutineScope(Dispatchers.IO).launch {
                        bookMarkViewModel.downloadImage(resource, item.imageName)
                        withContext(Dispatchers.Main) {
                            startWallpaperIntent(uri)
                        }
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {}
            })
    }

    private fun startWallpaperIntent(uri: Uri) {
        try {
            val wallpaperIntent = WallpaperManager
                .getInstance(requireContext())
                .getCropAndSetWallpaperIntent(uri)
                .setDataAndType(uri, "image/*")
                .putExtra("mimeType", "image/*")

            startActivity(wallpaperIntent)
            //startActivityForResult(wallpaperIntent, 13451)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun download(item: BookmarkImage) {
        Glide.with(this)
            .asBitmap()
            .load(item.imageUrlFull)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap>?
                ) {
                    bookMarkViewModel.downloadImage(resource, item.imageName)
                    requireContext().shortToast("Download Started")
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    requireContext().shortToast("Downloaded!")
                }
            })
    }

    private fun navigateOriginalRes(item: BookmarkImage) {
        findNavController().navigate(
            DetailFragmentDirections.actionDetailsToOriginalRes(
                item.imageUrlFull ?: ""
            )
        )
    }

}

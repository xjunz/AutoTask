package top.xjunz.tasker.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.FragmentAboutBinding
import top.xjunz.tasker.util.Icons

/**
 * @author xjunz 2021/9/2
 */
class AboutFragment : DialogFragment() {

    private lateinit var binding: FragmentAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.Base_Dialog)
        isCancelable = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentAboutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        runCatching {
            binding.ivIcon.setImageBitmap(Icons.myIcon)
        }.onFailure {
            binding.ivIcon.setImageResource(R.mipmap.ic_launcher)
        }
    }

}
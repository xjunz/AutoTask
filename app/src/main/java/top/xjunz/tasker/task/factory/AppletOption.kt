package top.xjunz.tasker.task.factory

import android.annotation.SuppressLint
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import top.xjunz.shared.ktx.unsafeCast
import top.xjunz.tasker.app
import top.xjunz.tasker.engine.flow.Applet
import top.xjunz.tasker.ktx.text
import top.xjunz.tasker.ui.ColorSchemes
import top.xjunz.tasker.util.unsupportedOperation

/**
 * The entity describing an applet's information. You can call [createApplet] to create an [Applet]
 * as per the option.
 *
 * @author xjunz 2022/09/22
 */
abstract class AppletOption(
    val appletId: Int, private val titleRes: Int, private val invertedTitleRes: Int
) : Comparable<AppletOption> {

    companion object {
        /**
         * Indicate that the inverted title of an option is auto-generated.
         *
         * @see AppletOption.invertedTitle
         */
        const val TITLE_AUTO_INVERTED = 0

        const val TITLE_NONE = -1

        private val DEFAULT_DESCRIBER: (Any) -> CharSequence = {
            it.toString()
        }
    }

    private var describer: (Any) -> CharSequence = DEFAULT_DESCRIBER

    /**
     * Whether this is an valid option able to yield an applet.
     */
    val isValid = appletId != -1

    /**
     * The id of the factory where the option is manufactured.
     */
    var factoryId: Int = -1

    /**
     * The category id identifying the option's category and its position in the category.
     *
     * @see AppletFactory
     */
    var categoryId: Int = -1

    /**
     * As per [Applet.isInverted].
     */
    var isInverted = false

    /**
     * As per [Applet.isInvertible].
     */
    val isInvertible get() = invertedTitleRes != TITLE_NONE

    /**
     * The index in all categories.
     */
    val categoryIndex: Int get() = categoryId ushr 8

    val title: CharSequence?
        get() = if (titleRes == TITLE_NONE) null else makeSpannedLabel(titleRes.text)

    private val invertedTitleResource: Int by lazy {
        @SuppressLint("DiscouragedApi")
        when (invertedTitleRes) {
            TITLE_AUTO_INVERTED -> {
                val invertedResName = "not_" + app.resources.getResourceEntryName(titleRes)
                val id = app.resources.getIdentifier(invertedResName, "string", app.packageName)
                check(id != 0) { "Resource id 'R.string.$invertedResName' not found!" }
                id
            }
            TITLE_NONE -> TITLE_NONE
            else -> invertedTitleRes
        }
    }

    val invertedTitle: CharSequence?
        get() = if (invertedTitleResource == TITLE_NONE) null else makeSpannedLabel(
            invertedTitleResource.text
        )

    val currentTitle get() = if (isInverted) invertedTitle else title

    private fun makeSpannedLabel(label: CharSequence): CharSequence {
        val index = label.indexOf('(')
        if (index > -1) {
            return SpannableStringBuilder().append(label.substring(0, index)).append(
                label.substring(index),
                ForegroundColorSpan(ColorSchemes.textColorDisabled),
                SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return label
    }

    fun getDescription(value: Any): CharSequence {
        return describer(value)
    }

    fun toggleInverted() {
        isInverted = !isInverted
    }

    fun createApplet(): Applet {
        return rawCreateApplet().also {
            it.id = factoryId shl 16 or appletId
            it.isInverted = isInverted
            it.relation = Applet.RELATION_AND
        }
    }

    fun <T : Any> withDescriber(block: (T) -> CharSequence): AppletOption {
        describer = { block(it.unsafeCast()) }
        return this
    }

    protected abstract fun rawCreateApplet(): Applet

    override fun compareTo(other: AppletOption): Int {
        check(factoryId == other.factoryId) {
            "Only applets with the same factory id are comparable!"
        }
        check(categoryId > -1)
        return categoryId.compareTo(other.categoryId)
    }
}

fun AppletCategoryOption(label: Int): AppletOption {
    return AppletOption(-1, label, AppletOption.TITLE_NONE) {
        unsupportedOperation()
    }
}

fun AppletOption(
    appletId: Int,
    title: Int,
    invertedTitle: Int = AppletOption.TITLE_AUTO_INVERTED,
    creator: () -> Applet
): AppletOption {
    return object : AppletOption(appletId, title, invertedTitle) {
        override fun rawCreateApplet(): Applet {
            return creator.invoke()
        }
    }
}

fun NotInvertibleAppletOption(appletId: Int, title: Int, creator: () -> Applet): AppletOption {
    return object : AppletOption(appletId, title, TITLE_NONE) {
        override fun rawCreateApplet(): Applet {
            return creator.invoke()
        }
    }
}
package top.xjunz.tasker.engine.runtime

/**
 * @author xjunz 2022/10/18
 */
class ComponentInfo {

    lateinit var pkgName: String

    var actName: String? = null

    var paneTitle: String? = null

    fun copyFrom(another: ComponentInfo) {
        paneTitle = another.paneTitle
        pkgName = another.pkgName
        actName = another.actName
    }

    override fun toString(): String {
        return "ComponentInfo(paneTitle=$paneTitle, pkgName=$pkgName, actName=$actName)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ComponentInfo

        if (paneTitle != other.paneTitle) return false
        if (pkgName != other.pkgName) return false
        if (actName != other.actName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = paneTitle?.hashCode() ?: 0
        result = 31 * result + pkgName.hashCode()
        result = 31 * result + (actName?.hashCode() ?: 0)
        return result
    }


}
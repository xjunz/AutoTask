package top.xjunz.tasker.task.inspector

/**
 * @author xjunz 2022/10/18
 */
data class ComponentInfo(
    var actLabel: String? = null,
    var pkgName: String? = null,
    var actName: String? = null
) {

    fun copyFrom(another: ComponentInfo) {
        actLabel = another.actLabel
        pkgName = another.pkgName
        actName = another.actName
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ComponentInfo) return false

        if (actLabel != other.actLabel) return false
        if (pkgName != other.pkgName) return false
        if (actName != other.actName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = actLabel?.hashCode() ?: 0
        result = 31 * result + pkgName.hashCode()
        result = 31 * result + actName.hashCode()
        return result
    }

    override fun toString(): String {
        return "ComponentInfo(actLabel=$actLabel, pkgName=$pkgName, actName=$actName)"
    }


}
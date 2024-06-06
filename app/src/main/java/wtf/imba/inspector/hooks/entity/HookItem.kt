package wtf.imba.inspector.hooks.entity

data class HookItem(
    val id: Int? = null,
    val className: String,
    val method: String,
    val constructor: Boolean,
    val state: Boolean
)

data class HookList(
    val hookList: List<HookItem>? = null
)

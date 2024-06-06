package wtf.imba.inspector.hooks.entity

data class FingerprintItem(
   val type: String,
    val name: String,
    val value: String,
    val newValue: String,
    val enable: Boolean
)

data class FingerprintList(
    val fingerprintItems: List<FingerprintItem>?
)

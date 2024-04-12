package data.templates

/**
 * @author legendsayantan
 * Represents a template of the whole prescription, with all its individual sections.
 */
data class PrescriptionTemplate(
    val name:String,
    val contents : List<ContentTemplate>
)

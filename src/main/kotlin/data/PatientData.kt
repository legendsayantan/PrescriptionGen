package data

import data.templates.ContentTemplate
import org.legendsayantan.data.BasicData

/**
 * @author legendsayantan
 * Represents a patient's data, with all its individual sections.
 */
data class PatientData(
    val basics : BasicData,
    val lastVisit: Long,
    val contents : List<ContentTemplate>
)
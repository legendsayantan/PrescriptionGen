package data.fields

import data.fields.Field

/**
 * @author legendsayantan
 * This class represents an individual text display field on the layout.
 */
data class TextField(
    override var text:String
): Field(text)
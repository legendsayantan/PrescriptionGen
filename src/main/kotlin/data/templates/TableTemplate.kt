package data.templates

import data.fields.Field
import data.fields.InputField

/**
 * @author legendsayantan
 * Represents a table of text-based content on the prescription.
 */
data class TableTemplate(
    override val name: String,
    val content: List<List<Field>>
) : ContentTemplate(name){
    fun fillWith(values:List<List<String>>):TableTemplate{
        val data = this.content
        val rowOffset = data.indexOfFirst { row -> row.find { it is InputField }!=null }
        values.forEachIndexed { rowIndex, strings ->
            val columnOffset = data[rowIndex+rowOffset].indexOfFirst { it is InputField }
            strings.forEachIndexed { columnIndex, string ->
                data[rowIndex+rowOffset][columnIndex+columnOffset].text = string
            }
        }
        return TableTemplate(name,data)
    }
}
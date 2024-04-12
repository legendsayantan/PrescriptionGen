package data.templates

import data.fields.InputField

/**
 * @author legendsayantan
 * Represents a text-input based section of content on the prescription.
 */
data class TextTemplate(
    override val name:String,
    val content : LinkedHashMap<String, InputField>
): ContentTemplate(name){
    fun fillWith(values:List<String>):TextTemplate{
        val data = this.content
        values.forEachIndexed { index, s ->
            data[data.keys.elementAt(index)]?.let { it.text = s }
        }
        return TextTemplate(name,data)
    }
}

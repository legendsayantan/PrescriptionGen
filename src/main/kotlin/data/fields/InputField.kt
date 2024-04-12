package data.fields

/**
 * @author legendsayantan
 * Represents a field that can take input from a list of options, or customised input.
 */
data class InputField(
    val options:List<String>,
    override var text:String = ""
): Field(text)
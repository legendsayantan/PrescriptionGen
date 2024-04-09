package org.legendsayantan.data

data class PdfData(
    val name:String,
    val id:Long,
    val sex:Sex,
    val dob:Long,
    val lastVisit: Long,
    val info : LinkedHashMap<String,String>,
    val tables: LinkedHashMap<String,Table>,
    val footer: LinkedHashMap<String,String>
){
    enum class Sex{
        M,F,O
    }
}
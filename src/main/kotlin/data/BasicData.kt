package org.legendsayantan.data

import data.PatientData

data class BasicData(
    val name:String,
    val id:Long,
    val sex: Sex,
    val dob:Long
){
    enum class Sex{
        M,F,O
    }
}


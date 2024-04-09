package org.legendsayantan

import org.legendsayantan.Generator.PdfPrescription
import org.legendsayantan.data.PdfData
import org.legendsayantan.data.Table

fun main() {
    val tableData = linkedMapOf<String, Table>()
    tableData["Other History :"] = Table(
        listOf(
            arrayOf("Eye", "Right Eye", "Left Eye"),
            arrayOf("Vision with/without glasses", "6/6", "6/6"),
            arrayOf("Vision with PH", "6/6", "6/6"),
            arrayOf("Lid & Adnexa", "6/6", "6/6"),
            arrayOf("Anterior Segment", "6/6", "6/6"),
            arrayOf("Posterior Segment", "6/6", "6/6"),
            arrayOf("IOP", "6/6", "6/6"),
            arrayOf("TBUT/Patency", "6/6", "6/6"),
        )
    )
    tableData["Spectacle Correction :"] = Table(
        listOf(
            arrayOf("Eye", "Spherical", "Cylindrical", "Axis", "Vision"),
            arrayOf("OD", "6/6", "6/6", "6/6", "6/6"),
            arrayOf("OS", "6/6", "6/6", "6/6", "6/6"),
        )
    )
    tableData["_1"] = Table(
        listOf(
            arrayOf("ADD", "ADD info here"),
            arrayOf("Suggestions", "Take care of eyes"),
        )
    )

    val data = PdfData(
        "Sayantan Paul",
        1,
        PdfData.Sex.M,
        1082946600000,
        System.currentTimeMillis(),
        linkedMapOf(
            "C/o: " to "Headache",
            "Ocular History: " to "Issues with eyes and ears and nose and throat",
            "Systemic History: " to "Issues with everything else in the head region and also the neck region and also the shoulder region"
        ),
        tableData,
        linkedMapOf("Advice: " to "Wear glasses everyday.")
    )
    PdfPrescription("collegelogo-full.png") { err ->
        println(err)
    }.createWith(data) {
        it.save("output.pdf")
    }
}
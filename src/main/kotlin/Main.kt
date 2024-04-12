package org.legendsayantan

import data.fields.InputField
import data.fields.TextField
import data.PatientData
import data.templates.TableTemplate
import data.templates.TextTemplate
import generator.PdfPrescription
import org.legendsayantan.data.BasicData
import org.legendsayantan.data.templates.ImageData

fun main() {
    //pdf format declaration
    val newInput : ()->InputField = { InputField(options = listOf("1", "2", "3", "4", "5", "6")) }
    val topText = TextTemplate(
        name = "top",
        content = linkedMapOf(
            "C/o" to InputField(listOf("lack of vision")),
            "Ocular History" to InputField(listOf("lack of vision")),
            "Systemic History" to InputField(listOf("heart disease", "headache")),
        )
    )
    val topTable = TableTemplate(
        name = "Other History",
        content = listOf(
            listOf(TextField("Eye"), TextField("Right Eye"), TextField("Left Eye")),
            listOf(TextField("Vision with/without glasses"), newInput(), newInput()),
            listOf(TextField("Vision with PH"), newInput(), newInput()),
            listOf(TextField("Lid & Adnexa"), newInput(), newInput()),
            listOf(TextField("Anterior Segment"), newInput(), newInput()),
            listOf(TextField("Posterior Segment"), newInput(), newInput()),
            listOf(TextField("IOP"), newInput(), newInput()),
            listOf(TextField("TBUT/Patency"), newInput(), newInput()),
        )
    )
    val midTable = TableTemplate(
        name = "Spectacle Correction",
        content = listOf(
            listOf(
                TextField("Eye"),
                TextField("Spherical"),
                TextField("Cylindrical"),
                TextField("Axis"),
                TextField("Vision")
            ),
            listOf(TextField("OD"), newInput(), newInput(), newInput(), newInput()),
            listOf(TextField("OS"), newInput(), newInput(), newInput(), newInput()),
            listOf(TextField("ADD"), newInput()),
            listOf(TextField("Suggestion"), newInput())
        )
    )
    val bottomText = TextTemplate(
        name = "bottom",
        content = linkedMapOf(
            "Advice" to InputField(listOf())
        )
    )

    //put data
    val topData = topText.fillWith(
        listOf(
            "Dimness of Vision for distance and near",
            "Spectacle wearing since 6 years",
            "Thyroid-5 years"
        )
    )
    val table1 = topTable.fillWith(
        listOf(
            listOf("6", "6"),
            listOf("6", "6"),
            listOf("6", "6"),
            listOf("6", "6"),
            listOf("6", "6"),
            listOf("6", "6"),
            listOf("6", "6"),
        )
    )
    val table2 = midTable.fillWith(
        listOf(
            listOf("1", "1", "2", "3"),
            listOf("5", "3", "2", "1"),
            listOf("0.5"),
            listOf("Take care of eyes")
        )
    )
    val footer = bottomText.fillWith(
        listOf(
            "Wear glasses everyday, and visit the doctor regularly. Do not forget to take your medicines on time."
        )
    )
    val images = ImageData(
        linkedMapOf(
            "Right Eye" to "13487_1",
            "Left Eye" to "13487_2"
        )
    )
    val data = PatientData(
        BasicData(
            "Sayantan Paul",
            13487,
            BasicData.Sex.M,
            1082946600000,
        ),
        System.currentTimeMillis(),
        listOf(topData, table1, table2, footer, images)
    )
    PdfPrescription("collegelogo-full.png") {
        println(it)
    }.apply {
        marginVertical = 50f
    }.createWith(data) {
        it.save("output.pdf")
    }
}
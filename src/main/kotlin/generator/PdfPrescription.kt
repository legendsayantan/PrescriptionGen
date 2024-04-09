package org.legendsayantan.Generator

import be.quodlibet.boxable.BaseTable
import be.quodlibet.boxable.Cell
import be.quodlibet.boxable.Row
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.legendsayantan.data.PdfData
import java.awt.Color
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.imageio.ImageIO


class PdfGen(val imagePath: String, val pdfData: PdfData, val onError: (String) -> Unit) {
    public var marginHorizontal = 75f
    public var marginVertical = 50f
    fun create(then: (PDDocument) -> Unit) {
        val document = PDDocument()
        try {
            // Create a new page and add it to the document
            val page = PDPage(PDRectangle.A4)
            document.addPage(page)

            // Start a content stream which will "hold" the to be created content
            PDPageContentStream(document, page).use { contentStream ->
                var offset: Float
                offset = addHeaderImage(document, contentStream, marginVertical)
                offset = addInformation(contentStream, offset)
                inflateHashMapTable(document, arrayListOf("Eye","Right Eye","Left Eye"),pdfData.otherHistory,offset)
            }
        } finally {
            then(document)
            document.close()
        }
    }

    private fun addHeaderImage(document: PDDocument, contentStream: PDPageContentStream, topOffset: Float): Float {
        val image = ImageIO.read(File(imagePath))
        val scaling = (0.7f).coerceAtMost(595.28f/image.width.toFloat())
        if (image != null) {
            val xOffset = ((595.28 - image.width * scaling) / 2).toFloat()
            val yOffset = 841.89f - topOffset - image.height * scaling
            contentStream.drawImage(
                PDImageXObject.createFromFile(imagePath, document),
                xOffset,
                yOffset,
                image.width * scaling,
                image.height * scaling
            )
            return yOffset
        } else {
            onError("Failed to process header image.")
            return -1f
        }
    }

    private fun addInformation(contentStream: PDPageContentStream, topOffset: Float): Float {
        var currentHeight = topOffset - 25f
        var h = 0f

        h = contentStream.putTextAt("Name: ", pdfData.name, marginHorizontal, currentHeight, 200f)

        contentStream.putTextAt(
            "Age/Sex: ", "${
                Calendar.getInstance().apply {
                    timeInMillis = (System.currentTimeMillis() - pdfData.dob)
                }.get(Calendar.YEAR) - 1970
            }/${pdfData.sex}", 325f, currentHeight
        )

        contentStream.putTextAt("Date: ", SimpleDateFormat("dd/MM/yyyy").format(pdfData.lastVisit), 425f, currentHeight)

        //next line
        currentHeight -= h + paragraphSpacing
        h = contentStream.putTextAt("C/o: ", pdfData.c_o, marginHorizontal, currentHeight)

        //next line
        currentHeight -= h + paragraphSpacing
        h = contentStream.putTextAt("Ocular History: ", pdfData.ocularHistory, marginHorizontal, currentHeight)

        //next line
        currentHeight -= h + paragraphSpacing
        h = contentStream.putTextAt("Systemic History: ", pdfData.systemicHistory, marginHorizontal, currentHeight)

        //next line
        currentHeight -= h + paragraphSpacing
        h = contentStream.putTextAt("Other History: ", "", marginHorizontal, currentHeight)

        return currentHeight-h
    }
    private fun inflateHashMapTable(document: PDDocument,colums:ArrayList<String>,hashMap: HashMap<String,IntArray>,yOffset:Float){
        var currentHeight = yOffset-25f
        val table = BaseTable(
            currentHeight, 0f, 0f, 500f, 0f, document,document.getPage(document.pages.count-1), true,
            true
        )

        //Create Header row
        val headerRow: Row<PDPage> = table.createRow(lineHeight)
        currentHeight -= lineHeight
        colums.forEach {
            headerRow.createCell(16.6667f, it).apply {
                font = PDType1Font.HELVETICA_BOLD
            }
        }

        table.addHeaderRow(headerRow)
    }

    companion object {
        var fontSize = 12f
        var lineHeight = 15f
        var paragraphSpacing = 10f
        
        fun PDPageContentStream.putTextAt(
            bold: String,
            normal: String,
            x: Float,
            y: Float,
            maxLength: Float = 450f
        ): Float {
            beginText()
            newLineAtOffset(x, y)
            showBold(bold)
            val x = showNormal(
                normal, maxLength,
                PDType1Font.HELVETICA.getStringWidth(bold) / 1000 * fontSize
            )
            endText()
            return x
        }

        private fun PDPageContentStream.showBold(text: String) {
            setFont(PDType1Font.HELVETICA_BOLD, fontSize)
            showText(text)
        }

        private fun PDPageContentStream.showNormal(text: String, maxWidth: Float, firstLineCompensation: Float): Float {
            var height = lineHeight
            setFont(PDType1Font.HELVETICA, fontSize)
            val words = text.split(" ")
            var line = StringBuilder()
            words.forEach { word ->
                val size =
                    PDType1Font.HELVETICA.getStringWidth(line.toString() + word) / 1000 * fontSize + if (height == lineHeight) firstLineCompensation else 0f
                if (size > maxWidth) {
                    height += lineHeight
                    showText(line.toString())
                    newLineAtOffset(0f, -lineHeight)
                    line = StringBuilder("$word ")
                } else {
                    line.append("$word ")
                }
            }
            if (line.isNotEmpty()) showText(line.toString())
            return height
        }
    }
}
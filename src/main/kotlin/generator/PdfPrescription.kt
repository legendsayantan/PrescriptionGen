package org.legendsayantan.Generator

import be.quodlibet.boxable.BaseTable
import be.quodlibet.boxable.HorizontalAlignment
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.legendsayantan.data.PdfData
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.imageio.ImageIO
import kotlin.collections.LinkedHashMap

class PdfPrescription(val imagePath: String, val onError: (String) -> Unit) {
    var marginHorizontal = 50f
    var marginVertical = 50f
    var imageScale = 1f
    fun createWith(pdfData: PdfData, then: (PDDocument) -> Unit) {
        val document = PDDocument()
        try {
            // Create a new page and add it to the document
            val page = PDPage(PDRectangle.A4)
            document.addPage(page)

            // Start a content stream which will "hold" the to be created content
            PDPageContentStream(document, page).use { contentStream ->
                var offset: Float
                offset = addHeaderImage(document, contentStream, marginVertical)
                offset = addName(contentStream, offset, pdfData)
                offset = inflateHashMap(contentStream, offset, pdfData.info)
                pdfData.tables.forEach {
                    offset = inflateTable(document, contentStream, it.key, it.value.data, offset)
                }
                offset = inflateHashMap(contentStream, offset, pdfData.footer)
            }
        } finally {
            then(document)
            document.close()
        }
    }

    private fun addHeaderImage(document: PDDocument, contentStream: PDPageContentStream, topOffset: Float): Float {
        val image = ImageIO.read(File(imagePath))
        val scaling = (imageScale).coerceAtMost((pageWidth - (marginHorizontal * 2)) / image.width.toFloat())
        if (image != null) {
            val xOffset = ((pageWidth - image.width * scaling) / 2).toFloat()
            val yOffset = pageHeight - topOffset - image.height * scaling
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

    private fun addName(contentStream: PDPageContentStream, topOffset: Float, pdfData: PdfData): Float {
        val currentHeight = topOffset - 25f

        val h: Float = contentStream.putTextAt(
            "Name: ",
            pdfData.name,
            marginHorizontal,
            currentHeight,
            pageWidth - (marginHorizontal * 2) - 200
        )

        contentStream.putTextAt(
            "Age/Sex: ", "${
                Calendar.getInstance().apply {
                    timeInMillis = (System.currentTimeMillis() - pdfData.dob)
                }.get(Calendar.YEAR) - 1970
            }/${pdfData.sex}", pageWidth - (marginHorizontal) - 200, currentHeight, maxLength = 100f
        )

        contentStream.putTextAt(
            "Date: ",
            SimpleDateFormat("dd/MM/yyyy").format(pdfData.lastVisit),
            pageWidth - (marginHorizontal) - 100,
            currentHeight, maxLength = 100f
        )

        return currentHeight - h
    }

    private fun inflateHashMap(
        contentStream: PDPageContentStream,
        topOffset: Float,
        hashMap: LinkedHashMap<String, String>
    ): Float {
        var currentHeight = topOffset - paragraphSpacing
        hashMap.forEach { (t, u) ->
            currentHeight -= contentStream.putTextAt(
                t,
                u,
                marginHorizontal,
                currentHeight,
                maxLength = pageWidth - (marginHorizontal * 2)
            ) + paragraphSpacing
        }
        return currentHeight
    }

    private fun inflateTable(
        document: PDDocument, contentStream: PDPageContentStream, name: String,
        data: List<Array<String>>,
        yOffset: Float
    ): Float {
        var currentHeight = yOffset
        val page = document.pages[document.pages.count - 1]
        if (name.isNotEmpty() && !name.startsWith("_")) {
            currentHeight -= contentStream.putTextAt(name, "", marginHorizontal, currentHeight) - 10f
        } else {
            currentHeight += 21
        }
        val tableWidth = page.mediaBox.width - (marginHorizontal * 2)
        val table = BaseTable(
            currentHeight,
            0f,
            0f,
            pageWidth - 150,
            marginHorizontal,
            document,
            page,
            true,
            true
        )

        val evenCellWidth = tableWidth / (data.first().size * 4.5f)
        val exactCellWidth = (data.maxOf { stringWidth(it[0]) } / 3.5f).coerceAtLeast(tableWidth/4.5f - 100f)

        data.forEach { array ->
            val row = table.createRow(lineHeight)
            array.forEachIndexed { index, s ->
                row.createCell(
                    if (data.first().size <= 2)
                        if (index == 0) exactCellWidth
                        else (tableWidth / 4.5f - exactCellWidth)
                    else evenCellWidth, s
                ).apply {
                    font = PdfPrescription.font
                    fontSize = PdfPrescription.fontSize
                    align = HorizontalAlignment.CENTER
                }
            }
            currentHeight -= row.cells[0].cellHeight
        }
        table.draw()
        return currentHeight - 20f
    }


    companion object {
        var font = PDType1Font.HELVETICA
        var fontBold = PDType1Font.HELVETICA_BOLD
        var fontSize = 12f
        var lineHeight = 15f
        var paragraphSpacing = 5f
        const val pageHeight = 841.89f
        const val pageWidth = 595.28f
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
            val r = showNormal(
                normal, maxLength,
                font.getStringWidth(bold) / 1000 * fontSize
            )
            endText()
            return r
        }

        private fun PDPageContentStream.showBold(text: String) {
            setFont(fontBold, fontSize)
            showText(text)
        }

        private fun PDPageContentStream.showNormal(text: String, maxWidth: Float, firstLineCompensation: Float): Float {
            var height = lineHeight
            setFont(font, fontSize)
            val words = text.split(" ")
            var line = StringBuilder()
            words.forEach { word ->
                val size =
                    font.getStringWidth(line.toString() + word) / 1000 * fontSize + if (height == lineHeight) firstLineCompensation else 0f
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

        private fun stringWidth(str: String): Float {
            return font.getStringWidth(str) / 1000 * fontSize
        }
    }
}
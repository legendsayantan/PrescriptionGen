package generator

import be.quodlibet.boxable.BaseTable
import be.quodlibet.boxable.HorizontalAlignment
import com.google.gson.Gson
import com.google.zxing.BarcodeFormat
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.qrcode.QRCodeWriter
import data.PatientData
import data.fields.Field
import data.fields.InputField
import data.fields.TextField
import data.templates.TableTemplate
import data.templates.TextTemplate
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.legendsayantan.data.BasicData
import org.legendsayantan.data.templates.ImageData
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.util.*
import javax.imageio.ImageIO
import kotlin.math.min

class PdfPrescription(val imagePath: String, val onError: (String) -> Unit) {
    var marginHorizontal = 50f
    var marginVertical = 50f
    var imageScale = 1f
    var qrCachePath = "qr.png"
    var qrSize = 100
    fun createWith(pdfData: PatientData, then: (PDDocument) -> Unit) {
        val document = PDDocument()
        try {
            // Create a new page and add it to the document
            val page = PDPage(PDRectangle.A4)
            document.addPage(page)

            // Start a content stream which will "hold" the to be created content
            PDPageContentStream(document, page).use { contentStream ->
                var offset: Float
                offset = addHeaderImage(document, contentStream, marginVertical, pdfData.basics)
                offset = addNames(contentStream, offset, pdfData.basics, pdfData.lastVisit)
                pdfData.contents.forEach {
                    when (it) {
                        is TextTemplate -> {
                            offset = inflateHashMap(contentStream, offset, it.content)
                        }

                        is TableTemplate -> {
                            offset = inflateTable(document, contentStream, it.name, it.content, offset)
                        }

                        is ImageData -> {
                            offset = inflateImages(document, contentStream, it.name, it.imageIds, offset)
                        }
                    }
                }
            }
        } finally {
            then(document)
            document.close()
        }
    }

    private fun createQrCode(data: BasicData, size: Int) {
        val qrCodeWriter = QRCodeWriter()
        val bitMatrix = qrCodeWriter.encode(Gson().toJson(data), BarcodeFormat.QR_CODE, size * 5, size * 5)
        val path: Path = FileSystems.getDefault().getPath(qrCachePath)
        MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path)
    }

    private fun addHeaderImage(
        document: PDDocument,
        contentStream: PDPageContentStream,
        topOffset: Float,
        data: BasicData
    ): Float {
        val image = ImageIO.read(File(imagePath))
        val scaling = (imageScale).coerceAtMost(
            (pageWidth - qrSize - (marginHorizontal * 2)) / image.width.toFloat()
        )
        val qrSize = image.height * scaling * 1.2f
        createQrCode(data, image.height * scaling.toInt())
        if (image != null) {
            val xOffset = marginHorizontal
            val yOffset = pageHeight - topOffset - image.height * scaling
            contentStream.drawImage(
                PDImageXObject.createFromFile(imagePath, document),
                xOffset,
                yOffset,
                image.width * scaling,
                image.height * scaling
            )
            contentStream.drawImage(
                PDImageXObject.createFromFile(qrCachePath, document),
                pageWidth - marginHorizontal - qrSize,
                yOffset - 5f,
                qrSize,
                qrSize
            )
            return yOffset
        } else {
            onError("Failed to process header image.")
            return -1f
        }
    }

    private fun addNames(contentStream: PDPageContentStream, topOffset: Float, pdfData: BasicData, date: Long): Float {
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
            SimpleDateFormat("dd/MM/yyyy").format(date),
            pageWidth - (marginHorizontal) - 100,
            currentHeight, maxLength = 100f
        )

        return currentHeight - h
    }

    private fun inflateHashMap(
        contentStream: PDPageContentStream,
        topOffset: Float,
        hashMap: LinkedHashMap<String, InputField>
    ): Float {
        var currentHeight = topOffset - paragraphSpacing
        hashMap.forEach { (t, u) ->
            currentHeight -= contentStream.putTextAt(
                "$t: ",
                u.text,
                marginHorizontal,
                currentHeight,
                maxLength = pageWidth - (marginHorizontal * 2)
            ) + paragraphSpacing
        }
        return currentHeight
    }

    private fun inflateTable(
        document: PDDocument, contentStream: PDPageContentStream, name: String,
        data: List<List<Field>>,
        yOffset: Float
    ): Float {
        var currentHeight = yOffset
        val page = document.pages[document.pages.count - 1]
        if (name.isNotEmpty() && !name.startsWith("_")) {
            currentHeight -= contentStream.putTextAt("$name: ", "", marginHorizontal, currentHeight) - 10f
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
        val exactCellWidth = (data.maxOf { stringWidth(it[0].text) } / 3f).coerceAtLeast(tableWidth / 4.5f - 100f)

        data.forEach { array ->
            val row = table.createRow(lineHeight)
            array.forEachIndexed { index, s ->
                row.createCell(
                    if (array.size <= 2)
                        if (index == 0) exactCellWidth
                        else (tableWidth / 4.5f - exactCellWidth)
                    else evenCellWidth, s.text
                ).apply {
                    font = if (s is TextField) Companion.fontBold else Companion.font
                    fontSize = Companion.fontSize
                    align = HorizontalAlignment.CENTER
                }
            }
            currentHeight -= row.cells.maxOf { it.cellHeight }
        }
        table.draw()
        return currentHeight - 20f
    }

    private fun inflateImages(
        document: PDDocument, contentStream: PDPageContentStream, name: String,
        data: LinkedHashMap<String, String>,
        yOffset: Float
    ): Float {
        var currentHeight = yOffset
        if (name.isNotEmpty() && !name.startsWith("_")) {
            currentHeight -= contentStream.putTextAt("$name: ", "", marginHorizontal, currentHeight) - 10f
        }
        val divider = marginHorizontal / 2
        val paths = data.map { it.key to ImageProvider.getImagePathFromID(it.value) }
        val images = paths.map { it.first to ImageIO.read(File(it.second)) }
        val totalImageWidth = paths.sumOf { ImageIO.read(File(it.second)).width } + divider * (paths.size - 1)
        val scale = min(
            (currentHeight - marginVertical) / images.maxOf { it.second.height }.toFloat(),
            (pageWidth - (marginHorizontal * 2)) / totalImageWidth
        )
        var xOffset = ((pageWidth - (totalImageWidth * scale)) / 2).coerceAtLeast(marginHorizontal)
        val y = currentHeight - images.maxOf { it.second.height } * scale
        paths.forEachIndexed { index, module ->
            val img = images[index].second
            contentStream.putTextAt(
                "",
                module.first,
                xOffset + (img.width * scale - stringWidth(module.first)) / 2,
                yOffset,
                maxLength = 100f
            )
            contentStream.drawImage(
                PDImageXObject.createFromFile(module.second, document),
                xOffset,
                y,
                img.width * scale,
                img.height * scale,
            )
            xOffset += (img.width * scale) + divider
        }
        return y
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
package org.legendsayantan.data.templates

import data.templates.ContentTemplate

data class ImageData(
    val imageIds : LinkedHashMap<String,String>,
    override val name : String = "Images"
):ContentTemplate(name)

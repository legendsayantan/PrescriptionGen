package generator

class ImageProvider {
    companion object{
        var image_location = "images/"
        fun getImagePathFromID(id:String):String{
            return "$image_location$id.jpg"
        }
    }
}
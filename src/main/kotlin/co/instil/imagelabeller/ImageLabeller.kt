/*
 * Copyright 2016 Instil Software.
 */
package co.instil.imagelabeller

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.vision.v1.Vision
import com.google.api.services.vision.v1.VisionScopes
import com.google.api.services.vision.v1.model.AnnotateImageRequest
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest
import com.google.api.services.vision.v1.model.Feature
import com.google.api.services.vision.v1.model.Image
import java.nio.file.FileSystems
import java.nio.file.Files
import kotlin.system.exitProcess

data class ImageLabel(val description: String, val score: Float)

val NO_IMAGE: ByteArray = ByteArray(0)

fun main(args: Array<String>) {
    runLabellerWith(args) { imageFilename, maxResults ->
        val imageContent = loadImage(imageFilename)
        exitIfImageWasNotLoaded(imageContent)
        labelImage(imageContent, maxResults) { labels, err ->
            if (err != null) {
                println(err)
            } else {
                labels?.forEach { println("${it.description} with score of ${it.score}") }
            }
        }
    }
}

fun labelImage(imageContent: ByteArray, maxResults: Int, processResults: (List<ImageLabel>?, String?) -> Unit){
    val annotationRequest = annotationRequestFor(imageContent, maxResults);
    val annotation = annotationFor(annotationRequest)
    val batchResponse = annotation.execute()
    val response = batchResponse.responses[0]
    if (response.error != null) {
        processResults(null, response.error.message)
    } else {
        val labelList = response.labelAnnotations.map { ImageLabel(it.description, it.score) }
        processResults(labelList, null)
    }
}

private fun runLabellerWith(args: Array<String>, labeller: (String, Int) -> Unit) {
    when(args.size) {
        1 -> labeller(args[0], 10)
        2 -> labeller(args[0], args[1].toInt())
        else -> {
            println("Please provide an image to label");
            exitProcess(0);
        }
    }
}

private fun loadImage(imageFilename: String): ByteArray {
    val imageFilepath = FileSystems.getDefault().getPath(imageFilename)
    return if (Files.exists(imageFilepath)) {
        Files.readAllBytes(imageFilepath)
    } else {
        NO_IMAGE
    }
}

private fun exitIfImageWasNotLoaded(image: ByteArray) {
    if (image == NO_IMAGE) {
        println("Could not load image")
        exitProcess(0)
    }
}

private fun annotationRequestFor(imageContent: ByteArray, maxResults: Int): AnnotateImageRequest {
    val image = Image().encodeContent(imageContent)
    val feature = Feature().setType("LABEL_DETECTION").setMaxResults(maxResults)
    return AnnotateImageRequest().setImage(image).setFeatures(listOf(feature))
}

private val vision by lazy {
    val credential = GoogleCredential.getApplicationDefault().createScoped(VisionScopes.all())
    val jsonFactory = JacksonFactory.getDefaultInstance()
    Vision.Builder(GoogleNetHttpTransport.newTrustedTransport(), jsonFactory, credential)
        .setApplicationName(System.getenv("GOOGLE_APPLICATION_NAME"))
        .build()
}

private fun annotationFor(annotationRequest: AnnotateImageRequest): Vision.Images.Annotate {
    val batchAnnotationRequest = batchAnnotationRequestFor(annotationRequest)
    return vision.images().annotate(batchAnnotationRequest)
}

private fun batchAnnotationRequestFor(annotationRequest: AnnotateImageRequest): BatchAnnotateImagesRequest {
    return BatchAnnotateImagesRequest().setRequests(listOf(annotationRequest))
}

/*
 * Copyright 2016 Instil Software.
 */
package co.instil.imagelabeller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.forms.MultiPartBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import kotlin.io.ConstantsKt;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.List;

import static co.instil.imagelabeller.ImageLabellerKt.labelImage;

public class WebImageLabeller extends Application<Configuration> {

    public static void main(String[] args) throws Exception {
        new WebImageLabeller().run(args);
    }

    @Override
    public void run(Configuration configuration, Environment environment) throws Exception {
        environment.jersey().register(new WebLabellerResource());
    }

    @Override
    public void initialize(Bootstrap<Configuration> bootstrap) {
        bootstrap.addBundle(new AssetsBundle("/public", "/", "index.html"));
        bootstrap.addBundle(new MultiPartBundle());
    }

    @Path("/")
    public static class WebLabellerResource {

        @POST
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        public void labelThisImage(
                @FormDataParam("image")InputStream imageInputStream,
                @FormDataParam("image")FormDataContentDisposition imageFileDetails,
                @QueryParam("maxResults")Integer maxResults,
                @Suspended AsyncResponse response) {

            byte[] imageContent = readImageFile(imageInputStream);
            if (null == maxResults) {
                maxResults = 10;
            }
            labelImage(imageContent, maxResults, (imageLabels, err) -> {
                if (err != null) {
                    response.resume(Response.serverError().build());
                } else {
                    String imageLabelsJson = buildJsonFor(imageLabels);
                    response.resume(Response.ok(imageLabelsJson).build());
                }
                return null;
            });
        }

        private byte[] readImageFile(InputStream imageInputStream) {
            return kotlin.io.ByteStreamsKt.readBytes(imageInputStream, ConstantsKt.DEFAULT_BUFFER_SIZE);
        }

        private String buildJsonFor(List<ImageLabel> imageLabels) {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                return objectMapper.writeValueAsString(imageLabels);
            } catch (Exception e) {
                return "[]";
            }
        }

    }

}

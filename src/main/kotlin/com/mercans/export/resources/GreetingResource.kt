package com.mercans.export.resources

import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

/**
 * Greeting resource
 *
 * @constructor Create empty Greeting resource
 */
@Path("/hello")
class GreetingResource {

    /**
     * Hello
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    fun hello(): String {
        return "Hello RESTEasy"
    }
}

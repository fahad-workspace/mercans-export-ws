package com.mercans.export.resources

import com.mercans.export.models.Request
import com.mercans.export.services.ExportService
import javax.inject.Inject
import javax.validation.constraints.NotNull
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

/**
 * Export resource
 *
 * @author Fahad Sarwar
 *
 * @property exportService
 * @constructor Create empty Export resource
 */
@Path("/export")
class ExportResource @Inject constructor(private val exportService: ExportService) {

    /**
     * Export
     * @param inputFileName
     * @param dynamicConfigurationFileName
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun export(
        @QueryParam("inputFileName")
        @NotNull
        inputFileName: String,
        @QueryParam("dynamicConfigurationFileName")
        @NotNull
        dynamicConfigurationFileName: String
    ): Request {
        return exportService.export(inputFileName, dynamicConfigurationFileName)
    }
}

package br.cepel.dpc.pldpro_auto.controller;

import br.cepel.dpc.pldpro_auto.application.SshAccessUseCase;
import br.cepel.dpc.pldpro_auto.dto.AccessHostRequest;
import br.cepel.dpc.pldpro_auto.dto.AccessHostResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/v1/ssh")
@Tag(name = "SSH", description = "Acesso remoto via SSH e execução de comandos")
public class SshController {

    private final SshAccessUseCase useCase;

    public SshController(SshAccessUseCase useCase) { this.useCase = useCase; }

    @PostMapping("/exec")
    @Operation(
            summary = "Executa um comando via SSH",
            description = """
            Primeiro acesso: envie 'bootstrap=true' + 'password' para instalar a chave pública e acelerar conexões futuras.
            Próximos acessos: omita 'bootstrap' e 'password' para autenticar por chave.""",
            // Se sua API for protegida por JWT, descomente a linha abaixo e configure o esquema 'bearerAuth' no OpenAPIConfig.
            // security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AccessHostRequest.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Primeiro acesso (instala chave)",
                                            value = """
                        {
                          "host": "10.0.0.12",
                          "port": 22,
                          "user": "pldpro",
                          "bootstrap": true,
                          "password": "senha_remota",
                          "command": "uname -a"
                        }"""
                                    ),
                                    @ExampleObject(
                                            name = "Acesso por chave (rápido)",
                                            value = """
                        {
                          "host": "10.0.0.12",
                          "port": 22,
                          "user": "pldpro",
                          "command": "ls -la"
                        }"""
                                    )
                            }
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Comando executado",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = AccessHostResponse.class),
                                    examples = {
                                            @ExampleObject(
                                                    name = "Sucesso",
                                                    value = """
                            {
                              "exitCode": 0,
                              "stdout": "Linux host 6.8.0-... x86_64 GNU/Linux\\n",
                              "stderr": ""
                            }"""
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Requisição inválida (ex.: bootstrap=true sem password)"),
                    @ApiResponse(responseCode = "401", description = "Não autorizado"),
                    @ApiResponse(responseCode = "500", description = "Erro interno durante a conexão/execução")
            }
    )
    public ResponseEntity<AccessHostResponse> exec(
            @org.springframework.web.bind.annotation.RequestBody AccessHostRequest req) throws Exception {
        return ResponseEntity.ok(useCase.execute(req));
    }
}
